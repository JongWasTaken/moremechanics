package dev.smto.moremechanics.util;

import dev.smto.moremechanics.MoreMechanics;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GuiUtils {
    public static class Elements {
        public static final GuiElementBuilder FILLER = GuiElementBuilder
                .from(Items.WHITE_STAINED_GLASS_PANE.getDefaultStack())
                .setItemName(Text.empty())
                .setComponent(DataComponentTypes.ITEM_MODEL, Models.FILLER)
                .hideTooltip();
        public static final GuiElementBuilder ARROW_RIGHT = GuiElementBuilder
                .from(Items.WHITE_STAINED_GLASS_PANE.getDefaultStack())
                .setItemName(Text.empty())
                .setComponent(DataComponentTypes.ITEM_MODEL, Models.ARROW_RIGHT)
                .hideTooltip();
    }

    public static class Models {
        public static final Identifier FILLER = MoreMechanics.id("gui/clear");
        public static final Identifier ARROW_RIGHT = MoreMechanics.id("gui/arrow_right");

        public static final Identifier X = MoreMechanics.id("gui/letter/x");
        public static final Identifier AXES = MoreMechanics.id("gui/shapes/axes");
        public static final Identifier CUBOID = MoreMechanics.id("gui/shapes/cuboid");
        public static final Identifier CYLINDER = MoreMechanics.id("gui/shapes/cylinder");
        public static final Identifier DISABLED = MoreMechanics.id("gui/shapes/disable");
        public static final Identifier DOME = MoreMechanics.id("gui/shapes/dome");
        public static final Identifier FULL_CUBOID = MoreMechanics.id("gui/shapes/full_cuboid");
        public static final Identifier HEXAGON = MoreMechanics.id("gui/shapes/hexagon");
        public static final Identifier ITEM = MoreMechanics.id("gui/shapes/item");
        public static final Identifier OCTAGON = MoreMechanics.id("gui/shapes/octagon");
        public static final Identifier PENTAGON = MoreMechanics.id("gui/shapes/pentagon");
        public static final Identifier PLANES = MoreMechanics.id("gui/shapes/planes");
        public static final Identifier SELECTED = MoreMechanics.id("gui/shapes/selected");
        public static final Identifier SPHERE = MoreMechanics.id("gui/shapes/sphere");
        public static final Identifier STACK = MoreMechanics.id("gui/shapes/stack");
        public static final Identifier TRIANGLE = MoreMechanics.id("gui/shapes/triangle");
        public static final Identifier DECREASE = MoreMechanics.id("gui/size/decrease");
        public static final Identifier INCREASE = MoreMechanics.id("gui/size/increase");
        public static final Identifier X_NEGATIVE = MoreMechanics.id("gui/size/neg_x");
        public static final Identifier Y_NEGATIVE = MoreMechanics.id("gui/size/neg_y");
        public static final Identifier Z_NEGATIVE = MoreMechanics.id("gui/size/neg_z");
        public static final Identifier X_POSITIVE = MoreMechanics.id("gui/size/pos_x");
        public static final Identifier Y_POSITIVE = MoreMechanics.id("gui/size/pos_y");
        public static final Identifier Z_POSITIVE = MoreMechanics.id("gui/size/pos_z");

        public static final Identifier MAGNIFYING_GLASS = MoreMechanics.id("gui/icon/search");
        public static final Identifier QUESTION_MARK = MoreMechanics.id("gui/icon/question_mark");
        public static final Identifier PREVIOUS = MoreMechanics.id("gui/icon/previous");
        public static final Identifier NEXT = MoreMechanics.id("gui/icon/next");
        public static final Identifier PLUS = MoreMechanics.id("gui/icon/plus");
        public static final Identifier PLUS_ONE = MoreMechanics.id("gui/icon/plus_one");
        public static final Identifier PLUS_TEN = MoreMechanics.id("gui/icon/plus_ten");
        public static final Identifier MINUS = MoreMechanics.id("gui/icon/minus");
        public static final Identifier MINUS_ONE = MoreMechanics.id("gui/icon/minus_one");
        public static final Identifier MINUS_TEN = MoreMechanics.id("gui/icon/minus_ten");

    }

    public static void fillEmptySlots(SlotGuiInterface gui) {
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getSlot(i) == null) {
                gui.setSlot(i, GuiUtils.Elements.FILLER);
            }
        }
    }

    public static void playClickSound(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER,
                player.getX(), player.getY(), player.getZ(),
                1.0F, 1.0F, 0
        ));
    }
}
