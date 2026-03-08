package com.investtrack.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─── Enums ────────────────────────────────────────────────────────────────────
enum class SecurityType { MUTUAL_FUND, SHARES, BOND, GOI_BOND, NPS, PF, FD, INSURANCE, PROPERTY, GOLD, CRYPTO, OTHER }
enum class AssetClass { EQUITY, DEBT, HYBRID, REAL_ESTATE, GOLD, CASH, COMMODITY, INTERNATIONAL, CRYPTO, OTHER }
enum class MFSchemeType {
    ELSS, LARGE_CAP, MID_CAP, SMALL_CAP, FLEXI_CAP, MULTI_CAP, INDEX, SECTORAL,
    SHORT_TERM, MEDIUM_TERM, LONG_TERM, LIQUID, OVERNIGHT, ARBITRAGE,
    HYBRID_AGGRESSIVE, HYBRID_CONSERVATIVE, BALANCED_ADVANTAGE, OTHER
}
enum class TransactionType {
    BUY, SELL, SIP, SWP, STP_IN, STP_OUT, INVEST, REDEEM, DIVIDEND, BONUS,
    PREMIUM, MATURITY, INTEREST, COUPON, DEPOSIT, WITHDRAWAL, SPLIT, RIGHTS
}
enum class LoanType { HOME_LOAN, CAR_LOAN, PERSONAL_LOAN, EDUCATION_LOAN, GOLD_LOAN, BUSINESS_LOAN, CREDIT_CARD, OTHER }
enum class CouponFrequency { MONTHLY, QUARTERLY, HALF_YEARLY, ANNUALLY }
enum class InsuranceType { TERM, ULIP, ENDOWMENT, MONEY_BACK, HEALTH, VEHICLE, OTHER }
enum class Relationship { SELF, SPOUSE, FATHER, MOTHER, SON, DAUGHTER, BROTHER, SISTER, OTHER }
enum class InterestType { FIXED, FLOATING }
enum class LoanAdjustment { REDUCE_EMI, REDUCE_TENURE }  // after rate change or prepayment

// ─── Family Member ────────────────────────────────────────────────────────────
@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: Relationship,
    val dateOfBirth: Long? = null,
    val pan: String = "",
    val email: String = "",
    val phone: String = "",
    val aadhaar: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Nominee ──────────────────────────────────────────────────────────────────
@Entity(
    tableName = "nominees",
    foreignKeys = [ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], ForeignKey.CASCADE)],
    indices = [Index("familyMemberId")]
)
data class Nominee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyMemberId: Long,
    val nomineeName: String,
    val relationship: Relationship,
    val dateOfBirth: Long? = null,
    val pan: String = "",
    val percentage: Double = 100.0,
    val isMinor: Boolean = false,
    val guardianName: String = ""
)

// ─── Security Master ──────────────────────────────────────────────────────────
@Entity(tableName = "security_master")
data class SecurityMaster(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val securityCode: String,
    val securityName: String,
    val securityType: SecurityType,
    val assetClass: AssetClass = AssetClass.EQUITY,
    // MF
    val schemeType: MFSchemeType? = null,
    val amcName: String = "",
    val isinCode: String = "",
    val exitLoadPercent: Double? = null,
    val expenseRatio: Double? = null,
    // Bond/GOI
    val couponRate: Double? = null,
    val couponFrequency: CouponFrequency? = null,
    val firstCouponDate: Long? = null,
    val maturityDate: Long? = null,
    val faceValue: Double? = null,
    val creditRating: String = "",
    // NPS/PF
    val npsSubType: String = "",
    val pfAccountNumber: String = "",
    val uanNumber: String = "",
    // FD
    val interestRate: Double? = null,
    val fdTenureMonths: Int? = null,
    val fdInterestType: String = "Simple",   // Simple / Compound
    val fdPayoutFrequency: CouponFrequency? = null,
    // Insurance
    val insuranceType: InsuranceType? = null,
    val premiumFrequency: CouponFrequency? = null,
    val sumAssured: Double? = null,
    val policyTerm: Int? = null,
    val premiumTerm: Int? = null,
    val policyNumber: String = "",
    val insurerName: String = "",
    // Property
    val propertyAddress: String = "",
    val propertyType: String = "",
    val carpetArea: Double? = null,          // sq ft
    val builtUpArea: Double? = null,
    // Gold
    val goldPurity: String = "",             // 24K, 22K, 18K
    val goldForm: String = "",               // Coin, Bar, Jewellery, SGB
    // Crypto
    val cryptoSymbol: String = "",
    val cryptoNetwork: String = "",

    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Transaction ──────────────────────────────────────────────────────────────
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], ForeignKey.CASCADE),
        ForeignKey(SecurityMaster::class, ["id"], ["securityId"], ForeignKey.CASCADE)
    ],
    indices = [Index("familyMemberId"), Index("securityId")]
),
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
    val nav: Double? = null,              // for MF: NAV on date
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

// ─── Price/NAV History ────────────────────────────────────────────────────────
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

// ─── Loan ─────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "loans",
    foreignKeys = [ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], ForeignKey.CASCADE)],
    indices = [Index("familyMemberId")]
)
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyMemberId: Long,
    val loanName: String,
    val loanType: LoanType,
    val lenderName: String = "",
    val accountNumber: String = "",
    val loanAmount: Double,
    val interestRate: Double,              // current/original annual %
    val interestType: InterestType = InterestType.FIXED,
    val tenureMonths: Int,
    val disbursementDate: Long,
    val emiAmount: Double,
    val emiDay: Int = 1,                   // day of month EMI is due
    val processingFee: Double = 0.0,
    val prepaymentCharges: Double = 0.0,
    val moratoriumMonths: Int = 0,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Loan Rate Change History ─────────────────────────────────────────────────
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

// ─── Loan Payment ─────────────────────────────────────────────────────────────
@Entity(
    tableName = "loan_payments",
    foreignKeys = [ForeignKey(Loan::class, ["id"], ["loanId"], ForeignKey.CASCADE)],
    indices = [Index("loanId")]
)
data class LoanPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val paymentDate: Long,
    val installmentNumber: Int,
    val emiPaid: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val outstandingBalance: Double,
    val isPrepayment: Boolean = false,
    val prepaymentAmount: Double = 0.0,
    val rateApplied: Double = 0.0,         // rate used for this installment
    val notes: String = ""
)

// ─── SIP Plan ─────────────────────────────────────────────────────────────────
@Entity(
    tableName = "sip_plans",
    foreignKeys = [
        ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], ForeignKey.CASCADE),
        ForeignKey(SecurityMaster::class, ["id"], ["securityId"], ForeignKey.CASCADE)
    ],
    indices = [Index("familyMemberId"), Index("securityId")]
)
data class SipPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyMemberId: Long,
    val securityId: Long,
    val folioNumber: String = "",
    val sipAmount: Double,
    val sipDate: Int,
    val startDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val notes: String = ""
)
