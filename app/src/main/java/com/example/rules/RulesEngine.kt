package com.example.rules

import kotlin.random.Random

object RulesEngine {

    // Simple roll generator
    fun roll(dice: Int, sides: Int): Int {
        if (dice <= 0 || sides <= 0) return 0
        var total = 0
        for (i in 0 until dice) {
            total += Random.nextInt(1, sides + 1)
        }
        return total
    }

    // Parsed roll e.g. "1d8+3" or "2d6-1" or "1d20"
    fun rollFormula(formula: String): RollResult {
        val sanitized = formula.replace(" ", "").lowercase()
        // Regex to parse X d Y [+/- Z]
        val regex = Regex("""^(\d+)d(\d+)(?:([+-])(\d+))?$""")
        val match = regex.matchEntire(sanitized)

        if (match != null) {
            val dice = match.groupValues[1].toInt()
            val sides = match.groupValues[2].toInt()
            val sign = match.groupValues[3]
            val modifier = if (match.groupValues[4].isNotEmpty()) match.groupValues[4].toInt() else 0

            val rolls = mutableListOf<Int>()
            var rollTotal = 0
            for (i in 0 until dice) {
                val r = Random.nextInt(1, sides + 1)
                rolls.add(r)
                rollTotal += r
            }

            val finalModifier = if (sign == "-") -modifier else modifier
            val total = rollTotal + finalModifier

            return RollResult(
                expression = formula,
                total = total,
                rawRolls = rolls,
                modifier = finalModifier,
                textDetail = "${rolls.joinToString("+")}${if (finalModifier != 0) (if (finalModifier > 0) "+$finalModifier" else finalModifier.toString()) else ""} = $total"
            )
        } else {
            // Try simple number
            val num = sanitized.toIntOrNull() ?: 0
            return RollResult(
                expression = formula,
                total = num,
                rawRolls = listOf(num),
                modifier = 0,
                textDetail = "$num"
            )
        }
    }

    fun rollD20(modifier: Int): D20Result {
        val natural = Random.nextInt(1, 21)
        val total = natural + modifier
        val isCritHit = natural == 20
        val isCritMiss = natural == 1
        return D20Result(natural, total, modifier, isCritHit, isCritMiss)
    }

    // Combat Attack mechanics
    fun resolveAttack(
        attackerName: String,
        attackModifier: Int,
        damageFormula: String,
        targetName: String,
        targetAc: Int
    ): AttackOutcome {
        val d20 = rollD20(attackModifier)
        
        var isHit = false
        var damageText = ""
        var damageDealt = 0

        if (d20.isNaturalCritSuccess) {
            isHit = true
            // Double dice damage for crit
            val roll1 = rollFormula(damageFormula)
            val roll2 = rollFormula(damageFormula)
            // Combine totals but add modifier only once
            val bonusDamage = roll1.total + roll2.total - roll1.modifier
            damageDealt = bonusDamage
            if (damageDealt < 1) damageDealt = 1
            damageText = "CRITICAL HIT! Heavy damage: $damageDealt ($damageFormula doubled)"
        } else if (d20.isNaturalCritFailure) {
            isHit = false
            damageText = "CRITICAL MISS! Completely fumbled!"
        } else {
            isHit = d20.total >= targetAc
            if (isHit) {
                val dmgResult = rollFormula(damageFormula)
                damageDealt = dmgResult.total
                if (damageDealt < 1) damageDealt = 1
                damageText = "Hit for $damageDealt damage (${dmgResult.textDetail})"
            } else {
                damageText = "Missed (attack roll ${d20.total} vs AC $targetAc)"
            }
        }

        return AttackOutcome(
            attackerName = attackerName,
            targetName = targetName,
            naturalRoll = d20.natural,
            totalRoll = d20.total,
            isHit = isHit,
            damageDealt = damageDealt,
            detailText = "Attack roll: ${d20.natural}${if (attackModifier >= 0) "+$attackModifier" else "$attackModifier"} = ${d20.total} vs AC $targetAc. $damageText"
        )
    }

    // Ability Check or Saving Throw against DC
    fun resolveCheck(
        characterName: String,
        modifier: Int,
        dc: Int,
        checkType: String = "Check"
    ): CheckOutcome {
        val d20 = rollD20(modifier)
        val success = if (d20.isNaturalCritSuccess) true else if (d20.isNaturalCritFailure) false else d20.total >= dc
        val resultText = if (success) "SUCCESS" else "FAILURE"
        return CheckOutcome(
            characterName = characterName,
            totalRoll = d20.total,
            naturalRoll = d20.natural,
            isSuccess = success,
            detailText = "$checkType $resultText: rolled ${d20.natural}${if (modifier >= 0) "+$modifier" else "$modifier"} = ${d20.total} vs DC $dc"
        )
    }
}

data class RollResult(
    val expression: String,
    val total: Int,
    val rawRolls: List<Int>,
    val modifier: Int,
    val textDetail: String
)

data class D20Result(
    val natural: Int,
    val total: Int,
    val modifier: Int,
    val isNaturalCritSuccess: Boolean,
    val isNaturalCritFailure: Boolean
)

data class AttackOutcome(
    val attackerName: String,
    val targetName: String,
    val naturalRoll: Int,
    val totalRoll: Int,
    val isHit: Boolean,
    val damageDealt: Int,
    val detailText: String
)

data class CheckOutcome(
    val characterName: String,
    val totalRoll: Int,
    val naturalRoll: Int,
    val isSuccess: Boolean,
    val detailText: String
)
