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

package ch.protonmail.android.mailconversation.domain.sample

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.mailmessage.domain.entity.AttachmentCount
import ch.protonmail.android.mailmessage.domain.entity.Recipient
import ch.protonmail.android.mailmessage.domain.sample.AttachmentCountSample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import me.proton.core.domain.entity.UserId

object ConversationSample {

    val WeatherForecast = build(
        conversationId = ConversationIdSample.WeatherForecast,
        subject = "Weather Forecast"
    )

    fun build(
        attachmentCount: AttachmentCount = AttachmentCountSample.build(),
        conversationId: ConversationId = ConversationIdSample.build(),
        labels: List<ConversationLabel> = listOf(ConversationLabelSample.build()),
        recipients: List<Recipient> = listOf(RecipientSample.Doe),
        senders: List<Recipient> = listOf(RecipientSample.John),
        subject: String = "subject",
        userId: UserId = UserIdSample.Primary
    ) = Conversation(
        attachmentCount = attachmentCount,
        conversationId = conversationId,
        expirationTime = 0,
        labels = labels,
        numAttachments = 0,
        numMessages = 0,
        numUnread = 0,
        order = 0,
        recipients = recipients,
        senders = senders,
        subject = subject,
        userId = userId
    )
}
