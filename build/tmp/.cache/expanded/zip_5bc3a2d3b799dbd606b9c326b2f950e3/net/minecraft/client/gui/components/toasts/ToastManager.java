package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.Queues;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ToastManager {
    private static final int SLOT_COUNT = 5;
    private static final int ALL_SLOTS_OCCUPIED = -1;
    final Minecraft minecraft;
    private final List<ToastManager.ToastInstance<?>> visibleToasts = new ArrayList<>();
    private final BitSet occupiedSlots = new BitSet(5);
    private final Deque<Toast> queued = Queues.newArrayDeque();

    public ToastManager(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    public void update() {
        this.visibleToasts.removeIf(p_362712_ -> {
            p_362712_.update();
            if (p_362712_.hasFinishedRendering()) {
                this.occupiedSlots.clear(p_362712_.firstSlotIndex, p_362712_.firstSlotIndex + p_362712_.occupiedSlotCount);
                return true;
            } else {
                return false;
            }
        });
        if (!this.queued.isEmpty() && this.freeSlotCount() > 0) {
            this.queued.removeIf(p_365698_ -> {
                int i = p_365698_.occcupiedSlotCount();
                int j = this.findFreeSlotsIndex(i);
                if (j == -1) {
                    return false;
                } else {
                    this.visibleToasts.add(new ToastManager.ToastInstance<>(p_365698_, j, i));
                    this.occupiedSlots.set(j, j + i);
                    return true;
                }
            });
        }
    }

    public void render(GuiGraphics pGuiGraphics) {
        if (!this.minecraft.options.hideGui) {
            int i = pGuiGraphics.guiWidth();

            for (ToastManager.ToastInstance<?> toastinstance : this.visibleToasts) {
                toastinstance.render(pGuiGraphics, i);
            }
        }
    }

    private int findFreeSlotsIndex(int pSlots) {
        if (this.freeSlotCount() >= pSlots) {
            int i = 0;

            for (int j = 0; j < 5; j++) {
                if (this.occupiedSlots.get(j)) {
                    i = 0;
                } else if (++i == pSlots) {
                    return j + 1 - i;
                }
            }
        }

        return -1;
    }

    private int freeSlotCount() {
        return 5 - this.occupiedSlots.cardinality();
    }

    @Nullable
    public <T extends Toast> T getToast(Class<? extends T> pToastClass, Object pToken) {
        for (ToastManager.ToastInstance<?> toastinstance : this.visibleToasts) {
            if (toastinstance != null
                && pToastClass.isAssignableFrom(toastinstance.getToast().getClass())
                && toastinstance.getToast().getToken().equals(pToken)) {
                return (T)toastinstance.getToast();
            }
        }

        for (Toast toast : this.queued) {
            if (pToastClass.isAssignableFrom(toast.getClass()) && toast.getToken().equals(pToken)) {
                return (T)toast;
            }
        }

        return null;
    }

    public void clear() {
        this.occupiedSlots.clear();
        this.visibleToasts.clear();
        this.queued.clear();
    }

    public void addToast(Toast pToast) {
        if (net.minecraftforge.client.event.ForgeEventFactoryClient.onToastAdd(pToast)) return;
        this.queued.add(pToast);
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    public double getNotificationDisplayTimeMultiplier() {
        return this.minecraft.options.notificationDisplayTime().get();
    }

    @OnlyIn(Dist.CLIENT)
    class ToastInstance<T extends Toast> {
        private static final long SLIDE_ANIMATION_DURATION_MS = 600L;
        private final T toast;
        final int firstSlotIndex;
        final int occupiedSlotCount;
        private long animationStartTime = -1L;
        private long becameFullyVisibleAt = -1L;
        private Toast.Visibility visibility = Toast.Visibility.SHOW;
        private long fullyVisibleFor;
        private float visiblePortion;
        private boolean hasFinishedRendering;

        ToastInstance(final T pToast, final int pFirstSlotIndex, final int pOccupiedSlotCount) {
            this.toast = pToast;
            this.firstSlotIndex = pFirstSlotIndex;
            this.occupiedSlotCount = pOccupiedSlotCount;
        }

        public T getToast() {
            return this.toast;
        }

        public boolean hasFinishedRendering() {
            return this.hasFinishedRendering;
        }

        private void calculateVisiblePortion(long pVisibilityTime) {
            float f = Mth.clamp((float)(pVisibilityTime - this.animationStartTime) / 600.0F, 0.0F, 1.0F);
            f *= f;
            if (this.visibility == Toast.Visibility.HIDE) {
                this.visiblePortion = 1.0F - f;
            } else {
                this.visiblePortion = f;
            }
        }

        public void update() {
            long i = Util.getMillis();
            if (this.animationStartTime == -1L) {
                this.animationStartTime = i;
                this.visibility.playSound(ToastManager.this.minecraft.getSoundManager());
            }

            if (this.visibility == Toast.Visibility.SHOW && i - this.animationStartTime <= 600L) {
                this.becameFullyVisibleAt = i;
            }

            this.fullyVisibleFor = i - this.becameFullyVisibleAt;
            this.calculateVisiblePortion(i);
            this.toast.update(ToastManager.this, this.fullyVisibleFor);
            Toast.Visibility toast$visibility = this.toast.getWantedVisibility();
            if (toast$visibility != this.visibility) {
                this.animationStartTime = i - (long)((int)((1.0F - this.visiblePortion) * 600.0F));
                this.visibility = toast$visibility;
                this.visibility.playSound(ToastManager.this.minecraft.getSoundManager());
            }

            this.hasFinishedRendering = this.visibility == Toast.Visibility.HIDE && i - this.animationStartTime > 600L;
        }

        public void render(GuiGraphics pGuiGraphics, int pGuiWidth) {
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate((float)pGuiWidth - (float)this.toast.width() * this.visiblePortion, (float)(this.firstSlotIndex * 32), 800.0F);
            this.toast.render(pGuiGraphics, ToastManager.this.minecraft.font, this.fullyVisibleFor);
            pGuiGraphics.pose().popPose();
        }
    }
}
