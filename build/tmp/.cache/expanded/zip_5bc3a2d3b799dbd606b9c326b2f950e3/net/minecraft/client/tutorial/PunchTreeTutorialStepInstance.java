package net.minecraft.client.tutorial;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PunchTreeTutorialStepInstance implements TutorialStepInstance {
    private static final int HINT_DELAY = 600;
    private static final Component TITLE = Component.translatable("tutorial.punch_tree.title");
    private static final Component DESCRIPTION = Component.translatable("tutorial.punch_tree.description", Tutorial.key("attack"));
    private final Tutorial tutorial;
    @Nullable
    private TutorialToast toast;
    private int timeWaiting;
    private int resetCount;

    public PunchTreeTutorialStepInstance(Tutorial pTutorial) {
        this.tutorial = pTutorial;
    }

    @Override
    public void tick() {
        this.timeWaiting++;
        if (!this.tutorial.isSurvival()) {
            this.tutorial.setStep(TutorialSteps.NONE);
        } else {
            Minecraft minecraft = this.tutorial.getMinecraft();
            if (this.timeWaiting == 1) {
                LocalPlayer localplayer = minecraft.player;
                if (localplayer != null) {
                    if (localplayer.getInventory().contains(ItemTags.LOGS)) {
                        this.tutorial.setStep(TutorialSteps.CRAFT_PLANKS);
                        return;
                    }

                    if (FindTreeTutorialStepInstance.hasPunchedTreesPreviously(localplayer)) {
                        this.tutorial.setStep(TutorialSteps.CRAFT_PLANKS);
                        return;
                    }
                }
            }

            if ((this.timeWaiting >= 600 || this.resetCount > 3) && this.toast == null) {
                this.toast = new TutorialToast(minecraft.font, TutorialToast.Icons.TREE, TITLE, DESCRIPTION, true);
                minecraft.getToastManager().addToast(this.toast);
            }
        }
    }

    @Override
    public void clear() {
        if (this.toast != null) {
            this.toast.hide();
            this.toast = null;
        }
    }

    @Override
    public void onDestroyBlock(ClientLevel pLevel, BlockPos pPos, BlockState pState, float pDiggingStage) {
        boolean flag = pState.is(BlockTags.LOGS);
        if (flag && pDiggingStage > 0.0F) {
            if (this.toast != null) {
                this.toast.updateProgress(pDiggingStage);
            }

            if (pDiggingStage >= 1.0F) {
                this.tutorial.setStep(TutorialSteps.OPEN_INVENTORY);
            }
        } else if (this.toast != null) {
            this.toast.updateProgress(0.0F);
        } else if (flag) {
            this.resetCount++;
        }
    }

    @Override
    public void onGetItem(ItemStack pStack) {
        if (pStack.is(ItemTags.LOGS)) {
            this.tutorial.setStep(TutorialSteps.CRAFT_PLANKS);
        }
    }
}