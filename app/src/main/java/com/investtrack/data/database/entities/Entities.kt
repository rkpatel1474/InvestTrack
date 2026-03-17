package com.investtrack.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ── Enums ─────────────────────────────────────────────────────────────────────
enum class SecurityType { MUTUAL_FUND, SHARES, BOND, GOI_BOND, NPS, PF, FD, INSURANCE, PROPERTY, GOLD, CRYPTO, OTHER }
enum class AssetClass { EQUITY, DEBT, HYBRID, REAL_ESTATE, GOLD, CASH, COMMODITY, OTHER }
enum class MFSchemeType { ELSS, LARGE_CAP, MID_CAP, SMALL_CAP, FLEXI_CAP, MULTI_CAP, INDEX, SECTORAL, SHORT_TERM, MEDIUM_TERM, LONG_TERM, LIQUID, OVERNIGHT, ARBITRAGE, HYBRID_AGGRESSIVE, HYBRID_CONSERVATIVE, BALANCED_ADVANTAGE, OTHER }
enum class TransactionType { BUY, SELL, SIP, SWP, STP_IN, STP_OUT, INVEST, REDEEM, DIVIDEND, BONUS, PREMIUM, MATURITY, INTEREST, COUPON, DEPOSIT, WITHDRAWAL, SPLIT, RIGHTS }
enum class LoanType { HOME_LOAN, CAR_LOAN, PERSONAL_LOAN, EDUCATION_LOAN, GOLD_LOAN, BUSINESS_LOAN, CREDIT_CARD, OTHER }
enum class CouponFrequency { MONTHLY, QUARTERLY, HALF_YEARLY, ANNUALLY }
enum class InsuranceType { TERM, ULIP, ENDOWMENT, MONEY_BACK, HEALTH, VEHICLE, OTHER }
enum class Relationship { SELF, SPOUSE, FATHER, MOTHER, SON, DAUGHTER, BROTHER, SISTER, OTHER }
enum class InterestType { FIXED, FLOATING }
enum class LoanAdjustment { REDUCE_EMI, REDUCE_TENURE }

// ── Family Member ─────────────────────────────────────────────────────────────
@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: Relationship = Relationship.SELF,
    val dateOfBirth: Long? = null,
    val panNumber: String = "",
    val aadhaarNumber: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Nominee ───────────────────────────────────────────────────────────────────
@Entity(
    tableName = "nominees",
    foreignKeys = [ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], ForeignKey.CASCADE)],
    indices = [Index("familyMemberId")]
)
data class Nominee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyMemberId: Long,
    val nomineeName: String,
    val relationship: Relationship = Relationship.OTHER,
    val percentage: Double = 100.0
)

// ── Security Master ───────────────────────────────────────────────────────────
@Entity(tableName = "security_master")
data class SecurityMaster(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val securityCode: String = "",
    val securityName: String,
    val securityType: SecurityType,
    val assetClass: AssetClass,
    // Auto price update identifiers
    // - Mutual Funds: AMFI scheme code (used to fetch NAV from AMFI)
    // - Shares: Yahoo symbol (e.g., RELIANCE.NS) for quote fetch
    val amfiSchemeCode: String = "",
    val yahooSymbol: String = "",
    val mfSchemeType: MFSchemeType? = null,
    val couponRate: Double? = null,
    val couponFrequency: CouponFrequency? = null,
    val maturityDate: Long? = null,
    val faceValue: Double? = null,
    val insuranceType: InsuranceType? = null,
    val sumAssured: Double? = null,
    val premiumAmount: Double? = null,
    val isin: String = "",
    val exchange: String = "",
    val sector: String = "",
    val goldForm: String = "",
    val cryptoSymbol: String = "",
    val cryptoNetwork: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Transaction ───────────────────────────────────────────────────────────────
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], ForeignKey.CASCADE),
        ForeignKey(SecurityMaster::class, ["id"], ["securityId"], ForeignKey.CASCADE)
    ],
    indices = [Index("familyMemberId"), Index("securityId")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyMemberId: Long,
    val securityId: Long,
    val transactionDate: Long,
    val transactionType: TransactionType,
    val units: Double? = null,
    val price: Double? = null,
    val amount: Double? = null,
    val nav: Double? = null,
    val stampDuty: Double = 0.0,
    val brokerage: Double = 0.0,
    val stt: Double = 0.0,
    val otherCharges: Double = 0.0,
    val gst: Double = 0.0,
    val sipReference: String = "",
    val folioNumber: String = "",
    val premiumDueDate: Long? = null,
    val policyYear: Int? = null,
    val maturityAmount: Double? = null,
    val fdMaturityDate: Long? = null,
    val isAutoCoupon: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ── Price History ─────────────────────────────────────────────────────────────
@Entity(
    tableName = "price_history",
    foreignKeys = [ForeignKey(SecurityMaster::class, ["id"], ["securityId"], ForeignKey.CASCADE)],
    indices = [Index("securityId"), Index(value = ["securityId", "priceDate"], unique = true)]
)
data class PriceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val securityId: Long,
    val priceDate: Long,
    val price: Double,
    val source: String = "Manual"
)

// ── Loan ──────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "loans",
    foreignKeys = [ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], ForeignKey.CASCADE)],
    indices = [Index("familyMemberId")]
)
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyMemberId: Long,
    val loanName: String,
    val loanType: LoanType = LoanType.OTHER,
    val lenderName: String = "",
    val accountNumber: String = "",
    val loanAmount: Double,
    val interestRate: Double,
    val tenureMonths: Int,
    val disbursementDate: Long,
    val emiAmount: Double,
    val processingFee: Double = 0.0,
    val notes: String = "",
    val isActive: Boolean = true
)

// ── Loan Rate Change ──────────────────────────────────────────────────────────
@Entity(
    tableName = "loan_rate_changes",
    foreignKeys = [ForeignKey(Loan::class, ["id"], ["loanId"], ForeignKey.CASCADE)],
    indices = [Index("loanId")]
)
data class LoanRateChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val effectiveDate: Long,
    val newRate: Double,
    val previousRate: Double,
    val outstandingAtChange: Double,
    val adjustment: LoanAdjustment = LoanAdjustment.REDUCE_EMI,
    val newEmi: Double? = null,
    val newTenure: Int? = null,
    val notes: String = ""
)

// ── Loan Payment ──────────────────────────────────────────────────────────────
@Entity(
    tableName = "loan_payments",
    foreignKeys = [ForeignKey(Loan::class, ["id"], ["loanId"], ForeignKey.CASCADE)],
    indices = [Index("loanId")]
)
data class LoanPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val paymentDate: Long,
    val installmentNumber: Int = 0,
    val emiPaid: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val outstandingBalance: Double,
    val isPrepayment: Boolean = false,
    val rateApplied: Double = 0.0
)

// ── SIP Plan ──────────────────────────────────────────────────────────────────
@Entity(
    tableName = "sip_plans",
    foreignKeys = [ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], ForeignKey.CASCADE)],
    indices = [Index("familyMemberId")]
)
data class SipPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyMemberId: Long,
    val securityId: Long,
    val sipAmount: Double,
    val sipDate: Int,
    val startDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = true
)
