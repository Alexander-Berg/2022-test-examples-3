package ru.yandex.direct.core.entity.vcard.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.entity.vcard.repository.internal.DbAddress;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.repository.TestAddressesRepository;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.VcardSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static ru.yandex.direct.core.entity.vcard.service.validation.VcardDefects.vcardIsInUse;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VcardServiceDeleteTest {

    @Autowired
    private VcardService vcardService1;

    @Autowired
    private VcardRepository vcardRepository;

    @Autowired
    private TestAddressesRepository testAddressesRepository;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private VcardSteps vcardSteps;

    private CampaignInfo campaignInfo;

    private int shard;
    private long clientUid;
    private ClientId clientId;

    @Before
    public void before() {
        campaignInfo = campaignSteps.createActiveTextCampaign();
        shard = campaignInfo.getShard();
        clientUid = campaignInfo.getUid();
        clientId = campaignInfo.getClientId();
    }

    // проверка результата операции

    @Test
    public void deleteVcards_OneValidVcardId_ResultIsSuccessful() {
        long vcardId = createUnusedVcard1().getId();
        MassResult<Long> result = delete(singletonList(vcardId));
        assertThat(result).is(matchedBy(isSuccessful(true)));
    }

    @Test
    public void deleteVcards_TwoValidVcardIds_ResultIsSuccessful() {
        long vcardId1 = createUnusedVcard1().getId();
        long vcardId2 = createUnusedVcard2().getId();
        MassResult<Long> result = delete(asList(vcardId1, vcardId2));
        assertThat(result).is(matchedBy(isSuccessful(true, true)));
    }

    @Test
    public void deleteVcards_OneInvalidVcardId_ResultHasElementError() {
        long vcardId = createUsedVcard1().getId();
        MassResult<Long> result = delete(singletonList(vcardId));
        var softly = new SoftAssertions();
        softly.assertThat(result).is(matchedBy(isSuccessful(false)));
        softly.assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), vcardIsInUse()))));
        softly.assertAll();
    }

    @Test
    public void deleteVcards_TwoInvalidVcardIds_ResultHasElementErrors() {
        long vcardId1 = createUsedVcard1().getId();
        long vcardId2 = -1;
        MassResult<Long> result = delete(asList(vcardId1, vcardId2));
        var softly = new SoftAssertions();
        softly.assertThat(result).is(matchedBy(isSuccessful(false, false)));
        softly.assertThat(result.getValidationResult())
                .is(matchedBy(allOf(
                        hasDefectDefinitionWith(validationError(path(index(0)), vcardIsInUse())),
                        hasDefectDefinitionWith(validationError(path(index(1)), objectNotFound()))
                        )));
        softly.assertAll();
    }

    @Test
    public void deleteVcards_OneValidAndOneInvalidVcardIds_ResultHasOneValidAndOneInvalidElements() {
        long vcardId1 = createUnusedVcard1().getId();
        long vcardId2 = -1;
        MassResult<Long> result = delete(asList(vcardId1, vcardId2));
        var softly = new SoftAssertions();
        softly.assertThat(result).is(matchedBy(isSuccessful(true, false)));
        softly.assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectDefinitionWith(validationError(path(index(1)), objectNotFound()))));
        softly.assertAll();
    }

    // проверка фактического удаления

    @Test
    public void deleteVcards_OneValidVcardId_DeletesVcard() {
        long vcardId = createUnusedVcard1().getId();
        MassResult<Long> result = delete(singletonList(vcardId));
        Set<Long> existingVcardInMetabase = vcardSteps.getExistingIdsInMetabase(List.of(vcardId));
        var softly = new SoftAssertions();
        softly.assertThat(result).is(matchedBy(isSuccessful(true)));
        softly.assertThat(isVcardExists(vcardId))
                .as("после удаления карточка должна отсутствовать в базe")
                .isFalse();
        softly.assertThat(existingVcardInMetabase)
                .as("визитка отсутствует в метабазе")
                .doesNotContain(vcardId);
        softly.assertAll();
    }

    @Test
    public void deleteVcards_OneValidVcardIdAndTwoInvalid_DeletesOnlyValidItem() {
        long vcardId1 = createUnusedVcard1().getId();
        long vcardId2 = createUnusedVcard2().getId();
        MassResult<Long> result = delete(asList(vcardId2, vcardId1, vcardId2));

        Set<Long> existingVcardInMetabase = vcardSteps.getExistingIdsInMetabase(List.of(vcardId1, vcardId2));
        var softly = new SoftAssertions();
        softly.assertThat(result).is(matchedBy(isSuccessful(false, true, false)));
        softly.assertThat(isVcardExists(vcardId1))
                .as("после удаления правильно указанная карточка должна отсутствовать в базе")
                .isFalse();
        softly.assertThat(isVcardExists(vcardId2))
                .as("после попытки удаления неправильно указанная карточка должна присутствовать в базе")
                .isTrue();
        softly.assertThat(existingVcardInMetabase)
                .as("удаленная визитка отсутствует в метабазе")
                .doesNotContain(vcardId1);
        softly.assertThat(existingVcardInMetabase)
                .as("неудаленная визитка осталась в метабазе")
                .contains(vcardId2);
        softly.assertAll();
    }

    @Test
    public void deleteVcards_OneInvalidVcardId_DoesNotDeleteAnything() {
        long vcardId = createUsedVcard1().getId();
        MassResult<Long> result = delete(singletonList(vcardId));

        Set<Long> existingVcardInMetabase = vcardSteps.getExistingIdsInMetabase(List.of(vcardId));
        var softly = new SoftAssertions();
        softly.assertThat(result).is(matchedBy(isSuccessful(false)));
        softly.assertThat(isVcardExists(vcardId))
                .as("после попытки удаления карточка должна присутствовать в базе")
                .isTrue();
        softly.assertThat(existingVcardInMetabase)
                .as("визитка осталась в метабазе")
                .contains(vcardId);
        softly.assertAll();
    }

    private boolean isVcardExists(Long vcardId) {
        return !getExistingVcards(singletonList(vcardId)).isEmpty();
    }

    private Set<Long> getExistingVcards(Collection<Long> vcardIds) {
        List<Vcard> vcards = vcardRepository.getVcards(shard, clientUid, vcardIds);
        return StreamEx.of(vcards)
                .map(Vcard::getId)
                .toSet();
    }

    private Set<Long> getExistingAddresses(Collection<Long> addressIds) {
        Map<Long, DbAddress> addresses = testAddressesRepository.getAddresses(shard, clientId, addressIds);
        return addresses.keySet();
    }

    private MassResult<Long> delete(List<Long> vcardIds) {
        long operatorUid = clientUid;
        return vcardService1.deleteVcards(vcardIds, operatorUid, clientId);
    }

    private Vcard createUnusedVcard1() {
        return vcardSteps.createVcard(fullVcard().withHouse("10"), campaignInfo).getVcard();
    }

    private Vcard createUnusedVcard2() {
        return vcardSteps.createVcard(fullVcard().withHouse("11"), campaignInfo).getVcard();
    }

    private Vcard createUsedVcard1() {
        VcardInfo vcardInfo = vcardSteps.createVcard(fullVcard().withHouse("12"), campaignInfo);
        OldTextBanner banner = activeTextBanner(null, null).withVcardId(vcardInfo.getVcardId());
        bannerSteps.createBanner(banner, campaignInfo);
        return vcardInfo.getVcard();
    }

    private Vcard createUsedVcard2() {
        VcardInfo vcardInfo = vcardSteps.createVcard(fullVcard().withHouse("13"), campaignInfo);
        OldTextBanner banner = activeTextBanner(null, null).withVcardId(vcardInfo.getVcardId());
        bannerSteps.createBanner(banner, campaignInfo);
        return vcardInfo.getVcard();
    }
}
