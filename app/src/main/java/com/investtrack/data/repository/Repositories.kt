package com.investtrack.data.repository

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
import com.investtrack.data.database.entities.SecurityType
import com.investtrack.data.database.entities.SipPlan
import com.investtrack.data.database.entities.Transaction
import com.investtrack.utils.FinancialUtils
import javax.inject.Inject

class FamilyRepository @Inject constructor(
    private val familyMemberDao: FamilyMemberDao,
    private val nomineeDao: NomineeDao
) {
    fun getAllMembers() = familyMemberDao.getAllMembers()
    suspend fun getMemberById(id: Long) = familyMemberDao.getMemberById(id)
    suspend fun insertMember(member: FamilyMember) = familyMemberDao.insert(member)
    suspend fun updateMember(member: FamilyMember) = familyMemberDao.update(member)
    suspend fun deleteMember(id: Long) = familyMemberDao.softDelete(id)
    fun getNomineesForMember(memberId: Long) = nomineeDao.getNomineesForMember(memberId)
    suspend fun insertNominee(nominee: Nominee) = nomineeDao.insert(nominee)
    suspend fun updateNominee(nominee: Nominee) = nomineeDao.update(nominee)
    suspend fun deleteNominee(nominee: Nominee) = nomineeDao.delete(nominee)
    suspend fun replaceNominees(memberId: Long, nominees: List<Nominee>) {
        nomineeDao.deleteAllForMember(memberId)
        nominees.forEach { nomineeDao.insert(it.copy(familyMemberId = memberId)) }
    }
}

class SecurityRepository @Inject constructor(private val dao: SecurityMasterDao) {
    fun getAllSecurities() = dao.getAllSecurities()
    fun getSecuritiesByType(type: SecurityType) = dao.getSecuritiesByType(type)
    suspend fun searchSecurities(query: String) = dao.searchSecurities(query)
    suspend fun getSecurityById(id: Long) = dao.getSecurityById(id)
    suspend fun insertSecurity(security: SecurityMaster) = dao.insert(security)
    suspend fun updateSecurity(security: SecurityMaster) = dao.update(security)
    suspend fun deleteSecurity(id: Long) = dao.softDelete(id)
}

class TransactionRepository @Inject constructor(private val dao: TransactionDao) {
    fun getAllTransactions() = dao.getAllTransactions()
    fun getTransactionsByMember(memberId: Long) = dao.getTransactionsByMember(memberId)
    fun getTransactionsBySecurity(securityId: Long) = dao.getTransactionsBySecurity(securityId)
    fun getRecentTransactions(limit: Int) = dao.getRecentTransactions(limit)
    suspend fun getById(id: Long) = dao.getTransactionById(id)
    suspend fun getByMemberAndSecurity(memberId: Long, securityId: Long) =
        dao.getTransactionsByMemberAndSecurity(memberId, securityId)
    suspend fun insert(transaction: Transaction) = dao.insert(transaction)
    suspend fun update(transaction: Transaction) = dao.update(transaction)
    suspend fun delete(transaction: Transaction) = dao.delete(transaction)
    suspend fun getAllDistinctSecurityIds() = dao.getAllDistinctSecurityIds()
}

class PriceRepository @Inject constructor(private val dao: PriceHistoryDao) {
    fun getPriceHistory(securityId: Long) = dao.getPriceHistory(securityId)
    suspend fun getLatestPrice(securityId: Long) = dao.getLatestPrice(securityId)
    suspend fun getPriceOnOrBefore(securityId: Long, date: Long) = dao.getPriceOnOrBefore(securityId, date)
    suspend fun insertPrice(price: PriceHistory) = dao.insert(price)
    suspend fun deletePrice(securityId: Long, date: Long) = dao.deletePrice(securityId, date)
}

class LoanRepository @Inject constructor(
    private val loanDao: LoanDao,
    private val paymentDao: LoanPaymentDao,
    private val rateChangeDao: LoanRateChangeDao
) {
    fun getAllLoans() = loanDao.getAllLoans()
    fun getLoansByMember(memberId: Long) = loanDao.getLoansByMember(memberId)
    suspend fun getLoanById(id: Long) = loanDao.getLoanById(id)
    suspend fun insertLoan(loan: Loan) = loanDao.insert(loan)
    suspend fun updateLoan(loan: Loan) = loanDao.update(loan)
    suspend fun deleteLoan(id: Long) = loanDao.softDelete(id)

    fun getPayments(loanId: Long) = paymentDao.getPaymentsForLoan(loanId)
    suspend fun insertPayment(payment: LoanPayment) = paymentDao.insert(payment)
    suspend fun deletePayment(payment: LoanPayment) = paymentDao.delete(payment)
    suspend fun getPaidInstallments(loanId: Long) = paymentDao.getPaidInstallmentCount(loanId)
    suspend fun getTotalPrincipalPaid(loanId: Long) = paymentDao.getTotalPrincipalPaid(loanId) ?: 0.0
    suspend fun getTotalInterestPaid(loanId: Long) = paymentDao.getTotalInterestPaid(loanId) ?: 0.0
    suspend fun getLastPayment(loanId: Long) = paymentDao.getLastPayment(loanId)

    fun getRateChanges(loanId: Long) = rateChangeDao.getRateChanges(loanId)
    suspend fun insertRateChange(change: LoanRateChange) = rateChangeDao.insert(change)
    suspend fun deleteRateChange(change: LoanRateChange) = rateChangeDao.delete(change)
}

