package dev.smto.moremechanics.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;

public class ExperienceUtils {
    public static int getBarCapacityAtLevel(int level) {
        if (level >= 30) {
            return 112 + ((level - 30) * 9);
        } else if (level >= 15) {
            return 37 + ((level - 15) * 5);
        } else if (level < 0) {
            return 0;
        } else {
            return 7 + (level * 2);
        }
    }

    public static Pair<Integer, Float> getLevelsAndPoints(int amount) {
        int level = 0;
        float exp = amount / (float) ExperienceUtils.getBarCapacityAtLevel(level);
        while (exp >= 1.0F) {
            exp = (exp - 1) * ExperienceUtils.getBarCapacityAtLevel(level);
            level += 1;
            exp = exp / ExperienceUtils.getBarCapacityAtLevel(level);
        }
        return new Pair<>(level, exp);
    }

    public static int getTotalPoints(int level, float exp) {
        int points = 0;
        for (int i = 0; i < level; i++) {
            points += ExperienceUtils.getBarCapacityAtLevel(i);
        }
        points += Math.round(ExperienceUtils.getBarCapacityAtLevel(level) * exp);
        return points;
    }

    private static void addLevelsToPlayer(PlayerEntity player, int levels) {
        int newV = ExperienceUtils.getTotalPoints(player.experienceLevel + levels, 0);
        int oldV = ExperienceUtils.getTotalPoints(player.experienceLevel, player.experienceProgress);
        int i = newV - oldV;
        player.addExperience(i);
        if (Math.round(player.experienceProgress) == 1) {
            player.addExperience(i);
        }
    }
}
