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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.maildetail.domain.model.DecryptedMessageBody
import ch.protonmail.android.maildetail.domain.usecase.ShouldShowRemoteContent
import ch.protonmail.android.maildetail.presentation.model.AttachmentUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageBodyAttachmentsUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageBodyUiModel
import ch.protonmail.android.maildetail.presentation.model.MimeTypeUiModel
import ch.protonmail.android.maildetail.presentation.usecase.InjectCssIntoDecryptedMessageBody
import ch.protonmail.android.maildetail.presentation.usecase.SanitizeHtmlOfDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.entity.MimeType
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MessageBodyUiModelMapper @Inject constructor(
    private val injectCssIntoDecryptedMessageBody: InjectCssIntoDecryptedMessageBody,
    private val sanitizeHtmlOfDecryptedMessageBody: SanitizeHtmlOfDecryptedMessageBody,
    private val shouldShowRemoteContent: ShouldShowRemoteContent
) {

    suspend fun toUiModel(userId: UserId, decryptedMessageBody: DecryptedMessageBody): MessageBodyUiModel {
        val sanitizedMessageBody = sanitizeHtmlOfDecryptedMessageBody(
            decryptedMessageBody.value,
            decryptedMessageBody.mimeType.toMimeTypeUiModel()
        )
        return MessageBodyUiModel(
            messageBody = injectCssIntoDecryptedMessageBody(
                sanitizedMessageBody,
                decryptedMessageBody.mimeType.toMimeTypeUiModel()
            ),
            mimeType = decryptedMessageBody.mimeType.toMimeTypeUiModel(),
            shouldShowRemoteContent = shouldShowRemoteContent(userId),
            attachments = if (decryptedMessageBody.attachments.isNotEmpty()) {
                MessageBodyAttachmentsUiModel(
                    attachments = decryptedMessageBody.attachments.map {
                        AttachmentUiModel(
                            attachmentId = it.attachmentId.id,
                            fileName = it.name.split(".").dropLast(1).joinToString("."),
                            extension = it.name.split(".").last(),
                            size = it.size,
                            mimeType = it.mimeType
                        )
                    }
                )
            } else null
        )
    }

    fun toUiModel(encryptedMessageBody: String) = MessageBodyUiModel(
        messageBody = encryptedMessageBody,
        mimeType = MimeTypeUiModel.PlainText,
        shouldShowRemoteContent = false,
        attachments = null
    )

    private fun MimeType.toMimeTypeUiModel() = when (this) {
        MimeType.PlainText -> MimeTypeUiModel.PlainText
        MimeType.Html, MimeType.MultipartMixed -> MimeTypeUiModel.Html
    }
}
