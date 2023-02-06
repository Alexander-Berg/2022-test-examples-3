package ru.yandex.chemodan.app.lentaloader.test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.dataapi.api.data.field.DataField;
import ru.yandex.chemodan.app.dataapi.support.RecordField;
import ru.yandex.chemodan.app.lentaloader.lenta.FindOrCreateResult;
import ru.yandex.chemodan.app.lentaloader.lenta.LentaManager;
import ru.yandex.chemodan.app.lentaloader.lenta.LentaRecordType;
import ru.yandex.chemodan.app.lentaloader.lenta.update.CreateHandler;
import ru.yandex.chemodan.app.lentaloader.lenta.update.LentaBlockCreateData;
import ru.yandex.chemodan.app.lentaloader.lenta.update.LentaBlockModifyData;
import ru.yandex.chemodan.app.lentaloader.lenta.update.UpdateHandler;
import ru.yandex.misc.lang.Check;

/**
 * @author dbrylev
 */
public class FieldUpdateCaptor<P, V> {
    private final RecordField<V> field;
    private final LentaRecordType type;
    private final MapF<String, DataField> existingData;

    private final ArgumentCaptor<LentaBlockCreateData> createCaptor;
    private final ArgumentCaptor<LentaBlockModifyData> modifyCaptor;

    private final Function1V<Option<P>> invoker;

    public FieldUpdateCaptor(
            RecordField<V> field, LentaRecordType type,
            LentaManager lentaManager, Function1V<Option<P>> invoker)
    {
        this(field, type, Cf.map(), lentaManager, invoker);
    }

    public FieldUpdateCaptor(
            RecordField<V> field, LentaRecordType type, MapF<String, DataField> existingData,
            LentaManager lentaManager, Function1V<Option<P>> invoker)
    {
        this.field = field;
        this.type = type;
        this.existingData = existingData;

        this.invoker = invoker;
        this.createCaptor = ArgumentCaptor.forClass(LentaBlockCreateData.class);
        this.modifyCaptor = ArgumentCaptor.forClass(LentaBlockModifyData.class);

        Mockito.when(lentaManager.findOrCreateBlock(Mockito.any(), createCaptor.capture(), Mockito.any()))
                .thenReturn(Mockito.mock(FindOrCreateResult.class));

        Mockito.when(lentaManager.findAndUpdateOrCreateBlock(Mockito.any(), modifyCaptor.capture(), Mockito.any()))
                .thenReturn(Mockito.mock(FindOrCreateResult.class));

        Mockito.when(lentaManager.findCachedAndUpdateOrCreateBlock(Mockito.any(), modifyCaptor.capture(), Mockito.any()))
                .thenReturn(Mockito.mock(FindOrCreateResult.class));
    }

    private Either<LentaBlockCreateData, LentaBlockModifyData> invokeAndCapture(Option<P> parameter) {
        int creations = createCaptor.getAllValues().size();
        int modifications = modifyCaptor.getAllValues().size();

        invoker.apply(parameter);

        if (creations == createCaptor.getAllValues().size() - 1) {
            Check.equals(modifications, modifyCaptor.getAllValues().size(), "Captured more than once");

            return Either.left(createCaptor.getValue());
        }
        if (modifications == modifyCaptor.getAllValues().size() - 1) {

            return Either.right(modifyCaptor.getValue());
        }

        if (creations != createCaptor.getAllValues().size() || modifications != modifyCaptor.getAllValues().size()) {
            throw new IllegalStateException("Captured more than once");
        }
        throw new IllegalStateException("Not captured");
    }

    public FieldUpdate<V> create(P parameter) {
        return create(Option.of(parameter));
    }

    public FieldUpdate<V> create(Option<P> parameter) {
        Either<LentaBlockCreateData, LentaBlockModifyData> captured = invokeAndCapture(parameter);

        CreateHandler.Action action = captured.fold(
                create -> create.handler.getAction(TestUtils.consBlock(type, Cf.map())),
                modify -> modify.createHandler.getAction(TestUtils.consBlock(type, Cf.map())));

        return action.specific.isRefusal()
                ? FieldUpdate.ignored()
                : FieldUpdate.changedTo(field.getO(action.specific.getData()));
    }

    public FieldUpdateCaptorClosure<P, V> update(V previous) {
        return new FieldUpdateCaptorClosure<>(this, Option.of(previous));
    }

    public FieldUpdateCaptorClosure<P, V> update(Option<V> previous) {
        return new FieldUpdateCaptorClosure<>(this, previous);
    }

    public FieldUpdate<V> update(V previous, P parameter) {
        return update(Option.of(previous), Option.of(parameter));
    }

    public FieldUpdate<V> update(Option<V> previous, P parameter) {
        return update(previous, Option.of(parameter));
    }

    public FieldUpdate<V> update(V previous, Option<P> parameter) {
        return update(Option.of(previous), parameter);
    }

    public FieldUpdate<V> update(Option<V> previous, Option<P> parameter) {
        Either<LentaBlockCreateData, LentaBlockModifyData> captured = invokeAndCapture(parameter);

        if (captured.isLeft()) {
            return FieldUpdate.ignored();
        }
        UpdateHandler.Action action = captured.getRight().updateHandler.getAction(
                TestUtils.consBlock(type, existingData.plus(Cf.toMap(previous.map(field::toData)))));

        if (action instanceof UpdateHandler.Update && ((UpdateHandler.Update) action).throttled) {
            return FieldUpdate.throttled(field.getO(((UpdateHandler.Update) action).specific.get()));
        }
        if (action instanceof UpdateHandler.Update) {
            return FieldUpdate.changedTo(field.getO(((UpdateHandler.Update) action).specific.get()));
        }
        if (action instanceof UpdateHandler.Ignore) {
            return FieldUpdate.ignored();
        }
        throw new IllegalStateException("Unexpected update action " + action);
    }
}
