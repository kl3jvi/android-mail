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

package ch.protonmail.android.mailupselling.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.annotations.ForceOneClickUpsellingDetailsOverride
import ch.protonmail.android.mailupselling.domain.model.DynamicPlansOneClickIds
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.DynamicEntitlementUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import me.proton.core.plan.domain.entity.DynamicEntitlement
import me.proton.core.plan.domain.entity.DynamicPlan
import javax.inject.Inject

internal class DynamicPlanEntitlementsUiMapper @Inject constructor(
    @ForceOneClickUpsellingDetailsOverride private val shouldOverrideEntitlementsList: Boolean
) {

    fun toUiModel(plan: DynamicPlan, upsellingEntryPoint: UpsellingEntryPoint): List<DynamicEntitlementUiModel> {
        if (!shouldOverrideEntitlementsList) return mapToDefaults(plan.entitlements)

        return when (plan.name) {
            DynamicPlansOneClickIds.PlusPlanId -> getPlusEntitlements(upsellingEntryPoint)
            DynamicPlansOneClickIds.UnlimitedPlanId -> UnlimitedOverriddenEntitlements
            else -> mapToDefaults(plan.entitlements)
        }
    }

    private fun mapToDefaults(list: List<DynamicEntitlement>): List<DynamicEntitlementUiModel> {
        return list.asSequence()
            .filterIsInstance(DynamicEntitlement.Description::class.java)
            .map { DynamicEntitlementUiModel.Default(TextUiModel.Text(it.text), it.iconUrl) }
            .toList()
    }

    private fun getPlusEntitlements(upsellingEntryPoint: UpsellingEntryPoint) = when (upsellingEntryPoint) {
        UpsellingEntryPoint.ContactGroups -> ContactGroupsPlusOverriddenEntitlements
        UpsellingEntryPoint.Folders -> FoldersPlusOverriddenEntitlements
        UpsellingEntryPoint.Labels -> LabelsPlusOverriddenEntitlements
        UpsellingEntryPoint.Mailbox -> MailboxPlusOverriddenEntitlements
        UpsellingEntryPoint.MobileSignature -> MobileSignaturePlusOverriddenEntitlements
    }

    private companion object {

        val MailboxPlusOverriddenEntitlements = listOf(
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_storage),
                localResource = R.drawable.ic_upselling_storage
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_email_addresses),
                localResource = R.drawable.ic_upselling_inbox
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_custom_domain),
                localResource = R.drawable.ic_upselling_globe
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_desktop_app),
                localResource = R.drawable.ic_upselling_rocket
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_folders_labels),
                localResource = R.drawable.ic_upselling_tag
            )
        )

        private val SharedPlusOverriddenEntitlements = listOf(
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_storage),
                localResource = R.drawable.ic_upselling_storage
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_email_addresses),
                localResource = R.drawable.ic_upselling_inbox
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_custom_domain),
                localResource = R.drawable.ic_upselling_globe
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_plus_feature_plus_7_features),
                localResource = R.drawable.ic_upselling_gift
            )
        )

        val ContactGroupsPlusOverriddenEntitlements = SharedPlusOverriddenEntitlements

        val FoldersPlusOverriddenEntitlements = SharedPlusOverriddenEntitlements

        val LabelsPlusOverriddenEntitlements = SharedPlusOverriddenEntitlements

        @Suppress("VariableMaxLength")
        val MobileSignaturePlusOverriddenEntitlements = SharedPlusOverriddenEntitlements

        val UnlimitedOverriddenEntitlements = listOf(
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_unlimited_description_override),
                localResource = R.drawable.ic_upselling_storage
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_unlimited_feature_mail_calendar),
                localResource = R.drawable.ic_upselling_mail
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_unlimited_feature_vpn),
                localResource = R.drawable.ic_upselling_vpn
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_unlimited_feature_drive),
                localResource = R.drawable.ic_upselling_drive
            ),
            DynamicEntitlementUiModel.Overridden(
                text = TextUiModel.TextRes(R.string.upselling_unlimited_feature_pass),
                localResource = R.drawable.ic_upselling_pass
            )
        )
    }
}
