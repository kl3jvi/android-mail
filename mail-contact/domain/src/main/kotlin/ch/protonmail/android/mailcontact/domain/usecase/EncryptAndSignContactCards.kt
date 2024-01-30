/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailcontact.domain.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcontact.domain.mapper.DecryptedContactMapper
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ezvcard.property.Uid
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.contact.domain.encryptAndSignContactCard
import me.proton.core.contact.domain.entity.ContactCard
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.contact.domain.signContactCard
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.useKeys
import me.proton.core.user.domain.UserManager
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class EncryptAndSignContactCards @Inject constructor(
    private val userManager: UserManager,
    private val cryptoContext: CryptoContext,
    private val contactRepository: ContactRepository,
    private val decryptContactCards: DecryptContactCards,
    private val decryptedContactMapper: DecryptedContactMapper
) {

    suspend operator fun invoke(
        userId: UserId,
        decryptedContact: DecryptedContact
    ): Either<EncryptingContactCardsError, List<ContactCard>> = either {
        // retrieve original ContactCards
        val contactWithCards = decryptedContact.id?.let {
            contactRepository.observeContactWithCards(
                userId,
                decryptedContact.id
            ).mapToEither().firstOrNull()?.getOrNull() ?: raise(EncryptingContactCardsError.ContactNotFoundInDB)
        }

        // decrypt them and check signatures
        val cardsToDecryptedCards = contactWithCards?.contactCards?.mapNotNull { contactCard ->
            val decryptedContactCard = decryptContactCards(
                userId,
                contactWithCards.copy(
                    // pass only one ContactCard so we don't lose the relation before- and -after decryption
                    contactCards = listOf(contactCard)
                )
            ).onLeft {
                raise(EncryptingContactCardsError.DecryptingContactCardError)
            }.getOrNull()?.firstOrNull()

            decryptedContactCard?.let { contactCard to it }
        }?.filter {
            // only take the correctly signed ones
            it.second.status == VerificationStatus.Success || it.second.status == VerificationStatus.NotSigned
        }

        // find first UID from existing VCards or generate new one
        val fallbackUid = cardsToDecryptedCards?.find { it.second.card.uid != null }?.second?.card?.uid ?: Uid.random()
        // generate fallback name in an unlikely case the Signed ContactCard doesn't contain it
        //  and it's not provided in our DecryptedContact
        val fallbackName = contactWithCards?.contact?.name
            ?: decryptedContact.formattedName?.value?.takeIfNotEmpty()
            ?: decryptedContact.structuredName?.let {
                it.given.plus(" ${it.family}")
            } ?: raise(EncryptingContactCardsError.MissingFormattedName)

        val clearTextContactCard = cardsToDecryptedCards?.find {
            it.first is ContactCard.ClearText
        }?.second?.card
        val signedContactCard = cardsToDecryptedCards?.find { it.first is ContactCard.Signed }?.second?.card
        val encryptedAndSignedContactCard = cardsToDecryptedCards?.find {
            it.first is ContactCard.Encrypted
        }?.second?.card

        // insert all properties from DecryptedContact where they belong inside the ContactCards, encrypt and sign
        val encryptedAndSignedContactCards = userManager.getUser(userId).useKeys(cryptoContext) {
            listOfNotNull(
                decryptedContactMapper.mapToClearTextContactCard(
                    fallbackUid,
                    clearTextContactCard
                )?.let { ContactCard.ClearText(it.write()) },
                decryptedContactMapper.mapToSignedContactCard(
                    fallbackUid,
                    fallbackName,
                    decryptedContact,
                    signedContactCard
                ).let { signContactCard(it) },
                decryptedContactMapper.mapToEncryptedAndSignedContactCard(
                    fallbackUid,
                    decryptedContact,
                    encryptedAndSignedContactCard
                ).let { encryptAndSignContactCard(it) }
            )
        }

        return encryptedAndSignedContactCards.right()
    }

}

sealed class EncryptingContactCardsError {
    object ContactNotFoundInDB : EncryptingContactCardsError()
    object DecryptingContactCardError : EncryptingContactCardsError()
    object MissingFormattedName : EncryptingContactCardsError()
}
