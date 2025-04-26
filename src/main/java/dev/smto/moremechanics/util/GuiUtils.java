package dev.smto.moremechanics.util;

import dev.smto.moremechanics.MoreMechanics;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GuiUtils {
    public static class Elements {
        public static final GuiElementBuilder FILLER = GuiElementBuilder
                .from(Items.WHITE_STAINED_GLASS_PANE.getDefaultStack())
                .setItemName(Text.empty())
                .setComponent(DataComponentTypes.ITEM_MODEL, Models.FILLER)
                .hideTooltip();
    }

    public static class Models {
        public static final Identifier FILLER = MoreMechanics.id("gui/clear");
        public static class Letters {
            public static final Identifier X = MoreMechanics.id("gui/letter/x");
            public static final Identifier QUESTION_MARK = MoreMechanics.id("gui/letter/question_mark");
            public static final Identifier ASTERISK = MoreMechanics.id("gui/letter/asterisk");
        }
        public static class Numbers {
            public static final Identifier ONE = MoreMechanics.id("gui/number/one");
            public static final Identifier ONE_GREEN = MoreMechanics.id("gui/number/one_green");
            public static final Identifier ONE_RED = MoreMechanics.id("gui/number/one_red");
            public static final Identifier TEN = MoreMechanics.id("gui/number/ten");
            public static final Identifier TEN_GREEN = MoreMechanics.id("gui/number/ten_green");
            public static final Identifier TEN_RED = MoreMechanics.id("gui/number/ten_red");
        }
        public static class Shapes {
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
        }
        public static class Sizes {
            public static final Identifier DECREASE = MoreMechanics.id("gui/size/decrease");
            public static final Identifier DECREASE_RED = MoreMechanics.id("gui/size/decrease_red");
            public static final Identifier INCREASE = MoreMechanics.id("gui/size/increase");
            public static final Identifier INCREASE_GREEN = MoreMechanics.id("gui/size/increase_green");
            public static final Identifier X_NEGATIVE = MoreMechanics.id("gui/size/neg_x");
            public static final Identifier Y_NEGATIVE = MoreMechanics.id("gui/size/neg_y");
            public static final Identifier Z_NEGATIVE = MoreMechanics.id("gui/size/neg_z");
            public static final Identifier X_POSITIVE = MoreMechanics.id("gui/size/pos_x");
            public static final Identifier Y_POSITIVE = MoreMechanics.id("gui/size/pos_y");
            public static final Identifier Z_POSITIVE = MoreMechanics.id("gui/size/pos_z");
        }
    }

    public static void fillEmptySlots(SlotGuiInterface gui) {
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getSlot(i) == null) {
                gui.setSlot(i, GuiUtils.Elements.FILLER);
            }
        }
    }
}
