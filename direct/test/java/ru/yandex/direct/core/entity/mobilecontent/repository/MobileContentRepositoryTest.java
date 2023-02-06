package ru.yandex.direct.core.entity.mobilecontent.repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.mobilecontent.container.MobileAppStoreUrl;
import ru.yandex.direct.core.entity.mobilecontent.model.AgeLabel;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentForBsTransport;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.misc.dataSize.DataSize;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.core.entity.mobilecontent.model.AvailableAction.download;
import static ru.yandex.direct.core.entity.mobilecontent.service.UpdateMobileContentService.PROPERTIES_TO_UPDATE;
import static ru.yandex.direct.core.testing.data.TestMobileContents.iosMobileContent;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileContentRepositoryTest {
    @Autowired
    private Steps steps;

    @Autowired
    private MobileContentRepository repository;

    private int shard;
    private MobileContentInfo mobileContentWithBsSyncedNo;

    @Before
    public void before() {
        mobileContentWithBsSyncedNo = steps.mobileContentSteps().createDefaultMobileContent();
        shard = mobileContentWithBsSyncedNo.getShard();
    }

    @Test
    public void testGetMobileContentIdsWithStatusBsSynced() {
        MobileContentInfo mobileContentWithBsSyncedYes = steps.mobileContentSteps().createMobileContent(
                shard,
                new MobileContentInfo()
                        .withMobileContent(iosMobileContent()
                                .withStatusBsSynced(StatusBsSynced.YES)));
        MobileContentInfo mobileContentWithBsSyncedSending = steps.mobileContentSteps().createMobileContent(
                shard,
                new MobileContentInfo()
                        .withMobileContent(iosMobileContent()
                                .withStatusBsSynced(StatusBsSynced.SENDING)));

        List<Long> contentIds = repository.getMobileContentIdsWithStatusBsSynced(shard, StatusBsSynced.NO);

        assertThat(contentIds)
                .contains(mobileContentWithBsSyncedNo.getMobileContentId())
                .doesNotContain(
                        mobileContentWithBsSyncedYes.getMobileContentId(),
                        mobileContentWithBsSyncedSending.getMobileContentId())
                .as("Получаем только идентификаторы контента в статусе NO");
    }

    @Test
    public void testChangeStatusBsSyncedSuccess() {
        repository.changeStatusBsSynced(shard, StatusBsSynced.NO, StatusBsSynced.SENDING,
                Collections.singletonList(mobileContentWithBsSyncedNo.getMobileContentId()));

        MobileContent mobileContent = repository.getMobileContent(shard,
                mobileContentWithBsSyncedNo.getMobileContentId());
        assertThat(mobileContent.getStatusBsSynced())
                .isEqualTo(StatusBsSynced.SENDING)
                .as("Статус синхронизации успешно поменялся с NO на SENDING");
    }

    @Test
    public void testChangeStatusBsSyncedWrongStatus() {
        repository.changeStatusBsSynced(shard, StatusBsSynced.SENDING,
                StatusBsSynced.YES, Collections.singletonList(mobileContentWithBsSyncedNo.getMobileContentId()));

        MobileContent mobileContent = repository.getMobileContent(shard,
                mobileContentWithBsSyncedNo.getMobileContentId());
        assertThat(mobileContent.getStatusBsSynced())
                .isEqualTo(StatusBsSynced.NO)
                .as("Статус синхронизации не поменялся с NO на YES");
    }

    @Test
    public void testGetMobileContentForBsExportWithIdsProvided() {
        steps.mobileContentSteps().createMobileContent(
                shard,
                new MobileContentInfo()
                        .withMobileContent(iosMobileContent()
                                .withStatusBsSynced(StatusBsSynced.SENDING)));
        MobileContentInfo mobileContentWithBsSyncedSendingTwo = steps.mobileContentSteps().createMobileContent(
                shard,
                new MobileContentInfo()
                        .withMobileContent(iosMobileContent()
                                .withStatusBsSynced(StatusBsSynced.SENDING)));

        List<MobileContentForBsTransport> mobileContents =
                repository.getMobileContentForBsExport(shard,
                        asList(mobileContentWithBsSyncedSendingTwo.getMobileContentId(),
                                mobileContentWithBsSyncedNo.getMobileContentId()), limited(Integer.MAX_VALUE));
        List<Long> selectedIds = mapList(mobileContents, MobileContentForBsTransport::getId);

        assertThat(selectedIds)
                .containsOnly(mobileContentWithBsSyncedSendingTwo.getMobileContentId())
                .as("Попал только контент в статусе SENDING с переданным id");
    }

    @Test
    public void testGetMobileContentForBsExportWithLimitProvided() {
        MobileContentInfo mobileContentWithBsSyncedSendingOne = steps.mobileContentSteps().createMobileContent(
                shard,
                new MobileContentInfo()
                        .withMobileContent(iosMobileContent()
                                .withStatusBsSynced(StatusBsSynced.SENDING)));
        MobileContentInfo mobileContentWithBsSyncedSendingTwo = steps.mobileContentSteps().createMobileContent(
                shard,
                new MobileContentInfo()
                        .withMobileContent(iosMobileContent()
                                .withStatusBsSynced(StatusBsSynced.SENDING)));

        List<MobileContentForBsTransport> mobileContents =
                repository.getMobileContentForBsExport(shard,
                        asList(mobileContentWithBsSyncedSendingTwo.getMobileContentId(),
                                mobileContentWithBsSyncedSendingOne.getMobileContentId()), limited(1));
        List<Long> selectedIds = mapList(mobileContents, MobileContentForBsTransport::getId);

        assertThat(selectedIds)
                .containsOnly(mobileContentWithBsSyncedSendingOne.getMobileContentId())
                .as("Попал только контент в статусе SENDING с переданным id и отсортированный по id и порядку " +
                        "создания");
    }

    @Test
    public void getOrCreate_onEmptyList_returnsEmptyResult() {
        DSLContext dslContext = repository.dslContextProvider.ppc(shard);
        List<Long> result = repository.getOrCreate(dslContext, mobileContentWithBsSyncedNo.getClientId(), emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    public void getOrCreate_onExistent_returnsExistentId() {
        DSLContext dslContext = repository.dslContextProvider.ppc(shard);
        MobileAppStoreUrl existent =
                MobileAppStoreUrl.fromMobileContent(mobileContentWithBsSyncedNo.getMobileContent());
        List<Long> result =
                repository.getOrCreate(dslContext, mobileContentWithBsSyncedNo.getClientId(), singletonList(existent));
        assertThat(result).containsOnly(mobileContentWithBsSyncedNo.getMobileContentId());
    }

    @Test
    public void getOrCreate_onNonExistent_returnsSomeId() {
        DSLContext dslContext = repository.dslContextProvider.ppc(shard);

        MobileAppStoreUrl nonExistent = createNewMobileAppStoreUrlByTemplate(
                mobileContentWithBsSyncedNo.getMobileContent());
        List<Long> result = repository.getOrCreate(
                dslContext, mobileContentWithBsSyncedNo.getClientId(), singletonList(nonExistent));
        assertThat(result).doesNotContainNull();
    }

    @Test
    public void getOrCreate_onNonExistent_setsTriesCountToZero() {
        DSLContext dslContext = repository.dslContextProvider.ppc(shard);

        MobileAppStoreUrl nonExistent = createNewMobileAppStoreUrlByTemplate(
                mobileContentWithBsSyncedNo.getMobileContent());
        List<Long> result = repository.getOrCreate(
                dslContext, mobileContentWithBsSyncedNo.getClientId(), singletonList(nonExistent));
        MobileContent mobileContent = repository.getMobileContent(shard,
                result.get(0));
        assertThat(mobileContent.getTriesCount()).isEqualTo(0);
    }

    @Test
    public void getOrCreate_onDuplicatedNonExistent_returnsEqualIds() {
        DSLContext dslContext = repository.dslContextProvider.ppc(shard);

        MobileAppStoreUrl nonExistent = createNewMobileAppStoreUrlByTemplate(
                mobileContentWithBsSyncedNo.getMobileContent());
        List<Long> result = repository.getOrCreate(
                dslContext, mobileContentWithBsSyncedNo.getClientId(), asList(nonExistent, nonExistent));
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(result.get(1));
    }

    @Test
    public void getOrCreate_onExistentAndNot() {
        DSLContext dslContext = repository.dslContextProvider.ppc(shard);

        MobileAppStoreUrl existent = MobileAppStoreUrl.fromMobileContent(
                mobileContentWithBsSyncedNo.getMobileContent());
        MobileAppStoreUrl nonExistent = createNewMobileAppStoreUrlByTemplate(
                mobileContentWithBsSyncedNo.getMobileContent());

        List<Long> result = repository.getOrCreate(
                dslContext, mobileContentWithBsSyncedNo.getClientId(), asList(existent, nonExistent));
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(mobileContentWithBsSyncedNo.getMobileContentId());
        assertThat(result.get(1)).isNotEqualTo(mobileContentWithBsSyncedNo.getMobileContentId());
    }

    @Test
    public void updateMobileContent_canUpdateAllFieldsFromUpdateService() {
        Long id = mobileContentWithBsSyncedNo.getMobileContent().getId();
        long domainId = steps.domainSteps().createDomain(shard).getDomainId();
        String testString = "test";
        long testLong = 1234L;

        ModelChanges<MobileContent> mc = new ModelChanges<>(id, MobileContent.class);

        mc.process(testString, MobileContent.NAME);
        mc.process(singletonMap(testString, emptyMap()), MobileContent.PRICES);
        mc.process(BigDecimal.valueOf(10d).setScale(2, BigDecimal.ROUND_UNNECESSARY), MobileContent.RATING);
        mc.process(testLong, MobileContent.RATING_VOTES);
        mc.process(testString, MobileContent.ICON_HASH);
        mc.process(testString, MobileContent.MIN_OS_VERSION);
        mc.process(DataSize.fromBytes(testLong), MobileContent.APP_SIZE);
        mc.process(singleton(download), MobileContent.AVAILABLE_ACTIONS);
        mc.process(domainId, MobileContent.PUBLISHER_DOMAIN_ID);
        mc.process(testString, MobileContent.GENRE);
        mc.process(AgeLabel._6_2B, MobileContent.AGE_LABEL);
        mc.process("my.bundle.id", MobileContent.BUNDLE_ID);
        mc.process(5000L, MobileContent.DOWNLOADS);
        mc.process(
                List.of(Map.of("width", "200", "path", "xxxx", "height", "300")),
                MobileContent.SCREENS);

        assumeThat(mc.getChangedPropsNames(), equalTo(PROPERTIES_TO_UPDATE));

        repository.updateMobileContent(shard,
                singletonList(mc.applyTo(mobileContentWithBsSyncedNo.getMobileContent())));

        MobileContent result = repository.getMobileContent(shard, id);
        for (ModelProperty modelProperty : mc.getChangedPropsNames()) {
            assertThat(modelProperty.get(result)).isEqualTo(mc.getChangedProp(modelProperty));
        }
    }

    private MobileAppStoreUrl createNewMobileAppStoreUrlByTemplate(MobileContent mobileContentTemplate) {
        String uniqStoreContentId = "yandex.app.xxx" + System.currentTimeMillis();
        return MobileAppStoreUrl.fromMobileContent(mobileContentTemplate.withStoreContentId(uniqStoreContentId));
    }

}
