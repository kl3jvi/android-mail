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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import java.util.Collections
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.toCustomUiModel
import ch.protonmail.android.maillabel.presentation.toUiModels
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.model.toMailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.GetMailboxActions
import ch.protonmail.android.mailmailbox.domain.usecase.MarkConversationsAsRead
import ch.protonmail.android.mailmailbox.domain.usecase.MarkConversationsAsUnread
import ch.protonmail.android.mailmailbox.domain.usecase.MarkMessagesAsRead
import ch.protonmail.android.mailmailbox.domain.usecase.MarkMessagesAsUnread
import ch.protonmail.android.mailmailbox.domain.usecase.MoveConversations
import ch.protonmail.android.mailmailbox.domain.usecase.MoveMessages
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveOnboarding
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.domain.usecase.RelabelConversations
import ch.protonmail.android.mailmailbox.domain.usecase.RelabelMessages
import ch.protonmail.android.mailmailbox.domain.usecase.SaveOnboarding
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.SwipeActionsMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState.Data.SelectionMode.SelectedMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.OnboardingState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxReducer
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxPagerFactory
import ch.protonmail.android.mailmessage.domain.model.LabelSelectionList
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailmessage.domain.usecase.GetConversationMessagesWithLabels
import ch.protonmail.android.mailmessage.domain.usecase.GetMessagesWithLabels
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveSwipeActionsPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@SuppressWarnings("LongParameterList", "TooManyFunctions", "LargeClass")
class MailboxViewModel @Inject constructor(
    private val mailboxPagerFactory: MailboxPagerFactory,
    private val observeCurrentViewMode: ObserveCurrentViewMode,
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMailLabels: ObserveMailLabels,
    private val observeCustomMailLabels: ObserveCustomMailLabels,
    private val observeDestinationMailLabels: ObserveExclusiveDestinationMailLabels,
    private val observeSwipeActionsPreference: ObserveSwipeActionsPreference,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val observeUnreadCounters: ObserveUnreadCounters,
    private val observeFolderColorSettings: ObserveFolderColorSettings,
    private val getMessagesWithLabels: GetMessagesWithLabels,
    private val getConversationMessagesWithLabels: GetConversationMessagesWithLabels,
    private val getMailboxActions: GetMailboxActions,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val mailboxItemMapper: MailboxItemUiModelMapper,
    private val swipeActionsMapper: SwipeActionsMapper,
    private val getContacts: GetContacts,
    private val markConversationsAsRead: MarkConversationsAsRead,
    private val markConversationsAsUnread: MarkConversationsAsUnread,
    private val markMessagesAsRead: MarkMessagesAsRead,
    private val markMessagesAsUnread: MarkMessagesAsUnread,
    private val relabelMessages: RelabelMessages,
    private val relabelConversations: RelabelConversations,
    private val moveConversations: MoveConversations,
    private val moveMessages: MoveMessages,
    private val deleteConversations: DeleteConversations,
    private val deleteMessages: DeleteMessages,
    private val starMessages: StarMessages,
    private val starConversations: StarConversations,
    private val unStarMessages: UnStarMessages,
    private val unStarConversations: UnStarConversations,
    private val mailboxReducer: MailboxReducer,
    private val observeMailFeature: ObserveMailFeature,
    private val dispatchersProvider: DispatcherProvider,
    private val observeOnboarding: ObserveOnboarding,
    private val saveOnboarding: SaveOnboarding
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
    private val mutableState = MutableStateFlow(initialState)
    private val itemIds = Collections.synchronizedList(mutableListOf<String>())

    val state: StateFlow<MailboxState> = mutableState.asStateFlow()
    val items: Flow<PagingData<MailboxItemUiModel>> = observePagingData().cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            if (shouldDisplayOnboarding()) {
                emitNewStateFrom(MailboxEvent.ShowOnboarding)
            }
        }

        observeCurrentMailLabel()
            .onEach { currentMailLabel ->
                currentMailLabel?.let {
                    emitNewStateFrom(MailboxEvent.SelectedLabelChanged(currentMailLabel))
                } ?: run {
                    emitNewStateFrom(MailboxEvent.SelectedLabelChanged(MailLabelId.System.Inbox.toMailLabel()))
                }
            }
            .filterNotNull()
            .combine(
                primaryUserId.filterNotNull().flatMapLatest { userId -> observeSwipeActionsPreference(userId) }
            ) { currentMailLabel, swipeActionsPreference ->
                val swipeActions = swipeActionsMapper(currentMailLabel.id.labelId, swipeActionsPreference)
                emitNewStateFrom(MailboxEvent.SwipeActionsChanged(swipeActions))
            }
            .launchIn(viewModelScope)

        selectedMailLabelId.flow
            .mapToExistingLabel()
            .pairWithCurrentLabelCount()
            .onEach { (currentMailLabel, currentLabelCount) ->
                itemIds.clear()
                emitNewStateFrom(MailboxEvent.NewLabelSelected(currentMailLabel, currentLabelCount))
            }
            .launchIn(viewModelScope)

        selectedMailLabelId.flow.mapToExistingLabel()
            .combine(state.observeSelectedMailboxItems()) { selectedMailLabelId, selectedMailboxItems ->
                getMailboxActions(selectedMailLabelId, selectedMailboxItems.none { it.isRead }).fold(
                    ifLeft = { MailboxEvent.MessageBottomBarEvent(BottomBarEvent.ErrorLoadingActions) },
                    ifRight = { actions ->
                        MailboxEvent.MessageBottomBarEvent(
                            BottomBarEvent.ActionsData(
                                actions.map { action -> actionUiModelMapper.toUiModel(action) }
                                    .toImmutableList()
                            )
                        )
                    }
                )
            }
            .distinctUntilChanged()
            .onEach { emitNewStateFrom(it) }
            .launchIn(viewModelScope)

        observeUnreadCounters()
            .mapToCurrentLabelCount()
            .filterNotNull()
            .onEach { currentLabelCount ->
                emitNewStateFrom(MailboxEvent.SelectedLabelCountChanged(currentLabelCount))
            }
            .launchIn(viewModelScope)

        primaryUserId
            .filterNotNull()
            .flatMapLatest { userId -> observeMailFeature(userId, MailFeatureId.SelectionMode) }
            .onEach {
                emitNewStateFrom(MailboxEvent.SelectionModeEnabledChanged(selectionModeEnabled = it.value))
            }
            .launchIn(viewModelScope)
    }

    @SuppressWarnings("ComplexMethod")
    internal fun submit(viewAction: MailboxViewAction) {
        viewModelScope.launch {
            when (viewAction) {
                is MailboxViewAction.ExitSelectionMode,
                is MailboxViewAction.DisableUnreadFilter,
                is MailboxViewAction.EnableUnreadFilter -> emitNewStateFrom(viewAction)

                is MailboxViewAction.MailboxItemsChanged -> handleMailboxItemChanged(viewAction.itemIds)
                is MailboxViewAction.OnItemAvatarClicked -> handleOnAvatarClicked(viewAction.item)
                is MailboxViewAction.OnItemLongClicked -> handleItemLongClick(viewAction.item)
                is MailboxViewAction.Refresh -> emitNewStateFrom(viewAction)
                is MailboxViewAction.RefreshCompleted -> emitNewStateFrom(viewAction)
                is MailboxViewAction.ItemClicked -> handleItemClick(viewAction.item)
                is MailboxViewAction.OnOfflineWithData -> emitNewStateFrom(viewAction)
                is MailboxViewAction.OnErrorWithData -> emitNewStateFrom(viewAction)
                is MailboxViewAction.MarkAsRead -> handleMarkAsReadAction(viewAction)
                is MailboxViewAction.MarkAsUnread -> handleMarkAsUnreadAction(viewAction)
                is MailboxViewAction.SwipeReadAction -> handleSwipeReadAction(viewAction)
                is MailboxViewAction.SwipeArchiveAction -> handleSwipeArchiveAction(viewAction)
                is MailboxViewAction.SwipeSpamAction -> handleSwipeSpamAction(viewAction)
                is MailboxViewAction.SwipeTrashAction -> handleSwipeTrashAction(viewAction)
                is MailboxViewAction.SwipeStarAction -> handleSwipeStarAction(viewAction)
                is MailboxViewAction.CloseOnboarding -> handleCloseOnboarding()
                is MailboxViewAction.Trash -> handleTrashAction()
                is MailboxViewAction.Delete -> handleDeleteAction()
                is MailboxViewAction.DeleteConfirmed -> handleDeleteConfirmedAction()
                is MailboxViewAction.DeleteDialogDismissed -> handleDeleteDialogDismissed()
                is MailboxViewAction.RequestLabelAsBottomSheet -> showLabelAsBottomSheetAndLoadData(viewAction)
                is MailboxViewAction.LabelAsToggleAction -> emitNewStateFrom(viewAction)
                is MailboxViewAction.LabelAsConfirmed -> onLabelAsConfirmed(viewAction.archiveSelected)
                is MailboxViewAction.RequestMoveToBottomSheet -> showMoveToBottomSheetAndLoadData(viewAction)
                is MailboxViewAction.MoveToDestinationSelected -> emitNewStateFrom(viewAction)
                is MailboxViewAction.MoveToConfirmed -> onMoveToConfirmed()
                is MailboxViewAction.RequestMoreActionsBottomSheet -> showMoreBottomSheet(viewAction)
                is MailboxViewAction.DismissBottomSheet -> emitNewStateFrom(viewAction)
                is MailboxViewAction.Star -> handleStarAction(viewAction)
                is MailboxViewAction.UnStar -> handleUnStarAction(viewAction)
                is MailboxViewAction.MoveToArchive,
                is MailboxViewAction.MoveToSpam -> handleMoveToAction(viewAction)
            }.exhaustive
        }
    }

    private suspend fun handleMailboxItemChanged(updatedItemIds: List<String>) {
        withContext(dispatchersProvider.Comp) {
            val removedItems = itemIds.filterNot { updatedItemIds.contains(it) }
            itemIds.clear()
            itemIds.addAll(updatedItemIds)
            Timber.d("Removed items: $removedItems")
            if (removedItems.isNotEmpty()) {
                when (val currentState = state.value.mailboxListState) {
                    is MailboxListState.Data.SelectionMode -> {
                        currentState.selectedMailboxItems
                            .map { it.id }
                            .filter { currentSelectedItem -> removedItems.contains(currentSelectedItem) }
                            .takeIf { it.isNotEmpty() }
                            ?.let { emitNewStateFrom(MailboxEvent.ItemsRemovedFromSelection(it)) }
                    }

                    else -> {}
                }
            }
        }
    }

    private suspend fun handleItemClick(item: MailboxItemUiModel) {
        when (state.value.mailboxListState) {
            is MailboxListState.Data.SelectionMode -> handleItemClickInSelectionMode(item)
            is MailboxListState.Data.ViewMode -> handleItemClickInViewMode(item)
            is MailboxListState.Loading -> {
                Timber.d("Loading state can't handle item clicks")
            }
        }
    }

    private suspend fun handleItemClickInViewMode(item: MailboxItemUiModel) {
        if (item.shouldOpenInComposer) {
            emitNewStateFrom(MailboxEvent.ItemClicked.OpenComposer(item))
        } else {
            emitNewStateFrom(MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(item, getPreferredViewMode()))
        }
    }

    private fun handleItemLongClick(item: MailboxItemUiModel) {
        when (val state = state.value.mailboxListState) {
            is MailboxListState.Data.ViewMode -> enterSelectionMode(item)
            else -> {
                Timber.d("Long click not supported in state: $state")
            }
        }
    }

    private fun handleOnAvatarClicked(item: MailboxItemUiModel) {
        when (val state = state.value.mailboxListState) {
            is MailboxListState.Data.ViewMode -> enterSelectionMode(item)
            is MailboxListState.Data.SelectionMode -> handleItemClickInSelectionMode(item)
            else -> {
                Timber.d("Avatar clicked not supported in state: $state")
            }
        }
    }

    private fun handleItemClickInSelectionMode(item: MailboxItemUiModel) {
        val selectionMode = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionMode == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }

        val event = if (selectionMode.selectedMailboxItems.any { it.id == item.id }) {
            if (selectionMode.selectedMailboxItems.size == 1) {
                MailboxViewAction.ExitSelectionMode
            } else {
                MailboxEvent.ItemClicked.ItemRemovedFromSelection(item)
            }
        } else {
            MailboxEvent.ItemClicked.ItemAddedToSelection(item)
        }

        emitNewStateFrom(event)
    }

    private fun enterSelectionMode(item: MailboxItemUiModel) {
        when (val state = state.value.mailboxListState) {
            is MailboxListState.Data.ViewMode -> emitNewStateFrom(MailboxEvent.EnterSelectionMode(item))
            else -> Timber.d("Cannot enter selection mode from state: $state")
        }
    }

    private fun observePagingData(): Flow<PagingData<MailboxItemUiModel>> =
        primaryUserId.filterNotNull().flatMapLatest { userId ->
            combine(
                state.observeMailLabelChanges(),
                state.observeUnreadFilterState(),
                observeViewModeByLocation()
            ) { selectedMailLabel, unreadFilterEnabled, viewMode ->
                mailboxPagerFactory.create(
                    userIds = listOf(userId),
                    selectedMailLabelId = selectedMailLabel.id,
                    filterUnread = unreadFilterEnabled,
                    type = viewMode.toMailboxItemType()
                )
            }.flatMapLatest { mapPagingData(userId, it) }
        }

    private suspend fun mapPagingData(
        userId: UserId,
        pager: Pager<MailboxPageKey, MailboxItem>
    ): Flow<PagingData<MailboxItemUiModel>> {
        return withContext(dispatchersProvider.Comp) {
            val contacts = getContacts()
            combine(
                pager.flow.cachedIn(viewModelScope),
                observeFolderColorSettings(userId)
            ) { pagingData, folderColorSettings ->
                pagingData.map {
                    withContext(dispatchersProvider.Comp) {
                        mailboxItemMapper.toUiModel(it, contacts, folderColorSettings)
                    }
                }
            }
        }
    }

    private suspend fun handleMarkAsReadAction(markAsReadOperation: MailboxViewAction.MarkAsRead) {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }

        val user = primaryUserId.filterNotNull().first()
        val selectedItemsByViewMode = selectionModeDataState.selectedMailboxItems.groupBy { it.type }
        selectedItemsByViewMode.keys.forEach { viewMode ->
            when (viewMode) {
                MailboxItemType.Conversation -> markConversationsAsRead(
                    userId = user,
                    conversationIds = selectedItemsByViewMode[viewMode]?.map { ConversationId(it.id) } ?: emptyList()
                )

                MailboxItemType.Message -> markMessagesAsRead(
                    userId = user,
                    messageIds = selectedItemsByViewMode[viewMode]?.map { MessageId(it.id) } ?: emptyList()
                )
            }
        }
        emitNewStateFrom(markAsReadOperation)
    }

    private suspend fun handleMarkAsUnreadAction(markAsReadOperation: MailboxViewAction.MarkAsUnread) {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val selectedItemsByViewMode = selectionModeDataState.selectedMailboxItems.groupBy { it.type }

        selectedItemsByViewMode.keys.forEach { viewMode ->
            when (viewMode) {
                MailboxItemType.Conversation -> markConversationsAsUnread(
                    userId = userId,
                    conversationIds = selectedItemsByViewMode[viewMode]?.map { ConversationId(it.id) } ?: emptyList()
                )

                MailboxItemType.Message -> markMessagesAsUnread(
                    userId = userId,
                    messageIds = selectedItemsByViewMode[viewMode]?.map { MessageId(it.id) } ?: emptyList()
                )
            }
        }
        emitNewStateFrom(markAsReadOperation)
    }

    private suspend fun handleSwipeReadAction(swipeReadAction: MailboxViewAction.SwipeReadAction) {
        if (swipeReadAction.isRead) {
            when (swipeReadAction.viewMode) {
                MailboxItemType.Conversation -> markConversationsAsUnread(
                    userId = swipeReadAction.userId,
                    conversationIds = listOf(ConversationId(swipeReadAction.itemId))
                )

                MailboxItemType.Message -> markMessagesAsUnread(
                    userId = swipeReadAction.userId,
                    messageIds = listOf(MessageId(swipeReadAction.itemId))
                )
            }
        } else {
            when (swipeReadAction.viewMode) {
                MailboxItemType.Conversation -> markConversationsAsRead(
                    userId = swipeReadAction.userId,
                    conversationIds = listOf(ConversationId(swipeReadAction.itemId))
                )

                MailboxItemType.Message -> markMessagesAsRead(
                    userId = swipeReadAction.userId,
                    messageIds = listOf(MessageId(swipeReadAction.itemId))
                )
            }
        }
        emitNewStateFrom(swipeReadAction)
    }

    private suspend fun handleSwipeStarAction(swipeStarAction: MailboxViewAction.SwipeStarAction) {
        if (swipeStarAction.isStarred) {
            when (swipeStarAction.viewMode) {
                MailboxItemType.Conversation -> unStarConversations(
                    userId = swipeStarAction.userId,
                    conversationIds = listOf(ConversationId(swipeStarAction.itemId))
                )

                MailboxItemType.Message -> unStarMessages(
                    userId = swipeStarAction.userId,
                    messageIds = listOf(MessageId(swipeStarAction.itemId))
                )
            }
        } else {
            when (swipeStarAction.viewMode) {
                MailboxItemType.Conversation -> starConversations(
                    userId = swipeStarAction.userId,
                    conversationIds = listOf(ConversationId(swipeStarAction.itemId))
                )

                MailboxItemType.Message -> starMessages(
                    userId = swipeStarAction.userId,
                    messageIds = listOf(MessageId(swipeStarAction.itemId))
                )
            }
        }
        emitNewStateFrom(swipeStarAction)
    }

    private suspend fun handleSwipeArchiveAction(swipeArchiveAction: MailboxViewAction.SwipeArchiveAction) {
        if (isActionAllowedForCurrentLabel(SystemLabelId.Archive.labelId)) {
            swipeArchiveAction.let {
                moveSingleItemToDestination(it.userId, it.itemId, SystemLabelId.Archive.labelId, it.viewMode)
            }
            emitNewStateFrom(swipeArchiveAction)
        }
    }

    private suspend fun handleSwipeSpamAction(swipeSpamAction: MailboxViewAction.SwipeSpamAction) {
        if (isActionAllowedForCurrentLabel(SystemLabelId.Spam.labelId)) {
            swipeSpamAction.let {
                moveSingleItemToDestination(it.userId, it.itemId, SystemLabelId.Spam.labelId, it.viewMode)
            }
            emitNewStateFrom(swipeSpamAction)
        }
    }

    private suspend fun handleSwipeTrashAction(swipeTrashAction: MailboxViewAction.SwipeTrashAction) {
        if (isActionAllowedForCurrentLabel(SystemLabelId.Trash.labelId)) {
            swipeTrashAction.let {
                moveSingleItemToDestination(it.userId, it.itemId, SystemLabelId.Trash.labelId, it.viewMode)
            }
            emitNewStateFrom(swipeTrashAction)
        }
    }

    private suspend fun moveSingleItemToDestination(
        userId: UserId,
        itemId: String,
        labelId: LabelId,
        type: MailboxItemType
    ) {
        when (type) {
            MailboxItemType.Conversation -> moveConversations(
                userId = userId,
                conversationIds = listOf(ConversationId(itemId)),
                labelId = labelId
            )

            MailboxItemType.Message -> moveMessages(
                userId = userId,
                messageIds = listOf(MessageId(itemId)),
                labelId = labelId
            )
        }
    }

    private fun showLabelAsBottomSheetAndLoadData(operation: MailboxViewAction) {
        val selectionMode = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionMode == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        viewModelScope.launch {
            emitNewStateFrom(operation)

            val userId = primaryUserId.filterNotNull().first()
            val labels = observeCustomMailLabels(userId).firstOrNull()
            val color = observeFolderColorSettings(userId).firstOrNull()

            if (labels == null) {
                emitNewStateFrom(MailboxEvent.ErrorRetrievingCustomMailLabels)
                return@launch
            }

            if (color == null) {
                emitNewStateFrom(MailboxEvent.ErrorRetrievingFolderColorSettings)
                return@launch
            }

            val mappedLabels = labels.onLeft {
                Timber.e("Error while observing custom labels")
            }.getOrElse { emptyList() }

            val messagesWithLabels = getMessagesWithLabels(userId, selectionMode.selectedMailboxItems)

            val (selectedLabels, partiallySelectedLabels) = mappedLabels.getLabelSelectionState(messagesWithLabels)
            val event = MailboxEvent.MailboxBottomSheetEvent(
                LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                    customLabelList = mappedLabels.map { it.toCustomUiModel(color, emptyMap(), null) }
                        .toImmutableList(),
                    selectedLabels = selectedLabels.toImmutableList(),
                    partiallySelectedLabels = partiallySelectedLabels.toImmutableList()
                )
            )
            emitNewStateFrom(event)
        }
    }

    private fun onLabelAsConfirmed(archiveSelected: Boolean) {
        val selectionState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        viewModelScope.launch {
            val userId = primaryUserId.filterNotNull().first()
            val labels = observeCustomMailLabels(userId).firstOrNull()?.onLeft {
                Timber.e("Error while observing custom labels when relabeling got confirmed: $it")
            }?.getOrElse { emptyList() }
            if (labels == null) {
                emitNewStateFrom(MailboxEvent.ErrorRetrievingCustomMailLabels)
                return@launch
            }

            val messagesWithLabels = getMessagesWithLabels(userId, selectionState.selectedMailboxItems)

            val previousSelection = labels.getLabelSelectionState(messagesWithLabels)
            val labelAsData = state.value.bottomSheetState?.contentState as? LabelAsBottomSheetState.Data
                ?: throw IllegalStateException("BottomSheetState is not LabelAsBottomSheetState.Data")

            val updatedSelection = labelAsData.getLabelSelectionState()
            val itemsGroupedByViewMode = selectionState.selectedMailboxItems.groupBy { it.type }
            itemsGroupedByViewMode.keys.forEach { viewMode ->
                if (archiveSelected) {
                    when (viewMode) {
                        MailboxItemType.Conversation ->
                            moveConversations(
                                userId,
                                itemsGroupedByViewMode[viewMode]?.map { ConversationId(it.id) } ?: emptyList(),
                                SystemLabelId.Archive.labelId
                            )

                        MailboxItemType.Message ->
                            moveMessages(
                                userId,
                                itemsGroupedByViewMode[viewMode]?.map { MessageId(it.id) } ?: emptyList(),
                                SystemLabelId.Archive.labelId
                            )
                    }
                }
                val operation = handleRelabelOperation(
                    userId = userId,
                    viewMode = viewMode,
                    selectedItems = itemsGroupedByViewMode[viewMode]?.toSet() ?: emptySet(),
                    currentSelectionList = previousSelection,
                    updatedSelectionList = updatedSelection,
                    archiveSelected = archiveSelected
                )
                emitNewStateFrom(operation)
            }
        }
    }

    private fun showMoveToBottomSheetAndLoadData(operation: MailboxViewAction) {
        val selectionState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        viewModelScope.launch {
            emitNewStateFrom(operation)

            val userId = primaryUserId.filterNotNull().first()
            val destinationFolder = observeDestinationMailLabels(userId).firstOrNull()
            val color = observeFolderColorSettings(userId).firstOrNull()

            if (destinationFolder == null) {
                emitNewStateFrom(MailboxEvent.ErrorRetrievingDestinationMailFolders)
                return@launch
            }

            if (color == null) {
                emitNewStateFrom(MailboxEvent.ErrorRetrievingFolderColorSettings)
                return@launch
            }

            val event = MailboxEvent.MailboxBottomSheetEvent(
                MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                    destinationFolder.toUiModels(color).let { it.folders + it.systems }.toImmutableList()
                )
            )
            emitNewStateFrom(event)
        }
    }

    private suspend fun onMoveToConfirmed() {
        val selectionState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val bottomSheetState = mutableState.value.bottomSheetState?.contentState
        if (bottomSheetState !is MoveToBottomSheetState.Data) {
            Timber.d("BottomSheetState is not MoveToBottomSheetState.Data")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val selectedFolder = bottomSheetState.selected
        if (selectedFolder == null) {
            Timber.d("Selected folder is null")
            return
        }
        handleMoveOperation(userId, selectionState, selectedFolder.id.labelId)
    }

    private suspend fun handleMoveToAction(viewAction: MailboxViewAction) {
        val selectionState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val targetLabel = when (viewAction) {
            is MailboxViewAction.MoveToArchive -> SystemLabelId.Archive.labelId
            is MailboxViewAction.MoveToSpam -> SystemLabelId.Spam.labelId
            else -> {
                Timber.e("Unsupported action: $viewAction")
                return
            }
        }
        handleMoveOperation(userId, selectionState, targetLabel)
    }

    private suspend fun handleMoveOperation(
        userId: UserId,
        selectionState: MailboxListState.Data.SelectionMode,
        targetLabelId: LabelId
    ) {
        val itemsGroupedByViewMode = selectionState.selectedMailboxItems.groupBy { it.type }
        itemsGroupedByViewMode.keys.forEach { viewMode ->
            when (viewMode) {
                MailboxItemType.Conversation -> moveConversations(
                    userId = userId,
                    conversationIds = itemsGroupedByViewMode[viewMode]?.map { ConversationId(it.id) } ?: emptyList(),
                    labelId = targetLabelId
                )

                MailboxItemType.Message -> moveMessages(
                    userId = userId,
                    messageIds = itemsGroupedByViewMode[viewMode]?.map { MessageId(it.id) } ?: emptyList(),
                    labelId = targetLabelId
                )
            }.fold(
                ifLeft = { MailboxEvent.ErrorMoving },
                ifRight = { MailboxViewAction.MoveToConfirmed }
            ).let { emitNewStateFrom(it) }
        }
    }

    private fun showMoreBottomSheet(operation: MailboxViewAction) {
        val selectionState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        emitNewStateFrom(operation)

        val usedStarAction = when (selectionState.selectedMailboxItems.all { it.isStarred }) {
            true -> Action.Unstar
            false -> Action.Star
        }

        emitNewStateFrom(
            MailboxEvent.MailboxBottomSheetEvent(
                MoreActionsBottomSheetState.MoreActionsBottomSheetEvent.ActionData(
                    listOf(usedStarAction, Action.Archive, Action.Spam)
                        .map { actionUiModelMapper.toUiModel(it) }
                        .toImmutableList()
                )
            )
        )
    }

    private suspend fun getMessagesWithLabels(
        userId: UserId,
        selectedItems: Set<SelectedMailboxItem>
    ): List<MessageWithLabels> {
        return when (getPreferredViewMode()) {
            ViewMode.ConversationGrouping ->
                getConversationMessagesWithLabels(userId, selectedItems.map { ConversationId(it.id) })

            ViewMode.NoConversationGrouping -> getMessagesWithLabels(userId, selectedItems.map { MessageId(it.id) })
        }.onLeft { Timber.e("Error while observing messages with labels") }.getOrElse { emptyList() }
    }

    private suspend fun handleRelabelOperation(
        userId: UserId,
        viewMode: MailboxItemType,
        selectedItems: Set<SelectedMailboxItem>,
        currentSelectionList: LabelSelectionList,
        updatedSelectionList: LabelSelectionList,
        archiveSelected: Boolean
    ): MailboxOperation {
        return when (viewMode) {
            MailboxItemType.Conversation -> relabelConversations(
                userId = userId,
                conversationIds = selectedItems.map { ConversationId(it.id) },
                currentSelections = currentSelectionList,
                updatedSelections = updatedSelectionList
            )

            MailboxItemType.Message -> relabelMessages(
                userId = userId,
                messageIds = selectedItems.map { MessageId(it.id) },
                currentSelections = currentSelectionList,
                updatedSelections = updatedSelectionList
            )
        }.fold(
            ifLeft = { MailboxEvent.ErrorLabeling },
            ifRight = { MailboxViewAction.LabelAsConfirmed(archiveSelected) }
        )
    }

    private suspend fun handleCloseOnboarding() {
        viewModelScope.launch {
            saveOnboarding(display = false)
            emitNewStateFrom(MailboxViewAction.CloseOnboarding)
        }
    }

    private suspend fun shouldDisplayOnboarding(): Boolean {
        return observeOnboarding().first().fold(
            ifLeft = { false },
            ifRight = { onboardingPreference -> onboardingPreference.display }
        )
    }

    private suspend fun handleTrashAction() {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val selectedItemsByViewMode = selectionModeDataState.selectedMailboxItems.groupBy { it.type }
        selectedItemsByViewMode.keys.forEach { viewMode ->
            when (viewMode) {
                MailboxItemType.Conversation -> moveConversations(
                    userId = userId,
                    conversationIds = selectedItemsByViewMode[viewMode]?.map { ConversationId(it.id) } ?: emptyList(),
                    labelId = SystemLabelId.Trash.labelId
                )

                MailboxItemType.Message -> moveMessages(
                    userId = userId,
                    messageIds = selectedItemsByViewMode[viewMode]?.map { MessageId(it.id) } ?: emptyList(),
                    labelId = SystemLabelId.Trash.labelId
                )
            }
        }
        emitNewStateFrom(MailboxEvent.Trash(selectionModeDataState.selectedMailboxItems.size))
    }

    private suspend fun handleDeleteAction() {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val event = MailboxEvent.Delete(
            viewMode = getPreferredViewMode(),
            numAffectedMessages = selectionModeDataState.selectedMailboxItems.size
        )
        emitNewStateFrom(event)
    }

    private suspend fun handleDeleteConfirmedAction() {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }

        val userId = primaryUserId.filterNotNull().first()
        val itemsByViewMode = selectionModeDataState.selectedMailboxItems.groupBy { it.type }
        itemsByViewMode.keys.forEach { viewMode ->
            when (viewMode) {
                MailboxItemType.Conversation -> {
                    deleteConversations(
                        userId = userId,
                        conversationIds = itemsByViewMode[viewMode]?.map { ConversationId(it.id) } ?: emptyList(),
                        currentLabelId = selectionModeDataState.currentMailLabel.id.labelId
                    )
                    emitNewStateFrom(
                        MailboxEvent.DeleteConfirmed(
                            ViewMode.ConversationGrouping,
                            selectionModeDataState.selectedMailboxItems.size
                        )
                    )
                }

                MailboxItemType.Message -> {
                    deleteMessages(
                        userId = userId,
                        messageIds = itemsByViewMode[viewMode]?.map { MessageId(it.id) } ?: emptyList(),
                        currentLabelId = selectionModeDataState.currentMailLabel.id.labelId
                    )
                    emitNewStateFrom(
                        MailboxEvent.DeleteConfirmed(
                            ViewMode.NoConversationGrouping,
                            selectionModeDataState.selectedMailboxItems.size
                        )
                    )
                }
            }
        }
    }

    private suspend fun handleStarAction(viewAction: MailboxViewAction) {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val selectedItemsByViewMode = selectionModeDataState.selectedMailboxItems.groupBy { it.type }
        selectedItemsByViewMode.keys.forEach { viewMode ->
            when (viewMode) {
                MailboxItemType.Conversation ->
                    starConversations(userId, selectionModeDataState.selectedMailboxItems.map { ConversationId(it.id) })

                MailboxItemType.Message ->
                    starMessages(userId, selectionModeDataState.selectedMailboxItems.map { MessageId(it.id) })
            }
        }
        emitNewStateFrom(viewAction)
    }

    private suspend fun handleUnStarAction(viewAction: MailboxViewAction) {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val selectedItemsByViewMode = selectionModeDataState.selectedMailboxItems.groupBy { it.type }

        selectedItemsByViewMode.keys.forEach { viewMode ->
            when (viewMode) {
                MailboxItemType.Conversation ->
                    unStarConversations(
                        userId,
                        selectionModeDataState.selectedMailboxItems.map { ConversationId(it.id) }
                    )

                MailboxItemType.Message ->
                    unStarMessages(userId, selectionModeDataState.selectedMailboxItems.map { MessageId(it.id) })
            }
        }
        emitNewStateFrom(viewAction)
    }

    private fun handleDeleteDialogDismissed() {
        emitNewStateFrom(MailboxViewAction.DeleteDialogDismissed)
    }

    private fun observeCurrentMailLabel() = observeMailLabels()
        .map { mailLabels ->
            mailLabels.allById[selectedMailLabelId.flow.value]
        }

    private fun Flow<MailLabelId>.mapToExistingLabel() = map {
        observeMailLabels().firstOrNull()?.let { mailLabels ->
            mailLabels.allById[selectedMailLabelId.flow.value]
        }
    }.filterNotNull()

    private fun observeUnreadCounters(): Flow<List<UnreadCounter>> = primaryUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            observeUnreadCounters(userId)
        }
    }

    private fun observeViewModeByLocation(): Flow<ViewMode> =
        primaryUserId.filterNotNull().flatMapLatest { userId ->
            selectedMailLabelId.flow.flatMapLatest { observeCurrentViewMode(userId, it) }.distinctUntilChanged()
        }

    private fun observeMailLabels() = primaryUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(MailLabels.Initial)
        } else {
            observeMailLabels(userId)
        }
    }

    private suspend fun getContacts(): List<Contact> {
        val userId = primaryUserId.firstOrNull() ?: return emptyList()

        return getContacts(userId).getOrElse {
            Timber.i("Failed getting user contacts for displaying mailbox. Fallback to using display name")
            emptyList()
        }
    }

    private suspend fun getPreferredViewMode(): ViewMode {
        val userId = primaryUserId.firstOrNull()

        return if (userId == null) {
            ObserveCurrentViewMode.DefaultViewMode
        } else {
            observeCurrentViewMode(userId).first()
        }
    }

    private fun Flow<MailLabel>.pairWithCurrentLabelCount() = map { currentLabel ->
        val currentLabelCount = observeUnreadCounters().firstOrNull()
            ?.find { it.labelId == currentLabel.id.labelId }
            ?.count
        Pair(currentLabel, currentLabelCount)
    }

    private fun Flow<List<UnreadCounter>>.mapToCurrentLabelCount() = map { unreadCounters ->
        val currentMailLabelId = selectedMailLabelId.flow.value
        unreadCounters.find { it.labelId == currentMailLabelId.labelId }?.count
    }

    private fun emitNewStateFrom(operation: MailboxOperation) {
        val state = mailboxReducer.newStateFrom(state.value, operation)
        mutableState.value = state
    }

    private fun Flow<MailboxState>.observeUnreadFilterState() =
        this.map { it.unreadFilterState as? UnreadFilterState.Data }
            .mapNotNull { it?.isFilterEnabled }
            .distinctUntilChanged()

    private fun Flow<MailboxState>.observeMailLabelChanges() =
        this.map { it.mailboxListState as? MailboxListState.Data.ViewMode }
            .mapNotNull { it?.currentMailLabel }
            .distinctUntilChanged()

    private fun Flow<MailboxState>.observeSelectedMailboxItems() =
        this.map { it.mailboxListState as? MailboxListState.Data.SelectionMode }
            .mapNotNull { it?.selectedMailboxItems }
            .distinctUntilChanged()

    private fun List<MailLabel.Custom>.getLabelSelectionState(messages: List<MessageWithLabels>): LabelSelectionList {
        val previousSelectedLabels = mutableListOf<LabelId>()
        val previousPartiallySelectedLabels = mutableListOf<LabelId>()
        this.forEach { label ->
            if (messages.allContainsLabel(label.id.labelId)) {
                previousSelectedLabels.add(label.id.labelId)
            } else if (messages.partiallyContainsLabel(label.id.labelId)) {
                previousPartiallySelectedLabels.add(label.id.labelId)
            }
        }
        return LabelSelectionList(
            selectedLabels = previousSelectedLabels,
            partiallySelectionLabels = previousPartiallySelectedLabels
        )
    }

    private fun LabelAsBottomSheetState.Data.getLabelSelectionState(): LabelSelectionList {
        val selectedLabels = this.labelUiModelsWithSelectedState
            .filter { it.selectedState == LabelSelectedState.Selected }
            .map { it.labelUiModel.id.labelId }

        val partiallySelectedLabels = this.labelUiModelsWithSelectedState
            .filter { it.selectedState == LabelSelectedState.PartiallySelected }
            .map { it.labelUiModel.id.labelId }
        return LabelSelectionList(
            selectedLabels = selectedLabels,
            partiallySelectionLabels = partiallySelectedLabels
        )
    }

    private fun List<MessageWithLabels>.allContainsLabel(labelId: LabelId): Boolean {
        return this.all { messageWithLabel ->
            messageWithLabel.labels.any { it.labelId == labelId }
        }
    }

    private fun List<MessageWithLabels>.partiallyContainsLabel(labelId: LabelId): Boolean {
        return this.any { messageWithLabel ->
            messageWithLabel.labels.any { it.labelId == labelId }
        }
    }

    private fun isActionAllowedForCurrentLabel(labelId: LabelId): Boolean {
        return state.value.mailboxListState.let { listState ->
            listState is MailboxListState.Data && listState.currentMailLabel.id.labelId != labelId
        }
    }

    companion object {

        val initialState = MailboxState(
            mailboxListState = MailboxListState.Loading(selectionModeEnabled = false),
            topAppBarState = MailboxTopAppBarState.Loading,
            unreadFilterState = UnreadFilterState.Loading,
            bottomAppBarState = BottomBarState.Data.Hidden(emptyList<ActionUiModel>().toImmutableList()),
            onboardingState = OnboardingState.Hidden,
            deleteDialogState = DeleteDialogState.Hidden,
            bottomSheetState = null,
            actionMessage = Effect.empty(),
            error = Effect.empty()
        )
    }
}
