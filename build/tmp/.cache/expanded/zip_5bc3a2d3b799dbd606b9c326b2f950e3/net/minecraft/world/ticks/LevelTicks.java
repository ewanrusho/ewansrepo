package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class LevelTicks<T> implements LevelTickAccess<T> {
    private static final Comparator<LevelChunkTicks<?>> CONTAINER_DRAIN_ORDER = (p_193246_, p_193247_) -> ScheduledTick.INTRA_TICK_DRAIN_ORDER
            .compare(p_193246_.peek(), p_193247_.peek());
    private final LongPredicate tickCheck;
    private final Long2ObjectMap<LevelChunkTicks<T>> allContainers = new Long2ObjectOpenHashMap<>();
    private final Long2LongMap nextTickForContainer = Util.make(new Long2LongOpenHashMap(), p_193262_ -> p_193262_.defaultReturnValue(Long.MAX_VALUE));
    private final Queue<LevelChunkTicks<T>> containersToTick = new PriorityQueue<>(CONTAINER_DRAIN_ORDER);
    private final Queue<ScheduledTick<T>> toRunThisTick = new ArrayDeque<>();
    private final List<ScheduledTick<T>> alreadyRunThisTick = new ArrayList<>();
    private final Set<ScheduledTick<?>> toRunThisTickSet = new ObjectOpenCustomHashSet<>(ScheduledTick.UNIQUE_TICK_HASH);
    private final BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> chunkScheduleUpdater = (p_193249_, p_193250_) -> {
        if (p_193250_.equals(p_193249_.peek())) {
            this.updateContainerScheduling(p_193250_);
        }
    };

    public LevelTicks(LongPredicate pTickCheck) {
        this.tickCheck = pTickCheck;
    }

    public void addContainer(ChunkPos pChunkPos, LevelChunkTicks<T> pChunkTicks) {
        long i = pChunkPos.toLong();
        this.allContainers.put(i, pChunkTicks);
        ScheduledTick<T> scheduledtick = pChunkTicks.peek();
        if (scheduledtick != null) {
            this.nextTickForContainer.put(i, scheduledtick.triggerTick());
        }

        pChunkTicks.setOnTickAdded(this.chunkScheduleUpdater);
    }

    public void removeContainer(ChunkPos pChunkPos) {
        long i = pChunkPos.toLong();
        LevelChunkTicks<T> levelchunkticks = this.allContainers.remove(i);
        this.nextTickForContainer.remove(i);
        if (levelchunkticks != null) {
            levelchunkticks.setOnTickAdded(null);
        }
    }

    @Override
    public void schedule(ScheduledTick<T> p_193252_) {
        long i = ChunkPos.asLong(p_193252_.pos());
        LevelChunkTicks<T> levelchunkticks = this.allContainers.get(i);
        if (levelchunkticks == null) {
            Util.logAndPauseIfInIde("Trying to schedule tick in not loaded position " + p_193252_.pos());
        } else {
            levelchunkticks.schedule(p_193252_);
        }
    }

    public void tick(long pGameTime, int pMaxAllowedTicks, BiConsumer<BlockPos, T> pTicker) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("collect");
        this.collectTicks(pGameTime, pMaxAllowedTicks, profilerfiller);
        profilerfiller.popPush("run");
        profilerfiller.incrementCounter("ticksToRun", this.toRunThisTick.size());
        this.runCollectedTicks(pTicker);
        profilerfiller.popPush("cleanup");
        this.cleanupAfterTick();
        profilerfiller.pop();
    }

    private void collectTicks(long pGameTime, int pMaxAllowedTicks, ProfilerFiller pProfiler) {
        this.sortContainersToTick(pGameTime);
        pProfiler.incrementCounter("containersToTick", this.containersToTick.size());
        this.drainContainers(pGameTime, pMaxAllowedTicks);
        this.rescheduleLeftoverContainers();
    }

    private void sortContainersToTick(long pGameTime) {
        ObjectIterator<Entry> objectiterator = Long2LongMaps.fastIterator(this.nextTickForContainer);

        while (objectiterator.hasNext()) {
            Entry entry = objectiterator.next();
            long i = entry.getLongKey();
            long j = entry.getLongValue();
            if (j <= pGameTime) {
                LevelChunkTicks<T> levelchunkticks = this.allContainers.get(i);
                if (levelchunkticks == null) {
                    objectiterator.remove();
                } else {
                    ScheduledTick<T> scheduledtick = levelchunkticks.peek();
                    if (scheduledtick == null) {
                        objectiterator.remove();
                    } else if (scheduledtick.triggerTick() > pGameTime) {
                        entry.setValue(scheduledtick.triggerTick());
                    } else if (this.tickCheck.test(i)) {
                        objectiterator.remove();
                        this.containersToTick.add(levelchunkticks);
                    }
                }
            }
        }
    }

    private void drainContainers(long pGameTime, int pMaxAllowedTicks) {
        LevelChunkTicks<T> levelchunkticks;
        while (this.canScheduleMoreTicks(pMaxAllowedTicks) && (levelchunkticks = this.containersToTick.poll()) != null) {
            ScheduledTick<T> scheduledtick = levelchunkticks.poll();
            this.scheduleForThisTick(scheduledtick);
            this.drainFromCurrentContainer(this.containersToTick, levelchunkticks, pGameTime, pMaxAllowedTicks);
            ScheduledTick<T> scheduledtick1 = levelchunkticks.peek();
            if (scheduledtick1 != null) {
                if (scheduledtick1.triggerTick() <= pGameTime && this.canScheduleMoreTicks(pMaxAllowedTicks)) {
                    this.containersToTick.add(levelchunkticks);
                } else {
                    this.updateContainerScheduling(scheduledtick1);
                }
            }
        }
    }

    private void rescheduleLeftoverContainers() {
        for (LevelChunkTicks<T> levelchunkticks : this.containersToTick) {
            this.updateContainerScheduling(levelchunkticks.peek());
        }
    }

    private void updateContainerScheduling(ScheduledTick<T> pTick) {
        this.nextTickForContainer.put(ChunkPos.asLong(pTick.pos()), pTick.triggerTick());
    }

    private void drainFromCurrentContainer(Queue<LevelChunkTicks<T>> pContainersToTick, LevelChunkTicks<T> pLevelChunkTicks, long pGameTime, int pMaxAllowedTicks) {
        if (this.canScheduleMoreTicks(pMaxAllowedTicks)) {
            LevelChunkTicks<T> levelchunkticks = pContainersToTick.peek();
            ScheduledTick<T> scheduledtick = levelchunkticks != null ? levelchunkticks.peek() : null;

            while (this.canScheduleMoreTicks(pMaxAllowedTicks)) {
                ScheduledTick<T> scheduledtick1 = pLevelChunkTicks.peek();
                if (scheduledtick1 == null
                    || scheduledtick1.triggerTick() > pGameTime
                    || scheduledtick != null && ScheduledTick.INTRA_TICK_DRAIN_ORDER.compare(scheduledtick1, scheduledtick) > 0) {
                    break;
                }

                pLevelChunkTicks.poll();
                this.scheduleForThisTick(scheduledtick1);
            }
        }
    }

    private void scheduleForThisTick(ScheduledTick<T> pTick) {
        this.toRunThisTick.add(pTick);
    }

    private boolean canScheduleMoreTicks(int pMaxAllowedTicks) {
        return this.toRunThisTick.size() < pMaxAllowedTicks;
    }

    private void runCollectedTicks(BiConsumer<BlockPos, T> pTicker) {
        while (!this.toRunThisTick.isEmpty()) {
            ScheduledTick<T> scheduledtick = this.toRunThisTick.poll();
            if (!this.toRunThisTickSet.isEmpty()) {
                this.toRunThisTickSet.remove(scheduledtick);
            }

            this.alreadyRunThisTick.add(scheduledtick);
            pTicker.accept(scheduledtick.pos(), scheduledtick.type());
        }
    }

    private void cleanupAfterTick() {
        this.toRunThisTick.clear();
        this.containersToTick.clear();
        this.alreadyRunThisTick.clear();
        this.toRunThisTickSet.clear();
    }

    @Override
    public boolean hasScheduledTick(BlockPos p_193254_, T p_193255_) {
        LevelChunkTicks<T> levelchunkticks = this.allContainers.get(ChunkPos.asLong(p_193254_));
        return levelchunkticks != null && levelchunkticks.hasScheduledTick(p_193254_, p_193255_);
    }

    @Override
    public boolean willTickThisTick(BlockPos p_193282_, T p_193283_) {
        this.calculateTickSetIfNeeded();
        return this.toRunThisTickSet.contains(ScheduledTick.probe(p_193283_, p_193282_));
    }

    private void calculateTickSetIfNeeded() {
        if (this.toRunThisTickSet.isEmpty() && !this.toRunThisTick.isEmpty()) {
            this.toRunThisTickSet.addAll(this.toRunThisTick);
        }
    }

    private void forContainersInArea(BoundingBox pArea, LevelTicks.PosAndContainerConsumer<T> pAction) {
        int i = SectionPos.posToSectionCoord((double)pArea.minX());
        int j = SectionPos.posToSectionCoord((double)pArea.minZ());
        int k = SectionPos.posToSectionCoord((double)pArea.maxX());
        int l = SectionPos.posToSectionCoord((double)pArea.maxZ());

        for (int i1 = i; i1 <= k; i1++) {
            for (int j1 = j; j1 <= l; j1++) {
                long k1 = ChunkPos.asLong(i1, j1);
                LevelChunkTicks<T> levelchunkticks = this.allContainers.get(k1);
                if (levelchunkticks != null) {
                    pAction.accept(k1, levelchunkticks);
                }
            }
        }
    }

    public void clearArea(BoundingBox pArea) {
        Predicate<ScheduledTick<T>> predicate = p_193241_ -> pArea.isInside(p_193241_.pos());
        this.forContainersInArea(pArea, (p_193276_, p_193277_) -> {
            ScheduledTick<T> scheduledtick = p_193277_.peek();
            p_193277_.removeIf(predicate);
            ScheduledTick<T> scheduledtick1 = p_193277_.peek();
            if (scheduledtick1 != scheduledtick) {
                if (scheduledtick1 != null) {
                    this.updateContainerScheduling(scheduledtick1);
                } else {
                    this.nextTickForContainer.remove(p_193276_);
                }
            }
        });
        this.alreadyRunThisTick.removeIf(predicate);
        this.toRunThisTick.removeIf(predicate);
    }

    public void copyArea(BoundingBox pArea, Vec3i pOffset) {
        this.copyAreaFrom(this, pArea, pOffset);
    }

    public void copyAreaFrom(LevelTicks<T> pLevelTicks, BoundingBox pArea, Vec3i pOffset) {
        List<ScheduledTick<T>> list = new ArrayList<>();
        Predicate<ScheduledTick<T>> predicate = p_200922_ -> pArea.isInside(p_200922_.pos());
        pLevelTicks.alreadyRunThisTick.stream().filter(predicate).forEach(list::add);
        pLevelTicks.toRunThisTick.stream().filter(predicate).forEach(list::add);
        pLevelTicks.forContainersInArea(pArea, (p_200931_, p_200932_) -> p_200932_.getAll().filter(predicate).forEach(list::add));
        LongSummaryStatistics longsummarystatistics = list.stream().mapToLong(ScheduledTick::subTickOrder).summaryStatistics();
        long i = longsummarystatistics.getMin();
        long j = longsummarystatistics.getMax();
        list.forEach(
            p_193260_ -> this.schedule(
                    new ScheduledTick<>(
                        p_193260_.type(),
                        p_193260_.pos().offset(pOffset),
                        p_193260_.triggerTick(),
                        p_193260_.priority(),
                        p_193260_.subTickOrder() - i + j + 1L
                    )
                )
        );
    }

    @Override
    public int count() {
        return this.allContainers.values().stream().mapToInt(TickAccess::count).sum();
    }

    @FunctionalInterface
    interface PosAndContainerConsumer<T> {
        void accept(long pPos, LevelChunkTicks<T> pContainer);
    }
}