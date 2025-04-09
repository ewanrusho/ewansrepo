package net.minecraft.util.parsing.packrat;

import java.util.ArrayList;
import java.util.List;

public interface ErrorCollector<S> {
    void store(int pCursor, SuggestionSupplier<S> pSuggestions, Object pReason);

    default void store(int pCursor, Object pReason) {
        this.store(pCursor, SuggestionSupplier.empty(), pReason);
    }

    void finish(int pCursor);

    public static class LongestOnly<S> implements ErrorCollector<S> {
        private final List<ErrorEntry<S>> entries = new ArrayList<>();
        private int lastCursor = -1;

        private void discardErrorsFromShorterParse(int pCursor) {
            if (pCursor > this.lastCursor) {
                this.lastCursor = pCursor;
                this.entries.clear();
            }
        }

        @Override
        public void finish(int p_334009_) {
            this.discardErrorsFromShorterParse(p_334009_);
        }

        @Override
        public void store(int p_331115_, SuggestionSupplier<S> p_329965_, Object p_332125_) {
            this.discardErrorsFromShorterParse(p_331115_);
            if (p_331115_ == this.lastCursor) {
                this.entries.add(new ErrorEntry<>(p_331115_, p_329965_, p_332125_));
            }
        }

        public List<ErrorEntry<S>> entries() {
            return this.entries;
        }

        public int cursor() {
            return this.lastCursor;
        }
    }
}