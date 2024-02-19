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

package ch.protonmail.android.navigation.route

import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import ch.protonmail.android.MainActivity
import ch.protonmail.android.feature.account.RemoveAccountDialog
import ch.protonmail.android.feature.account.SignOutAccountDialog
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen
import ch.protonmail.android.mailcontact.presentation.contacgroupform.ContactGroupFormScreen
import ch.protonmail.android.mailcontact.presentation.contactdetails.ContactDetailsScreen
import ch.protonmail.android.mailcontact.presentation.contactform.ContactFormScreen
import ch.protonmail.android.mailcontact.presentation.contactgroupdetails.ContactGroupDetailsScreen
import ch.protonmail.android.mailcontact.presentation.contactlist.ContactListScreen
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetail
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.ui.MessageDetail
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.maillabel.presentation.folderform.FolderFormScreen
import ch.protonmail.android.maillabel.presentation.folderlist.FolderListScreen
import ch.protonmail.android.maillabel.presentation.folderparentlist.ParentFolderListScreen
import ch.protonmail.android.maillabel.presentation.labelform.LabelFormScreen
import ch.protonmail.android.maillabel.presentation.labellist.LabelListScreen
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreen
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsettings.presentation.settings.MainSettingsScreen
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.model.SavedStateKey
import me.proton.core.compose.navigation.get
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.takeIfNotBlank


internal fun NavGraphBuilder.addConversationDetail(actions: ConversationDetail.Actions) {
    composable(route = Destination.Screen.Conversation.route) {
        ConversationDetailScreen(actions = actions)
    }
}

internal fun NavGraphBuilder.addMailbox(
    navController: NavHostController,
    openDrawerMenu: () -> Unit,
    showOfflineSnackbar: () -> Unit,
    showFeatureMissingSnackbar: () -> Unit,
    showRefreshErrorSnackbar: () -> Unit
) {
    composable(route = Destination.Screen.Mailbox.route) {
        MailboxScreen(
            actions = MailboxScreen.Actions.Empty.copy(
                navigateToMailboxItem = { request ->
                    val destination = when (request.shouldOpenInComposer) {
                        true -> Destination.Screen.EditDraftComposer(MessageId(request.itemId.value))
                        false -> when (request.itemType) {
                            MailboxItemType.Message -> Destination.Screen.Message(MessageId(request.itemId.value))
                            MailboxItemType.Conversation ->
                                Destination.Screen.Conversation(
                                    ConversationId(request.itemId.value),
                                    request.subItemId?.let { mailboxItemId ->
                                        MessageId(mailboxItemId.value)
                                    }
                                )
                        }
                    }
                    navController.navigate(destination)
                },
                navigateToComposer = { navController.navigate(Destination.Screen.Composer.route) },
                openDrawerMenu = openDrawerMenu,
                showOfflineSnackbar = showOfflineSnackbar,
                showRefreshErrorSnackbar = showRefreshErrorSnackbar,
                showFeatureMissingSnackbar = showFeatureMissingSnackbar,
                onAddLabel = { navController.navigate(Destination.Screen.CreateLabel.route) },
                onAddFolder = { navController.navigate(Destination.Screen.CreateFolder.route) }
            )
        )
    }
}

internal fun NavGraphBuilder.addMessageDetail(actions: MessageDetail.Actions) {
    composable(route = Destination.Screen.Message.route) {
        MessageDetailScreen(actions = actions)
    }
}

internal fun NavGraphBuilder.addComposer(
    navController: NavHostController,
    activityActions: MainActivity.Actions,
    showDraftSavedSnackbar: () -> Unit,
    showMessageSendingSnackbar: () -> Unit,
    showMessageSendingOfflineSnackbar: () -> Unit
) {
    val actions = ComposerScreen.Actions(
        onCloseComposerClick = navController::popBackStack,
        onSetMessagePasswordClick = { messageId, senderEmail ->
            navController.navigate(Destination.Screen.SetMessagePassword(messageId, senderEmail))
        },
        showDraftSavedSnackbar = showDraftSavedSnackbar,
        showMessageSendingSnackbar = showMessageSendingSnackbar,
        showMessageSendingOfflineSnackbar = showMessageSendingOfflineSnackbar
    )
    composable(route = Destination.Screen.Composer.route) { ComposerScreen(actions) }
    composable(route = Destination.Screen.EditDraftComposer.route) { ComposerScreen(actions) }
    composable(route = Destination.Screen.MessageActionComposer.route) { ComposerScreen(actions) }
    composable(route = Destination.Screen.ShareFileComposer.route) {
        ComposerScreen(
            actions.copy(
                onCloseComposerClick = { activityActions.finishActivity() }
            )
        )
    }
}

internal fun NavGraphBuilder.addSignOutAccountDialog(navController: NavHostController) {
    dialog(route = Destination.Dialog.SignOut.route) {
        SignOutAccountDialog(
            userId = it.get<String>(SignOutAccountDialog.USER_ID_KEY)?.takeIfNotBlank()?.let(::UserId),
            onSignedOut = { navController.popBackStack() },
            onCancelled = { navController.popBackStack() }
        )
    }
}

