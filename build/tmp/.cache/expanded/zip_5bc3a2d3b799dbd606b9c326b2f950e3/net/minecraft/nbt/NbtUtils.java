package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

public final class NbtUtils {
    private static final Comparator<ListTag> YXZ_LISTTAG_INT_COMPARATOR = Comparator.<ListTag>comparingInt(p_178074_ -> p_178074_.getInt(1))
        .thenComparingInt(p_178070_ -> p_178070_.getInt(0))
        .thenComparingInt(p_178066_ -> p_178066_.getInt(2));
    private static final Comparator<ListTag> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.<ListTag>comparingDouble(p_178060_ -> p_178060_.getDouble(1))
        .thenComparingDouble(p_178056_ -> p_178056_.getDouble(0))
        .thenComparingDouble(p_178042_ -> p_178042_.getDouble(2));
    public static final String SNBT_DATA_TAG = "data";
    private static final char PROPERTIES_START = '{';
    private static final char PROPERTIES_END = '}';
    private static final String ELEMENT_SEPARATOR = ",";
    private static final char KEY_VALUE_SEPARATOR = ':';
    private static final Splitter COMMA_SPLITTER = Splitter.on(",");
    private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int INDENT = 2;
    private static final int NOT_FOUND = -1;

    private NbtUtils() {
    }

    @VisibleForTesting
    public static boolean compareNbt(@Nullable Tag pTag, @Nullable Tag pOther, boolean pCompareListTag) {
        if (pTag == pOther) {
            return true;
        } else if (pTag == null) {
            return true;
        } else if (pOther == null) {
            return false;
        } else if (!pTag.getClass().equals(pOther.getClass())) {
            return false;
        } else if (pTag instanceof CompoundTag compoundtag) {
            CompoundTag compoundtag1 = (CompoundTag)pOther;
            if (compoundtag1.size() < compoundtag.size()) {
                return false;
            } else {
                for (String s : compoundtag.getAllKeys()) {
                    Tag tag2 = compoundtag.get(s);
                    if (!compareNbt(tag2, compoundtag1.get(s), pCompareListTag)) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            if (pTag instanceof ListTag listtag && pCompareListTag) {
                ListTag listtag1 = (ListTag)pOther;
                if (listtag.isEmpty()) {
                    return listtag1.isEmpty();
                }

                if (listtag1.size() < listtag.size()) {
                    return false;
                }

                for (Tag tag : listtag) {
                    boolean flag = false;

                    for (Tag tag1 : listtag1) {
                        if (compareNbt(tag, tag1, pCompareListTag)) {
                            flag = true;
                            break;
                        }
                    }

                    if (!flag) {
                        return false;
                    }
                }

                return true;
            }

            return pTag.equals(pOther);
        }
    }

    public static IntArrayTag createUUID(UUID pUuid) {
        return new IntArrayTag(UUIDUtil.uuidToIntArray(pUuid));
    }

    public static UUID loadUUID(Tag pTag) {
        if (pTag.getType() != IntArrayTag.TYPE) {
            throw new IllegalArgumentException(
                "Expected UUID-Tag to be of type " + IntArrayTag.TYPE.getName() + ", but found " + pTag.getType().getName() + "."
            );
        } else {
            int[] aint = ((IntArrayTag)pTag).getAsIntArray();
            if (aint.length != 4) {
                throw new IllegalArgumentException("Expected UUID-Array to be of length 4, but found " + aint.length + ".");
            } else {
                return UUIDUtil.uuidFromIntArray(aint);
            }
        }
    }

    public static Optional<BlockPos> readBlockPos(CompoundTag pTag, String pKey) {
        int[] aint = pTag.getIntArray(pKey);
        return aint.length == 3 ? Optional.of(new BlockPos(aint[0], aint[1], aint[2])) : Optional.empty();
    }

    public static Tag writeBlockPos(BlockPos pPos) {
        return new IntArrayTag(new int[]{pPos.getX(), pPos.getY(), pPos.getZ()});
    }

    public static BlockState readBlockState(HolderGetter<Block> pBlockGetter, CompoundTag pTag) {
        if (!pTag.contains("Name", 8)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            ResourceLocation resourcelocation = ResourceLocation.parse(pTag.getString("Name"));
            Optional<? extends Holder<Block>> optional = pBlockGetter.get(ResourceKey.create(Registries.BLOCK, resourcelocation));
            if (optional.isEmpty()) {
                return Blocks.AIR.defaultBlockState();
            } else {
                Block block = optional.get().value();
                BlockState blockstate = block.defaultBlockState();
                if (pTag.contains("Properties", 10)) {
                    CompoundTag compoundtag = pTag.getCompound("Properties");
                    StateDefinition<Block, BlockState> statedefinition = block.getStateDefinition();

                    for (String s : compoundtag.getAllKeys()) {
                        Property<?> property = statedefinition.getProperty(s);
                        if (property != null) {
                            blockstate = setValueHelper(blockstate, property, s, compoundtag, pTag);
                        }
                    }
                }

                return blockstate;
            }
        }
    }

    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(
        S pStateHolder, Property<T> pProperty, String pPropertyName, CompoundTag pPropertiesTag, CompoundTag pBlockStateTag
    ) {
        Optional<T> optional = pProperty.getValue(pPropertiesTag.getString(pPropertyName));
        if (optional.isPresent()) {
            return pStateHolder.setValue(pProperty, optional.get());
        } else {
            LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", pPropertyName, pPropertiesTag.getString(pPropertyName), pBlockStateTag);
            return pStateHolder;
        }
    }

    public static CompoundTag writeBlockState(BlockState pState) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", BuiltInRegistries.BLOCK.getKey(pState.getBlock()).toString());
        Map<Property<?>, Comparable<?>> map = pState.getValues();
        if (!map.isEmpty()) {
            CompoundTag compoundtag1 = new CompoundTag();

            for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
                Property<?> property = entry.getKey();
                compoundtag1.putString(property.getName(), getName(property, entry.getValue()));
            }

            compoundtag.put("Properties", compoundtag1);
        }

        return compoundtag;
    }

