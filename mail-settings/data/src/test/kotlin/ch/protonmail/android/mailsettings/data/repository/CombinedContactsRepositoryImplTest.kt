/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.data.repository

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import app.cash.turbine.test
import ch.protonmail.android.mailsettings.data.DataStoreProvider
import ch.protonmail.android.mailsettings.domain.model.CombinedContactsPreference
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CombinedContactsRepositoryImplTest {

    private val preferences = mockk<Preferences>()
    private val dataStoreProvider = mockk<DataStoreProvider> {
        every { this@mockk.combinedContactsDataStore } returns mockk dataStore@{
            every { this@dataStore.data } returns flowOf(preferences)
        }
    }

    private lateinit var combinedContactsRepository: CombinedContactsRepository

    @Before
    fun setUp() {
        combinedContactsRepository = CombinedContactsRepositoryImpl(dataStoreProvider)
    }

    @Test
    fun returnsFalseWhenNoPreferenceIsStoredLocally() = runTest {
        // Given
        coEvery { preferences.get<Boolean>(any()) } returns null
        // When
        combinedContactsRepository.observe().test {
            // Then
            assertEquals(CombinedContactsPreference(false), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun returnsLocallyStoredPreferenceFromDataStoreWhenAvailable() = runTest {
        // Given
        coEvery { preferences[booleanPreferencesKey("hasCombinedContactsPrefKey")] } returns true
        // When
        combinedContactsRepository.observe().test {
            // Then
            assertEquals(CombinedContactsPreference(true), awaitItem())
            awaitComplete()
        }
    }
}
