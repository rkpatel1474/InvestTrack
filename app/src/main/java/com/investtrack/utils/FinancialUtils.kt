package com.investtrack.utils

import kotlin.math.abs
import kotlin.math.pow

object FinancialUtils {

    // ─── EMI Calculator ────────────────────────────────────────────────────────
    /**
     * Calculate EMI using standard formula: EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     * @param principal Loan amount
     * @param annualRate Annual interest rate in percent (e.g. 8.5)
     * @param tenureMonths Number of months
     */
    fun calculateEMI(principal: Double, annualRate: Double, tenureMonths: Int): Double {
        if (annualRate == 0.0) return principal / tenureMonths
        val r = annualRate / (12 * 100)
        val emi = principal * r * (1 + r).pow(tenureMonths.toDouble()) /
                ((1 + r).pow(tenureMonths.toDouble()) - 1)
        return emi
    }

    /**
     * Generate full amortisation schedule
     */
    fun generateAmortisationSchedule(
        principal: Double,
        annualRate: Double,
        tenureMonths: Int,
        emiAmount: Double  // user-adjusted EMI
    ): List<AmortisationRow> {
        val schedule = mutableListOf<AmortisationRow>()
        val r = annualRate / (12 * 100)
        var balance = principal

        for (i in 1..tenureMonths) {
            if (balance <= 0) break
            val interest = balance * r
            val principalPaid = minOf(emiAmount - interest, balance)
            balance -= principalPaid
            if (balance < 0.01) balance = 0.0
            schedule.add(
                AmortisationRow(
                    installment = i,
                    emi = emiAmount,
                    principal = principalPaid,
                    interest = interest,
                    balance = balance
                )
            )
        }
        return schedule
    }

    data class AmortisationRow(
        val installment: Int,
        val emi: Double,
        val principal: Double,
        val interest: Double,
        val balance: Double
    )

    // ─── XIRR ──────────────────────────────────────────────────────────────────
    /**
     * Calculate XIRR using Newton-Raphson iteration.
     * @param cashflows List of (epochMillis, amount) — negative = outflow, positive = inflow
     */
    fun calculateXIRR(cashflows: List<Pair<Long, Double>>, guess: Double = 0.1): Double? {
        if (cashflows.isEmpty()) return null
        val sorted = cashflows.sortedBy { it.first }
        val t0 = sorted.first().first

        fun xnpv(rate: Double): Double {
            return sorted.sumOf { (date, cf) ->
                val years = (date - t0) / (365.25 * 24 * 3600 * 1000.0)
                cf / (1 + rate).pow(years)
            }
        }

        fun dxnpv(rate: Double): Double {
            return sorted.sumOf { (date, cf) ->
                val years = (date - t0) / (365.25 * 24 * 3600 * 1000.0)
                -years * cf / (1 + rate).pow(years + 1)
            }
        }

        var rate = guess
        repeat(100) {
            val npv = xnpv(rate)
            val dnpv = dxnpv(rate)
            if (abs(dnpv) < 1e-12) return null
            val newRate = rate - npv / dnpv
            if (abs(newRate - rate) < 1e-6) {
                rate = newRate
                return rate
            }
            rate = newRate
            if (rate <= -1) rate = -0.99
        }
        return if (abs(xnpv(rate)) < 1.0) rate else null
    }

    // ─── Portfolio Calculations ───────────────────────────────────────────────
    fun absoluteReturn(costValue: Double, marketValue: Double): Double {
        if (costValue == 0.0) return 0.0
        return (marketValue - costValue) / costValue * 100
    }

    fun cagr(costValue: Double, marketValue: Double, years: Double): Double {
        if (costValue <= 0 || years <= 0) return 0.0
        return ((marketValue / costValue).pow(1.0 / years) - 1) * 100
    }

    // ─── Coupon Calculator ────────────────────────────────────────────────────
    fun calculateCouponAmount(faceValue: Double, couponRate: Double, frequency: com.investtrack.data.database.entities.CouponFrequency): Double {
        val periodsPerYear = when (frequency) {
            com.investtrack.data.database.entities.CouponFrequency.MONTHLY -> 12
            com.investtrack.data.database.entities.CouponFrequency.QUARTERLY -> 4
            com.investtrack.data.database.entities.CouponFrequency.HALF_YEARLY -> 2
            com.investtrack.data.database.entities.CouponFrequency.ANNUALLY -> 1
        }
        return faceValue * couponRate / 100 / periodsPerYear
    }

    fun formatCurrency(amount: Double): String {
        return when {
            amount >= 1_00_00_000 -> "₹%.2f Cr".format(amount / 1_00_00_000)
            amount >= 1_00_000 -> "₹%.2f L".format(amount / 1_00_000)
            amount >= 1_000 -> "₹%.2f K".format(amount / 1_000)
            else -> "₹%.2f".format(amount)
        }
    }

    fun formatCurrencyFull(amount: Double): String = "₹%,.2f".format(amount)

    fun formatPercent(value: Double): String = "%.2f%%".format(value)
}