    public static CompoundTag writeFluidState(FluidState pState) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", BuiltInRegistries.FLUID.getKey(pState.getType()).toString());
        Map<Property<?>, Comparable<?>> map = pState.getValues();
        if (!map.isEmpty()) {
            CompoundTag compoundtag1 = new CompoundTag();

            for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
                Property<?> property = entry.getKey();
                compoundtag1.putString(property.getName(), getName(property, entry.getValue()));
            }

            compoundtag.put("Properties", compoundtag1);
        }

        return compoundtag;
    }

    private static <T extends Comparable<T>> String getName(Property<T> pProperty, Comparable<?> pValue) {
        return pProperty.getName((T)pValue);
    }

    public static String prettyPrint(Tag pTag) {
        return prettyPrint(pTag, false);
    }

    public static String prettyPrint(Tag pTag, boolean pPrettyPrintArray) {
        return prettyPrint(new StringBuilder(), pTag, 0, pPrettyPrintArray).toString();
    }

    public static StringBuilder prettyPrint(StringBuilder pStringBuilder, Tag pTag, int pIndentLevel, boolean pPrettyPrintArray) {
        switch (pTag.getId()) {
            case 0:
                break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
                pStringBuilder.append(pTag);
                break;
            case 7:
                ByteArrayTag bytearraytag = (ByteArrayTag)pTag;
                byte[] abyte = bytearraytag.getAsByteArray();
                int k1 = abyte.length;
                indent(pIndentLevel, pStringBuilder).append("byte[").append(k1).append("] {\n");
                if (pPrettyPrintArray) {
                    indent(pIndentLevel + 1, pStringBuilder);

                    for (int i2 = 0; i2 < abyte.length; i2++) {
                        if (i2 != 0) {
                            pStringBuilder.append(',');
                        }

                        if (i2 % 16 == 0 && i2 / 16 > 0) {
                            pStringBuilder.append('\n');
                            if (i2 < abyte.length) {
                                indent(pIndentLevel + 1, pStringBuilder);
                            }
                        } else if (i2 != 0) {
                            pStringBuilder.append(' ');
                        }

                        pStringBuilder.append(String.format(Locale.ROOT, "0x%02X", abyte[i2] & 255));
                    }
                } else {
                    indent(pIndentLevel + 1, pStringBuilder).append(" // Skipped, supply withBinaryBlobs true");
                }

                pStringBuilder.append('\n');
                indent(pIndentLevel, pStringBuilder).append('}');
                break;
            case 9:
                ListTag listtag = (ListTag)pTag;
                int k = listtag.size();
                int j1 = listtag.getElementType();
                String s1 = j1 == 0 ? "undefined" : TagTypes.getType(j1).getPrettyName();
                indent(pIndentLevel, pStringBuilder).append("list<").append(s1).append(">[").append(k).append("] [");
                if (k != 0) {
                    pStringBuilder.append('\n');
                }

                for (int i3 = 0; i3 < k; i3++) {
                    if (i3 != 0) {
                        pStringBuilder.append(",\n");
                    }

                    indent(pIndentLevel + 1, pStringBuilder);
                    prettyPrint(pStringBuilder, listtag.get(i3), pIndentLevel + 1, pPrettyPrintArray);
                }

                if (k != 0) {
                    pStringBuilder.append('\n');
                }

                indent(pIndentLevel, pStringBuilder).append(']');
                break;
            case 10:
                CompoundTag compoundtag = (CompoundTag)pTag;
                List<String> list = Lists.newArrayList(compoundtag.getAllKeys());
                Collections.sort(list);
                indent(pIndentLevel, pStringBuilder).append('{');
                if (pStringBuilder.length() - pStringBuilder.lastIndexOf("\n") > 2 * (pIndentLevel + 1)) {
                    pStringBuilder.append('\n');
                    indent(pIndentLevel + 1, pStringBuilder);
                }

                int i1 = list.stream().mapToInt(String::length).max().orElse(0);
                String s = Strings.repeat(" ", i1);

                for (int l2 = 0; l2 < list.size(); l2++) {
                    if (l2 != 0) {
                        pStringBuilder.append(",\n");
                    }

                    String s2 = list.get(l2);
                    indent(pIndentLevel + 1, pStringBuilder).append('"').append(s2).append('"').append(s, 0, s.length() - s2.length()).append(": ");
                    prettyPrint(pStringBuilder, compoundtag.get(s2), pIndentLevel + 1, pPrettyPrintArray);
                }

                if (!list.isEmpty()) {
                    pStringBuilder.append('\n');
                }

                indent(pIndentLevel, pStringBuilder).append('}');
                break;
            case 11:
                IntArrayTag intarraytag = (IntArrayTag)pTag;
                int[] aint = intarraytag.getAsIntArray();
                int l = 0;

                for (int k3 : aint) {
                    l = Math.max(l, String.format(Locale.ROOT, "%X", k3).length());
                }

                int l1 = aint.length;
                indent(pIndentLevel, pStringBuilder).append("int[").append(l1).append("] {\n");
                if (pPrettyPrintArray) {
                    indent(pIndentLevel + 1, pStringBuilder);

                    for (int k2 = 0; k2 < aint.length; k2++) {
                        if (k2 != 0) {
                            pStringBuilder.append(',');
                        }

                        if (k2 % 16 == 0 && k2 / 16 > 0) {
                            pStringBuilder.append('\n');
                            if (k2 < aint.length) {
                                indent(pIndentLevel + 1, pStringBuilder);
                            }
                        } else if (k2 != 0) {
                            pStringBuilder.append(' ');
                        }

                        pStringBuilder.append(String.format(Locale.ROOT, "0x%0" + l + "X", aint[k2]));
                    }
                } else {
                    indent(pIndentLevel + 1, pStringBuilder).append(" // Skipped, supply withBinaryBlobs true");
                }

                pStringBuilder.append('\n');
                indent(pIndentLevel, pStringBuilder).append('}');
                break;
            case 12:
                LongArrayTag longarraytag = (LongArrayTag)pTag;
                long[] along = longarraytag.getAsLongArray();
                long i = 0L;

                for (long j : along) {
                    i = Math.max(i, (long)String.format(Locale.ROOT, "%X", j).length());
                }

                long j2 = (long)along.length;
                indent(pIndentLevel, pStringBuilder).append("long[").append(j2).append("] {\n");
                if (pPrettyPrintArray) {
                    indent(pIndentLevel + 1, pStringBuilder);

                    for (int j3 = 0; j3 < along.length; j3++) {
                        if (j3 != 0) {
                            pStringBuilder.append(',');
                        }

                        if (j3 % 16 == 0 && j3 / 16 > 0) {
                            pStringBuilder.append('\n');
                            if (j3 < along.length) {
                                indent(pIndentLevel + 1, pStringBuilder);
                            }
                        } else if (j3 != 0) {
                            pStringBuilder.append(' ');
                        }

                        pStringBuilder.append(String.format(Locale.ROOT, "0x%0" + i + "X", along[j3]));
                    }
                } else {
                    indent(pIndentLevel + 1, pStringBuilder).append(" // Skipped, supply withBinaryBlobs true");
                }

                pStringBuilder.append('\n');
                indent(pIndentLevel, pStringBuilder).append('}');
                break;
            default:
                pStringBuilder.append("<UNKNOWN :(>");
        }

        return pStringBuilder;
    }

    private static StringBuilder indent(int pIndentLevel, StringBuilder pStringBuilder) {
        int i = pStringBuilder.lastIndexOf("\n") + 1;
        int j = pStringBuilder.length() - i;

        for (int k = 0; k < 2 * pIndentLevel - j; k++) {
            pStringBuilder.append(' ');
        }

        return pStringBuilder;
    }

    public static Component toPrettyComponent(Tag pTag) {
        return new TextComponentTagVisitor("").visit(pTag);
    }

    public static String structureToSnbt(CompoundTag pTag) {
        return new SnbtPrinterTagVisitor().visit(packStructureTemplate(pTag));
    }

    public static CompoundTag snbtToStructure(String pText) throws CommandSyntaxException {
        return unpackStructureTemplate(TagParser.parseTag(pText));
    }

    @VisibleForTesting
    static CompoundTag packStructureTemplate(CompoundTag pTag) {
        boolean flag = pTag.contains("palettes", 9);
        ListTag listtag;
        if (flag) {
            listtag = pTag.getList("palettes", 9).getList(0);
        } else {
            listtag = pTag.getList("palette", 10);
        }

        ListTag listtag1 = listtag.stream()
            .map(CompoundTag.class::cast)
            .map(NbtUtils::packBlockState)
            .map(StringTag::valueOf)
            .collect(Collectors.toCollection(ListTag::new));
        pTag.put("palette", listtag1);
        if (flag) {
            ListTag listtag2 = new ListTag();
            ListTag listtag3 = pTag.getList("palettes", 9);
            listtag3.stream().map(ListTag.class::cast).forEach(p_178049_ -> {
                CompoundTag compoundtag = new CompoundTag();

                for (int i = 0; i < p_178049_.size(); i++) {
                    compoundtag.putString(listtag1.getString(i), packBlockState(p_178049_.getCompound(i)));
                }

                listtag2.add(compoundtag);
            });
            pTag.put("palettes", listtag2);
        }

        if (pTag.contains("entities", 9)) {
            ListTag listtag4 = pTag.getList("entities", 10);
            ListTag listtag6 = listtag4.stream()
                .map(CompoundTag.class::cast)
                .sorted(Comparator.comparing(p_178080_ -> p_178080_.getList("pos", 6), YXZ_LISTTAG_DOUBLE_COMPARATOR))
                .collect(Collectors.toCollection(ListTag::new));
            pTag.put("entities", listtag6);
        }

        ListTag listtag5 = pTag.getList("blocks", 10)
            .stream()
            .map(CompoundTag.class::cast)
            .sorted(Comparator.comparing(p_178078_ -> p_178078_.getList("pos", 3), YXZ_LISTTAG_INT_COMPARATOR))
            .peek(p_178045_ -> p_178045_.putString("state", listtag1.getString(p_178045_.getInt("state"))))
            .collect(Collectors.toCollection(ListTag::new));
        pTag.put("data", listtag5);
        pTag.remove("blocks");
        return pTag;
    }

    @VisibleForTesting
    static CompoundTag unpackStructureTemplate(CompoundTag pTag) {
        ListTag listtag = pTag.getList("palette", 8);
        Map<String, Tag> map = listtag.stream()
            .map(StringTag.class::cast)
            .map(StringTag::getAsString)
            .collect(ImmutableMap.toImmutableMap(Function.identity(), NbtUtils::unpackBlockState));
        if (pTag.contains("palettes", 9)) {
            pTag.put(
                "palettes",
                pTag.getList("palettes", 10)
                    .stream()
                    .map(CompoundTag.class::cast)
                    .map(p_178033_ -> map.keySet().stream().map(p_178033_::getString).map(NbtUtils::unpackBlockState).collect(Collectors.toCollection(ListTag::new)))
                    .collect(Collectors.toCollection(ListTag::new))
            );
            pTag.remove("palette");
        } else {
            pTag.put("palette", map.values().stream().collect(Collectors.toCollection(ListTag::new)));
        }

        if (pTag.contains("data", 9)) {
            Object2IntMap<String> object2intmap = new Object2IntOpenHashMap<>();
            object2intmap.defaultReturnValue(-1);

            for (int i = 0; i < listtag.size(); i++) {
                object2intmap.put(listtag.getString(i), i);
            }

            ListTag listtag1 = pTag.getList("data", 10);

            for (int j = 0; j < listtag1.size(); j++) {
                CompoundTag compoundtag = listtag1.getCompound(j);
                String s = compoundtag.getString("state");
                int k = object2intmap.getInt(s);
                if (k == -1) {
                    throw new IllegalStateException("Entry " + s + " missing from palette");
                }

                compoundtag.putInt("state", k);
            }

            pTag.put("blocks", listtag1);
            pTag.remove("data");
        }

        return pTag;
    }

    @VisibleForTesting
    static String packBlockState(CompoundTag pTag) {
        StringBuilder stringbuilder = new StringBuilder(pTag.getString("Name"));
        if (pTag.contains("Properties", 10)) {
            CompoundTag compoundtag = pTag.getCompound("Properties");
            String s = compoundtag.getAllKeys()
                .stream()
                .sorted()
                .map(p_178036_ -> p_178036_ + ":" + compoundtag.get(p_178036_).getAsString())
                .collect(Collectors.joining(","));
            stringbuilder.append('{').append(s).append('}');
        }

        return stringbuilder.toString();
    }

    @VisibleForTesting
    static CompoundTag unpackBlockState(String pBlockStateText) {
        CompoundTag compoundtag = new CompoundTag();
        int i = pBlockStateText.indexOf(123);
        String s;
        if (i >= 0) {
            s = pBlockStateText.substring(0, i);
            CompoundTag compoundtag1 = new CompoundTag();
            if (i + 2 <= pBlockStateText.length()) {
                String s1 = pBlockStateText.substring(i + 1, pBlockStateText.indexOf(125, i));
                COMMA_SPLITTER.split(s1).forEach(p_178040_ -> {
                    List<String> list = COLON_SPLITTER.splitToList(p_178040_);
                    if (list.size() == 2) {
                        compoundtag1.putString(list.get(0), list.get(1));
                    } else {
                        LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", pBlockStateText);
                    }
                });
                compoundtag.put("Properties", compoundtag1);
            }
        } else {
            s = pBlockStateText;
        }

        compoundtag.putString("Name", s);
        return compoundtag;
    }

    public static CompoundTag addCurrentDataVersion(CompoundTag pTag) {
        int i = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        return addDataVersion(pTag, i);
    }

    public static CompoundTag addDataVersion(CompoundTag pTag, int pDataVersion) {
        pTag.putInt("DataVersion", pDataVersion);
        return pTag;
    }

    public static int getDataVersion(CompoundTag pTag, int pDefaultValue) {
        return pTag.contains("DataVersion", 99) ? pTag.getInt("DataVersion") : pDefaultValue;
    }
}