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

package ch.protonmail.android.uitest.e2e.settings

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ch.protonmail.android.MainActivity
import ch.protonmail.android.di.NetworkConfigModule
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import ch.protonmail.android.uitest.rule.createMockLoginTestRule
import ch.protonmail.android.uitest.rule.createMockWebServerRuleChain
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.network.data.di.BaseProtonApiUrl
import me.proton.core.user.domain.UserManager
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(NetworkConfigModule::class)
class SettingsFlowTest {

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var mockWebServer: MockWebServer

    private val composeTestRule = createAndroidComposeRule<MainActivity>()
    private val loginTestRule = createMockLoginTestRule(::accountManager, ::userManager)

    @get:Rule
    val ruleChain = createMockWebServerRuleChain(
        mockWebServer = ::mockWebServer,
        composeTestRule = composeTestRule,
        loginTestRule = loginTestRule
    )

    private val menuRobot = MenuRobot(composeTestRule)

    @Test
    fun openAccountSettings() {
        menuRobot
            .openSettings()
            .openUserAccountSettings()
            .verify { accountSettingsScreenIsDisplayed() }
    }

    @Test
    fun openConversationModeSetting() {
        menuRobot
            .openSettings()
            .openUserAccountSettings()
            .verify { accountSettingsScreenIsDisplayed() }
            .openConversationMode()
            .verify { conversationModeToggleIsDisplayedAndEnabled() }
    }

    @Test
    fun openSettingAndChangePreferredTheme() {
        menuRobot
            .openSettings()
            .openThemeSettings()
            .selectSystemDefault()
            .verify { defaultThemeSettingIsSelected() }
            .selectDarkTheme()
            .verify { darkThemeIsSelected() }
    }

    @Test
    fun openSettingAndChangePreferredLanguage() {
        val languageSettingsRobot = menuRobot
            .openSettings()
            .openLanguageSettings()
            .selectSystemDefault()
            .verify { defaultLanguageIsSelected() }
            .selectSpanish()
            .verify {
                spanishLanguageIsSelected()
                appLanguageChangedToSpanish()
            }
            .selectBrazilianPortuguese()
            .verify {
                brazilianPortugueseLanguageIsSelected()
                appLanguageChangedToPortuguese()
            }

        composeTestRule.waitForIdle()

        /*
         * Once Brazilian was selected, we can't just use `selectSystemDefault` to go back to default language,
         * since the values returned from `string.mail_settings_system_default` are still the default language ones
         * while the app is now in Brazilian, which causes a failure as "System default" string is not found.
         * The assumption is that this happens because the Instrumentation's context is not updated when changing lang
         */
        languageSettingsRobot
            .selectSystemDefaultFromBrazilian()
            .verify { defaultLanguageIsSelected() }
    }

    @Test
    fun openPasswordManagementSettings() {
        menuRobot
            .openSettings()
            .openUserAccountSettings()
            .openPasswordManagement()
            .verify { passwordManagementElementsDisplayed() }
    }

    @Test
    fun openSettingsAndChangeLeftSwipeAction() {
        menuRobot
            .openSettings()
            .openSwipeActions()
            .openSwipeLeft()
            .selectArchive()
            .navigateUpToSwipeActions()
            .verify { swipeLeft { isArchive() } }
    }

    @Test
    fun openSettingsAndChangeRightSwipeAction() {
        menuRobot
            .openSettings()
            .openSwipeActions()
            .openSwipeRight()
            .selectMarkRead()
            .navigateUpToSwipeActions()
            .verify { swipeRight { isMarkRead() } }
    }

    @Test
    fun openSettingsAndChangeCombinedContactsSetting() {
        menuRobot
            .openSettings()
            .openCombinedContactsSettings()
            .turnOnCombinedContacts()
            .verify { combinedContactsSettingIsToggled() }
    }

    @Test
    fun openSettingsAndChangeAlternativeRoutingSetting() {
        menuRobot
            .openSettings()
            .openAlternativeRoutingSettings()
            .turnOffAlternativeRouting()
            .verify { alternativeRoutingSettingIsToggled() }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestModule {

        @Provides
        @BaseProtonApiUrl
        fun baseProtonApiUrl(mockWebServer: MockWebServer) = mockWebServer.url("/")
    }
}
