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

package ch.protonmail.android.uitest.models.detail

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import ch.protonmail.android.maildetail.presentation.ui.header.MessageDetailHeaderTestTags
import ch.protonmail.android.uitest.util.child

internal class ExtendedHeaderRowEntryModel(parent: SemanticsNodeInteraction) {

    private val icon = parent.child {
        hasTestTag(MessageDetailHeaderTestTags.ExtendedHeaderIcon)
    }

    private val text = parent.child {
        hasTestTag(MessageDetailHeaderTestTags.ExtendedHeaderText)
    }

    // region verification
    fun hasIcon() = apply {
        icon.assertExists()
    }

    fun hasText(value: String) = apply {
        text.assertTextEquals(value)
    }
    // endregion
}
