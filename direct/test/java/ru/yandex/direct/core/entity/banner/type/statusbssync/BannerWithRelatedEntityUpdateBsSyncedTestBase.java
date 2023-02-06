package ru.yandex.direct.core.entity.banner.type.statusbssync;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public abstract class BannerWithRelatedEntityUpdateBsSyncedTestBase<N extends BannerWithSystemFields> extends BannerClientInfoUpdateOperationTestBase {

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void updateBanner_NoChange_StatusBsSyncedYes() {
        Long bid = createBannerWithRelatedEntity();

        ModelChanges<N> modelChanges = getPlainModelChanges(bid);

        updateAndCheckResult(modelChanges, StatusBsSynced.YES);
    }

    @Test
    public void updateBanner_AddProperty_StatusBsSyncedNo() {
        Long bid = createPlainBanner();

        ModelChanges<N> modelChanges = getPlainModelChanges(bid);
        setNewRelatedEntity(modelChanges);

        updateAndCheckResult(modelChanges, StatusBsSynced.NO);
    }

    @Test
    public void updateBanner_ChangeProperty_StatusBsSyncedNo() {
        Long bid = createBannerWithRelatedEntity();

        ModelChanges<N> modelChanges = getPlainModelChanges(bid);
        setNewRelatedEntity(modelChanges);

        updateAndCheckResult(modelChanges, StatusBsSynced.NO);
    }

    @Test
    public void updateBanner_DeleteProperty_StatusBsSyncedNo() {
        Long bid = createBannerWithRelatedEntity();

        ModelChanges<N> modelChanges = getPlainModelChanges(bid);
        deleteRelatedEntity(modelChanges);

        updateAndCheckResult(modelChanges, StatusBsSynced.NO);
    }

    private void updateAndCheckResult(ModelChanges<N> modelChanges, StatusBsSynced expectedStatus) {
        Long id = prepareAndApplyValid(modelChanges);
        N actualBanner = getBanner(id);
        assertThat(actualBanner.getStatusBsSynced(), equalTo(expectedStatus));
    }

    protected abstract Long createPlainBanner();

    protected abstract Long createBannerWithRelatedEntity();

    protected abstract ModelChanges<N> getPlainModelChanges(Long bannerId);

    protected abstract void setNewRelatedEntity(ModelChanges<N> modelChanges);

    protected abstract void deleteRelatedEntity(ModelChanges<N> modelChanges);

}
