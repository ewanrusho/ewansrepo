package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ProfiledReloadInstance extends SimpleReloadInstance<ProfiledReloadInstance.State> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Stopwatch total = Stopwatch.createUnstarted();

    public ProfiledReloadInstance(
        ResourceManager pResourceManager, List<PreparableReloadListener> pListeners, Executor pBackgroundExecutor, Executor pGameExecutor, CompletableFuture<Unit> pAlsoWaitedFor
    ) {
        super(
            pBackgroundExecutor,
            pGameExecutor,
            pResourceManager,
            pListeners,
            (p_358736_, p_358737_, p_358738_, p_358739_, p_358740_) -> {
                AtomicLong atomiclong = new AtomicLong();
                AtomicLong atomiclong1 = new AtomicLong();
                CompletableFuture<Void> completablefuture = p_358738_.reload(
                    p_358736_, p_358737_, profiledExecutor(p_358739_, atomiclong, p_358738_.getName()), profiledExecutor(p_358740_, atomiclong1, p_358738_.getName())
                );
                return completablefuture.thenApplyAsync(p_358734_ -> {
                    LOGGER.debug("Finished reloading {}", p_358738_.getName());
                    return new ProfiledReloadInstance.State(p_358738_.getName(), atomiclong, atomiclong1);
                }, pGameExecutor);
            },
            pAlsoWaitedFor
        );
        this.total.start();
        this.allDone = this.allDone.thenApplyAsync(this::finish, pGameExecutor);
    }

    private static Executor profiledExecutor(Executor pExecutor, AtomicLong pTimeTaken, String pName) {
        return p_358744_ -> pExecutor.execute(() -> {
                ProfilerFiller profilerfiller = Profiler.get();
                profilerfiller.push(pName);
                long i = Util.getNanos();
                p_358744_.run();
                pTimeTaken.addAndGet(Util.getNanos() - i);
                profilerfiller.pop();
            });
    }

    private List<ProfiledReloadInstance.State> finish(List<ProfiledReloadInstance.State> pDatapoints) {
        this.total.stop();
        long i = 0L;
        LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

        for (ProfiledReloadInstance.State profiledreloadinstance$state : pDatapoints) {
            long j = TimeUnit.NANOSECONDS.toMillis(profiledreloadinstance$state.preparationNanos.get());
            long k = TimeUnit.NANOSECONDS.toMillis(profiledreloadinstance$state.reloadNanos.get());
            long l = j + k;
            String s = profiledreloadinstance$state.name;
            LOGGER.info("{} took approximately {} ms ({} ms preparing, {} ms applying)", s, l, j, k);
            i += k;
        }

        LOGGER.info("Total blocking time: {} ms", i);
        return pDatapoints;
    }

    public static record State(String name, AtomicLong preparationNanos, AtomicLong reloadNanos) {
    }
}