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

package ch.protonmail.android.uitest.robot.mailbox.inbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailcommon.presentation.compose.AvatarTestTags
import ch.protonmail.android.maillabel.R
import ch.protonmail.android.mailmailbox.presentation.TEST_TAG_UNREAD_FILTER
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreen
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxItemTestTags
import ch.protonmail.android.uitest.models.mailbox.InboxListItemEntry
import ch.protonmail.android.uitest.robot.mailbox.MailboxRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.MoveToFolderRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.SelectionStateRobotInterface
import ch.protonmail.android.uitest.robot.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.onAllNodesWithText
import me.proton.core.test.android.robots.CoreRobot
import me.proton.core.test.android.robots.CoreVerify

@Suppress("unused", "MemberVisibilityCanBePrivate", "ExpressionBodySyntax")
class InboxRobot(
    override val composeTestRule: ComposeContentTestRule
) : CoreRobot(), MailboxRobotInterface {

    override fun clickMessageByPosition(position: Int): MessageRobot {
        composeTestRule.onNodeWithTag(MAILBOX_TAG)
            .awaitDisplayed(composeTestRule)
            .onChildAt(position)
            .awaitDisplayed(composeTestRule)
            .performClick()

        return super.clickMessageByPosition(position)
    }

    override fun swipeLeftMessageAtPosition(position: Int): InboxRobot {
        super.swipeLeftMessageAtPosition(position)
        return this
    }

    override fun longClickMessageOnPosition(position: Int): SelectionStateRobot {
        super.longClickMessageOnPosition(position)
        return SelectionStateRobot(composeTestRule)
    }

    override fun deleteMessageWithSwipe(position: Int): InboxRobot {
        super.deleteMessageWithSwipe(position)
        return this
    }

    override fun refreshMessageList(): InboxRobot {
        super.refreshMessageList()
        return this
    }

    fun filterUnreadMessages(): InboxRobot {
        composeTestRule
            .onNodeWithTag(TEST_TAG_UNREAD_FILTER)
            .performClick()

        return this
    }

    class SelectionStateRobot(
        private val composeRule: ComposeContentTestRule
    ) : SelectionStateRobotInterface {

        override fun exitMessageSelectionState(): InboxRobot {
            super.exitMessageSelectionState()
            return InboxRobot(composeRule)
        }

        override fun selectMessage(position: Int): SelectionStateRobot {
            super.selectMessage(position)
            return this
        }

        override fun addLabel(): InboxRobot {
            super.addLabel()
            return InboxRobot(composeRule)
        }

        override fun addFolder(): MoveToFolderRobot {
            super.addFolder()
            return MoveToFolderRobot(composeRule)
        }

        fun moveToTrash(): InboxRobot {
            return InboxRobot(composeRule)
        }
    }

    class MoveToFolderRobot(
        private val composeRule: ComposeContentTestRule
    ) : MoveToFolderRobotInterface {

        override fun moveToExistingFolder(name: String): InboxRobot {
            super.moveToExistingFolder(name)
            return InboxRobot(composeRule)
        }
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    inner class Verify(private val composeRule: ComposeContentTestRule) : CoreVerify() {

        fun mailboxScreenDisplayed() {
            composeRule.waitUntil(timeoutMillis = 60_000) {
                composeRule.onAllNodesWithText(R.string.label_title_inbox)
                    .fetchSemanticsNodes(false)
                    .isNotEmpty()
            }

            composeRule.onNodeWithTag(MailboxScreen.TestTag).assertIsDisplayed()
        }

        fun unreadFilterIsDisplayed() {
            composeRule
                .onNodeWithTag(TEST_TAG_UNREAD_FILTER)
                .assertIsDisplayed()
                .assertIsNotSelected()
        }

        fun unreadFilterIsSelected() {
            composeRule
                .onNodeWithTag(TEST_TAG_UNREAD_FILTER)
                .assertIsDisplayed()
                .assertIsSelected()
        }

        fun listItemsAreShown(vararg inboxEntries: InboxListItemEntry) {
            val mailboxItemMatcher = hasTestTag(MailboxItemTestTags.ITEM_ROW)
            val avatarItemMatcher = hasParent(hasTestTag(AvatarTestTags.AVATAR) and hasParent(mailboxItemMatcher))
            val participantsItemMatcher = hasTestTag(MailboxItemTestTags.PARTICIPANTS) and hasParent(mailboxItemMatcher)
            val subjectItemMatcher = hasTestTag(MailboxItemTestTags.SUBJECT) and hasParent(mailboxItemMatcher)
            val dateItemMatcher = hasTestTag(MailboxItemTestTags.DATE) and hasParent(mailboxItemMatcher)

            for (entry in inboxEntries) {
                composeRule.waitUntil(timeoutMillis = 30_000) {
                    composeRule.onAllNodes(mailboxItemMatcher).fetchSemanticsNodes().size > 1
                }

                composeRule
                    .onAllNodes(
                        matcher = avatarItemMatcher,
                        useUnmergedTree = true
                    )[entry.index]
                    .assertTextEquals(entry.avatarText)

                composeRule
                    .onAllNodes(
                        matcher = participantsItemMatcher,
                        useUnmergedTree = true
                    )[entry.index]
                    .assertTextEquals(entry.participants)

                composeRule
                    .onAllNodes(
                        matcher = subjectItemMatcher,
                        useUnmergedTree = true
                    )[entry.index]
                    .assertTextEquals(entry.subject)

                composeRule
                    .onAllNodes(
                        matcher = dateItemMatcher,
                        useUnmergedTree = true
                    )[entry.index]
                    .assertTextEquals(entry.date)
            }
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify(composeTestRule).apply(block)

    companion object {

        private const val MAILBOX_TAG = "MailboxList"
    }
}
