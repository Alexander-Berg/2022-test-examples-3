package ru.yandex.market.clab.common.test.assertions;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.mbo.http.ModelEdit;

import static ru.yandex.market.clab.common.mbo.ProtoUtils.toByteArray;

/**
 * @author anmalysh
 * @since 1/21/2019
 */
public class GoodAssert extends AbstractObjectAssert<GoodAssert, Good> {

    public GoodAssert(Good actual) {
        super(actual, GoodAssert.class);
    }

    public static GoodAssert assertThatGood(Good actual) {
        return new GoodAssert(actual);
    }

    public void hasStorageHierarchy(ModelEdit.Hierarchy hierarchy) {
        isNotNull();
        Assertions.assertThat(actual.getStorageModel()).isEqualTo(toByteArray(hierarchy.getModel()));
        Assertions.assertThat(actual.getStorageModification()).isEqualTo(toByteArray(hierarchy.getModification()));
        Assertions.assertThat(actual.getStorageMsku()).isEqualTo(toByteArray(hierarchy.getSku()));
    }

    public void hasBaseHierarchy(ModelEdit.Hierarchy hierarchy) {
        isNotNull();
        Assertions.assertThat(actual.getBaseModel()).isEqualTo(toByteArray(hierarchy.getModel()));
        Assertions.assertThat(actual.getBaseModification()).isEqualTo(toByteArray(hierarchy.getModification()));
        Assertions.assertThat(actual.getBaseMsku()).isEqualTo(toByteArray(hierarchy.getSku()));
    }

    public void hasBaseLastReadHierarchy(ModelEdit.Hierarchy hierarchy) {
        isNotNull();
        Assertions.assertThat(actual.getBaseLastReadModel()).isEqualTo(toByteArray(hierarchy.getModel()));
        Assertions.assertThat(actual.getBaseLastReadModification()).isEqualTo(toByteArray(hierarchy.getModification()));
        Assertions.assertThat(actual.getBaseLastReadMsku()).isEqualTo(toByteArray(hierarchy.getSku()));
    }

    public void hasEditedHierarchy(ModelEdit.Hierarchy hierarchy) {
        isNotNull();
        Assertions.assertThat(actual.getEditedModel()).isEqualTo(toByteArray(hierarchy.getModel()));
        Assertions.assertThat(actual.getEditedModification()).isEqualTo(toByteArray(hierarchy.getModification()));
        Assertions.assertThat(actual.getEditedMsku()).isEqualTo(toByteArray(hierarchy.getSku()));
    }
}
