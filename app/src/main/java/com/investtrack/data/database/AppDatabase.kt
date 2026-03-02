package com.investtrack.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.investtrack.data.database.dao.*
import com.investtrack.data.database.entities.*

@Database(
    entities = [
        FamilyMember::class,
        Nominee::class,
        SecurityMaster::class,
        Transaction::class,
        PriceHistory::class,
        Loan::class,
        LoanPayment::class,
        SipPlan::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun nomineeDao(): NomineeDao
    abstract fun securityMasterDao(): SecurityMasterDao
    abstract fun transactionDao(): TransactionDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun loanDao(): LoanDao
    abstract fun loanPaymentDao(): LoanPaymentDao
    abstract fun sipPlanDao(): SipPlanDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "investtrack.db"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