// ─── Portfolio Summary ────────────────────────────────────────────────────────
data class HoldingSummary(
    val security: SecurityMaster,
    val totalUnits: Double,
    val totalCost: Double,
    val averagePrice: Double,
    val marketValue: Double,
    val gain: Double,
    val gainPercent: Double,
    val xirr: Double?
)

data class PortfolioSummary(
    val totalCost: Double,
    val totalMarketValue: Double,
    val totalGain: Double,
    val gainPercent: Double,
    val totalLoans: Double,
    val netWorth: Double,
    val holdings: List<HoldingSummary>
)

class PortfolioRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val securityMasterDao: SecurityMasterDao
) {
    suspend fun getHoldings(familyMemberId: Long? = null): List<HoldingSummary> {
        val allSecurityIds = transactionDao.getAllDistinctSecurityIds()
        return allSecurityIds.mapNotNull { secId ->
            val security = securityMasterDao.getSecurityById(secId) ?: return@mapNotNull null
            val txns = if (familyMemberId != null)
                transactionDao.getTransactionsByMemberAndSecurity(familyMemberId, secId)
            else
                transactionDao.getTransactionsByMemberAndSecurity(0, secId).let {
                    transactionDao.getAllDistinctSecurityIds()
                    transactionDao.getTransactionsByMemberAndSecurity(familyMemberId ?: 0, secId)
                }

            val allTxns = transactionDao.getTransactionsByMemberAndSecurity(
                familyMemberId ?: -1, secId
            ).takeIf { familyMemberId != null }
                ?: run {
                    val ids = transactionDao.getAllDistinctSecurityIds()
                    // get all txns for this security regardless of member
                    val result = mutableListOf<Transaction>()
                    // simplified: just use all transactions for security
                    result
                }

            // Get all transactions for this security
            val secTxns = transactionDao.getTransactionsByMemberAndSecurity(0, secId)
            val latestPrice = priceHistoryDao.getLatestPrice(secId)?.price

            // Calculate holdings
            var units = 0.0
            var cost = 0.0
            for (t in secTxns) {
                val isBuy = t.transactionType.name in listOf("BUY", "SIP", "INVEST", "DEPOSIT", "PREMIUM", "BONUS")
                val isSell = t.transactionType.name in listOf("SELL", "REDEEM", "WITHDRAWAL", "SWP")
                val tUnits = t.units ?: 0.0
                val tCost = t.amount ?: ((t.units ?: 0.0) * (t.price ?: 0.0))
                if (isBuy) { units += tUnits; cost += tCost }
                if (isSell) { units -= tUnits; cost -= tCost }
            }
            if (units <= 0.001 && cost <= 0.0) return@mapNotNull null

            val mv = if (latestPrice != null && units > 0) units * latestPrice else cost
            val gain = mv - cost
            val gainPct = if (cost > 0) (gain / cost) * 100 else 0.0

            HoldingSummary(
                security = security,
                totalUnits = units,
                totalCost = cost,
                averagePrice = if (units > 0) cost / units else 0.0,
                marketValue = mv,
                gain = gain,
                gainPercent = gainPct,
                xirr = null
            )
        }
    }

    suspend fun getPortfolioSummary(): PortfolioSummary {
        val holdings = getHoldings()
        val totalCost = holdings.sumOf { it.totalCost }
        val totalMV = holdings.sumOf { it.marketValue }
        val totalGain = totalMV - totalCost
        val gainPct = if (totalCost > 0) (totalGain / totalCost) * 100 else 0.0
        return PortfolioSummary(
            totalCost = totalCost,
            totalMarketValue = totalMV,
            totalGain = totalGain,
            gainPercent = gainPct,
            totalLoans = 0.0,
            netWorth = totalMV,
            holdings = holdings
        )
    }
}
