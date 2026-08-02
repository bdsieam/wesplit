package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

// 1. Data Models / Entities

@Entity(tableName = "billing_groups")
data class BillingGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g., "July 2026"
    val description: String = "No description",
    val createdEpochMillis: Long = System.currentTimeMillis(),
    val members: List<String> = emptyList()
)

data class ParticipantSplit(
    val participantName: String,
    val splitAmount: Double,
    val isInvolved: Boolean
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val description: String,
    val amount: Double,
    val paidBy: String,
    val dateEpochMillis: Long,
    val isAllParticipants: Boolean,
    val splits: List<ParticipantSplit>,
    val category: String = "Other",
    val attachmentPath: String? = null,
    val isAdvance: Boolean = false
)

// 2. Room Type Converters

class Converters {
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        val array = JSONArray()
        for (item in list) {
            array.put(item)
        }
        return array.toString()
    }

    @TypeConverter
    fun fromSplitListString(value: String): List<ParticipantSplit> {
        val list = mutableListOf<ParticipantSplit>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ParticipantSplit(
                        participantName = obj.optString("name", ""),
                        splitAmount = obj.optDouble("amount", 0.0),
                        isInvolved = obj.optBoolean("involved", true)
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    @TypeConverter
    fun toSplitListString(list: List<ParticipantSplit>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("name", item.participantName)
            obj.put("amount", item.splitAmount)
            obj.put("involved", item.isInvolved)
            array.put(obj)
        }
        return array.toString()
    }
}

// 3. Room DAO

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM billing_groups ORDER BY createdEpochMillis DESC")
    fun getAllGroups(): Flow<List<BillingGroup>>

    @Query("SELECT * FROM billing_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): BillingGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: BillingGroup): Long

    @Update
    suspend fun updateGroup(group: BillingGroup)

    @Delete
    suspend fun deleteGroup(group: BillingGroup)

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun deleteExpensesForGroup(groupId: Long)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY dateEpochMillis DESC, id DESC")
    fun getExpensesForGroup(groupId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM billing_groups")
    suspend fun getAllGroupsList(): List<BillingGroup>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesList(): List<Expense>

    @Query("DELETE FROM billing_groups")
    suspend fun deleteAllGroups()

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}

// 4. Room Database

@Database(entities = [BillingGroup::class, Expense::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "wexpense_database_v3"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
