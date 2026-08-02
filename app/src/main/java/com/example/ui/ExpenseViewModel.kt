package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BillingGroup
import com.example.data.Expense
import com.example.data.ExpenseDatabase
import com.example.data.ExpenseRepository
import com.example.data.ParticipantSplit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Balances State
data class ParticipantBalance(
    val name: String,
    val charged: Double,
    val paid: Double,
    val balance: Double
)

// Settle Instructions State
data class SettleInstruction(
    val debtor: String,
    val creditor: String,
    val amount: Double
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseRepository
    private val prefs = application.getSharedPreferences("wexpense_prefs", android.content.Context.MODE_PRIVATE)

    val selectedGroupId = MutableStateFlow<Long?>(null)
    val expenses = MutableStateFlow<List<Expense>>(emptyList())
    val userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")

    fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        userName.value = name
    }

    init {
        val database = ExpenseDatabase.getDatabase(application)
        repository = ExpenseRepository(database.expenseDao())
        
        // Prepopulate with high fidelity sandbox data if empty
        viewModelScope.launch {
            prepopulateIfEmpty()
        }

        // Collect expenses based on selectedGroupId safely with job cancellation
        var collectionJob: kotlinx.coroutines.Job? = null
        viewModelScope.launch {
            selectedGroupId.collect { id ->
                collectionJob?.cancel()
                if (id == null) {
                    expenses.value = emptyList()
                } else {
                    collectionJob = viewModelScope.launch {
                        repository.getExpensesForGroup(id).collect {
                            expenses.value = it
                        }
                    }
                }
            }
        }
    }

    // Tab state (0: Expenses, 1: Balance, 2: Share, 3: Web Preview)
    val activeTab = MutableStateFlow(0)

    // Search and Sort states
    val searchQuery = MutableStateFlow("")
    val sortOption = MutableStateFlow("Date Desc") // "Date Desc", "Amount Desc", "Amount Asc", "Description Asc"
    val showCategoryFilter = MutableStateFlow<String?>(null) // null for all

    // Groups State
    val allGroups: StateFlow<List<BillingGroup>> = repository.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Group Details
    val selectedGroup: StateFlow<BillingGroup?> = combine(allGroups, selectedGroupId) { groups, id ->
        groups.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered and Sorted Expenses
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        expenses,
        searchQuery,
        sortOption,
        showCategoryFilter
    ) { expenseList, query, sort, catFilter ->
        var list = expenseList

        // Search query
        if (query.isNotBlank()) {
            list = list.filter {
                it.description.contains(query, ignoreCase = true) ||
                it.paidBy.contains(query, ignoreCase = true)
            }
        }

        // Category filter
        if (catFilter != null) {
            list = list.filter { it.category.equals(catFilter, ignoreCase = true) }
        }

        // Sorting
        when (sort) {
            "Date Desc" -> list.sortedByDescending { it.dateEpochMillis }
            "Amount Desc" -> list.sortedByDescending { it.amount }
            "Amount Asc" -> list.sortedBy { it.amount }
            "Description Asc" -> list.sortedBy { it.description.lowercase() }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations: Total expenses for selected group
    val totalGroupExpenses: StateFlow<Double> = expenses.mapStateFlow { list ->
        list.sumOf { it.amount }
    }

    // Calculations: Per-person balances
    val participantBalances: StateFlow<List<ParticipantBalance>> = combine(
        selectedGroup,
        expenses
    ) { group, expenseList ->
        if (group == null) return@combine emptyList()

        val members = group.members
        val paidMap = members.associateWith { 0.0 }.toMutableMap()
        val chargedMap = members.associateWith { 0.0 }.toMutableMap()

        for (expense in expenseList) {
            // Payer gets credit for paying
            paidMap[expense.paidBy] = (paidMap[expense.paidBy] ?: 0.0) + expense.amount

            if (expense.isAllParticipants) {
                // Split equally among involved members
                // Find members who are checked/involved
                val involvedMembers = expense.splits.filter { it.isInvolved }.map { it.participantName }
                val targetMembers = if (involvedMembers.isEmpty()) members else involvedMembers
                val share = expense.amount / targetMembers.size.toDouble()
                for (m in targetMembers) {
                    chargedMap[m] = (chargedMap[m] ?: 0.0) + share
                }
            } else {
                // Custom split amounts
                for (split in expense.splits) {
                    if (split.isInvolved) {
                        chargedMap[split.participantName] = (chargedMap[split.participantName] ?: 0.0) + split.splitAmount
                    }
                }
            }
        }

        members.map { member ->
            val paid = paidMap[member] ?: 0.0
            val charged = chargedMap[member] ?: 0.0
            val balance = paid - charged
            ParticipantBalance(
                name = member,
                charged = charged,
                paid = paid,
                balance = balance
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settle instructions
    val settleInstructions: StateFlow<List<SettleInstruction>> = participantBalances.mapStateFlow { balances ->
        val debtors = mutableListOf<Pair<String, Double>>()
        val creditors = mutableListOf<Pair<String, Double>>()

        for (b in balances) {
            if (b.balance < -0.01) {
                debtors.add(b.name to -b.balance)
            } else if (b.balance > 0.01) {
                creditors.add(b.name to b.balance)
            }
        }

        val instructions = mutableListOf<SettleInstruction>()
        val dList = debtors.toMutableList()
        val cList = creditors.toMutableList()

        var dIdx = 0
        var cIdx = 0

        while (dIdx < dList.size && cIdx < cList.size) {
            val d = dList[dIdx]
            val c = cList[cIdx]

            if (d.second <= 0.0) {
                dIdx++
                continue
            }
            if (c.second <= 0.0) {
                cIdx++
                continue
            }

            val settleAmount = minOf(d.second, c.second)
            if (settleAmount > 0.01) {
                instructions.add(SettleInstruction(d.first, c.first, settleAmount))
            }

            dList[dIdx] = d.first to (d.second - settleAmount)
            cList[cIdx] = c.first to (c.second - settleAmount)

            if (dList[dIdx].second <= 0.01) {
                dIdx++
            }
            if (cList[cIdx].second <= 0.01) {
                cIdx++
            }
        }
        instructions
    }

    // State helper extension
    private fun <T, R> StateFlow<T>.mapStateFlow(transform: (T) -> R): StateFlow<R> {
        val mutable = MutableStateFlow(transform(this.value))
        viewModelScope.launch {
            collect {
                mutable.value = transform(it)
            }
        }
        return mutable
    }

    // Database Actions

    fun createBillingGroup(name: String, description: String, members: List<String>) {
        viewModelScope.launch {
            val group = BillingGroup(
                name = name,
                description = description.ifBlank { "No description" },
                members = members
            )
            val newId = repository.insertGroup(group)
            if (selectedGroupId.value == null) {
                selectedGroupId.value = newId
            }
        }
    }

    fun updateBillingGroup(group: BillingGroup, name: String, description: String, members: List<String>) {
        viewModelScope.launch {
            val updated = group.copy(
                name = name,
                description = description.ifBlank { "No description" },
                members = members
            )
            repository.updateGroup(updated)
        }
    }

    fun deleteBillingGroup(group: BillingGroup) {
        viewModelScope.launch {
            repository.deleteExpensesForGroup(group.id)
            repository.deleteGroup(group)
            if (selectedGroupId.value == group.id) {
                // fallback to another group or null
                val remaining = allGroups.value.filter { it.id != group.id }
                selectedGroupId.value = remaining.firstOrNull()?.id
            }
        }
    }

    fun selectGroup(groupId: Long) {
        selectedGroupId.value = groupId
    }

    fun addExpense(
        description: String,
        amount: Double,
        paidBy: String,
        dateMillis: Long,
        isAll: Boolean,
        splits: List<ParticipantSplit>,
        category: String,
        attachmentPath: String? = null,
        isAdvance: Boolean = false
    ) {
        val groupId = selectedGroupId.value ?: return
        viewModelScope.launch {
            val expense = Expense(
                groupId = groupId,
                description = description,
                amount = amount,
                paidBy = paidBy,
                dateEpochMillis = dateMillis,
                isAllParticipants = isAll,
                splits = splits,
                category = category,
                attachmentPath = attachmentPath,
                isAdvance = isAdvance
            )
            repository.insertExpense(expense)
        }
    }

    fun settleDebt(debtor: String, creditor: String, amount: Double) {
        val group = selectedGroup.value ?: return
        viewModelScope.launch {
            val splits = group.members.map { member ->
                com.example.data.ParticipantSplit(
                    participantName = member,
                    splitAmount = if (member == creditor) amount else 0.0,
                    isInvolved = member == creditor
                )
            }
            val expense = com.example.data.Expense(
                groupId = group.id,
                description = "Settled: $debtor to $creditor",
                amount = amount,
                paidBy = debtor,
                dateEpochMillis = System.currentTimeMillis(),
                isAllParticipants = false,
                splits = splits,
                category = "Settle"
            )
            repository.insertExpense(expense)
        }
    }

    fun updateExpense(
        id: Long,
        description: String,
        amount: Double,
        paidBy: String,
        dateMillis: Long,
        isAll: Boolean,
        splits: List<ParticipantSplit>,
        category: String,
        attachmentPath: String? = null,
        isAdvance: Boolean = false
    ) {
        val groupId = selectedGroupId.value ?: return
        viewModelScope.launch {
            val expense = Expense(
                id = id,
                groupId = groupId,
                description = description,
                amount = amount,
                paidBy = paidBy,
                dateEpochMillis = dateMillis,
                isAllParticipants = isAll,
                splits = splits,
                category = category,
                attachmentPath = attachmentPath,
                isAdvance = isAdvance
            )
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // Prepopulate Sandbox High-fidelity Data to match Screenshot
    private suspend fun prepopulateIfEmpty() = withContext(Dispatchers.IO) {
        // Do not prepopulate anything. Keep the database completely clean/fresh.
        val groups = repository.getAllGroupsList()
        if (groups.isNotEmpty()) {
            selectedGroupId.value = groups.first().id
        }
    }

    suspend fun exportBackupAsJsonString(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("backupVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val groupsList = repository.getAllGroupsList()
        val groupsArray = JSONArray()
        for (g in groupsList) {
            val gObj = JSONObject()
            gObj.put("id", g.id)
            gObj.put("name", g.name)
            gObj.put("description", g.description)
            gObj.put("createdEpochMillis", g.createdEpochMillis)
            
            val membersArr = JSONArray()
            for (m in g.members) {
                membersArr.put(m)
            }
            gObj.put("members", membersArr)
            groupsArray.put(gObj)
        }
        root.put("groups", groupsArray)

        val expensesList = repository.getAllExpensesList()
        val expensesArray = JSONArray()
        for (e in expensesList) {
            val eObj = JSONObject()
            eObj.put("id", e.id)
            eObj.put("groupId", e.groupId)
            eObj.put("description", e.description)
            eObj.put("amount", e.amount)
            eObj.put("paidBy", e.paidBy)
            eObj.put("dateEpochMillis", e.dateEpochMillis)
            eObj.put("isAllParticipants", e.isAllParticipants)
            eObj.put("category", e.category)
            eObj.put("isAdvance", e.isAdvance)
            eObj.put("attachmentPath", e.attachmentPath ?: JSONObject.NULL)

            val splitsArr = JSONArray()
            for (s in e.splits) {
                val sObj = JSONObject()
                sObj.put("name", s.participantName)
                sObj.put("amount", s.splitAmount)
                sObj.put("involved", s.isInvolved)
                splitsArr.put(sObj)
            }
            eObj.put("splits", splitsArr)
            expensesArray.put(eObj)
        }
        root.put("expenses", expensesArray)

        return@withContext root.toString(2)
    }

    suspend fun importBackupFromJsonString(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            
            val groupsArray = root.getJSONArray("groups")
            val groups = mutableListOf<BillingGroup>()
            for (i in 0 until groupsArray.length()) {
                val gObj = groupsArray.getJSONObject(i)
                
                val membersArr = gObj.getJSONArray("members")
                val members = mutableListOf<String>()
                for (j in 0 until membersArr.length()) {
                    members.add(membersArr.getString(j))
                }

                groups.add(
                    BillingGroup(
                        id = gObj.getLong("id"),
                        name = gObj.getString("name"),
                        description = gObj.optString("description", "No description"),
                        createdEpochMillis = gObj.optLong("createdEpochMillis", System.currentTimeMillis()),
                        members = members
                    )
                )
            }

            val expensesArray = root.getJSONArray("expenses")
            val expenses = mutableListOf<Expense>()
            for (i in 0 until expensesArray.length()) {
                val eObj = expensesArray.getJSONObject(i)
                
                val splitsArr = eObj.getJSONArray("splits")
                val splits = mutableListOf<ParticipantSplit>()
                for (j in 0 until splitsArr.length()) {
                    val sObj = splitsArr.getJSONObject(j)
                    splits.add(
                        ParticipantSplit(
                            participantName = sObj.getString("name"),
                            splitAmount = sObj.optDouble("amount", 0.0),
                            isInvolved = sObj.optBoolean("involved", true)
                        )
                    )
                }

                val attachment = if (eObj.isNull("attachmentPath")) null else eObj.optString("attachmentPath", null)

                expenses.add(
                    Expense(
                        id = eObj.getLong("id"),
                        groupId = eObj.getLong("groupId"),
                        description = eObj.getString("description"),
                        amount = eObj.getDouble("amount"),
                        paidBy = eObj.getString("paidBy"),
                        dateEpochMillis = eObj.getLong("dateEpochMillis"),
                        isAllParticipants = eObj.optBoolean("isAllParticipants", true),
                        splits = splits,
                        category = eObj.optString("category", "Other"),
                        attachmentPath = attachment,
                        isAdvance = eObj.optBoolean("isAdvance", false)
                    )
                )
            }

            if (groups.isNotEmpty()) {
                repository.restoreDatabaseBackup(groups, expenses)
                
                val lastGroupId = groups.firstOrNull()?.id
                if (lastGroupId != null) {
                    selectedGroupId.value = lastGroupId
                }
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    // Google Drive Sync states
    val isGoogleDriveConnected = MutableStateFlow(prefs.getBoolean("gdrive_connected", false))
    val googleDriveEmail = MutableStateFlow(prefs.getString("gdrive_email", "") ?: "")
    val googleDriveFolderName = MutableStateFlow(prefs.getString("gdrive_folder", "WeXpense") ?: "WeXpense")
    val googleDriveAutoSync = MutableStateFlow(prefs.getBoolean("gdrive_autosync", true))
    val googleDriveLastSync = MutableStateFlow(prefs.getString("gdrive_lastsync", "Never") ?: "Never")

    fun connectGoogleDrive(email: String) {
        prefs.edit()
            .putBoolean("gdrive_connected", true)
            .putString("gdrive_email", email)
            .putString("gdrive_lastsync", "Never")
            .apply()
        isGoogleDriveConnected.value = true
        googleDriveEmail.value = email
        googleDriveLastSync.value = "Never"
    }

    fun disconnectGoogleDrive() {
        prefs.edit()
            .putBoolean("gdrive_connected", false)
            .putString("gdrive_email", "")
            .putString("gdrive_lastsync", "Never")
            .apply()
        isGoogleDriveConnected.value = false
        googleDriveEmail.value = ""
        googleDriveLastSync.value = "Never"
    }

    fun updateGoogleDriveAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean("gdrive_autosync", enabled).apply()
        googleDriveAutoSync.value = enabled
    }

    fun triggerGoogleDriveSync(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // Simulate reading all data and serializing (takes a brief moment)
            kotlinx.coroutines.delay(1500)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault())
            val formattedDate = sdf.format(java.util.Date())
            prefs.edit().putString("gdrive_lastsync", formattedDate).apply()
            googleDriveLastSync.value = formattedDate
            onComplete()
        }
    }
}
