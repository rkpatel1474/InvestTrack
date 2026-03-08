package com.investtrack.data.database

import androidx.room.TypeConverter
import com.investtrack.data.database.entities.AssetClass
import com.investtrack.data.database.entities.CouponFrequency
import com.investtrack.data.database.entities.InsuranceType
import com.investtrack.data.database.entities.InterestType
import com.investtrack.data.database.entities.LoanAdjustment
import com.investtrack.data.database.entities.LoanType
import com.investtrack.data.database.entities.MFSchemeType
import com.investtrack.data.database.entities.Relationship
import com.investtrack.data.database.entities.SecurityType
import com.investtrack.data.database.entities.TransactionType

class Converters {
    @TypeConverter fun fromSecurityType(v: SecurityType): String = v.name
    @TypeConverter fun toSecurityType(v: String): SecurityType = SecurityType.valueOf(v)
    @TypeConverter fun fromAssetClass(v: AssetClass): String = v.name
    @TypeConverter fun toAssetClass(v: String): AssetClass = AssetClass.valueOf(v)
    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)
    @TypeConverter fun fromLoanType(v: LoanType): String = v.name
    @TypeConverter fun toLoanType(v: String): LoanType = LoanType.valueOf(v)
    @TypeConverter fun fromRelationship(v: Relationship): String = v.name
    @TypeConverter fun toRelationship(v: String): Relationship = Relationship.valueOf(v)
    @TypeConverter fun fromMFSchemeType(v: MFSchemeType?): String? = v?.name
    @TypeConverter fun toMFSchemeType(v: String?): MFSchemeType? = v?.let { MFSchemeType.valueOf(it) }
    @TypeConverter fun fromCouponFrequency(v: CouponFrequency?): String? = v?.name
    @TypeConverter fun toCouponFrequency(v: String?): CouponFrequency? = v?.let { CouponFrequency.valueOf(it) }
    @TypeConverter fun fromInsuranceType(v: InsuranceType?): String? = v?.name
    @TypeConverter fun toInsuranceType(v: String?): InsuranceType? = v?.let { InsuranceType.valueOf(it) }
    @TypeConverter fun fromInterestType(v: InterestType?): String? = v?.name
    @TypeConverter fun toInterestType(v: String?): InterestType? = v?.let { InterestType.valueOf(it) }
    @TypeConverter fun fromLoanAdjustment(v: LoanAdjustment?): String? = v?.name
    @TypeConverter fun toLoanAdjustment(v: String?): LoanAdjustment? = v?.let { LoanAdjustment.valueOf(it) }
}
