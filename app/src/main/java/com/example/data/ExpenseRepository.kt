package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {
    fun getAllGroups(): Flow<List<BillingGroup>> = dao.getAllGroups()
    
    suspend fun getGroupById(id: Long): BillingGroup? = dao.getGroupById(id)
    
    suspend fun insertGroup(group: BillingGroup): Long = dao.insertGroup(group)
    
    suspend fun updateGroup(group: BillingGroup) = dao.updateGroup(group)
    
    suspend fun deleteGroup(group: BillingGroup) = dao.deleteGroup(group)

    fun getExpensesForGroup(groupId: Long): Flow<List<Expense>> = dao.getExpensesForGroup(groupId)
    
    suspend fun getAllGroupsList(): List<BillingGroup> = dao.getAllGroupsList()
    suspend fun getAllExpensesList(): List<Expense> = dao.getAllExpensesList()

    suspend fun restoreDatabaseBackup(groups: List<BillingGroup>, expenses: List<Expense>) {
        dao.deleteAllExpenses()
        dao.deleteAllGroups()
        for (g in groups) {
            dao.insertGroup(g)
        }
        for (e in expenses) {
            dao.insertExpense(e)
        }
    }
    
    suspend fun insertExpense(expense: Expense): Long = dao.insertExpense(expense)
    
    suspend fun updateExpense(expense: Expense) = dao.updateExpense(expense)
    
    suspend fun deleteExpense(expense: Expense) = dao.deleteExpense(expense)
}
