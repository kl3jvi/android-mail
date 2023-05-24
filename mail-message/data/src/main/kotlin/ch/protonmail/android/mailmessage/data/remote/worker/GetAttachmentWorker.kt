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

package ch.protonmail.android.mailmessage.data.remote.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.domain.util.requireNotBlank
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailmessage.data.R
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.domain.entity.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.data.remote.AttachmentApi
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import timber.log.Timber

@HiltWorker
class GetAttachmentWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider,
    private val attachmentLocalDataSource: AttachmentLocalDataSource,
    private val notificationManager: NotificationManager,
    private val notificationProvider: NotificationProvider
) : CoroutineWorker(context, workerParameters) {

    private val userId = UserId(extractStringFromWorkerParams(RawUserIdKey, "User id"))
    private val messageId = MessageId(extractStringFromWorkerParams(RawMessageIdKey, "Message id"))
    private val attachmentId = AttachmentId(extractStringFromWorkerParams(RawAttachmentIdKey, "Attachment id"))

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {

        Timber.d("Start downloading attachment $attachmentId")
        setWorkerStatusToDb(AttachmentWorkerStatus.Running)

        setForegroundAsync(createForegroundInfo(attachmentId))

        Timber.d("Foreground information set")

        val result = try {
            apiProvider.get<AttachmentApi>(userId).invoke {
                getAttachment(attachmentId = attachmentId.id)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get attachment")
            setWorkerStatusToDb(AttachmentWorkerStatus.Failed)
            return Result.failure()
        }

        val responseBody = try {
            result.valueOrThrow
        } catch (e: Exception) {
            Timber.w(e, "Failed to extract attachment from response")
            setWorkerStatusToDb(AttachmentWorkerStatus.Failed)
            return Result.failure()
        }

        return when (result) {
            is ApiResult.Success -> {
                Timber.d("Attachment $attachmentId downloaded successfully")
                attachmentLocalDataSource.upsertAttachment(
                    userId = userId,
                    messageId = messageId,
                    attachmentId = attachmentId,
                    attachment = responseBody.bytes(),
                    status = AttachmentWorkerStatus.Success
                )
                Result.success()
            }
            else -> {
                Timber.d("Failed to get attachment")
                setWorkerStatusToDb(AttachmentWorkerStatus.Failed)
                Result.failure()
            }
        }
    }

    private fun createForegroundInfo(attachmentId: AttachmentId): ForegroundInfo {
        val notificationChannel = createNotificationChannel()
        notificationManager.createNotificationChannel(notificationChannel)
        return ForegroundInfo(attachmentId.id.hashCode(), createNotification(notificationChannel))
    }

    private fun createNotificationChannel(): NotificationChannel {
        val channelName = R.string.attachment_download_notification_channel_name
        val channelDescription = R.string.attachment_download_notification_channel_description
        return notificationProvider.provideNotificationChannel(
            context = context,
            channelId = NotificationProvider.ATTACHMENT_CHANNEL_ID,
            channelName = channelName,
            channelDescription = channelDescription
        )
    }

    private fun createNotification(notificationChannel: NotificationChannel): Notification {
        return notificationProvider.provideNotification(
            context = context,
            channel = notificationChannel,
            title = R.string.attachment_download_notification_title
        )
    }

    private suspend fun setWorkerStatusToDb(status: AttachmentWorkerStatus) {
        attachmentLocalDataSource.updateAttachmentDownloadStatus(userId, messageId, attachmentId, status)
    }

    private fun extractStringFromWorkerParams(key: String, fieldName: String) =
        requireNotBlank(workerParameters.inputData.getString(key), fieldName = fieldName)


    companion object {

        internal const val RawUserIdKey = "getAttachmentWorkParamUserId"
        internal const val RawMessageIdKey = "getAttachmentWorkerParamMessageId"
        internal const val RawAttachmentIdKey = "getAttachmentWorkerParamAttachmentId"

        fun params(
            userId: UserId,
            messageId: MessageId,
            attachmentId: AttachmentId
        ) = mapOf(
            RawUserIdKey to userId.id,
            RawMessageIdKey to messageId.id,
            RawAttachmentIdKey to attachmentId.id
        )
    }
}
