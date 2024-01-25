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

package ch.protonmail.android.mailcontact.presentation.model

import java.time.LocalDate
import android.graphics.Bitmap
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontact.presentation.R
import me.proton.core.contact.domain.entity.ContactId
import kotlin.random.Random

const val CONTACT_NAME_MAX_LENGTH = 190
// We remove one to avoid issue with space when computing the full name
const val CONTACT_FIRST_LAST_NAME_MAX_LENGTH = CONTACT_NAME_MAX_LENGTH / 2 - 1

data class ContactFormUiModel(
    val id: ContactId?,
    val avatar: ContactFormAvatar,
    val displayName: String,
    val firstName: String,
    val lastName: String,
    // Using `MutableList` so that the items content can be updated without recomposing the instantiated list items.
    val emails: MutableList<InputField.SingleTyped>,
    val telephones: MutableList<InputField.SingleTyped>,
    val addresses: MutableList<InputField.Address>,
    val birthday: InputField.Birthday?,
    val notes: MutableList<InputField.Note>,
    val others: MutableList<InputField>,
    val otherTypes: List<FieldType.OtherType>
)

sealed interface InputField {

    data class SingleTyped(
        val value: String,
        val selectedType: FieldType
    ) : InputField

    data class Address(
        val streetAddress: String,
        val postalCode: String,
        val city: String,
        val region: String,
        val country: String,
        val selectedType: FieldType
    ) : InputField

    data class ImageTyped(
        val value: Bitmap,
        val selectedType: FieldType
    ) : InputField

    data class DateTyped(
        val value: LocalDate,
        val selectedType: FieldType
    ) : InputField

    data class Birthday(
        val value: LocalDate
    ) : InputField

    data class Note(
        val value: String
    ) : InputField
}

fun getEmailTypeByValue(value: TextUiModel) =
    FieldType.EmailType.values().find { it.localizedValue == value } ?: FieldType.EmailType.Email

fun getTelephoneTypeByValue(value: TextUiModel) =
    FieldType.TelephoneType.values().find { it.localizedValue == value } ?: FieldType.TelephoneType.Telephone

fun getAddressTypeByValue(value: TextUiModel) =
    FieldType.AddressType.values().find { it.localizedValue == value } ?: FieldType.AddressType.Address

fun getOtherTypeByValue(value: TextUiModel) =
    FieldType.OtherType.values().find { it.localizedValue == value } ?: FieldType.OtherType.Role

sealed interface FieldType {
    val localizedValue: TextUiModel

    enum class EmailType(override val localizedValue: TextUiModel) : FieldType {
        Email(TextUiModel(R.string.contact_type_email)),
        Home(TextUiModel(R.string.contact_type_home)),
        Work(TextUiModel(R.string.contact_type_work)),
        Other(TextUiModel(R.string.contact_type_other))
    }

    enum class TelephoneType(override val localizedValue: TextUiModel) : FieldType {
        Telephone(TextUiModel(R.string.contact_type_phone)),
        Home(TextUiModel(R.string.contact_type_home)),
        Work(TextUiModel(R.string.contact_type_work)),
        Other(TextUiModel(R.string.contact_type_other)),
        Mobile(TextUiModel(R.string.contact_type_mobile)),
        Main(TextUiModel(R.string.contact_type_main)),
        Fax(TextUiModel(R.string.contact_type_fax)),
        Pager(TextUiModel(R.string.contact_type_pager))
    }

    enum class AddressType(override val localizedValue: TextUiModel) : FieldType {
        Address(TextUiModel(R.string.contact_type_address)),
        Home(TextUiModel(R.string.contact_type_home)),
        Work(TextUiModel(R.string.contact_type_work)),
        Other(TextUiModel(R.string.contact_type_other))
    }

    enum class OtherType(override val localizedValue: TextUiModel) : FieldType {
        Photo(TextUiModel(R.string.contact_property_photo)),
        Organization(TextUiModel(R.string.contact_property_organization)),
        Title(TextUiModel(R.string.contact_property_title)),
        Role(TextUiModel(R.string.contact_property_role)),
        TimeZone(TextUiModel(R.string.contact_property_time_zone)),
        Logo(TextUiModel(R.string.contact_property_logo)),
        Member(TextUiModel(R.string.contact_property_member)),
        Language(TextUiModel(R.string.contact_property_language)),
        Url(TextUiModel(R.string.contact_property_url)),
        Gender(TextUiModel(R.string.contact_property_gender)),
        Anniversary(TextUiModel(R.string.contact_property_anniversary))
    }
}

enum class Section {
    Emails,
    Telephones,
    Addresses,
    Notes,
    Others
}

sealed interface ContactFormAvatar {

    // Use data class with camera icon res id here once we implement image picker.
    object Empty : ContactFormAvatar

    data class Photo(
        val bitmap: Bitmap
    ) : ContactFormAvatar
}

val emptyContactFormUiModel = ContactFormUiModel(
    id = null,
    avatar = ContactFormAvatar.Empty,
    displayName = "",
    firstName = "",
    lastName = "",
    emails = mutableListOf(),
    telephones = mutableListOf(),
    addresses = mutableListOf(),
    birthday = null,
    notes = mutableListOf(),
    others = mutableListOf(),
    otherTypes = FieldType.OtherType.values().toList()
)
val emptyEmailField = InputField.SingleTyped(
    value = "",
    selectedType = FieldType.EmailType.Email
)
val emptyTelephoneField = InputField.SingleTyped(
    value = "",
    selectedType = FieldType.TelephoneType.Telephone
)
val emptyAddressField = InputField.Address(
    streetAddress = "",
    postalCode = "",
    city = "",
    region = "",
    country = "",
    selectedType = FieldType.AddressType.Address
)
val emptyNoteField = InputField.Note(
    value = ""
)

fun emptyRandomOtherField() = InputField.SingleTyped(
    value = "",
    selectedType = listOf(
        FieldType.OtherType.Organization,
        FieldType.OtherType.Title,
        FieldType.OtherType.Role,
        FieldType.OtherType.TimeZone,
        FieldType.OtherType.Member,
        FieldType.OtherType.Language,
        FieldType.OtherType.Url,
        FieldType.OtherType.Gender
    ).run {
        this[Random.nextInt(this.size)]
    }
)
