package com.investtrack.di

import android.content.Context
import com.investtrack.data.database.AppDatabase
import com.investtrack.data.database.dao.*
import com.investtrack.data.repository.*
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
    @Provides @Singleton fun provideLoanPaymentDao(db: AppDatabase): LoanPaymentDao = db.loanPaymentDao()
    @Provides @Singleton fun provideSipPlanDao(db: AppDatabase): SipPlanDao = db.sipPlanDao()

    @Provides @Singleton
    fun provideFamilyRepository(familyMemberDao: FamilyMemberDao, nomineeDao: NomineeDao) =
        FamilyRepository(familyMemberDao, nomineeDao)

    @Provides @Singleton
    fun provideSecurityRepository(dao: SecurityMasterDao) = SecurityRepository(dao)

    @Provides @Singleton
    fun provideTransactionRepository(dao: TransactionDao) = TransactionRepository(dao)

    @Provides @Singleton
    fun providePriceRepository(dao: PriceHistoryDao) = PriceRepository(dao)

    @Provides @Singleton
    fun provideLoanRepository(loanDao: LoanDao, paymentDao: LoanPaymentDao) = LoanRepository(loanDao, paymentDao)

    @Provides @Singleton
    fun providePortfolioRepository(
        transactionDao: TransactionDao,
        priceHistoryDao: PriceHistoryDao,
        securityMasterDao: SecurityMasterDao
    ) = PortfolioRepository(transactionDao, priceHistoryDao, securityMasterDao)
}
