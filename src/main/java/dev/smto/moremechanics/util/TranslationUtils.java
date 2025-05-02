package dev.smto.moremechanics.util;

import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class TranslationUtils {
    private static final List<String> TRANSLATION_KEYS = new ArrayList<>();

    public static void loadTranslationKeys() {
        TranslationUtils.TRANSLATION_KEYS.clear();
        try {
            TranslationUtils.TRANSLATION_KEYS.addAll(JsonParser.parseString(ResourceProvider.readFile("assets/moremechanics/lang", "en_us.json")).getAsJsonObject().keySet());
        } catch (Throwable ignored) {
            throw new RuntimeException("en_us.json could not be found/read in/from the JAR!");
        }
    }

    @Deprecated
    public static void setTooltipText(Consumer<Text> tooltip, @Nullable String flavorText, @Nullable String explanationText) {
        if(flavorText != null)
        {
            var flavorTextLines = flavorText.split("\n");
            for (var line : flavorTextLines) {
                tooltip.accept(Text.of("§" + Formatting.DARK_PURPLE.getCode() + "§" + Formatting.ITALIC.getCode() + line));
            }
        }
        if(explanationText != null)
        {
            var explanationTextLines = explanationText.split("\n");
            for (var line : explanationTextLines) {
                tooltip.accept(Text.of("§" + Formatting.GOLD.getCode() + line));
            }
        }
    }

    public static void setTooltipText(Consumer<Text> tooltip, @Nullable MutableText flavorText, @Nullable MutableText explanationText) {
        if(flavorText != null)
        {
            tooltip.accept(flavorText.formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
        }
        if(explanationText != null)
        {
            tooltip.accept(explanationText.formatted(Formatting.GOLD));
        }
    }

    public static void setTooltipText(Consumer<Text> tooltip, ItemStack stack) {
        TranslationUtils.setTooltipText(tooltip, stack.getItem().getTranslationKey());
    }

    public static void setTooltipText(Consumer<Text> tooltip, ItemStack stack, @Nullable Integer flavorTextLineCount, @Nullable Integer explanationTextCount) {
        TranslationUtils.setTooltipText(tooltip, stack.getItem().getTranslationKey(), flavorTextLineCount, explanationTextCount);
    }

    public static void setTooltipText(Consumer<Text> tooltip, String translationBaseKey) {
        int flavorTextLineCount = 0;
        int explanationTextCount = 0;

        String fKey = translationBaseKey + ".f_description.";
        while(TranslationUtils.TRANSLATION_KEYS.contains(fKey + flavorTextLineCount)) {
            flavorTextLineCount++;
        }

        String eKey = translationBaseKey + ".e_description.";
        while(TranslationUtils.TRANSLATION_KEYS.contains(eKey + explanationTextCount)) {
            explanationTextCount++;
        }

        if (explanationTextCount == 0 && flavorTextLineCount == 0) return;
        TranslationUtils.setTooltipText(tooltip, translationBaseKey, flavorTextLineCount, explanationTextCount);
    }

    public static void setTooltipText(Consumer<Text> tooltip, String translationBaseKey, @Nullable Integer flavorTextLineCount, @Nullable Integer explanationTextCount) {
        if (flavorTextLineCount != null) {
            if (flavorTextLineCount != 0) {
                for (int i = 0; i < flavorTextLineCount; i++) {
                    tooltip.accept(Text.translatable(translationBaseKey + ".f_description." + i).formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
                }
            }
        }
        if (explanationTextCount != null) {
            if (explanationTextCount != 0) {
                for (int i = 0; i < explanationTextCount; i++) {
                    tooltip.accept(Text.translatable(translationBaseKey + ".e_description." + i).formatted(Formatting.GOLD));
                }
            }
        }
    }

    public static void setTooltipText(Consumer<Text> tooltip, @Nullable List<MutableText> flavorText, @Nullable List<MutableText> explanationText) {
        if(flavorText != null)
        {
            for (MutableText mutableText : flavorText) {
                tooltip.accept(mutableText.formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
            }
        }
        if(explanationText != null)
        {
            for (MutableText mutableText : explanationText) {
                tooltip.accept(mutableText.formatted(Formatting.GOLD));
            }
        }
    }

    public static void showTitle(ServerPlayerEntity player, String text) {
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.of(text)));
    }

    public static void showSubtitle(ServerPlayerEntity player, String text) {
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.of("")));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.of(text)));
    }

    public static void showTitleWithSubtitle(ServerPlayerEntity player, String title, String subtitle) {
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.of(title)));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.of(subtitle)));
    }
}
