package net.minecraft.recipebook;

import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

public interface PlaceRecipeHelper {
    static <T> void placeRecipe(int pWidth, int pHeight, Recipe<?> pRecipe, Iterable<T> pIngredients, PlaceRecipeHelper.Output<T> pOutput) {
        if (pRecipe instanceof ShapedRecipe shapedrecipe) {
            placeRecipe(pWidth, pHeight, shapedrecipe.getWidth(), shapedrecipe.getHeight(), pIngredients, pOutput);
        } else {
            placeRecipe(pWidth, pHeight, pWidth, pHeight, pIngredients, pOutput);
        }
    }

    static <T> void placeRecipe(int pGridWidth, int pGridHeight, int pWidth, int pHeight, Iterable<T> pIngredients, PlaceRecipeHelper.Output<T> pOutput) {
        Iterator<T> iterator = pIngredients.iterator();
        int i = 0;

        for (int j = 0; j < pGridHeight; j++) {
            boolean flag = (float)pHeight < (float)pGridHeight / 2.0F;
            int k = Mth.floor((float)pGridHeight / 2.0F - (float)pHeight / 2.0F);
            if (flag && k > j) {
                i += pGridWidth;
                j++;
            }

            for (int l = 0; l < pGridWidth; l++) {
                if (!iterator.hasNext()) {
                    return;
                }

                flag = (float)pWidth < (float)pGridWidth / 2.0F;
                k = Mth.floor((float)pGridWidth / 2.0F - (float)pWidth / 2.0F);
                int i1 = pWidth;
                boolean flag1 = l < pWidth;
                if (flag) {
                    i1 = k + pWidth;
                    flag1 = k <= l && l < k + pWidth;
                }

                if (flag1) {
                    pOutput.addItemToSlot(iterator.next(), i, l, j);
                } else if (i1 == l) {
                    i += pGridWidth - l;
                    break;
                }

                i++;
            }
        }
    }

    @FunctionalInterface
    public interface Output<T> {
        void addItemToSlot(T pItem, int pSlot, int pX, int pY);
    }
}