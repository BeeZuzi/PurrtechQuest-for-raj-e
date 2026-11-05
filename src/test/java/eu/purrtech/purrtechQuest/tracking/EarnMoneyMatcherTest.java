package eu.purrtech.purrtechQuest.tracking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link EarnMoneyMatcher#balanceOf}/{@link EarnMoneyMatcher#isPresent} need a live Vault/Bukkit instance
 * (same limitation as everywhere else in this test suite), so this exercises {@link
 * EarnMoneyMatcher#clampedProgress} directly — the pure part that turns a live balance and an
 * acceptance-time baseline into an EARN_MONEY objective's progress value.
 */
class EarnMoneyMatcherTest {

    @Test
    void belowTargetProgressesTowardsIt() {
        // Baseline 1000, balance 1650 — 650 earned towards a 1000 target.
        assertEquals(650, EarnMoneyMatcher.clampedProgress(1650.0, 1000.0, 1000));
    }

    @Test
    void atOrAboveTargetClampsToTheTarget() {
        assertEquals(1000, EarnMoneyMatcher.clampedProgress(2000.0, 1000.0, 1000));
        assertEquals(1000, EarnMoneyMatcher.clampedProgress(6000.0, 1000.0, 1000));
    }

    @Test
    void zeroBaselineBehavesAsAnAbsoluteBalanceCheck() {
        // A player who never had a baseline recorded (e.g. pre-existing progress from before this feature)
        // falls back to baseline 0 — earning from nothing is the same as reaching the raw balance.
        assertEquals(650, EarnMoneyMatcher.clampedProgress(650.0, 0.0, 1000));
    }

    @Test
    void netLossSinceBaselineFloorsToZeroRatherThanNegativeProgress() {
        assertEquals(0, EarnMoneyMatcher.clampedProgress(750.0, 1000.0, 1000));
    }

    @Test
    void theBalanceGainAloneFallsWhenMoneyIsSpent() {
        int atTarget = EarnMoneyMatcher.clampedProgress(2000.0, 1000.0, 1000);
        int afterSpending = EarnMoneyMatcher.clampedProgress(1400.0, 1000.0, 1000);
        assertEquals(1000, atTarget);
        assertEquals(400, afterSpending);
    }

    @Test
    void storedProgressNeverFallsWhenMoneyIsSpentAfterEarningIt() {
        // Sell items for 70 (objective of 50 reached), then spend: the objective stays reached.
        int reached = EarnMoneyMatcher.nextProgress(0, 1070.0, 1000.0, 50);
        assertEquals(50, reached);
        assertEquals(50, EarnMoneyMatcher.nextProgress(reached, 1010.0, 1000.0, 50));
        assertEquals(50, EarnMoneyMatcher.nextProgress(reached, 900.0, 1000.0, 50));
    }

    @Test
    void storedProgressStillClimbsAsMoreIsEarned() {
        assertEquals(30, EarnMoneyMatcher.nextProgress(0, 1030.0, 1000.0, 50));
        assertEquals(45, EarnMoneyMatcher.nextProgress(30, 1045.0, 1000.0, 50));
        assertEquals(30, EarnMoneyMatcher.nextProgress(30, 1020.0, 1000.0, 50));
    }

    @Test
    void fractionalGainTruncatesRatherThanRounds() {
        // Whole-number progress, same "good enough" precision tradeoff as SPEND_MONEY.
        assertEquals(650, EarnMoneyMatcher.clampedProgress(1650.9, 1000.0, 1000));
    }

    @Test
    void moneyEarnedAfterSpendingCountsEvenWhileTheBalanceIsStillBelowWhereItStarted() {
        // The case from a real server: accepted at 369 710, bought things down to 365 005, then sold for 187.
        var afterSpending = EarnMoneyMatcher.step(0, 365005.5, 369710.5, 369710.5, 50);
        assertEquals(0, afterSpending.progress());
        assertEquals(365005.5, afterSpending.lastSeen());

        var afterSelling = EarnMoneyMatcher.step(afterSpending.progress(), 365192.5, afterSpending.lastSeen(), 369710.5, 50);
        assertEquals(50, afterSelling.progress());
    }

    @Test
    void earningMoreThanTheTargetCompletesIt() {
        assertEquals(50, EarnMoneyMatcher.step(0, 1070.0, 1000.0, 1000.0, 50).progress());
    }

    @Test
    void spendingNeverLowersProgressAndEarningResumesFromTheLowerBalance() {
        var earned = EarnMoneyMatcher.step(0, 1030.0, 1000.0, 1000.0, 50);
        assertEquals(30, earned.progress());

        var spent = EarnMoneyMatcher.step(earned.progress(), 900.0, earned.lastSeen(), 1000.0, 50);
        assertEquals(30, spent.progress());

        var earnedAgain = EarnMoneyMatcher.step(spent.progress(), 915.0, spent.lastSeen(), 1000.0, 50);
        assertEquals(45, earnedAgain.progress());
    }

    @Test
    void fractionsOfAUnitAddUpAcrossChecks() {
        var first = EarnMoneyMatcher.step(0, 1000.6, 1000.0, 1000.0, 50);
        assertEquals(0, first.progress());
        var second = EarnMoneyMatcher.step(first.progress(), 1001.2, first.lastSeen(), 1000.0, 50);
        assertEquals(1, second.progress());
    }

    @Test
    void progressSavedBeforeThisTrackingFallsBackToTheOldNetGain() {
        var migrated = EarnMoneyMatcher.step(10, 1040.0, null, 1000.0, 50);
        assertEquals(40, migrated.progress());
        assertEquals(1040.0, migrated.lastSeen());
    }
}