internal fun NavGraphBuilder.addSetMessagePassword(navController: NavHostController) {
    composable(route = Destination.Screen.SetMessagePassword.route) {
        SetMessagePasswordScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}

internal fun NavGraphBuilder.addRemoveAccountDialog(navController: NavHostController) {
    dialog(route = Destination.Dialog.RemoveAccount.route) {
        RemoveAccountDialog(
            userId = it.get<String>(RemoveAccountDialog.USER_ID_KEY)?.takeIfNotBlank()?.let(::UserId),
            onRemoved = { navController.popBackStack() },
            onCancelled = { navController.popBackStack() }
        )
    }
}

internal fun NavGraphBuilder.addSettings(navController: NavHostController) {
    composable(route = Destination.Screen.Settings.route) {
        MainSettingsScreen(
            actions = MainSettingsScreen.Actions(
                onAccountClick = {
                    navController.navigate(Destination.Screen.AccountSettings.route)
                },
                onThemeClick = {
                    navController.navigate(Destination.Screen.ThemeSettings.route)
                },
                onPushNotificationsClick = {
                    navController.navigate(Destination.Screen.Notifications.route)
                },
                onAutoLockClick = {
                    navController.navigate(Destination.Screen.AutoLockSettings.route)
                },
                onAlternativeRoutingClick = {
                    navController.navigate(Destination.Screen.AlternativeRoutingSettings.route)
                },
                onAppLanguageClick = {
                    navController.navigate(Destination.Screen.LanguageSettings.route)
                },
                onCombinedContactsClick = {
                    navController.navigate(Destination.Screen.CombinedContactsSettings.route)
                },
                onSwipeActionsClick = {
                    navController.navigate(Destination.Screen.SwipeActionsSettings.route)
                },
                onClearCacheClick = {},
                onBackClick = {
                    navController.popBackStack()
                }
            )
        )
    }
}

internal fun NavGraphBuilder.addLabelList(
    navController: NavHostController,
    showLabelListErrorLoadingSnackbar: () -> Unit
) {
    composable(route = Destination.Screen.LabelList.route) {
        LabelListScreen(
            actions = LabelListScreen.Actions(
                onBackClick = {
                    navController.popBackStack()
                },
                onLabelSelected = { labelId ->
                    navController.navigate(Destination.Screen.EditLabel(labelId))
                },
                onAddLabelClick = {
                    navController.navigate(Destination.Screen.CreateLabel.route)
                },
                showLabelListErrorLoadingSnackbar = showLabelListErrorLoadingSnackbar
            )
        )
    }
}

internal fun NavGraphBuilder.addLabelForm(
    navController: NavHostController,
    showLabelSavedSnackbar: () -> Unit,
    showLabelDeletedSnackbar: () -> Unit
) {
    val actions = LabelFormScreen.Actions.Empty.copy(
        onBackClick = {
            navController.popBackStack()
        },
        showLabelSavedSnackbar = showLabelSavedSnackbar,
        showLabelDeletedSnackbar = showLabelDeletedSnackbar
    )
    composable(route = Destination.Screen.CreateLabel.route) { LabelFormScreen(actions) }
    composable(route = Destination.Screen.EditLabel.route) { LabelFormScreen(actions) }
}

internal fun NavGraphBuilder.addFolderList(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit
) {
    composable(route = Destination.Screen.FolderList.route) {
        FolderListScreen(
            actions = FolderListScreen.Actions(
                onBackClick = {
                    navController.popBackStack()
                },
                onFolderSelected = { labelId ->
                    navController.navigate(Destination.Screen.EditFolder(labelId))
                },
                onAddFolderClick = {
                    navController.navigate(Destination.Screen.CreateFolder.route)
                },
                exitWithErrorMessage = { message ->
                    navController.popBackStack()
                    showErrorSnackbar(message)
                }
            )
        )
    }
}

internal fun NavGraphBuilder.addFolderForm(
    navController: NavHostController,
    showSuccessSnackbar: (message: String) -> Unit,
    showErrorSnackbar: (message: String) -> Unit
) {
    val actions = FolderFormScreen.Actions.Empty.copy(
        onBackClick = {
            navController.popBackStack()
        },
        onFolderParentClick = { labelId, currentParentLabelId ->
            navController.navigate(Destination.Screen.ParentFolderList(labelId, currentParentLabelId))
        },
        exitWithSuccessMessage = { message ->
            navController.popBackStack()
            showSuccessSnackbar(message)
        },
        exitWithErrorMessage = { message ->
            navController.popBackStack()
            showErrorSnackbar(message)
        }
    )
    composable(route = Destination.Screen.CreateFolder.route) {
        FolderFormScreen(
            actions,
            currentParentLabelId = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>(
                SavedStateKey.CurrentParentFolderId.key
            )?.observeAsState()
        )
    }
    composable(route = Destination.Screen.EditFolder.route) {
        FolderFormScreen(
            actions,
            currentParentLabelId = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>(
                SavedStateKey.CurrentParentFolderId.key
            )?.observeAsState()
        )
    }
}

internal fun NavGraphBuilder.addParentFolderList(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit
) {
    val actions = ParentFolderListScreen.Actions.Empty.copy(
        onBackClick = {
            navController.popBackStack()
        },
        onFolderSelected = { labelId ->
            navController.previousBackStackEntry?.savedStateHandle?.set(
                SavedStateKey.CurrentParentFolderId.key,
                labelId.id
            )
            navController.popBackStack()
        },
        onNoneClick = {
            navController.previousBackStackEntry?.savedStateHandle?.set(
                SavedStateKey.CurrentParentFolderId.key,
                ""
            )
            navController.popBackStack()
        },
        exitWithErrorMessage = { message ->
            navController.popBackStack()
            showErrorSnackbar(message)
        }
    )
    composable(route = Destination.Screen.ParentFolderList.route) {
        ParentFolderListScreen(actions)
    }
}

internal fun NavGraphBuilder.addContacts(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    composable(route = Destination.Screen.Contacts.route) {
        ContactListScreen(
            actions = ContactListScreen.Actions(
                openContactForm = {
                    navController.navigate(Destination.Screen.CreateContact.route)
                },
                openContactGroupForm = {
                    navController.navigate(Destination.Screen.CreateContactGroup.route)
                },
                openImportContact = {
                    showFeatureMissingSnackbar()
                },
                onContactSelected = { contactId ->
                    navController.navigate(Destination.Screen.ContactDetails(contactId))
                },
                onContactGroupSelected = { labelId ->
                    navController.navigate(Destination.Screen.ContactGroupDetails(labelId))
                },
                onBackClick = {
                    navController.popBackStack()
                },
                exitWithErrorMessage = { message ->
                    navController.popBackStack()
                    showErrorSnackbar(message)
                }
            )
        )
    }
}

internal fun NavGraphBuilder.addContactDetails(
    navController: NavHostController,
    showSuccessSnackbar: (message: String) -> Unit,
    showErrorSnackbar: (message: String) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    val actions = ContactDetailsScreen.Actions.Empty.copy(
        onBackClick = { navController.popBackStack() },
        exitWithSuccessMessage = { message ->
            navController.popBackStack()
            showSuccessSnackbar(message)
        },
        exitWithErrorMessage = { message ->
            navController.popBackStack()
            showErrorSnackbar(message)
        },
        onEditClick = { contactId ->
            navController.navigate(Destination.Screen.EditContact(contactId))
        },
        showFeatureMissingSnackbar = { showFeatureMissingSnackbar() },
        navigateToComposer = {
            navController.navigate(Destination.Screen.MessageActionComposer(DraftAction.ComposeToAddresses(listOf(it))))
        }
    )
    composable(route = Destination.Screen.ContactDetails.route) {
        ContactDetailsScreen(actions)
    }
}

internal fun NavGraphBuilder.addContactForm(
    navController: NavHostController,
    showSuccessSnackbar: (message: String) -> Unit,
    showErrorSnackbar: (message: String) -> Unit
) {

    val actions = ContactFormScreen.Actions.Empty.copy(
        onCloseClick = {
            navController.popBackStack()
        },
        exitWithSuccessMessage = { message ->
            navController.popBackStack()
            showSuccessSnackbar(message)
        },
        exitWithErrorMessage = { message ->
            navController.popBackStack()
            showErrorSnackbar(message)
        }
    )
    composable(route = Destination.Screen.CreateContact.route) {
        ContactFormScreen(actions)
    }
    composable(route = Destination.Screen.EditContact.route) {
        ContactFormScreen(actions)
    }
}

internal fun NavGraphBuilder.addContactGroupDetails(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    val actions = ContactGroupDetailsScreen.Actions(
        onBackClick = { navController.popBackStack() },
        exitWithErrorMessage = { message ->
            navController.popBackStack()
            showErrorSnackbar(message)
        },
        onEditClick = { labelId ->
            navController.navigate(Destination.Screen.EditContactGroup(labelId))
        },
        navigateToComposer = { emails ->
            navController.navigate(Destination.Screen.MessageActionComposer(DraftAction.ComposeToAddresses(emails)))
        },
        showFeatureMissingSnackbar = showFeatureMissingSnackbar
    )
    composable(route = Destination.Screen.ContactGroupDetails.route) {
        ContactGroupDetailsScreen(actions)
    }
}

internal fun NavGraphBuilder.addContactGroupForm(
    navController: NavHostController,
    showErrorSnackbar: (message: String) -> Unit
) {
    val actions = ContactGroupFormScreen.Actions.Empty.copy(
        onClose = { navController.popBackStack() },
        exitWithErrorMessage = { message ->
            navController.popBackStack()
            showErrorSnackbar(message)
        }
    )
    composable(route = Destination.Screen.CreateContactGroup.route) {
        ContactGroupFormScreen(actions)
    }
    composable(route = Destination.Screen.EditContactGroup.route) {
        ContactGroupFormScreen(actions)
    }
}
