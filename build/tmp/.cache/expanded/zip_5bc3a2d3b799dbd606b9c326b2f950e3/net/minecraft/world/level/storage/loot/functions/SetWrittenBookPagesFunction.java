package net.minecraft.world.level.storage.loot.functions;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetWrittenBookPagesFunction extends LootItemConditionalFunction {
    public static final Codec<Component> PAGE_CODEC = ComponentSerialization.CODEC
        .validate(p_335313_ -> WrittenBookContent.CONTENT_CODEC.encodeStart(JavaOps.INSTANCE, p_335313_).map(p_336204_ -> p_335313_));
    public static final MapCodec<SetWrittenBookPagesFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_332217_ -> commonFields(p_332217_)
                .and(
                    p_332217_.group(
                        WrittenBookContent.pagesCodec(PAGE_CODEC).fieldOf("pages").forGetter(p_336220_ -> p_336220_.pages),
                        ListOperation.UNLIMITED_CODEC.forGetter(p_335675_ -> p_335675_.pageOperation)
                    )
                )
                .apply(p_332217_, SetWrittenBookPagesFunction::new)
    );
    private final List<Filterable<Component>> pages;
    private final ListOperation pageOperation;

    protected SetWrittenBookPagesFunction(List<LootItemCondition> pConditions, List<Filterable<Component>> pPages, ListOperation pPageOperation) {
        super(pConditions);
        this.pages = pPages;
        this.pageOperation = pPageOperation;
    }

    @Override
    protected ItemStack run(ItemStack p_328011_, LootContext p_329576_) {
        p_328011_.update(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY, this::apply);
        return p_328011_;
    }

    @VisibleForTesting
    public WrittenBookContent apply(WrittenBookContent pWrittenBookContent) {
        List<Filterable<Component>> list = this.pageOperation.apply(pWrittenBookContent.pages(), this.pages);
        return pWrittenBookContent.withReplacedPages(list);
    }

    @Override
    public LootItemFunctionType<SetWrittenBookPagesFunction> getType() {
        return LootItemFunctions.SET_WRITTEN_BOOK_PAGES;
    }
}