package ru.yandex.chemodan.app.lentaloader.test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function2V;
import ru.yandex.chemodan.app.dataapi.support.RecordField;
import ru.yandex.chemodan.app.lentaloader.lenta.LentaManager;
import ru.yandex.chemodan.app.lentaloader.lenta.LentaRecordType;

/**
 * @author dbrylev
 */
public class FieldUpdateCaptor2<P1, P2, V> {
    private final FieldUpdateCaptor<Tuple2<P1, P2>, V> captor;

    public FieldUpdateCaptor2(
            RecordField<V> field, LentaRecordType type,
            LentaManager lentaManager, Function2V<P1, P2> invoker)
    {
        this.captor = new FieldUpdateCaptor<>(
                field, type, lentaManager, t -> invoker.apply(t.get().get1(), t.get().get2()));
    }

    public FieldUpdate<V> create(P1 p1, P2 p2) {
        return captor.create(Tuple2.tuple(p1, p2));
    }

    public FieldUpdate<V> update(V previous, P1 p1, P2 p2) {
        return captor.update(previous, Tuple2.tuple(p1, p2));
    }

    public FieldUpdate<V> update(Option<V> previous, P1 p1, P2 p2) {
        return captor.update(previous, Tuple2.tuple(p1, p2));
    }
}
