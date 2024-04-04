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

package ch.protonmail.android.mailmessage.presentation.mapper

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmessage.presentation.R
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import okhttp3.internal.toImmutableList
import org.junit.Test
import kotlin.test.assertEquals

internal class DetailMoreActionsBottomSheetUiMapperTest {

    private val mapper = DetailMoreActionsBottomSheetUiMapper()

    @Test
    fun `should map to the correct header ui model`() {
        // Given
        val expected = DetailMoreActionsBottomSheetState.MessageDataUiModel(
            headerSubjectText = TextUiModel(ExpectedSubject),
            headerDescriptionText = TextUiModel.TextResWithArgs(
                R.string.bottom_sheet_more_header_message_from,
                listOf(ExpectedSender)
            ),
            messageId = ExpectedMessageId
        )

        // When
        val actual = mapper.toHeaderUiModel(ExpectedSender, ExpectedSubject, ExpectedMessageId)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should map the to correct ui model given just one recipient`() {
        // Given
        val expectedList = listOf(
            ActionUiModel(Action.Reply),
            ActionUiModel(Action.Forward),
            ActionUiModel(Action.ViewInLightMode),
            ActionUiModel(Action.ViewInDarkMode),
            ActionUiModel(Action.Print),
            ActionUiModel(Action.ReportPhishing)
        ).toImmutableList()

        // When
        val actual = mapper.mapMoreActionUiModels(ExpectedSender, SingleRecipientCount)

        // Then
        assertEquals(expectedList, actual)
    }

    @Test
    fun `should map the to correct ui model given multiple recipients`() {
        // Given
        val expectedList = listOf(
            ActionUiModel(
                Action.Reply,
                description = TextUiModel.TextResWithArgs(
                    R.string.action_reply_to_description,
                    listOf(ExpectedSender)
                ),
                contentDescription = TextUiModel.TextResWithArgs(
                    R.string.action_reply_to_content_description,
                    listOf(ExpectedSender)
                )
            ),
            ActionUiModel(Action.ReplyAll),
            ActionUiModel(Action.Forward),
            ActionUiModel(Action.ViewInLightMode),
            ActionUiModel(Action.ViewInDarkMode),
            ActionUiModel(Action.Print),
            ActionUiModel(Action.ReportPhishing)
        ).toImmutableList()

        // When
        val actual = mapper.mapMoreActionUiModels(ExpectedSender, PluralRecipientCount)

        // Then
        assertEquals(expectedList, actual)
    }

    private companion object {

        const val ExpectedSender = "Sender"
        const val ExpectedSubject = "A subject"
        const val ExpectedMessageId = "messageId"
        const val SingleRecipientCount = 1
        const val PluralRecipientCount = 10
    }
}
