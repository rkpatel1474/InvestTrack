package com.investtrack.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────

enum class SecurityType {
    MUTUAL_FUND, SHARES, BOND, GOI_BOND, NPS, PF, FD, INSURANCE, PROPERTY, GOLD, OTHER
}

enum class AssetClass {
    EQUITY, DEBT, HYBRID, REAL_ESTATE, GOLD, CASH, COMMODITY, INTERNATIONAL, OTHER
}

enum class MFSchemeType {
    ELSS, LARGE_CAP, MID_CAP, SMALL_CAP, FLEXI_CAP, MULTI_CAP, INDEX, SECTORAL,
    SHORT_TERM, MEDIUM_TERM, LONG_TERM, LIQUID, OVERNIGHT, ARBITRAGE, HYBRID_AGGRESSIVE,
    HYBRID_CONSERVATIVE, BALANCED_ADVANTAGE, OTHER
}

enum class TransactionType {
    BUY, SELL, SIP, SWP, STP_IN, STP_OUT, INVEST, REDEEM, DIVIDEND, BONUS,
    PREMIUM, MATURITY, INTEREST, COUPON, DEPOSIT, WITHDRAWAL
}

enum class LoanType {
    HOME_LOAN, CAR_LOAN, PERSONAL_LOAN, EDUCATION_LOAN, GOLD_LOAN, BUSINESS_LOAN, OTHER
}

enum class CouponFrequency { MONTHLY, QUARTERLY, HALF_YEARLY, ANNUALLY }

enum class InsuranceType { TERM, ULIP, ENDOWMENT, MONEY_BACK, HEALTH, VEHICLE, OTHER }

enum class Relationship {
    SELF, SPOUSE, FATHER, MOTHER, SON, DAUGHTER, BROTHER, SISTER, OTHER
}

// ─────────────────────────────────────────────────────────
// Family Member
// ─────────────────────────────────────────────────────────

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: Relationship,
    val dateOfBirth: Long? = null,   // epoch millis
    val pan: String = "",
    val email: String = "",
    val phone: String = "",
    val aadhaar: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────
// Nominee
// ─────────────────────────────────────────────────────────

@Entity(
    tableName = "nominees",
    foreignKeys = [ForeignKey(
        entity = FamilyMember::class,
        parentColumns = ["id"],
        childColumns = ["familyMemberId"],
        onDelete = ForeignKey.CASCADE
    )],
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

// ─────────────────────────────────────────────────────────
// Security Master
// ─────────────────────────────────────────────────────────

@Entity(tableName = "security_master")
data class SecurityMaster(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val securityCode: String,
    val securityName: String,
    val securityType: SecurityType,
    val assetClass: AssetClass = AssetClass.EQUITY,

    // MF specific
    val schemeType: MFSchemeType? = null,
    val amcName: String = "",
    val isinCode: String = "",

    // Bond / GOI specific
    val couponRate: Double? = null,
    val couponFrequency: CouponFrequency? = null,
    val firstCouponDate: Long? = null,
    val maturityDate: Long? = null,
    val faceValue: Double? = null,

    // NPS specific
    val npsSubType: String = "",    // Tier 1 / Tier 2 / pension fund name
    val pfAccountNumber: String = "",

    // FD specific
    val interestRate: Double? = null,
    val fdTenureMonths: Int? = null,

    // Insurance specific
    val insuranceType: InsuranceType? = null,
    val premiumFrequency: CouponFrequency? = null,
    val sumAssured: Double? = null,
    val policyTerm: Int? = null,        // years
    val premiumTerm: Int? = null,       // years
    val policyNumber: String = "",
    val insurerName: String = "",

    // Property
    val propertyAddress: String = "",
    val propertyType: String = "",       // Residential, Commercial, Plot

    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────
// Transaction
// ─────────────────────────────────────────────────────────

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(SecurityMaster::class, ["id"], ["securityId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("familyMemberId"), Index("securityId")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyMemberId: Long,
    val securityId: Long,
    val transactionDate: Long,           // epoch millis
    val transactionType: TransactionType,

    // For units-based (Shares, MF, Bonds, GOI)
    val units: Double? = null,
    val price: Double? = null,           // price per unit / NAV

    // For amount-based (NPS, PF, FD, Insurance, Property)
    val amount: Double? = null,

    // Derived / editable
    val stampDuty: Double = 0.0,
    val brokerage: Double = 0.0,
    val stt: Double = 0.0,
    val otherCharges: Double = 0.0,

    // For SIP/SWP
    val sipReference: String = "",
    val folioNumber: String = "",

    // Insurance
    val premiumDueDate: Long? = null,
    val policyYear: Int? = null,

    // FD
    val maturityAmount: Double? = null,
    val fdMaturityDate: Long? = null,

    // Coupon auto-generated flag
    val isAutoCoupon: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────
// Price / NAV History
// ─────────────────────────────────────────────────────────

@Entity(
    tableName = "price_history",
    foreignKeys = [ForeignKey(SecurityMaster::class, ["id"], ["securityId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("securityId"), Index(value = ["securityId", "priceDate"], unique = true)]
)
data class PriceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val securityId: Long,
    val priceDate: Long,     // epoch millis
    val price: Double,       // NAV / market price / property value
    val source: String = "Manual"  // Manual / API
)

// ─────────────────────────────────────────────────────────
// Loan
// ─────────────────────────────────────────────────────────

@Entity(
    tableName = "loans",
    foreignKeys = [ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], onDelete = ForeignKey.CASCADE)],
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
    val interestRate: Double,           // annual %
    val tenureMonths: Int,
    val disbursementDate: Long,         // epoch millis
    val emiAmount: Double,              // user-editable
    val processingFee: Double = 0.0,
    val prepaymentCharges: Double = 0.0,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────
// Loan Payment (EMI tracking)
// ─────────────────────────────────────────────────────────

@Entity(
    tableName = "loan_payments",
    foreignKeys = [ForeignKey(Loan::class, ["id"], ["loanId"], onDelete = ForeignKey.CASCADE)],
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
    val notes: String = ""
)

// ─────────────────────────────────────────────────────────
// SIP Plan
// ─────────────────────────────────────────────────────────

@Entity(
    tableName = "sip_plans",
    foreignKeys = [
        ForeignKey(FamilyMember::class, ["id"], ["familyMemberId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(SecurityMaster::class, ["id"], ["securityId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("familyMemberId"), Index("securityId")]
)
data class SipPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyMemberId: Long,
    val securityId: Long,
    val folioNumber: String = "",
    val sipAmount: Double,
    val sipDate: Int,               // day of month 1-28
    val startDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val notes: String = ""
)
