package com.investtrack.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.investtrack.data.database.dao.FamilyMemberDao
import com.investtrack.data.database.dao.LoanDao
import com.investtrack.data.database.dao.LoanPaymentDao
import com.investtrack.data.database.dao.LoanRateChangeDao
import com.investtrack.data.database.dao.NomineeDao
import com.investtrack.data.database.dao.PriceHistoryDao
import com.investtrack.data.database.dao.SecurityMasterDao
import com.investtrack.data.database.dao.SipPlanDao
import com.investtrack.data.database.dao.TransactionDao
import com.investtrack.data.database.entities.FamilyMember
import com.investtrack.data.database.entities.Loan
import com.investtrack.data.database.entities.LoanPayment
import com.investtrack.data.database.entities.LoanRateChange
import com.investtrack.data.database.entities.Nominee
import com.investtrack.data.database.entities.PriceHistory
import com.investtrack.data.database.entities.SecurityMaster
import com.investtrack.data.database.entities.SipPlan
import com.investtrack.data.database.entities.Transaction

@TypeConverters(Converters::class)
@Database(
    entities = [
        FamilyMember::class,
        Nominee::class,
        SecurityMaster::class,
        Transaction::class,
        PriceHistory::class,
        Loan::class,
        LoanRateChange::class,
        LoanPayment::class,
        SipPlan::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun nomineeDao(): NomineeDao
    abstract fun securityMasterDao(): SecurityMasterDao
    abstract fun transactionDao(): TransactionDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun loanDao(): LoanDao
    abstract fun loanRateChangeDao(): LoanRateChangeDao
    abstract fun loanPaymentDao(): LoanPaymentDao
    abstract fun sipPlanDao(): SipPlanDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS loan_rate_changes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, loanId INTEGER NOT NULL, effectiveDate INTEGER NOT NULL, newRate REAL NOT NULL, previousRate REAL NOT NULL, outstandingAtChange REAL NOT NULL, adjustment TEXT NOT NULL DEFAULT 'REDUCE_EMI', newEmi REAL, newTenure INTEGER, notes TEXT NOT NULL DEFAULT '', FOREIGN KEY(loanId) REFERENCES loans(id) ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_loan_rate_changes_loanId ON loan_rate_changes(loanId)")
                database.execSQL("ALTER TABLE security_master ADD COLUMN goldForm TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE security_master ADD COLUMN cryptoSymbol TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE security_master ADD COLUMN cryptoNetwork TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE loan_payments ADD COLUMN rateApplied REAL NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE security_master ADD COLUMN amfiSchemeCode TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE security_master ADD COLUMN yahooSymbol TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "investtrack.db"
                )
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
