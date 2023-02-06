package ru.yandex.market.cashier.mocks;

import java.util.Collection;
import java.util.Iterator;

// HACK: Чтобы была возможность еще раз перечитать элемент, см checkOptionalCreateServiceProductCall
public class OneElementBackIterator<T> implements Iterator<T> {
    private final Iterator<T> original;
    private T previous;
    private boolean returnPrevious = false;
    private Collection<T> source;

    public OneElementBackIterator(Iterator<T> original) {
        this.original = original;
    }

    // Нужно перевести весь код ReturnTestHelper'а на логику не зависящую от порядка запросов
    // А пока тут будет этот хак только для метода ReturnTestHelper#checkBalanceCallsForFFItemsReturn
    public OneElementBackIterator(Collection<T> source) {
        this.original = source.iterator();
        this.source = source;
    }

    @Override
    public boolean hasNext() {
        return returnPrevious || original.hasNext();
    }

    @Override
    public T next() {
        if (returnPrevious) {
            returnPrevious = false;
            return previous;
        }
        previous = original.next();
        return previous;
    }

    public Collection<T> getSource() {
        return source;
    }

    public void skipAdvanceOnNext() {
        this.returnPrevious = true;
    }
}
