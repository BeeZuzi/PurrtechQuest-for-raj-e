package eu.purrtech.purrtechQuest.tracking;

import eu.purrtech.purrtechQuest.integration.VaultEconomyHook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

/**
 * Resolves a player's current Vault balance for EARN_MONEY objectives — "earn" here means gaining
 * {@code target} on top of the balance the player had when they accepted the quest (that starting balance
 * is snapshotted once, at acceptance, as the objective's baseline; see
 * {@code QuestService#captureEarnMoneyBaselines}), checked live on each poll (see
 * {@code QuestService#checkEarnMoneyObjectives}). "Earn" counts every rise of the balance between two checks
 * ({@link #step}) and ignores every fall, so spending money — buying something before or after selling
 * items — never eats into what was earned. Progress only ever grows.
 */
public final class EarnMoneyMatcher {

    private EarnMoneyMatcher() {
    }

    public static boolean isPresent() {
        return VaultEconomyHook.resolve() != null;
    }

    /** Only call once {@link #isPresent()} is true — there's no economy to ask otherwise. */
    public static double balanceOf(Player player) {
        return VaultEconomyHook.resolve().getBalance(player);
    }

    /** The result of one balance check: the progress to store and the balance to compare with next time. */
    public record Step(int progress, double lastSeen) {
    }

    /**
     * One balance check of an EARN_MONEY objective. {@code lastSeen} is the balance the previous check saw
     * ({@code null} for progress saved before earnings were tracked this way: it then falls back to the old
     * net gain over {@code baseline}). A rise since then adds its whole units to the progress; a fall only
     * moves {@code lastSeen} down, so money earned afterwards counts in full even when the balance is still
     * below where it was at acceptance. A fraction of a unit stays unspent in {@code lastSeen} until it adds up.
     */
    public static Step step(int previous, double balance, Double lastSeen, double baseline, int target) {
        if (lastSeen == null) {
            return new Step(nextProgress(previous, balance, baseline, target), balance);
        }
        double rise = balance - lastSeen;
        if (rise < 0) {
            return new Step(previous, balance);
        }
        long whole = (long) Math.floor(rise);
        if (whole == 0) {
            return new Step(previous, lastSeen);
        }
        int progress = (int) Math.min((long) previous + whole, target);
        return new Step(progress, lastSeen + whole);
    }

    /**
     * The progress to store after a balance check: the larger of what was already reached and what the balance
     * shows now, so spending money after earning it never lowers an objective's progress.
     */
    public static int nextProgress(int previous, double balance, double baseline, int target) {
        return Math.max(previous, clampedProgress(balance, baseline, target));
    }

    /**
     * How much of an EARN_MONEY objective's {@code target} gain is satisfied right now — {@code baseline}
     * is the player's balance at quest acceptance, so {@code balance - baseline} is what they've earned
     * since then. A net loss (balance below baseline) floors to 0 rather than counting as negative
     * progress, and a gain past the target clamps to the target rather than overshooting it.
     */
    public static int clampedProgress(double balance, double baseline, int target) {
        return (int) Math.min(Math.max(balance - baseline, 0), target);
    }
}
