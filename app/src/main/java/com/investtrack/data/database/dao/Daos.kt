package com.investtrack.data.database.dao

import androidx.room.*
import com.investtrack.data.database.entities.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────
// FamilyMember DAO
// ─────────────────────────────────────────────────────────

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members WHERE isActive = 1 ORDER BY name")
    fun getAllMembers(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: Long): FamilyMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: FamilyMember): Long

    @Update
    suspend fun update(member: FamilyMember)

    @Query("UPDATE family_members SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

// ─────────────────────────────────────────────────────────
// Nominee DAO
// ─────────────────────────────────────────────────────────

@Dao
interface NomineeDao {
    @Query("SELECT * FROM nominees WHERE familyMemberId = :memberId")
    fun getNomineesForMember(memberId: Long): Flow<List<Nominee>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(nominee: Nominee): Long

    @Update
    suspend fun update(nominee: Nominee)

    @Delete
    suspend fun delete(nominee: Nominee)

    @Query("DELETE FROM nominees WHERE familyMemberId = :memberId")
    suspend fun deleteAllForMember(memberId: Long)
}

// ─────────────────────────────────────────────────────────
// SecurityMaster DAO
// ─────────────────────────────────────────────────────────

@Dao
interface SecurityMasterDao {
    @Query("SELECT * FROM security_master WHERE isActive = 1 ORDER BY securityName")
    fun getAllSecurities(): Flow<List<SecurityMaster>>

    @Query("SELECT * FROM security_master WHERE isActive = 1 AND securityType = :type ORDER BY securityName")
    fun getSecuritiesByType(type: SecurityType): Flow<List<SecurityMaster>>

    @Query("SELECT * FROM security_master WHERE isActive = 1 AND (securityName LIKE '%' || :query || '%' OR securityCode LIKE '%' || :query || '%') ORDER BY securityName")
    suspend fun searchSecurities(query: String): List<SecurityMaster>

    @Query("SELECT * FROM security_master WHERE id = :id")
    suspend fun getSecurityById(id: Long): SecurityMaster?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(security: SecurityMaster): Long

    @Update
    suspend fun update(security: SecurityMaster)

    @Query("UPDATE security_master SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

// ─────────────────────────────────────────────────────────
// Transaction DAO
// ─────────────────────────────────────────────────────────

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE familyMemberId = :memberId ORDER BY transactionDate DESC")
    fun getTransactionsByMember(memberId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE securityId = :securityId ORDER BY transactionDate ASC")
    fun getTransactionsBySecurity(securityId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE familyMemberId = :memberId AND securityId = :securityId ORDER BY transactionDate ASC")
    suspend fun getTransactionsByMemberAndSecurity(memberId: Long, securityId: Long): List<Transaction>

    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    @Query("SELECT DISTINCT securityId FROM transactions WHERE familyMemberId = :memberId")
    suspend fun getDistinctSecurityIds(memberId: Long): List<Long>

    @Query("SELECT DISTINCT securityId FROM transactions")
    suspend fun getAllDistinctSecurityIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}

// ─────────────────────────────────────────────────────────
// PriceHistory DAO
// ─────────────────────────────────────────────────────────

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history WHERE securityId = :securityId ORDER BY priceDate DESC")
    fun getPriceHistory(securityId: Long): Flow<List<PriceHistory>>

    @Query("SELECT * FROM price_history WHERE securityId = :securityId ORDER BY priceDate DESC LIMIT 1")
    suspend fun getLatestPrice(securityId: Long): PriceHistory?

    @Query("SELECT * FROM price_history WHERE securityId = :securityId AND priceDate <= :date ORDER BY priceDate DESC LIMIT 1")
    suspend fun getPriceOnOrBefore(securityId: Long, date: Long): PriceHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(price: PriceHistory): Long

    @Query("DELETE FROM price_history WHERE securityId = :securityId AND priceDate = :date")
    suspend fun deletePrice(securityId: Long, date: Long)
}

// ─────────────────────────────────────────────────────────
// Loan DAO
// ─────────────────────────────────────────────────────────

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE isActive = 1 ORDER BY disbursementDate DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE familyMemberId = :memberId AND isActive = 1 ORDER BY disbursementDate DESC")
    fun getLoansByMember(memberId: Long): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: Loan): Long

    @Update
    suspend fun update(loan: Loan)

    @Query("UPDATE loans SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)
}

// ─────────────────────────────────────────────────────────
// LoanPayment DAO
// ─────────────────────────────────────────────────────────

@Dao
interface LoanPaymentDao {
    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paymentDate ASC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPayment>>

    @Query("SELECT COUNT(*) FROM loan_payments WHERE loanId = :loanId AND isPrepayment = 0")
    suspend fun getPaidInstallmentCount(loanId: Long): Int

    @Query("SELECT SUM(principalPaid) FROM loan_payments WHERE loanId = :loanId")
    suspend fun getTotalPrincipalPaid(loanId: Long): Double?

    @Query("SELECT SUM(interestPaid) FROM loan_payments WHERE loanId = :loanId")
    suspend fun getTotalInterestPaid(loanId: Long): Double?

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paymentDate DESC LIMIT 1")
    suspend fun getLastPayment(loanId: Long): LoanPayment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: LoanPayment): Long

    @Delete
    suspend fun delete(payment: LoanPayment)
}

// ─────────────────────────────────────────────────────────
// SipPlan DAO
// ─────────────────────────────────────────────────────────

@Dao
interface SipPlanDao {
    @Query("SELECT * FROM sip_plans WHERE isActive = 1")
    fun getActiveSipPlans(): Flow<List<SipPlan>>

    @Query("SELECT * FROM sip_plans WHERE familyMemberId = :memberId AND isActive = 1")
    fun getSipPlansByMember(memberId: Long): Flow<List<SipPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sipPlan: SipPlan): Long

    @Update
    suspend fun update(sipPlan: SipPlan)

    @Query("UPDATE sip_plans SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}
