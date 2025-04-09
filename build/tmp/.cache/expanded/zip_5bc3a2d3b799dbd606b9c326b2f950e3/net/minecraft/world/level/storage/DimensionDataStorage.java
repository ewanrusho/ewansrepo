package net.minecraft.world.level.storage;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class DimensionDataStorage implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, Optional<SavedData>> cache = new HashMap<>();
    private final DataFixer fixerUpper;
    private final HolderLookup.Provider registries;
    private final Path dataFolder;
    private CompletableFuture<?> pendingWriteFuture = CompletableFuture.completedFuture(null);

    public DimensionDataStorage(Path pDataFolder, DataFixer pFixerUpper, HolderLookup.Provider pRegistries) {
        this.fixerUpper = pFixerUpper;
        this.dataFolder = pDataFolder;
        this.registries = pRegistries;
    }

    private Path getDataFile(String pFilename) {
        return this.dataFolder.resolve(pFilename + ".dat");
    }

    public <T extends SavedData> T computeIfAbsent(SavedData.Factory<T> pFactory, String pName) {
        T t = this.get(pFactory, pName);
        if (t != null) {
            return t;
        } else {
            T t1 = (T)pFactory.constructor().get();
            this.set(pName, t1);
            return t1;
        }
    }

    @Nullable
    public <T extends SavedData> T get(SavedData.Factory<T> pFactory, String pName) {
        Optional<SavedData> optional = this.cache.get(pName);
        if (optional == null) {
            optional = Optional.ofNullable(this.readSavedData(pFactory.deserializer(), pFactory.type(), pName));
            this.cache.put(pName, optional);
        }

        return (T)optional.orElse(null);
    }

    @Nullable
    private <T extends SavedData> T readSavedData(BiFunction<CompoundTag, HolderLookup.Provider, T> pReader, DataFixTypes pDataFixType, String pFilename) {
        try {
            Path path = this.getDataFile(pFilename);
            if (Files.exists(path)) {
                CompoundTag compoundtag = this.readTagFromDisk(pFilename, pDataFixType, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                return pReader.apply(compoundtag.getCompound("data"), this.registries);
            }
        } catch (Exception exception) {
            LOGGER.error("Error loading saved data: {}", pFilename, exception);
        }

        return null;
    }

    public void set(String pName, SavedData pSavedData) {
        this.cache.put(pName, Optional.of(pSavedData));
        pSavedData.setDirty();
    }

    public CompoundTag readTagFromDisk(String pFilename, DataFixTypes pDataFixType, int pVersion) throws IOException {
        CompoundTag compoundtag1;
        try (
            InputStream inputstream = Files.newInputStream(this.getDataFile(pFilename));
            PushbackInputStream pushbackinputstream = new PushbackInputStream(new FastBufferedInputStream(inputstream), 2);
        ) {
            CompoundTag compoundtag;
            if (this.isGzip(pushbackinputstream)) {
                compoundtag = NbtIo.readCompressed(pushbackinputstream, NbtAccounter.unlimitedHeap());
            } else {
                try (DataInputStream datainputstream = new DataInputStream(pushbackinputstream)) {
                    compoundtag = NbtIo.read(datainputstream);
                }
            }

            int i = NbtUtils.getDataVersion(compoundtag, 1343);
            // Forge: Allow the data fixer to be null, leaving the modder responsible for keeping track of their own data formats
            if (pDataFixType == null)
                compoundtag1 = compoundtag;
            else
            compoundtag1 = pDataFixType.update(this.fixerUpper, compoundtag, i, pVersion);
        }

        return compoundtag1;
    }

    private boolean isGzip(PushbackInputStream pInputStream) throws IOException {
        byte[] abyte = new byte[2];
        boolean flag = false;
        int i = pInputStream.read(abyte, 0, 2);
        if (i == 2) {
            int j = (abyte[1] & 255) << 8 | abyte[0] & 255;
            if (j == 35615) {
                flag = true;
            }
        }

        if (i != 0) {
            pInputStream.unread(abyte, 0, i);
        }

        return flag;
    }

    public CompletableFuture<?> scheduleSave() {
        Map<Path, CompoundTag> map = this.collectDirtyTagsToSave();
        if (map.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        } else {
            int i = Util.maxAllowedExecutorThreads();
            int j = map.size();
            if (j > i) {
                this.pendingWriteFuture = this.pendingWriteFuture.thenCompose(p_375371_ -> {
                    List<CompletableFuture<?>> list = new ArrayList<>(i);
                    int k = Mth.positiveCeilDiv(j, i);

                    for (List<Entry<Path, CompoundTag>> list1 : Iterables.partition(map.entrySet(), k)) {
                        list.add(CompletableFuture.runAsync(() -> {
                            for (Entry<Path, CompoundTag> entry : list1) {
                                tryWrite(entry.getKey(), entry.getValue());
                            }
                        }, Util.ioPool()));
                    }

                    return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
                });
            } else {
                this.pendingWriteFuture = this.pendingWriteFuture
                    .thenCompose(
                        p_360653_ -> CompletableFuture.allOf(
                                map.entrySet()
                                    .stream()
                                    .map(p_375366_ -> CompletableFuture.runAsync(() -> tryWrite(p_375366_.getKey(), p_375366_.getValue()), Util.ioPool()))
                                    .toArray(CompletableFuture[]::new)
                            )
                    );
            }

            return this.pendingWriteFuture;
        }
    }

    private Map<Path, CompoundTag> collectDirtyTagsToSave() {
        Map<Path, CompoundTag> map = new Object2ObjectArrayMap<>();
        this.cache
            .forEach(
                (p_360648_, p_360649_) -> p_360649_.filter(SavedData::isDirty)
                        .ifPresent(p_360658_ -> map.put(this.getDataFile(p_360648_), p_360658_.save(this.registries)))
            );
        return map;
    }

    private static void tryWrite(Path pPath, CompoundTag pTag) {
        try {
            NbtIo.writeCompressed(pTag, pPath);
        } catch (IOException ioexception) {
            LOGGER.error("Could not save data to {}", pPath.getFileName(), ioexception);
        }
    }

    public void saveAndJoin() {
        this.scheduleSave().join();
    }

    @Override
    public void close() {
        this.saveAndJoin();
    }
}
