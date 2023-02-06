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

package ch.protonmail.android.maildetail.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.maildetail.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.maildetail.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.RelabelMessage
import ch.protonmail.android.maildetail.domain.usecase.StarMessage
import ch.protonmail.android.maildetail.domain.usecase.UnStarMessage
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailActionBarUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.LabelAsBottomSheetState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.model.MoveToBottomSheetState
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailReducer
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.toCustomUiModel
import ch.protonmail.android.maillabel.presentation.toUiModels
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class MessageDetailViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMessageWithLabels: ObserveMessageWithLabels,
    private val getDecryptedMessageBody: GetDecryptedMessageBody,
    private val messageDetailReducer: MessageDetailReducer,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val observeDetailActions: ObserveMessageDetailActions,
    private val observeDestinationMailLabels: ObserveExclusiveDestinationMailLabels,
    private val observeFolderColor: ObserveFolderColorSettings,
    private val observeCustomMailLabels: ObserveCustomMailLabels,
    private val markUnread: MarkMessageAsUnread,
    private val getContacts: GetContacts,
    private val starMessage: StarMessage,
    private val unStarMessage: UnStarMessage,
    private val savedStateHandle: SavedStateHandle,
    private val messageDetailHeaderUiModelMapper: MessageDetailHeaderUiModelMapper,
    private val messageBodyUiModelMapper: MessageBodyUiModelMapper,
    private val messageDetailActionBarUiModelMapper: MessageDetailActionBarUiModelMapper,
    private val moveMessage: MoveMessage,
    private val relabelMessage: RelabelMessage
) : ViewModel() {

    private val messageId = requireMessageId()
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val mutableDetailState = MutableStateFlow(initialState)

    val state: StateFlow<MessageDetailState> = mutableDetailState.asStateFlow()

    init {
        Timber.d("Open detail screen for message ID: $messageId")
        observeMessageWithLabels(messageId)
        getMessageBody(messageId)
        observeBottomBarActions(messageId)
    }

    fun submit(action: MessageViewAction) {
        when (action) {
            is MessageViewAction.Reload -> reloadMessageBody(messageId)
            is MessageViewAction.Star -> starMessage()
            is MessageViewAction.UnStar -> unStarMessage()
            is MessageViewAction.MarkUnread -> markMessageUnread()
            is MessageViewAction.Trash -> trashMessage()
            is MessageViewAction.RequestMoveToBottomSheet -> showMoveToBottomSheetAndLoadData(action)
            is MessageViewAction.DismissBottomSheet -> dismissBottomSheet(action)
            is MessageViewAction.MoveToDestinationSelected -> moveToDestinationSelected(action.mailLabelId)
            is MessageViewAction.MoveToDestinationConfirmed -> onBottomSheetDestinationConfirmed(action.mailLabelText)
            is MessageViewAction.RequestLabelAsBottomSheet -> showLabelAsBottomSheetAndLoadData(action)
            is MessageViewAction.LabelAsToggleAction -> onLabelSelected(action.labelId)
            is MessageViewAction.LabelAsConfirmed -> onLabelAsConfirmed(action.archiveSelected)
        }.exhaustive
    }

    private fun starMessage() {
        primaryUserId.mapLatest { userId ->
            starMessage(userId, messageId).fold(
                ifLeft = { MessageDetailEvent.ErrorAddingStar },
                ifRight = { MessageViewAction.Star }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun unStarMessage() {
        primaryUserId.mapLatest { userId ->
            unStarMessage(userId, messageId).fold(
                ifLeft = { MessageDetailEvent.ErrorRemovingStar },
                ifRight = { MessageViewAction.UnStar }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun markMessageUnread() {
        primaryUserId.mapLatest { userId ->
            markUnread(userId, messageId).fold(
                ifLeft = { MessageDetailEvent.ErrorMarkingUnread },
                ifRight = { MessageViewAction.MarkUnread }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun trashMessage() {
        primaryUserId.mapLatest { userId ->
            moveMessage(userId, messageId, SystemLabelId.Trash.labelId).fold(
                ifLeft = { MessageDetailEvent.ErrorMovingToTrash },
                ifRight = { MessageViewAction.Trash }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun dismissBottomSheet(action: MessageViewAction) {
        viewModelScope.launch { emitNewStateFrom(action) }
    }

    private fun moveToDestinationSelected(mailLabelId: MailLabelId) {
        viewModelScope.launch {
            emitNewStateFrom(MessageViewAction.MoveToDestinationSelected(mailLabelId))
        }
    }

    private fun onBottomSheetDestinationConfirmed(mailLabelText: String) {
        primaryUserId.mapLatest { userId ->
            val bottomSheetState = state.value.bottomSheetState?.contentState
            if (bottomSheetState is MoveToBottomSheetState.Data) {
                bottomSheetState.moveToDestinations.firstOrNull { it.isSelected }?.let {
                    moveMessage(userId, messageId, it.id.labelId).fold(
                        ifLeft = { MessageDetailEvent.ErrorMovingMessage },
                        ifRight = { MessageViewAction.MoveToDestinationConfirmed(mailLabelText) }
                    )
                } ?: throw IllegalStateException("No destination selected")
            } else {
                MessageDetailEvent.ErrorMovingMessage
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun observeMessageWithLabels(messageId: MessageId) {
        primaryUserId.flatMapLatest { userId ->
            val contacts = getContacts(userId).getOrElse { emptyList() }
            return@flatMapLatest observeMessageWithLabels(userId, messageId).mapLatest { either ->
                either.fold(
                    ifLeft = { MessageDetailEvent.NoCachedMetadata },
                    ifRight = { messageWithLabels ->
                        MessageDetailEvent.MessageWithLabelsEvent(
                            messageDetailActionBarUiModelMapper.toUiModel(messageWithLabels.message),
                            messageDetailHeaderUiModelMapper.toUiModel(messageWithLabels, contacts)
                        )
                    }
                )
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun getMessageBody(messageId: MessageId) {
        viewModelScope.launch {
            val userId = primaryUserId.first()
            val event = getDecryptedMessageBody(userId, messageId).fold(
                ifLeft = { getDecryptedMessageBodyError ->
                    when (getDecryptedMessageBodyError) {
                        is GetDecryptedMessageBodyError.Decryption -> {
                            MessageDetailEvent.ErrorDecryptingMessageBody(
                                messageBody = messageBodyUiModelMapper.toUiModel(
                                    getDecryptedMessageBodyError.encryptedMessageBody
                                )
                            )
                        }
                        is GetDecryptedMessageBodyError.Data -> {
                            MessageDetailEvent.ErrorGettingMessageBody(
                                isNetworkError = getDecryptedMessageBodyError.dataError == DataError.Remote.Http(
                                    NetworkError.NoNetwork
                                )
                            )
                        }
                    }
                },
                ifRight = { MessageDetailEvent.MessageBodyEvent(messageBodyUiModelMapper.toUiModel(it)) }
            )
            emitNewStateFrom(event)
        }
    }

    private fun reloadMessageBody(messageId: MessageId) {
        viewModelScope.launch {
            emitNewStateFrom(MessageViewAction.Reload)
            getMessageBody(messageId)
        }
    }

    private fun observeBottomBarActions(messageId: MessageId) {
        primaryUserId.flatMapLatest { userId ->
            observeDetailActions(userId, messageId).mapLatest { either ->
                either.fold(
                    ifLeft = { MessageDetailEvent.MessageBottomBarEvent(BottomBarEvent.ErrorLoadingActions) },
                    ifRight = { actions ->
                        val actionUiModels = actions.map { actionUiModelMapper.toUiModel(it) }
                        MessageDetailEvent.MessageBottomBarEvent(BottomBarEvent.ActionsData(actionUiModels))
                    }
                )
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun showMoveToBottomSheetAndLoadData(initialEvent: MessageViewAction) {
        primaryUserId.flatMapLatest { userId ->
            combine(
                observeDestinationMailLabels(userId),
                observeFolderColor(userId)
            ) { folders, color ->
                MessageDetailEvent.MessageBottomSheetEvent(
                    MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                        folders.toUiModels(color).let { it.folders + it.systems }
                    )
                )
            }
        }.onStart {
            emitNewStateFrom(initialEvent)
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun showLabelAsBottomSheetAndLoadData(initialEvent: MessageViewAction) = viewModelScope.launch {
        emitNewStateFrom(initialEvent)

        val userId = primaryUserId.first()
        val labels = observeCustomMailLabels(userId).first()
        val color = observeFolderColor(userId).first()
        val message = observeMessageWithLabels(userId, messageId).first()

        val mappedLabels = labels.tapLeft {
            Timber.e("Error while observing custom labels")
        }.getOrElse { emptyList() }

        val selectedLabels = message.fold(
            ifLeft = { emptyList() },
            ifRight = { messageWithLabels ->
                messageWithLabels.labels
                    .filter { it.type == LabelType.MessageLabel }
                    .map { it.labelId }
            }
        )

        val event = MessageDetailEvent.MessageBottomSheetEvent(
            LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                customLabelList = mappedLabels.map { it.toCustomUiModel(color, emptyMap(), null) },
                selectedLabels = selectedLabels
            )
        )
        emitNewStateFrom(event)
    }

    private fun onLabelAsConfirmed(archiveSelected: Boolean) = viewModelScope.launch {
        val userId = primaryUserId.first()
        val messageWithLabels = checkNotNull(
            observeMessageWithLabels(userId, messageId).first().orNull()
        ) { "Message not found" }

        val previousSelectedLabels = messageWithLabels.labels
            .filter { it.type == LabelType.MessageLabel }
            .map { it.labelId }

        val labelAsData =
            mutableDetailState.value.bottomSheetState?.contentState as? LabelAsBottomSheetState.Data
                ?: throw IllegalStateException("BottomSheetState is not LabelAsBottomSheetState.Data")

        val newSelectedLabels = labelAsData.labelUiModelsWithSelectedState
            .filter { it.selectedState == LabelSelectedState.Selected }
            .map { it.labelUiModel.id.labelId }

        if (archiveSelected) {
            moveMessage(
                userId,
                messageId,
                SystemLabelId.Archive.labelId
            ).tapLeft { Timber.e("Move message failed: $it") }
        }

        val operation =
            relabelMessage(
                userId = userId,
                messageId = messageId,
                currentLabelIds = previousSelectedLabels,
                updatedLabelIds = newSelectedLabels
            ).fold(
                ifLeft = {
                    Timber.e("Relabel message failed: $it")
                    MessageDetailEvent.ErrorLabelingMessage
                },
                ifRight = { MessageViewAction.LabelAsConfirmed(archiveSelected) }
            )
        emitNewStateFrom(operation)
    }

    private fun onLabelSelected(labelId: LabelId) {
        viewModelScope.launch { emitNewStateFrom(MessageViewAction.LabelAsToggleAction(labelId)) }
    }

    private suspend fun emitNewStateFrom(operation: MessageDetailOperation) {
        val updatedState = messageDetailReducer.newStateFrom(state.value, operation)
        mutableDetailState.emit(updatedState)
    }

    private fun requireMessageId(): MessageId {
        val messageIdParam = savedStateHandle.get<String>(MessageDetailScreen.MESSAGE_ID_KEY)
            ?: throw IllegalStateException("No Message id given")

        return MessageId(messageIdParam)
    }

    companion object {

        val initialState = MessageDetailState.Loading
    }
}
