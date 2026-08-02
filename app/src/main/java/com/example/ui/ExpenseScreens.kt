package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BillingGroup
import com.example.data.Expense
import com.example.data.ParticipantSplit
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueLight
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.theme.SurfaceLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainExpenseAppScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val groups by viewModel.allGroups.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showAddExpenseScreen by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    // Support file export (backup)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonBackup = viewModel.exportBackupAsJsonString()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonBackup.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Database Backup saved successfully!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to save backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Support file import (restore)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    }
                    if (jsonString != null) {
                        val success = viewModel.importBackupFromJsonString(jsonString)
                        if (success) {
                            Toast.makeText(context, "Database Restored successfully! All split groups updated.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to restore backup: Invalid file format", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to read backup file", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error importing backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Restore Local Database", color = RoyalBlue, fontWeight = FontWeight.Bold) },
            text = { Text("WARNING: Restoring from a backup will overwrite all current groups and split records with the backup file data. This action is permanent. Do you wish to proceed?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        try {
                            importLauncher.launch(arrayOf("application/json"))
                        } catch (e: Exception) {
                            try {
                                importLauncher.launch(arrayOf("*/*"))
                            } catch (e2: Exception) {
                                Toast.makeText(context, "No file picker available", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Overwrite & Restore")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Navigation Drawer to switch between months
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = SurfaceLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // App Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp, top = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(RoyalBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "App Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "WeXpense",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                            Text(
                                text = "Group Billing & Splits",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Add billing period / group button
                    Button(
                        onClick = { showAddGroupDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("add_group_drawer_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Group")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Billing Group", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Billing Periods",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Chronological List of Months / Groups
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(groups) { group ->
                            val isSelected = selectedGroup?.id == group.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) RoyalBlue.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        viewModel.selectGroup(group.id)
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp)
                                    .testTag("group_item_${group.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Group Icon",
                                    tint = if (isSelected) RoyalBlue else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.name,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) RoyalBlue else Color.Black
                                    )
                                    Text(
                                        text = group.description,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = RoyalBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Database Backup & Restore section
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RoyalBlue.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .border(1.dp, RoyalBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Backup Icon",
                                tint = RoyalBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Database Backup & Sync",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                        }
                        
                        Text(
                            text = "Save database backup to Google Storage / Google Drive to prevent any data loss.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Export Button
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    try {
                                        exportLauncher.launch("WeXpense_Backup.json")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open file saver", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.5f)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Export DB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Import Button
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    showRestoreConfirmDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.5f)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Import DB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = selectedGroup?.name ?: "WeXpense",
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue,
                                fontSize = 20.sp
                            )
                            selectedGroup?.let {
                                Text(
                                    text = "Group: ${it.description.takeIf { d -> d.isNotBlank() } ?: "Apartment 4B"}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp, end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, BorderColor, CircleShape)
                                .clickable { scope.launch { drawerState.open() } },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu Drawer",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    actions = {
                        // User Profile circular avatar top-right
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RoyalBlue)
                                .clickable { showProfileDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JD",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        if (activeTab == 0 && selectedGroup != null) {
                            ExpenseOverflowMenu(viewModel) {
                                // callback to add advanced payment prefilled
                                editingExpense = null
                                showAddExpenseScreen = true
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BackgroundLight
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceLight,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    val items = listOf(
                        Triple(0, "Expenses", Icons.Default.ShoppingBag),
                        Triple(1, "Balance", Icons.Default.Scale),
                        Triple(2, "Share", Icons.Default.Share),
                        Triple(3, "Web Preview", Icons.Default.OpenInBrowser)
                    )
                    items.forEach { (index, label, icon) ->
                        NavigationBarItem(
                            selected = activeTab == index,
                            onClick = { viewModel.activeTab.value = index },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(imageVector = icon, contentDescription = label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RoyalBlue,
                                selectedTextColor = RoyalBlue,
                                indicatorColor = RoyalBlue.copy(alpha = 0.12f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_tab_$index")
                        )
                    }
                }
            },
            floatingActionButton = {
                if (activeTab == 0 && selectedGroup != null) {
                    FloatingActionButton(
                        onClick = {
                            editingExpense = null
                            showAddExpenseScreen = true
                        },
                        containerColor = RoyalBlue,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("add_expense_fab")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
                    }
                }
            },
            containerColor = BackgroundLight
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (selectedGroup == null) {
                    // Empty Group State
                    EmptyGroupState { showAddGroupDialog = true }
                } else {
                    when (activeTab) {
                        0 -> ExpensesTabScreen(
                            viewModel = viewModel,
                            onEditExpense = { expense ->
                                editingExpense = expense
                                showAddExpenseScreen = true
                            }
                        )
                        1 -> BalanceTabScreen(viewModel = viewModel)
                        2 -> ShareTabScreen(viewModel = viewModel)
                        3 -> WebReportPreviewScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Add Group Dialog
    if (showAddGroupDialog) {
        AddGroupDialog(
            onDismiss = { showAddGroupDialog = false },
            onSave = { name, desc, members ->
                viewModel.createBillingGroup(name, desc, members)
                showAddGroupDialog = false
            }
        )
    }

    // Add/Edit Expense Dialog Screen
    if (showAddExpenseScreen) {
        AddEditExpenseScreen(
            expense = editingExpense,
            groupMembers = selectedGroup?.members ?: emptyList(),
            onDismiss = { showAddExpenseScreen = false },
            onSave = { desc, amount, paidBy, date, isAll, splits, category ->
                if (editingExpense == null) {
                    viewModel.addExpense(desc, amount, paidBy, date, isAll, splits, category)
                } else {
                    viewModel.updateExpense(editingExpense!!.id, desc, amount, paidBy, date, isAll, splits, category)
                }
                showAddExpenseScreen = false
            },
            onDelete = {
                editingExpense?.let { viewModel.deleteExpense(it) }
                showAddExpenseScreen = false
            }
        )
    }

    // Profile Details Dialog
    if (showProfileDialog) {
        ProfileDialog(onDismiss = { showProfileDialog = false })
    }
}

// EMPTY GROUP PLACEHOLDER
@Composable
fun EmptyGroupState(onAddGroup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = "No Groups",
            tint = RoyalBlue.copy(alpha = 0.4f),
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to WeXpense!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = RoyalBlue
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create or select a Billing Group from the side menu or tap below to start tracking your group expenses and split bills easily.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddGroup,
            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("create_first_group_button")
        ) {
            Text("Create Billing Group", fontWeight = FontWeight.Bold)
        }
    }
}

// EXPENSE OVERFLOW MENU
@Composable
fun ExpenseOverflowMenu(
    viewModel: ExpenseViewModel,
    onAddAdvanced: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sortOption by viewModel.sortOption.collectAsState()
    val showFilter by viewModel.showCategoryFilter.collectAsState()

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Sort/Filter Options",
                tint = RoyalBlue
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceLight)
        ) {
            Text(
                text = "Sort by",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalBlue,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            listOf(
                "Date Desc" to "Latest First",
                "Amount Desc" to "Highest Amount",
                "Amount Asc" to "Lowest Amount",
                "Description Asc" to "A-Z Description"
            ).forEach { (opt, label) ->
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, fontSize = 14.sp)
                            if (sortOption == opt) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = RoyalBlue, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    onClick = {
                        viewModel.sortOption.value = opt
                        expanded = false
                    }
                )
            }

            Text(
                text = "Filter by Category",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalBlue,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            listOf(
                null to "Show All",
                "Food" to "Food",
                "Transport" to "Transport",
                "Utilities" to "Utilities",
                "Shopping" to "Shopping"
            ).forEach { (cat, label) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, fontSize = 14.sp)
                            if (showFilter == cat) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = RoyalBlue, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    onClick = {
                        viewModel.showCategoryFilter.value = cat
                        expanded = false
                    }
                )
            }

            DropdownMenuItem(
                text = { Text("Add advanced payment", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CoralAccent) },
                onClick = {
                    expanded = false
                    onAddAdvanced()
                }
            )
        }
    }
}

// --- 1. EXPENSES TAB SCREEN ---
@Composable
fun ExpensesTabScreen(
    viewModel: ExpenseViewModel,
    onEditExpense: (Expense) -> Unit
) {
    val expenses by viewModel.filteredExpenses.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar at the top
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search description or payer...", fontSize = 14.sp, color = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("search_bar"),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RoyalBlue,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = SurfaceLight,
                unfocusedContainerColor = SurfaceLight,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        )

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses found.",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Group expenses by relative date header
            val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val grouped = expenses.groupBy { expense ->
                val dateStr = sdf.format(Date(expense.dateEpochMillis))
                if (dateStr == todayStr) "Today" else dateStr
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                grouped.forEach { (dateHeader, itemsList) ->
                    item {
                        Text(
                            text = dateHeader,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
                        )
                    }

                    items(itemsList) { expense ->
                        ExpenseItemCard(expense = expense, onClick = { onEditExpense(expense) })
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onClick: () -> Unit
) {
    val (categoryBgColor, categoryIconColor) = when (expense.category.lowercase()) {
        "food" -> Pair(Color(0xFFE8EDFF), RoyalBlue)
        "transport" -> Pair(Color(0xFFFFF0EB), CoralAccent)
        "utilities" -> Pair(Color(0xFFE8EDFF), RoyalBlue)
        "shopping" -> Pair(Color(0xFFFFF0EB), CoralAccent)
        else -> Pair(Color(0xFFF1F3F9), TextSecondary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("expense_card_${expense.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Indicator Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(categoryBgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (expense.category.lowercase()) {
                    "food" -> Icons.Default.ShoppingBag
                    "transport" -> Icons.Default.TravelExplore
                    "utilities" -> Icons.Default.CloudSync
                    "shopping" -> Icons.Default.ShoppingBag
                    else -> Icons.Default.Category
                }
                Icon(
                    imageVector = icon,
                    contentDescription = expense.category,
                    tint = categoryIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Paid by ${expense.paidBy}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                // Bangladesh Currency Symbol ৳
                Text(
                    text = String.format(Locale.getDefault(), "%.2f ৳", expense.amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(expense.dateEpochMillis)),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}


// --- 2. BALANCE & SETTLEMENT TAB SCREEN ---
@Composable
fun BalanceTabScreen(viewModel: ExpenseViewModel) {
    val totalExpenses by viewModel.totalGroupExpenses.collectAsState()
    val balances by viewModel.participantBalances.collectAsState()
    val settlements by viewModel.settleInstructions.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()

    var summaryExpanded by remember { mutableStateOf(true) }
    var settleExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // Top summary card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Expenses: ${String.format(Locale.getDefault(), "%.2f ৳", totalExpenses)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                val dateStr = selectedGroup?.let {
                    SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(it.createdEpochMillis))
                } ?: "N/A"
                Text(
                    text = "Created $dateStr",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        // Summary Section (Expandable)
        ExpandableSection(
            title = "Summary",
            expanded = summaryExpanded,
            onToggle = { summaryExpanded = !summaryExpanded }
        ) {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) {
                balances.forEach { b ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(RoyalBlue.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = b.name.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = b.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "Charged %.2f, Paid %.2f", b.charged, b.paid),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    "%s%.2f ৳",
                                    if (b.balance >= 0) "+" else "",
                                    b.balance
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (b.balance >= 0) StatusGreen else StatusRed
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // How to settle? Section (Expandable)
        ExpandableSection(
            title = "How to settle?",
            expanded = settleExpanded,
            onToggle = { settleExpanded = !settleExpanded }
        ) {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)) {
                if (settlements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceLight, RoundedCornerShape(16.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Everyone is completely settled!",
                            fontWeight = FontWeight.Bold,
                            color = StatusGreen,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    settlements.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.debtor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = StatusRed
                                    )
                                    Text(
                                        text = "should pay to ${item.creditor}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = String.format(Locale.getDefault(), "%.2f ৳", item.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = RoyalBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalBlue
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle Section",
                tint = RoyalBlue
            )
        }
        AnimatedVisibility(visible = expanded) {
            content()
        }
    }
}


// --- 3. SHARING & EXPORTING TAB SCREEN ---
@Composable
fun ShareTabScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val totalExpenses by viewModel.totalGroupExpenses.collectAsState()
    val balances by viewModel.participantBalances.collectAsState()
    val settlements by viewModel.settleInstructions.collectAsState()
    val group by viewModel.selectedGroup.collectAsState()

    var showChartDialog by remember { mutableStateOf(false) }
    var showPDFDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 2x2 grid of action cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Share Text Action
                ShareActionCard(
                    title = "Share Summary",
                    description = "Generate text report",
                    icon = Icons.Default.Share,
                    modifier = Modifier.weight(1f)
                ) {
                    // Generate report text
                    val sb = StringBuilder()
                    sb.append("📊 *WeXpense Report: ${group?.name ?: "Summary"}*\n")
                    sb.append("Total Group Expenses: ${String.format(Locale.getDefault(), "%.2f ৳", totalExpenses)}\n\n")
                    sb.append("*Person-wise Balance:*\n")
                    balances.forEach {
                        sb.append("- ${it.name}: Paid: ${it.paid} | Charged: ${it.charged} | Balance: ${String.format(Locale.getDefault(), "%.2f", it.balance)}\n")
                    }
                    sb.append("\n*Suggested Settlements:*\n")
                    if (settlements.isEmpty()) {
                        sb.append("All settled up! 🎉\n")
                    } else {
                        settlements.forEach {
                            sb.append("- ${it.debtor} pays ${String.format(Locale.getDefault(), "%.2f ৳", it.amount)} to ${it.creditor}\n")
                        }
                    }
                    clipboard.setText(AnnotatedString(sb.toString()))
                    Toast.makeText(context, "Report summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                }

                // Chart Action
                ShareActionCard(
                    title = "Categorized",
                    description = "Expense category split",
                    icon = Icons.Default.Assessment,
                    modifier = Modifier.weight(1f)
                ) {
                    showChartDialog = true
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cloud Sync Action
                ShareActionCard(
                    title = "Cloud Sync",
                    description = "Synchronize online",
                    icon = Icons.Default.Sync,
                    modifier = Modifier.weight(1f)
                ) {
                    showSyncDialog = true
                }

                // PDF Export Action
                ShareActionCard(
                    title = "Export PDF",
                    description = "Download statements",
                    icon = Icons.Default.PictureAsPdf,
                    modifier = Modifier.weight(1f)
                ) {
                    showPDFDialog = true
                }
            }
        }
    }

    // Category Chart Dialog
    if (showChartDialog) {
        CategoryChartDialog(viewModel = viewModel, onDismiss = { showChartDialog = false })
    }

    // Export PDF Dialog
    if (showPDFDialog) {
        ExportPDFDialog(viewModel = viewModel, onDismiss = { showPDFDialog = false })
    }

    // Sync Cloud Dialog
    if (showSyncDialog) {
        SyncCloudDialog(onDismiss = { showSyncDialog = false })
    }
}

@Composable
fun ShareActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick)
            .testTag("share_card_${title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = RoyalBlue,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// category chart dialog
@Composable
fun CategoryChartDialog(viewModel: ExpenseViewModel, onDismiss: () -> Unit) {
    val expenses by viewModel.expenses.collectAsState()
    
    // Group expenses by category
    val breakdown = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    val total = breakdown.values.sum()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Category Breakdown",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (total == 0.0) {
                    Text("No expenses available for charting.", color = Color.Gray)
                } else {
                    // Renders custom visual representation
                    breakdown.forEach { (category, amount) ->
                        val ratio = if (total > 0) amount / total else 0.0
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(String.format(Locale.getDefault(), "%.2f ৳ (%.1f%%)", amount, ratio * 100), fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio.toFloat())
                                        .fillMaxHeight()
                                        .background(RoyalBlue, RoundedCornerShape(5.dp))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

// Synchronize online dialog (mock)
@Composable
fun SyncCloudDialog(onDismiss: () -> Unit) {
    var isSyncing by remember { mutableStateOf(true) }

    remember {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            isSyncing = false
        }, 1800)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(color = RoyalBlue, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Syncing with expcount.com Cloud...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = StatusGreen, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Synchronization Successful!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Your balances are up to date.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)) {
                        Text("Awesome")
                    }
                }
            }
        }
    }
}

// Export PDF Dialog representation
@Composable
fun ExportPDFDialog(viewModel: ExpenseViewModel, onDismiss: () -> Unit) {
    val group by viewModel.selectedGroup.collectAsState()
    val totalExpenses by viewModel.totalGroupExpenses.collectAsState()
    val balances by viewModel.participantBalances.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val context = LocalContext.current

    val groupMembers = group?.members ?: emptyList()
    val selectedMembers = remember(groupMembers) {
        mutableStateListOf<String>().apply { addAll(groupMembers) }
    }

    val selectedParticipantFilter = if (selectedMembers.size == groupMembers.size) {
        "All"
    } else if (selectedMembers.isEmpty()) {
        "None"
    } else {
        selectedMembers.joinToString(", ")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Monthly Statement PDF", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RoyalBlue)
                Spacer(modifier = Modifier.height(12.dp))

                // Selector for member before download: Multi-select Checkboxes
                Text("Select Members to Include:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(4.dp))
                
                // Select All / Deselect All Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (selectedMembers.size == groupMembers.size) {
                                selectedMembers.clear()
                            } else {
                                selectedMembers.clear()
                                selectedMembers.addAll(groupMembers)
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedMembers.size == groupMembers.size,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                selectedMembers.clear()
                                selectedMembers.addAll(groupMembers)
                            } else {
                                selectedMembers.clear()
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                // Grid of member checkboxes
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groupMembers.size) { index ->
                        val member = groupMembers[index]
                        val isSelected = selectedMembers.contains(member)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        selectedMembers.remove(member)
                                    } else {
                                        selectedMembers.add(member)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (!selectedMembers.contains(member)) selectedMembers.add(member)
                                    } else {
                                        selectedMembers.remove(member)
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(member, fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Formatted document simulation
                val filteredExpenses = if (selectedParticipantFilter == "All") {
                    expenses
                } else {
                    expenses.filter { it.paidBy in selectedMembers }
                }
                val filteredTotal = filteredExpenses.sumOf { it.amount }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("WEXPENSE STATEMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                            Text("CONFIDENTIAL", fontSize = 8.sp, color = StatusRed)
                        }
                        Text("Billing Period: ${group?.name}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("Generated On: 02 Aug, 2026", fontSize = 8.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray))
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text("Total Statement Value: ${formatAmount(filteredTotal)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Small table representation
                        val displayBalances = if (selectedParticipantFilter == "All") {
                            balances
                        } else {
                            balances.filter { it.name in selectedMembers }
                        }

                        displayBalances.take(3).forEach { b ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(b.name, fontSize = 9.sp)
                                Text("Paid: ${formatAmount(b.paid)} | Due: ${formatAmount(b.balance)}", fontSize = 8.sp)
                            }
                        }
                        if (displayBalances.size > 3) {
                            Text("... and ${displayBalances.size - 3} others", fontSize = 8.sp, color = Color.Gray)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val uri = generateAndSaveReportPdf(
                                context = context,
                                group = group,
                                expenses = expenses,
                                balances = balances,
                                selectedParticipant = selectedParticipantFilter,
                                showChart = true,
                                showWhoPaid = true,
                                showWhen = true,
                                showInvolves = true,
                                showSummary = true
                            )
                            if (uri != null) {
                                Toast.makeText(context, "Statement saved to Downloads folder!", Toast.LENGTH_LONG).show()
                                openReportPdf(context, uri)
                            } else {
                                Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                    ) {
                        Text("Download PDF")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}


// --- 4. WEB REPORT PREVIEW SCREEN (expcount.com) ---

// Helper to format currency
fun formatAmount(amount: Double): String {
    val formatter = java.text.DecimalFormat("#,##0.00")
    return "${formatter.format(amount)} ৳"
}

// Helper to determine who the expense involves
fun getInvolvesText(expense: Expense, allMembers: List<String>): String {
    if (expense.isAllParticipants) return "all"
    val involved = expense.splits.filter { it.isInvolved }.map { it.participantName }
    val uninvolved = expense.splits.filter { !it.isInvolved }.map { it.participantName }
    return when {
        involved.isEmpty() -> "none"
        involved.size == allMembers.size -> "all"
        uninvolved.size == 1 -> "All except ${uninvolved.first()}"
        else -> involved.joinToString(", ")
    }
}

// Save PDF Helper using modern MediaStore (Android Q+) and legacy support
fun savePdfToDownloads(context: android.content.Context, pdfDocument: android.graphics.pdf.PdfDocument, filename: String): android.net.Uri? {
    val resolver = context.contentResolver
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
    }
    
    val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    } else {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val file = java.io.File(downloadsDir, filename)
        try {
            androidx.core.content.FileProvider.getUriForFile(context, "com.example.fileprovider", file)
        } catch (e: Exception) {
            android.net.Uri.fromFile(file)
        }
    }
    
    try {
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
    return uri
}

// Generate a High-Fidelity Real PDF Document matching the visual style perfectly
fun generateAndSaveReportPdf(
    context: android.content.Context,
    group: BillingGroup?,
    expenses: List<Expense>,
    balances: List<com.example.ui.ParticipantBalance>,
    selectedParticipant: String,
    showChart: Boolean,
    showWhoPaid: Boolean,
    showWhen: Boolean,
    showInvolves: Boolean,
    showSummary: Boolean
): android.net.Uri? {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    
    val paint = android.graphics.Paint().apply { isAntiAlias = true }
    
    val titlePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#1A3B8B")
        textSize = 24f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    
    val subtitlePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#74777F")
        textSize = 10f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }

    val bodyPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#1A1C1E")
        textSize = 10f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }

    val boldBodyPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#1A1C1E")
        textSize = 10f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val tableHeaderPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#1A3B8B")
        textSize = 9f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val borderPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#E1E2EC")
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 1f
    }

    val fillHeaderPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#F1F3F9")
        style = android.graphics.Paint.Style.FILL
    }

    // Pie chart slice colors
    val colors = listOf(
        android.graphics.Color.parseColor("#1A3B8B"), // Royal Blue
        android.graphics.Color.parseColor("#FF5722"), // Coral Accent
        android.graphics.Color.parseColor("#388E3C"), // Status Green
        android.graphics.Color.parseColor("#1976D2"), // Status Blue
        android.graphics.Color.parseColor("#9C27B0"), // Purple
        android.graphics.Color.parseColor("#00BCD4")  // Cyan
    )

    val selectedList = if (selectedParticipant == "All" || selectedParticipant == "All Participants") {
        null
    } else {
        selectedParticipant.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    // Draw Title Header (Left side of top)
    val groupName = group?.name ?: "July 2026"
    val pdfTitle = if (selectedList == null) {
        groupName
    } else {
        "$groupName - ${selectedList.joinToString(", ")}"
    }
    canvas.drawText(pdfTitle, 40f, 60f, titlePaint)
    
    val createdStr = group?.let {
        "Created on ${SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date(it.createdEpochMillis))}"
    } ?: "Created on 22-Jul-2026"
    canvas.drawText(createdStr, 40f, 78f, subtitlePaint)

    var currentY = 110f

    // Draw Pie Chart on the Right top (or center top depending on visibility)
    if (showChart) {
        val chartBalances = if (selectedList == null) {
            balances.filter { it.paid > 0.0 }
        } else {
            balances.filter { it.name in selectedList && it.paid > 0.0 }
        }
        if (chartBalances.isNotEmpty()) {
            val totalPaid = chartBalances.sumOf { it.paid }
            
            val chartBoxLeft = 320f
            val chartBoxTop = 40f
            val chartBoxRight = 555f
            val chartBoxBottom = 180f
            
            // Draw background white card
            paint.color = android.graphics.Color.WHITE
            paint.style = android.graphics.Paint.Style.FILL
            canvas.drawRoundRect(android.graphics.RectF(chartBoxLeft, chartBoxTop, chartBoxRight, chartBoxBottom), 12f, 12f, paint)
            
            // Border
            canvas.drawRoundRect(android.graphics.RectF(chartBoxLeft, chartBoxTop, chartBoxRight, chartBoxBottom), 12f, 12f, borderPaint)
            
            // Header text
            val chartTitlePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#1A3B8B")
                textSize = 9f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            canvas.drawText("Participant wise expense ratio", chartBoxLeft + 15f, chartBoxTop + 20f, chartTitlePaint)
            
            // Draw Donut Chart (Center: 380, 115, Radius: 40)
            val rectF = android.graphics.RectF(340f, 65f, 420f, 145f)
            var startAngle = 0f
            chartBalances.forEachIndexed { index, b ->
                val sweepAngle = ((b.paid / totalPaid) * 360f).toFloat()
                paint.color = colors[index % colors.size]
                paint.style = android.graphics.Paint.Style.FILL
                canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
                
                // Draw thin white separators
                paint.color = android.graphics.Color.WHITE
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 1.5f
                canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
                
                startAngle += sweepAngle
            }
            
            // Draw Legend
            val legendTextPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#1A1C1E")
                textSize = 8f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            }
            var legendY = chartBoxTop + 45f
            chartBalances.forEachIndexed { index, b ->
                if (legendY < chartBoxBottom - 10f) {
                    paint.color = colors[index % colors.size]
                    paint.style = android.graphics.Paint.Style.FILL
                    canvas.drawCircle(440f, legendY - 3f, 4f, paint)
                    
                    val pct = (b.paid / totalPaid) * 100
                    val legendText = "${b.name}: ${String.format(Locale.US, "%.1f%%", pct)}"
                    canvas.drawText(legendText, 450f, legendY, legendTextPaint)
                    legendY += 15f
                }
            }
            
            currentY = 200f
        }
    }

    // Filter expenses based on selected participant
    val filteredTableExpenses = if (selectedList == null) {
        expenses
    } else {
        expenses.filter { it.paidBy in selectedList }
    }

    // Determine Columns to show in Main Table
    val columns = mutableListOf<String>()
    if (showWhoPaid) columns.add("Who Paid?")
    columns.add("For what reasons?")
    columns.add("How Much?")
    if (showWhen) columns.add("When?")
    if (showInvolves) columns.add("Involves?")

    val colCount = columns.size
    val tableLeft = 40f
    val tableWidth = 515f
    val tableRight = tableLeft + tableWidth
    
    // Draw Main Table Header
    canvas.drawText("Itemized Transactions", tableLeft, currentY - 8f, boldBodyPaint.apply { textSize = 11f })
    
    // Header Bar Background
    canvas.drawRect(tableLeft, currentY, tableRight, currentY + 24f, fillHeaderPaint)
    canvas.drawRect(tableLeft, currentY, tableRight, currentY + 24f, borderPaint)

    // Calculate column X positions
    val colWidths = FloatArray(colCount)
    var totalWeight = 0f
    columns.forEach { col ->
        totalWeight += when (col) {
            "Who Paid?" -> 1.0f
            "For what reasons?" -> 1.5f
            "How Much?" -> 1.0f
            "When?" -> 1.0f
            "Involves?" -> 1.2f
            else -> 1.0f
        }
    }
    
    var colX = tableLeft
    columns.forEachIndexed { i, col ->
        val weight = when (col) {
            "Who Paid?" -> 1.0f
            "For what reasons?" -> 1.5f
            "How Much?" -> 1.0f
            "When?" -> 1.0f
            "Involves?" -> 1.2f
            else -> 1.0f
        }
        val width = (weight / totalWeight) * tableWidth
        colWidths[i] = width
        
        canvas.drawText(col, colX + 8f, currentY + 16f, tableHeaderPaint)
        colX += width
    }

    currentY += 24f
    
    val formatter = java.text.DecimalFormat("#,##0.00")
    val allMembers = group?.members ?: emptyList()
    
    // Draw Rows
    filteredTableExpenses.forEach { exp ->
        if (currentY > 780f) {
            // Break early or keep on single page compactly
        }

        canvas.drawLine(tableLeft, currentY, tableRight, currentY, borderPaint)
        
        var x = tableLeft
        columns.forEachIndexed { i, col ->
            val text = when (col) {
                "Who Paid?" -> exp.paidBy
                "For what reasons?" -> exp.description
                "How Much?" -> "${formatter.format(exp.amount)} ৳"
                "When?" -> SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date(exp.dateEpochMillis))
                "Involves?" -> getInvolvesText(exp, allMembers)
                else -> ""
            }
            val paintToUse = if (col == "How Much?") boldBodyPaint.apply { textSize = 8.5f } else bodyPaint.apply { textSize = 8.5f }
            
            val maxWidth = colWidths[i] - 12f
            var truncatedText = text
            if (paintToUse.measureText(text) > maxWidth) {
                var len = text.length
                while (len > 0 && paintToUse.measureText(text.substring(0, len) + "...") > maxWidth) {
                    len--
                }
                truncatedText = if (len > 0) text.substring(0, len) + "..." else "..."
            }
            
            canvas.drawText(truncatedText, x + 8f, currentY + 14f, paintToUse)
            x += colWidths[i]
        }
        currentY += 20f
    }
    
    canvas.drawLine(tableLeft, currentY, tableRight, currentY, borderPaint)

    // Total display
    val sumAmount = filteredTableExpenses.sumOf { it.amount }
    val totalStr = "Total : ${formatter.format(sumAmount)} ৳"
    canvas.drawText(totalStr, tableRight - boldBodyPaint.apply { textSize = 10f }.measureText(totalStr) - 10f, currentY + 16f, boldBodyPaint)
    
    currentY += 35f

    // Draw Summary Section
    if (showSummary) {
        canvas.drawText("Summary", tableLeft, currentY - 8f, boldBodyPaint.apply { textSize = 11f })
        
        val summaryCols = listOf("Participants", "Charged", "Paid", "Due")
        val sumColWidth = tableWidth / 4f
        
        // Header
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + 20f, fillHeaderPaint)
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + 20f, borderPaint)
        
        var sx = tableLeft
        summaryCols.forEach { col ->
            canvas.drawText(col, sx + 8f, currentY + 13f, tableHeaderPaint)
            sx += sumColWidth
        }
        
        currentY += 20f
        
        val filteredBalances = if (selectedList == null) {
            balances
        } else {
            balances.filter { it.name in selectedList }
        }
        
        filteredBalances.forEach { b ->
            canvas.drawLine(tableLeft, currentY, tableRight, currentY, borderPaint)
            
            canvas.drawText(b.name, tableLeft + 8f, currentY + 13f, bodyPaint.apply { textSize = 8.5f })
            canvas.drawText("${formatter.format(b.charged)} ৳", tableLeft + sumColWidth + 8f, currentY + 13f, bodyPaint)
            canvas.drawText("${formatter.format(b.paid)} ৳", tableLeft + (sumColWidth * 2f) + 8f, currentY + 13f, bodyPaint)
            
            val duePaint = android.graphics.Paint(bodyPaint).apply {
                color = if (b.balance >= 0) android.graphics.Color.parseColor("#388E3C") else android.graphics.Color.parseColor("#D32F2F")
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textSize = 8.5f
            }
            canvas.drawText("${formatter.format(b.balance)} ৳", tableLeft + (sumColWidth * 3f) + 8f, currentY + 13f, duePaint)
            
            currentY += 18f
        }
        canvas.drawLine(tableLeft, currentY, tableRight, currentY, borderPaint)
    }

    // Draw Signature Logo
    val footerY = 810f
    val logoTextPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#1A3B8B")
        textSize = 14f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val logoSubTextPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#74777F")
        textSize = 7f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
    }
    
    val expText = "exp"
    val countText = "count"
    val logoX = tableRight - logoTextPaint.measureText(expText + countText)
    canvas.drawText(expText, logoX, footerY, logoTextPaint)
    
    val countPaint = android.graphics.Paint(logoTextPaint).apply {
        color = android.graphics.Color.parseColor("#1A1C1E")
    }
    canvas.drawText(countText, logoX + logoTextPaint.measureText(expText), footerY, countPaint)
    
    val tagline = "manage expenses better ever..."
    canvas.drawText(tagline, tableRight - logoSubTextPaint.measureText(tagline), footerY + 10f, logoSubTextPaint)

    pdfDocument.finishPage(page)
    
    val filename = "${groupName.replace(" ", "_")}_Statement.pdf"
    val uri = savePdfToDownloads(context, pdfDocument, filename)
    pdfDocument.close()
    return uri
}

// Trigger standard share sheet
fun shareReportPdf(context: android.content.Context, uri: android.net.Uri) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        putExtra(android.content.Intent.EXTRA_SUBJECT, "WeXpense Statement / Report")
        putExtra(android.content.Intent.EXTRA_TEXT, "Hello, please find attached the expense report statement generated from WeXpense.")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share PDF Statement"))
}

// Open PDF in local PDF viewer
fun openReportPdf(context: android.content.Context, uri: android.net.Uri) {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No PDF viewer app found to view the file", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun WebReportPreviewScreen(viewModel: ExpenseViewModel) {
    val totalExpenses by viewModel.totalGroupExpenses.collectAsState()
    val balances by viewModel.participantBalances.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val group by viewModel.selectedGroup.collectAsState()
    val context = LocalContext.current

    // Web report settings & visibility toggles matching screen exactly
    var selectedParticipantFilter by remember { mutableStateOf("All") }
    var participantFilterExpanded by remember { mutableStateOf(false) }
    var showChart by remember { mutableStateOf(true) }
    var showWhoPaid by remember { mutableStateOf(true) }
    var showWhen by remember { mutableStateOf(true) }
    var showInvolves by remember { mutableStateOf(true) }
    var showSummary by remember { mutableStateOf(true) }

    // Dialog control for successful PDF export
    var showPDFSuccessDialog by remember { mutableStateOf(false) }
    var lastGeneratedPdfUri by remember { mutableStateOf<android.net.Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFE9EEF4))
    ) {
        // Mock Browser Header Frame
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD6DBE1))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Browser window circle controls
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5F56), CircleShape))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFBD2E), CircleShape))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF27C93F), CircleShape))
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Address bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "https://expcount.com/report/${group?.name?.lowercase()?.replace(" ", "-") ?: "july-2026"}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Web App content frame matching screenshot
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            
            // Toolbar layout: "Select participant [All v]", "PDF [down icon]" and "Send Email"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F3F9), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select participant", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Box {
                        Row(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                .clickable { participantFilterExpanded = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(selectedParticipantFilter, fontSize = 12.sp, color = TextPrimary)
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Dropdown", modifier = Modifier.size(16.dp), tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = participantFilterExpanded,
                            onDismissRequest = { participantFilterExpanded = false },
                            modifier = Modifier.background(SurfaceLight)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All") },
                                onClick = {
                                    selectedParticipantFilter = "All"
                                    participantFilterExpanded = false
                                }
                            )
                            group?.members?.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member) },
                                    onClick = {
                                        selectedParticipantFilter = member
                                        participantFilterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val uri = generateAndSaveReportPdf(
                                context,
                                group,
                                expenses,
                                balances,
                                selectedParticipantFilter,
                                showChart,
                                showWhoPaid,
                                showWhen,
                                showInvolves,
                                showSummary
                            )
                            if (uri != null) {
                                lastGeneratedPdfUri = uri
                                showPDFSuccessDialog = true
                                Toast.makeText(context, "Statement PDF downloaded successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to generate statement PDF.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5BC0DE)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Download PDF", modifier = Modifier.size(14.dp), tint = Color.White)
                        }
                    }

                    Button(
                        onClick = {
                            val uri = generateAndSaveReportPdf(
                                context,
                                group,
                                expenses,
                                balances,
                                selectedParticipantFilter,
                                showChart,
                                showWhoPaid,
                                showWhen,
                                showInvolves,
                                showSummary
                            )
                            if (uri != null) {
                                shareReportPdf(context, uri)
                            } else {
                                Toast.makeText(context, "Please download PDF first.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5BC0DE)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Send Email", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main layout in Row: Info and Ratio Pie Chart Side by Side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Billing period and info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group?.name ?: "July 2026",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val dateStr = group?.let {
                        "Created on ${SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date(it.createdEpochMillis))}"
                    } ?: "Created on 22-Jul-2026"
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // Right Column: Pie Chart representation with Checkbox Toggle
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.End
                ) {
                    // Hide/Show Checkbox for Chart
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("(Hide/Show", fontSize = 11.sp, color = TextSecondary)
                        Checkbox(
                            checked = showChart,
                            onCheckedChange = { showChart = it },
                            modifier = Modifier.size(20.dp),
                            colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                        )
                        Text(")", fontSize = 11.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (showChart) {
                        // Participant Ratio Custom Visual Donut Chart Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Participant wise expense ratio",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val chartBalances = balances.filter { it.paid > 0.0 }
                                if (chartBalances.isEmpty()) {
                                    Text("No paid expenses to chart.", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally))
                                } else {
                                    val totalPaid = chartBalances.sumOf { it.paid }
                                    val sliceColors = listOf(RoyalBlue, CoralAccent, StatusGreen, StatusBlue, Color.Magenta, Color.Cyan)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        // Visual Pie representation
                                        Canvas(modifier = Modifier.size(60.dp)) {
                                            var startAngle = 0f
                                            chartBalances.forEachIndexed { idx, item ->
                                                val sweepAngle = ((item.paid / totalPaid) * 360f).toFloat()
                                                val color = sliceColors[idx % sliceColors.size]
                                                drawArc(
                                                    color = color,
                                                    startAngle = startAngle,
                                                    sweepAngle = sweepAngle,
                                                    useCenter = true,
                                                    size = Size(size.width, size.height)
                                                )
                                                startAngle += sweepAngle
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        // Legend inside card
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            chartBalances.forEachIndexed { idx, item ->
                                                val color = sliceColors[idx % sliceColors.size]
                                                val pct = (item.paid / totalPaid) * 100
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = String.format(Locale.US, "%s: %.1f%%", item.name, pct),
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main expenses section
            // Column Visibility inline checklist above table
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("(Hide/Show: ", fontSize = 10.sp, color = TextSecondary)
                
                Checkbox(
                    checked = showWhoPaid,
                    onCheckedChange = { showWhoPaid = it },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                )
                Text("Who Paid?", fontSize = 10.sp, color = TextPrimary)
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Checkbox(
                    checked = showWhen,
                    onCheckedChange = { showWhen = it },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                )
                Text("When?", fontSize = 10.sp, color = TextPrimary)
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Checkbox(
                    checked = showInvolves,
                    onCheckedChange = { showInvolves = it },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                )
                Text("Involves? )", fontSize = 10.sp, color = TextPrimary)
            }

            // Table 1: Itemized Transactions Table
            val filteredTableExpenses = if (selectedParticipantFilter == "All") {
                expenses
            } else {
                expenses.filter { it.paidBy == selectedParticipantFilter }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
            ) {
                // Table Headers based on visible choices
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F3F9))
                        .padding(8.dp)
                ) {
                    if (showWhoPaid) Text("Who Paid?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text("For what reasons?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1.5f))
                    Text("How Much?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                    if (showWhen) Text("When?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                    if (showInvolves) Text("Involves?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1.2f))
                }
                
                if (filteredTableExpenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No matching expenses found.", fontSize = 11.sp, color = TextSecondary)
                    }
                } else {
                    filteredTableExpenses.forEach { exp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showWhoPaid) Text(exp.paidBy, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(exp.description, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.weight(1.5f))
                            Text(formatAmount(exp.amount), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                            if (showWhen) {
                                val dStr = SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date(exp.dateEpochMillis))
                                Text(dStr, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                            }
                            if (showInvolves) {
                                val invStr = getInvolvesText(exp, group?.members ?: emptyList())
                                Text(invStr, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.weight(1.2f))
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(BorderColor))
                    }
                }
            }

            // Total section
            val totalFilteredExpenses = filteredTableExpenses.sumOf { it.amount }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp, end = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Total : ${formatAmount(totalFilteredExpenses)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }

            // Table 2: Participant Summary Table
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("Summary", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("(Hide/Show", fontSize = 10.sp, color = TextSecondary)
                Checkbox(
                    checked = showSummary,
                    onCheckedChange = { showSummary = it },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                )
                Text(")", fontSize = 10.sp, color = TextSecondary)
            }

            if (showSummary) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F3F9))
                            .padding(8.dp)
                    ) {
                        Text("Participants", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text("Charged", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text("Paid", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                        Text("Due", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                    }
                    balances.forEach { b ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(b.name, fontSize = 10.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(formatAmount(b.charged), fontSize = 10.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(formatAmount(b.paid), fontSize = 10.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(
                                text = formatAmount(b.balance),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (b.balance >= 0) StatusGreen else StatusRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(BorderColor))
                    }
                }
            }

            // Bottom controls: Preview button and expcount signature footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (lastGeneratedPdfUri != null) {
                            openReportPdf(context, lastGeneratedPdfUri!!)
                        } else {
                            // Direct preview by generating the PDF and immediately opening it!
                            val uri = generateAndSaveReportPdf(
                                context,
                                group,
                                expenses,
                                balances,
                                selectedParticipantFilter,
                                showChart,
                                showWhoPaid,
                                showWhen,
                                showInvolves,
                                showSummary
                            )
                            if (uri != null) {
                                lastGeneratedPdfUri = uri
                                openReportPdf(context, uri)
                            } else {
                                Toast.makeText(context, "Error launching preview PDF.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5BC0DE)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Preview", fontSize = 11.sp, color = Color.White)
                }

                // expcount Signature footer logo
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "exp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue
                        )
                        Text(
                            text = "count",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "manage expenses better ever...",
                        fontSize = 8.sp,
                        color = TextSecondary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }

    // High-Fidelity PDF Export Success Dialog with Sharing & Opening capabilities
    if (showPDFSuccessDialog && lastGeneratedPdfUri != null) {
        AlertDialog(
            onDismissRequest = { showPDFSuccessDialog = false },
            title = { Text("PDF Statement Exported!", fontWeight = FontWeight.Bold, color = RoyalBlue) },
            text = {
                Text(
                    text = "The billing statement has been successfully generated as a professional PDF and saved into your Downloads folder.\n\nFile Name: ${(group?.name ?: "July_2026").replace(" ", "_")}_Statement.pdf",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPDFSuccessDialog = false
                        openReportPdf(context, lastGeneratedPdfUri!!)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Open PDF")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showPDFSuccessDialog = false
                            shareReportPdf(context, lastGeneratedPdfUri!!)
                        }
                    ) {
                        Text("Share", color = RoyalBlue)
                    }
                    TextButton(
                        onClick = { showPDFSuccessDialog = false }
                    ) {
                        Text("Close", color = Color.Gray)
                    }
                }
            },
            containerColor = SurfaceLight
        )
    }
}


// --- 5. ADD GROUP DIALOG ---
@Composable
fun AddGroupDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, members: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var membersInput by remember { mutableStateOf("Sieam, Meraz, Tareq, Nehal") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Billing Period / Group", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RoyalBlue) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name (e.g., July 2026)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_group_name_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = membersInput,
                    onValueChange = { membersInput = it },
                    label = { Text("Members (comma separated list)") },
                    placeholder = { Text("e.g. Sieam, Meraz, Tareq") },
                    modifier = Modifier.fillMaxWidth().testTag("add_group_members_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val membersList = membersInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        onSave(name, desc, membersList)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = SurfaceLight
    )
}

// --- 6. ADD/EDIT EXPENSE SCREEN ---
@Composable
fun AddEditExpenseScreen(
    expense: Expense?,
    groupMembers: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        description: String,
        amount: Double,
        paidBy: String,
        dateMillis: Long,
        isAll: Boolean,
        splits: List<ParticipantSplit>,
        category: String
    ) -> Unit,
    onDelete: () -> Unit
) {
    var description by remember { mutableStateOf(expense?.description ?: "") }
    var amountStr by remember { mutableStateOf(expense?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var paidBy by remember { mutableStateOf(expense?.paidBy ?: groupMembers.firstOrNull() ?: "") }
    var category by remember { mutableStateOf(expense?.category ?: "Food") }
    var dateMillis by remember { mutableStateOf(expense?.dateEpochMillis ?: System.currentTimeMillis()) }
    var isAllParticipants by remember { mutableStateOf(expense?.isAllParticipants ?: true) }

    // Dropdowns
    var payerExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Splits state management
    val splits = remember {
        val list = mutableStateListOf<ParticipantSplit>()
        groupMembers.forEach { m ->
            val existing = expense?.splits?.find { it.participantName == m }
            list.add(
                ParticipantSplit(
                    participantName = m,
                    splitAmount = existing?.splitAmount ?: 0.0,
                    isInvolved = existing?.isInvolved ?: true
                )
            )
        }
        list
    }

    // Live update split amounts when amount or checkboxes change in equal (All) split mode
    val amount = amountStr.toDoubleOrNull() ?: 0.0
    if (isAllParticipants) {
        val involvedCount = splits.count { it.isInvolved }
        val share = if (involvedCount > 0) amount / involvedCount else 0.0
        splits.forEachIndexed { index, split ->
            splits[index] = split.copy(
                splitAmount = if (split.isInvolved) share else 0.0
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Top Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF808080)) // Gray color similar to Screenshot #3
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Cancel, contentDescription = "Cancel", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Text(
                text = if (expense == null) "ADD EXPENSE" else "EDIT EXPENSE",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
            TextButton(
                onClick = {
                    if (description.isNotBlank() && amount > 0) {
                        onSave(
                            description,
                            amount,
                            paidBy,
                            dateMillis,
                            isAllParticipants,
                            splits.toList(),
                            category
                        )
                    }
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("SAVE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Description input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("What?", modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Gray)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("e.g Food, Transport etc.") },
                    modifier = Modifier.weight(1f).testTag("expense_desc_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }

            // Payer dropdown selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Who paid?", modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Gray)
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = paidBy,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { payerExpanded = true }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Dropdown")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { payerExpanded = true }.testTag("expense_payer_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    DropdownMenu(
                        expanded = payerExpanded,
                        onDismissRequest = { payerExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.6f).background(SurfaceLight)
                    ) {
                        groupMembers.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member) },
                                onClick = {
                                    paidBy = member
                                    payerExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Amount input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("How much?", modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Gray)
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = { Text("৳", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray) },
                    modifier = Modifier.weight(1f).testTag("expense_amount_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }

            // Quick Toolbar Options Row (Attachment, Category, Date)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { }
                ) {
                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Attach", tint = RoyalBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Attachment", fontSize = 11.sp, color = Color.Gray)
                }

                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { categoryExpanded = true }
                    ) {
                        Icon(imageVector = Icons.Default.Category, contentDescription = "Category", tint = RoyalBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(category, fontSize = 11.sp, color = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(SurfaceLight)
                    ) {
                        listOf("Food", "Transport", "Utilities", "Shopping", "Other").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Picker (Mock)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { }
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Date", tint = RoyalBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Today", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Split Logic (Participant Selection Header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RoyalBlue.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isAllParticipants,
                        onCheckedChange = { isAllParticipants = it },
                        colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                    )
                    Text("All", fontWeight = FontWeight.Bold, color = RoyalBlue)
                }
                Text(
                    text = "Custom",
                    fontWeight = FontWeight.Bold,
                    color = if (!isAllParticipants) RoyalBlue else Color.Gray,
                    modifier = Modifier
                        .clickable { isAllParticipants = false }
                        .padding(8.dp)
                )
            }

            // Participant Rows list
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                splits.forEachIndexed { index, split ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = split.isInvolved,
                                onCheckedChange = { checked ->
                                    splits[index] = split.copy(isInvolved = checked)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = RoyalBlue),
                                modifier = Modifier.testTag("split_checkbox_${split.participantName}")
                            )
                            Text(split.participantName, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }

                        // Share value input/display
                        if (isAllParticipants) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f ৳", split.splitAmount),
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            // Custom amount field
                            var customAmtStr by remember(split.splitAmount) {
                                mutableStateOf(if (split.splitAmount > 0) String.format(Locale.US, "%.2f", split.splitAmount) else "")
                            }
                            OutlinedTextField(
                                value = customAmtStr,
                                onValueChange = { input ->
                                    customAmtStr = input
                                    val amt = input.toDoubleOrNull() ?: 0.0
                                    splits[index] = split.copy(splitAmount = amt)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("0.00") },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(48.dp)
                                    .testTag("split_custom_input_${split.participantName}"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalBlue,
                                    unfocusedBorderColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }

            // Delete button for edit screen
            if (expense != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("delete_expense_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Expense", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// PROFILE DIALOG
@Composable
fun ProfileDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(RoyalBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "User", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Sieam (You)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RoyalBlue)
                Text("pranggols@gmail.com", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Role", fontSize = 11.sp, color = Color.Gray)
                        Text("Admin", fontWeight = FontWeight.Bold, color = CoralAccent)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Settle Status", fontSize = 11.sp, color = Color.Gray)
                        Text("Creditor", fontWeight = FontWeight.Bold, color = StatusGreen)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)) {
                    Text("Done")
                }
            }
        }
    }
}
