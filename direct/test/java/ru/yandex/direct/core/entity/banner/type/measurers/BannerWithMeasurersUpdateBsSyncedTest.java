package ru.yandex.direct.core.entity.banner.type.measurers;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithMeasurers.MEASURERS;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithMeasurersUpdateBsSyncedTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {
    @Autowired
    private DslContextProvider dslContextProvider;

    private long creativeId;
    private LocalDateTime someTime;
    private ClientInfo defaultClientInfo;

    @Before
    public void setUp() throws Exception {
        List<OldBannerMeasurer> measurers = List.of(
                new OldBannerMeasurer()
                        .withBannerMeasurerSystem(OldBannerMeasurerSystem.ADRIVER)
                        .withParams("{\"json\": \"json\"}")
                        .withHasIntegration(true),
                new OldBannerMeasurer()
                        .withBannerMeasurerSystem(OldBannerMeasurerSystem.MOAT)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false));
        defaultClientInfo = steps.clientSteps().createDefaultClient();
        creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultHtml5Creative(defaultClientInfo, creativeId);
        bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeId)
                        .withMeasurers(measurers), defaultClientInfo);
        someTime = LocalDateTime.now().minusMinutes(7).withNano(0);
        setLastChange();
    }

    @Test
    public void updateBanner_NoChange_StatusBsSyncedYes() {
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class);

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);
        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        assertThat(actualBanner.getLastChange(), equalTo(someTime));
    }

    @Test
    public void updateBanner_ChangeMeasurers_StatusBsSyncedNo() {
        var newMeasurers = List.of(new BannerMeasurer()
                .withBannerMeasurerSystem(BannerMeasurerSystem.SIZMEK)
                .withParams("{\"json\": \"json\"}")
                .withHasIntegration(true));
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(newMeasurers, MEASURERS);

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat(actualBanner.getLastChange(), not(equalTo(someTime)));
    }

    @Test
    public void updateBanner_ChangeMeasurersOrder_StatusBsSyncedYes() {
        List<BannerMeasurer> newMeasurers = List.of(
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.MOAT)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false),
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.ADRIVER)
                        .withParams("{\"json\": \"json\"}")
                        .withHasIntegration(true)
        );
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(newMeasurers, MEASURERS);

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        assertThat(actualBanner.getLastChange(), equalTo(someTime));
    }

    @Test
    public void updateBanner_ChangeMeasurersSameList_StatusBsSyncedYes() {
        List<BannerMeasurer> newMeasurers = List.of(
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.ADRIVER)
                        .withParams("{\"json\": \"json\"}")
                        .withHasIntegration(true),
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.MOAT)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false)
        );
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(newMeasurers, MEASURERS);

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        assertThat(actualBanner.getLastChange(), equalTo(someTime));
    }

    @Test
    public void updateBanner_DeleteMeasurers_StatusBsSyncedNo() {
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(emptyList(), MEASURERS);

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat(actualBanner.getLastChange(), not(equalTo(someTime)));
    }

    @Test
    public void updateBanner_AddMeasurers_StatusBsSyncedNo() {
        var newMeasurers = List.of(new BannerMeasurer()
                .withBannerMeasurerSystem(BannerMeasurerSystem.SIZMEK)
                .withParams("{\"json\": \"json\"}")
                .withHasIntegration(true));
        bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeId), defaultClientInfo);
        ModelChanges<CpmBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(newMeasurers, MEASURERS);
        setLastChange();

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat(actualBanner.getLastChange(), not(equalTo(someTime)));
    }

    @Test
    public void updateBanner_ChangeEmptyMeasurersToNull_StatusBsSyncedYes() {
        bannerInfo = steps.bannerSteps().createBanner(activeCpmBanner(null, null, creativeId)
                .withMeasurers(emptyList()), defaultClientInfo);
        ModelChanges<CpmBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(null, MEASURERS);
        setLastChange();

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        assertThat(actualBanner.getLastChange(), equalTo(someTime));
    }

    @Test
    public void updateBanner_ChangeEmptyMeasurersToEmptyList_StatusBsSyncedYes() {
        bannerInfo = steps.bannerSteps().createBanner(activeCpmBanner(null, null, creativeId)
                .withMeasurers(emptyList()), defaultClientInfo);
        ModelChanges<CpmBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(emptyList(), MEASURERS);
        setLastChange();

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        assertThat(actualBanner.getLastChange(), equalTo(someTime));
    }

    private void setLastChange() {
        dslContextProvider.ppc(bannerInfo.getShard())
                .update(BANNERS)
                .set(BANNERS.LAST_CHANGE, someTime)
                .where(BANNERS.BID.equal(bannerInfo.getBannerId()))
                .execute();
    }
}
