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

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import ch.protonmail.android.mailcommon.domain.usecase.GetDefaultLocale
import javax.inject.Inject
import kotlin.time.Duration

private const val MILLIS_IN_A_DAY = 86_400_000L

class MailboxItemTimeFormatter @Inject constructor(
    private val calendar: Calendar,
    private val getDefaultLocale: GetDefaultLocale
) {

    operator fun invoke(itemTime: Duration): String {
        if (itemTime.isToday()) {
            return itemTime.format(DateFormat.Today)
        }
        return "foo"
    }

    private fun Duration.format(format: DateFormat) = SimpleDateFormat(
        format.pattern,
        getDefaultLocale()
    ).format(
        Date(this.inWholeMilliseconds)
    )

    private fun Duration.isToday(): Boolean {
        val currentTimeMillis = calendar.time.time
        return currentTimeMillis - this.inWholeMilliseconds < MILLIS_IN_A_DAY
    }

    private enum class DateFormat(val pattern: String) {
        Today("HH:mm")
    }
}
