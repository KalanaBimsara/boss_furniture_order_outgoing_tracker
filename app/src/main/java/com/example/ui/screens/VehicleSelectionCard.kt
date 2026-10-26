package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AccentButton
import com.example.ui.components.SectionCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.appTextFieldColors
import com.example.ui.theme.Motion
import com.example.viewmodel.DispatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSelectionCard(
    viewModel: DispatchViewModel,
    accentColor: Color
) {
    val selectedOption by viewModel.selectedVehicleOption.collectAsStateWithLifecycle()
    val pickMeNumber by viewModel.pickMeVehicleNumber.collectAsStateWithLifecycle()
    val customNumber by viewModel.customVehicleNumber.collectAsStateWithLifecycle()
    val savedCustomVehicles by viewModel.savedCustomVehicles.collectAsStateWithLifecycle()

    var dropdownExpanded by remember { mutableStateOf(false) }
    val allOptions = remember(savedCustomVehicles) {
        viewModel.getAvailableVehicleOptions()
    }

    val effectiveVehicle = remember(selectedOption, pickMeNumber, customNumber) {
        viewModel.getEffectiveVehicleNumber()
    }
    val assignedDriverUuid = remember(selectedOption, pickMeNumber, customNumber) {
        viewModel.getAssignedDriverUuid()
    }
    val assignedSummary = remember(selectedOption, pickMeNumber, customNumber) {
        viewModel.getAssignedDeliverySummary()
    }
    val fieldColors = appTextFieldColors(accentColor)

    SectionCard(modifier = Modifier.testTag("vehicle_selection_card")) {
        SectionHeader(
            title = "Transport vehicle",
            subtitle = "Carries the dispatch / return items",
            icon = Icons.Default.DirectionsBus,
            accent = accentColor,
            trailing = {
                StatusPill(
                    text = effectiveVehicle,
                    color = accentColor,
                    modifier = Modifier
                        .widthIn(max = 140.dp)
                        .animateContentSize(Motion.emphasized())
                        .testTag("active_vehicle_badge")
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                label = { Text("Vehicle number") },
                leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .testTag("vehicle_dropdown_field"),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors
            )

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                allOptions.forEach { option ->
                    val isSelected = option == selectedOption
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (option == DispatchViewModel.VEHICLE_CUSTOM_OPTION) {
                                    "Custom vehicle (add / enter vehicle #)"
                                } else {
                                    option
                                },
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (option) {
                                    DispatchViewModel.VEHICLE_PICK_ME -> Icons.Default.LocalTaxi
                                    DispatchViewModel.VEHICLE_CUSTOM_OPTION -> Icons.Default.AddCircleOutline
                                    else -> Icons.Default.LocalShipping
                                },
                                contentDescription = null,
                                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            viewModel.setSelectedVehicleOption(option)
                            dropdownExpanded = false
                        },
                        modifier = Modifier.testTag("vehicle_option_${option.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }

        // Pick Me vehicle number input
        AnimatedVisibility(
            visible = selectedOption == DispatchViewModel.VEHICLE_PICK_ME,
            enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
            exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
        ) {
            OutlinedTextField(
                value = pickMeNumber,
                onValueChange = { viewModel.setPickMeVehicleNumber(it) },
                label = { Text("Pick Me vehicle number") },
                placeholder = { Text("e.g. CAA-4512 or WP CAB-1122") },
                leadingIcon = { Icon(Icons.Default.LocalTaxi, contentDescription = null) },
                trailingIcon = {
                    if (pickMeNumber.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setPickMeVehicleNumber("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Pick Me vehicle number")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("pick_me_vehicle_number_input"),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors
            )
        }

        // Custom vehicle number input + save button
        AnimatedVisibility(
            visible = selectedOption == DispatchViewModel.VEHICLE_CUSTOM_OPTION,
            enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
            exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customNumber,
                    onValueChange = { viewModel.setCustomVehicleNumber(it) },
                    label = { Text("Custom vehicle number") },
                    placeholder = { Text("e.g. WP-DAG-8890 (Kamal)") },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_vehicle_number_input"),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = fieldColors
                )
                AccentButton(
                    text = "Save",
                    icon = Icons.Default.Add,
                    onClick = { viewModel.addAndSelectCustomVehicle(customNumber) },
                    enabled = customNumber.isNotBlank(),
                    accent = accentColor,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(56.dp)
                        .testTag("add_custom_vehicle_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("driver_assignment_badge"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (assignedDriverUuid != null) Icons.Default.Badge else Icons.Default.LocalShipping,
                contentDescription = "Assigned Driver / Delivery Option",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = assignedSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
