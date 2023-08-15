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

package ch.protonmail.android.mailmessage.data.local

import java.io.File
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.model.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface AttachmentLocalDataSource {

    suspend fun observeAttachmentMetadata(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Flow<MessageAttachmentMetadata?>

    /**
     * Get the attachment for the given [userId], [messageId] and [attachmentId].
     */
    suspend fun getAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Local, MessageAttachmentMetadata>

    /**
     * Get the embedded image for the given [userId], [messageId] and [attachmentId].
     */
    suspend fun getEmbeddedImage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Local, File>

    /**
     * Get all running attachment metadata for the given [userId] and [messageIds].
     * @param userId The id of the attachments belonging to the user.
     * @param messageIds The ids of the affected messages
     */
    suspend fun getDownloadingAttachmentsForMessages(
        userId: UserId,
        messageIds: List<MessageId>
    ): List<MessageAttachmentMetadata>

    /**
     * Upsert the attachment for the given [userId], [messageId] and [attachmentId].
     */
    suspend fun upsertAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        encryptedAttachment: ByteArray,
        status: AttachmentWorkerStatus
    )

    suspend fun updateAttachmentDownloadStatus(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        status: AttachmentWorkerStatus
    )

    /**
     * Store the embedded image for the given [userId], [messageId] and [attachmentId].
     */
    suspend fun storeEmbeddedImage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        encryptedAttachment: ByteArray
    )

    /**
     * Delete the attachment for the given [userId] and [messageId].
     * @return true if deleting was successful, false otherwise.
     */
    suspend fun deleteAttachments(userId: UserId, messageId: MessageId): Boolean
}
