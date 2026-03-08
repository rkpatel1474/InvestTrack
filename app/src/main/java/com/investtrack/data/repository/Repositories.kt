package com.investtrack.data.repository

import com.investtrack.data.database.dao.*
import com.investtrack.data.database.entities.*
import com.investtrack.utils.FinancialUtils
import com.investtrack.utils.DateUtils
import com.investtrack.utils.DateUtils.toDisplayDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────
// Family Repository
// ─────────────────────────────────────────────────────────

@Singleton
class FamilyRepository @Inject constructor(
    private val familyMemberDao: FamilyMemberDao,
    private val nomineeDao: NomineeDao
) {
    fun getAllMembers(): Flow<List<FamilyMember>> = familyMemberDao.getAllMembers()
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

// ─────────────────────────────────────────────────────────
// Security Repository
// ─────────────────────────────────────────────────────────

@Singleton
class SecurityRepository @Inject constructor(private val dao: SecurityMasterDao) {
    fun getAllSecurities(): Flow<List<SecurityMaster>> = dao.getAllSecurities()
    fun getSecuritiesByType(type: SecurityType) = dao.getSecuritiesByType(type)
    suspend fun searchSecurities(query: String) = dao.searchSecurities(query)
    suspend fun getSecurityById(id: Long) = dao.getSecurityById(id)
    suspend fun insertSecurity(security: SecurityMaster) = dao.insert(security)
    suspend fun updateSecurity(security: SecurityMaster) = dao.update(security)
    suspend fun deleteSecurity(id: Long) = dao.softDelete(id)
}

// ─────────────────────────────────────────────────────────
// Transaction Repository
// ─────────────────────────────────────────────────────────

@Singleton
class TransactionRepository @Inject constructor(private val dao: TransactionDao) {
    fun getAllTransactions() = dao.getAllTransactions()
    fun getTransactionsByMember(memberId: Long) = dao.getTransactionsByMember(memberId)
    fun getTransactionsBySecurity(securityId: Long) = dao.getTransactionsBySecurity(securityId)
    fun getRecentTransactions(limit: Int = 10) = dao.getRecentTransactions(limit)
    suspend fun getByMemberAndSecurity(memberId: Long, securityId: Long) =
        dao.getTransactionsByMemberAndSecurity(memberId, securityId)
    suspend fun insert(transaction: Transaction) = dao.insert(transaction)
    suspend fun update(transaction: Transaction) = dao.update(transaction)
    suspend fun delete(transaction: Transaction) = dao.delete(transaction)
    suspend fun getAllDistinctSecurityIds() = dao.getAllDistinctSecurityIds()
}

// ─────────────────────────────────────────────────────────
// Price Repository
// ─────────────────────────────────────────────────────────

@Singleton
class PriceRepository @Inject constructor(private val dao: PriceHistoryDao) {
    fun getPriceHistory(securityId: Long) = dao.getPriceHistory(securityId)
    suspend fun getLatestPrice(securityId: Long) = dao.getLatestPrice(securityId)
    suspend fun getPriceOnOrBefore(securityId: Long, date: Long) = dao.getPriceOnOrBefore(securityId, date)
    suspend fun insertPrice(price: PriceHistory) = dao.insert(price)
    suspend fun deletePrice(securityId: Long, date: Long) = dao.deletePrice(securityId, date)
}

// ─────────────────────────────────────────────────────────
// Loan Repository
// ─────────────────────────────────────────────────────────

@Singleton
class LoanRepository @Inject constructor(
    private val loanDao: LoanDao,
    private val paymentDao: LoanPaymentDao
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
}

// ─────────────────────────────────────────────────────────
// Portfolio Repository (calculation-heavy)
// ─────────────────────────────────────────────────────────

data class HoldingSummary(
    val securityId: Long,
    val securityCode: String,
    val securityName: String,
    val securityType: SecurityType,
    val assetClass: AssetClass,
    val familyMemberId: Long,
    val memberName: String,
    val totalUnits: Double,
    val avgCostPrice: Double,
    val totalCost: Double,
    val currentPrice: Double,
    val marketValue: Double,
    val unrealizedGain: Double,
    val absoluteReturn: Double,
    val xirr: Double?,
    val cagr: Double?,
    val firstPurchaseDate: Long,
    val latestPriceDate: Long?
)

data class PortfolioSummary(
    val totalCost: Double,
    val totalMarketValue: Double,
    val totalGain: Double,
    val absoluteReturn: Double,
    val xirr: Double?,
    val cagr: Double?,
    val assetClassBreakdown: Map<AssetClass, AssetClassSummary>,
    val securityTypeBreakdown: Map<SecurityType, Double>
)

data class AssetClassSummary(
    val assetClass: AssetClass,
    val totalCost: Double,
    val marketValue: Double,
    val gain: Double,
    val percentage: Double
)

@Singleton
class PortfolioRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val securityMasterDao: SecurityMasterDao
) {
    suspend fun getHoldings(familyMemberId: Long? = null): List<HoldingSummary> {
        val securityIds = if (familyMemberId != null)
            transactionDao.getAllDistinctSecurityIds()
        else
            transactionDao.getAllDistinctSecurityIds()

        val holdings = mutableListOf<HoldingSummary>()
        val now = System.currentTimeMillis()

        // When filtering by member, only get securities that member has transactions for
        val memberSecurityIds = if (familyMemberId != null)
            transactionDao.getDistinctSecurityIdsByMember(familyMemberId)
        else securityIds

        for (secId in memberSecurityIds) {
            val security = securityMasterDao.getSecurityById(secId) ?: continue
            val txns = if (familyMemberId != null)
                transactionDao.getTransactionsByMemberAndSecurity(familyMemberId, secId)
            else
                transactionDao.getTransactionsBySecurity(secId).first()

            if (txns.isEmpty()) continue

            val latestPrice = priceHistoryDao.getLatestPrice(secId)
            val currentPrice = latestPrice?.price ?: 0.0

            // Calculate for unit-based securities
            var totalUnits = 0.0
            var totalCost = 0.0
            val cashflows = mutableListOf<Pair<Long, Double>>()

            for (txn in txns) {
                when {
                    txn.transactionType in listOf(TransactionType.BUY, TransactionType.SIP, TransactionType.STP_IN, TransactionType.INVEST) -> {
                        val units = txn.units ?: 0.0
                        val price = txn.price ?: 0.0
                        val amount = txn.amount ?: (units * price)
                        totalUnits += units
                        totalCost += amount + txn.stampDuty + txn.brokerage + txn.stt + txn.otherCharges
                        cashflows.add(txn.transactionDate to -(amount + txn.stampDuty + txn.brokerage + txn.stt + txn.otherCharges))
                    }
                    txn.transactionType in listOf(TransactionType.SELL, TransactionType.SWP, TransactionType.STP_OUT, TransactionType.REDEEM) -> {
                        val units = txn.units ?: 0.0
                        val price = txn.price ?: 0.0
                        val amount = txn.amount ?: (units * price)
                        totalUnits -= units
                        totalCost -= (totalCost / (totalUnits + units)) * units
                        cashflows.add(txn.transactionDate to amount)
                    }
                    txn.transactionType in listOf(TransactionType.COUPON, TransactionType.DIVIDEND, TransactionType.INTEREST) -> {
                        cashflows.add(txn.transactionDate to (txn.amount ?: 0.0))
                    }
                }
            }

            if (totalUnits <= 0.001 && totalCost <= 0.01) continue

            val marketValue = totalUnits * currentPrice + if (totalUnits <= 0.001) totalCost else 0.0
            val finalMV = if (currentPrice == 0.0) totalCost else marketValue
            val unrealizedGain = finalMV - totalCost
            val absReturn = FinancialUtils.absoluteReturn(totalCost, finalMV)

            // Add current market value as final inflow for XIRR
            val xirrCashflows = cashflows.toMutableList()
            if (finalMV > 0) xirrCashflows.add(now to finalMV)
            val xirr = FinancialUtils.calculateXIRR(xirrCashflows)

            val firstPurchaseDate = txns.minOf { it.transactionDate }
            val years = DateUtils.yearsBetween(firstPurchaseDate, now)
            val cagr = FinancialUtils.cagr(totalCost, finalMV, years)

            holdings.add(
                HoldingSummary(
                    securityId = secId,
                    securityCode = security.securityCode,
                    securityName = security.securityName,
                    securityType = security.securityType,
                    assetClass = security.assetClass,
                    familyMemberId = familyMemberId ?: 0L,
                    memberName = "",
                    totalUnits = totalUnits,
                    avgCostPrice = if (totalUnits > 0) totalCost / totalUnits else 0.0,
                    totalCost = totalCost,
                    currentPrice = currentPrice,
                    marketValue = finalMV,
                    unrealizedGain = unrealizedGain,
                    absoluteReturn = absReturn,
                    xirr = xirr?.times(100),
                    cagr = cagr,
                    firstPurchaseDate = firstPurchaseDate,
                    latestPriceDate = latestPrice?.priceDate
                )
            )
        }
        return holdings
    }

    suspend fun getPortfolioSummary(): PortfolioSummary {
        val holdings = getHoldings()
        val totalCost = holdings.sumOf { it.totalCost }
        val totalMV = holdings.sumOf { it.marketValue }
        val totalGain = totalMV - totalCost
        val absReturn = FinancialUtils.absoluteReturn(totalCost, totalMV)

        val now = System.currentTimeMillis()
        val allCashflows = mutableListOf<Pair<Long, Double>>()
        // Simplified portfolio XIRR
        val allTxns = transactionDao.getAllTransactions().first()
        for (txn in allTxns) {
            when (txn.transactionType) {
                TransactionType.BUY, TransactionType.SIP, TransactionType.INVEST ->
                    allCashflows.add(txn.transactionDate to -(txn.amount ?: ((txn.units ?: 0.0) * (txn.price ?: 0.0))))
                TransactionType.SELL, TransactionType.REDEEM, TransactionType.SWP ->
                    allCashflows.add(txn.transactionDate to (txn.amount ?: ((txn.units ?: 0.0) * (txn.price ?: 0.0))))
                else -> {}
            }
        }
        if (totalMV > 0) allCashflows.add(now to totalMV)
        val xirr = FinancialUtils.calculateXIRR(allCashflows)

        val oldestDate = allTxns.minOfOrNull { it.transactionDate } ?: now
        val years = DateUtils.yearsBetween(oldestDate, now)
        val cagr = FinancialUtils.cagr(totalCost, totalMV, years)

        val assetBreakdown = holdings.groupBy { it.assetClass }.mapValues { (ac, h) ->
            val cost = h.sumOf { it.totalCost }
            val mv = h.sumOf { it.marketValue }
            AssetClassSummary(ac, cost, mv, mv - cost, if (totalMV > 0) mv / totalMV * 100 else 0.0)
        }
        val typeBreakdown = holdings.groupBy { it.securityType }.mapValues { (_, h) ->
            h.sumOf { it.marketValue }
        }

        return PortfolioSummary(totalCost, totalMV, totalGain, absReturn, xirr?.times(100), cagr, assetBreakdown, typeBreakdown)
    }
}
