package com.investtrack.di

import android.content.Context
import com.investtrack.data.database.AppDatabase
import com.investtrack.data.database.dao.FamilyMemberDao
import com.investtrack.data.database.dao.LoanDao
import com.investtrack.data.database.dao.LoanPaymentDao
import com.investtrack.data.database.dao.LoanRateChangeDao
import com.investtrack.data.database.dao.NomineeDao
import com.investtrack.data.database.dao.PriceHistoryDao
import com.investtrack.data.database.dao.SecurityMasterDao
import com.investtrack.data.database.dao.SipPlanDao
import com.investtrack.data.database.dao.TransactionDao
import com.investtrack.data.preferences.PreferencesManager
import com.investtrack.data.repository.FamilyRepository
import com.investtrack.data.repository.LoanRepository
import com.investtrack.data.repository.PortfolioRepository
import com.investtrack.data.repository.PriceRepository
import com.investtrack.data.repository.SecurityRepository
import com.investtrack.data.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase = AppDatabase.getInstance(ctx)

    @Provides @Singleton fun provideFamilyMemberDao(db: AppDatabase): FamilyMemberDao = db.familyMemberDao()
    @Provides @Singleton fun provideNomineeDao(db: AppDatabase): NomineeDao = db.nomineeDao()
    @Provides @Singleton fun provideSecurityMasterDao(db: AppDatabase): SecurityMasterDao = db.securityMasterDao()
    @Provides @Singleton fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides @Singleton fun providePriceHistoryDao(db: AppDatabase): PriceHistoryDao = db.priceHistoryDao()
    @Provides @Singleton fun provideLoanDao(db: AppDatabase): LoanDao = db.loanDao()
    @Provides @Singleton fun provideLoanRateChangeDao(db: AppDatabase): LoanRateChangeDao = db.loanRateChangeDao()
    @Provides @Singleton fun provideLoanPaymentDao(db: AppDatabase): LoanPaymentDao = db.loanPaymentDao()
    @Provides @Singleton fun provideSipPlanDao(db: AppDatabase): SipPlanDao = db.sipPlanDao()

    @Provides @Singleton
    fun providePreferencesManager(@ApplicationContext ctx: Context) = PreferencesManager(ctx)

    @Provides @Singleton
    fun provideFamilyRepository(fmd: FamilyMemberDao, nd: NomineeDao) = FamilyRepository(fmd, nd)

    @Provides @Singleton
    fun provideSecurityRepository(dao: SecurityMasterDao) = SecurityRepository(dao)

    @Provides @Singleton
    fun provideTransactionRepository(dao: TransactionDao) = TransactionRepository(dao)

    @Provides @Singleton
    fun providePriceRepository(dao: PriceHistoryDao) = PriceRepository(dao)

    @Provides @Singleton
    fun provideLoanRepository(ld: LoanDao, pd: LoanPaymentDao, rd: LoanRateChangeDao) = LoanRepository(ld, pd, rd)

    @Provides @Singleton
    fun providePortfolioRepository(td: TransactionDao, ph: PriceHistoryDao, sm: SecurityMasterDao) =
        PortfolioRepository(td, ph, sm)
}
