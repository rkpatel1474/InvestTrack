package com.investtrack.data.database

import androidx.room.TypeConverter
import com.investtrack.data.database.entities.SecurityType
import com.investtrack.data.database.entities.AssetClass
import com.investtrack.data.database.entities.MFSchemeType
import com.investtrack.data.database.entities.TransactionType
import com.investtrack.data.database.entities.LoanType
import com.investtrack.data.database.entities.CouponFrequency
import com.investtrack.data.database.entities.InsuranceType
import com.investtrack.data.database.entities.Relationship

class Converters {
    @TypeConverter fun fromSecurityType(v: SecurityType) = v.name
    @TypeConverter fun toSecurityType(v: String) = SecurityType.valueOf(v)

    @TypeConverter fun fromAssetClass(v: AssetClass) = v.name
    @TypeConverter fun toAssetClass(v: String) = AssetClass.valueOf(v)

    @TypeConverter fun fromMFSchemeType(v: MFSchemeType?) = v?.name
    @TypeConverter fun toMFSchemeType(v: String?) = v?.let { MFSchemeType.valueOf(it) }

    @TypeConverter fun fromTransactionType(v: TransactionType) = v.name
    @TypeConverter fun toTransactionType(v: String) = TransactionType.valueOf(v)

    @TypeConverter fun fromLoanType(v: LoanType) = v.name
    @TypeConverter fun toLoanType(v: String) = LoanType.valueOf(v)

    @TypeConverter fun fromCouponFrequency(v: CouponFrequency?) = v?.name
    @TypeConverter fun toCouponFrequency(v: String?) = v?.let { CouponFrequency.valueOf(it) }

    @TypeConverter fun fromInsuranceType(v: InsuranceType?) = v?.name
    @TypeConverter fun toInsuranceType(v: String?) = v?.let { InsuranceType.valueOf(it) }

    @TypeConverter fun fromRelationship(v: Relationship) = v.name
    @TypeConverter fun toRelationship(v: String) = Relationship.valueOf(v)
}
