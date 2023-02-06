package ru.yandex.direct.core.entity.keyword.service.validation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.keyword.container.CampaignIdAndKeywordIdPair;
import ru.yandex.direct.core.entity.keyword.container.KeywordDeleteInfo;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusarch;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignNoRights;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DeleteKeywordValidationServiceTest {

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CampaignSubObjectAccessCheckerFactory campaignSubObjectAccessCheckerFactory;

    @Autowired
    private DeleteKeywordValidationService deleteKeywordValidationService;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private TestBannerRepository testBannerRepository;

    @Autowired
    private Steps steps;

    private KeywordInfo keyword1;
    private KeywordInfo keyword2;
    private long operatorUid;
    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        keyword1 = steps.keywordSteps().createDefaultKeyword();
        keyword2 = steps.keywordSteps().createKeyword(keyword1.getAdGroupInfo());

        operatorUid = keyword1.getAdGroupInfo().getUid();
        clientId = keyword1.getAdGroupInfo().getClientId();
        shard = keyword1.getShard();
    }

    @Test
    public void validate_Successful() {
        ValidationResult<List<Long>, Defect> result = validate(asList(keyword1, keyword2));
        assertThat(result.hasAnyErrors(), is(false));
    }

    @Test
    public void validate_NullId_notNullDefect() {
        KeywordInfo nullIdKeyword = steps.keywordSteps().createDefaultKeyword();
        nullIdKeyword.getKeyword().setId(null);

        ValidationResult<List<Long>, Defect> result = validate(asList(keyword1, nullIdKeyword));
        assertThat(result, hasDefectDefinitionWith(validationError(path(index(1)), notNull())));
    }

    @Test
    public void validate_DoesNotExist_DoesNotExistDefect() {
        KeywordInfo deletedKeyword = steps.keywordSteps().createKeyword(keyword1.getAdGroupInfo());
        keywordRepository.deleteKeywords(dslContextProvider.ppc(shard).configuration(),
                Collections.singletonList(new CampaignIdAndKeywordIdPair(deletedKeyword.getCampaignId(),
                        deletedKeyword.getId())));
        ValidationResult<List<Long>, Defect> result = validate(asList(deletedKeyword, keyword1));
        assertThat(result, hasDefectDefinitionWith(validationError(path(index(0)), objectNotFound())));
    }

    @Test
    public void validate_OperatorHasNoRights_NoRightsCantWriteDefect() {
        RbacService rbacService = mock(RbacService.class);
        when(rbacService.getWritableCampaigns(anyLong(), anyCollection())).thenReturn(Collections.emptySet());
        DeleteKeywordValidationService deleteKeywordValidationService =
                new DeleteKeywordValidationService(rbacService, campaignSubObjectAccessCheckerFactory);

        ValidationResult<List<Long>, Defect> result =
                validate(asList(keyword1, keyword2), deleteKeywordValidationService);
        assertThat(result, hasDefectDefinitionWith(validationError(path(index(0)), campaignNoRights())));
        assertThat(result, hasDefectDefinitionWith(validationError(path(index(1)), campaignNoRights())));
    }

    @Test
    public void validate_CantDeleteFormArchivedCampaign_nonArchivedCampaign() {
        long campaignId = keyword1.getCampaignId();
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.ARCHIVED, CampaignsArchived.Yes)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
        ValidationResult<List<Long>, Defect> result = validate(asList(keyword1, keyword2));
        assertThat(result,
                hasDefectDefinitionWith(validationError(path(index(0)), archivedCampaignModification())));
        assertThat(result,
                hasDefectDefinitionWith(validationError(path(index(1)), archivedCampaignModification())));

    }

    @Test
    public void validate_ArchivedAdGroup_CanDeleteKeyword() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createDefaultBanner(keyword1.getAdGroupInfo());
        int shard = bannerInfo.getShard();
        Long bannerId = bannerInfo.getBannerId();

        testBannerRepository.updateStatusArchive(shard, bannerId, BannersStatusarch.Yes);

        ValidationResult<List<Long>, Defect> result = validate(asList(keyword1, keyword2));

        assertThat("Должна быть возможность удалять ключевые фразы из архивных групп",
                result, hasNoDefectsDefinitions());
    }

    private ValidationResult<List<Long>, Defect> validate(List<KeywordInfo> keywords) {
        return validate(keywords, deleteKeywordValidationService);
    }

    private ValidationResult<List<Long>, Defect> validate(List<KeywordInfo> keywords,
                                                          DeleteKeywordValidationService deleteKeywordValidationService) {
        List<Long> ids = mapList(keywords, KeywordInfo::getId);
        Map<Long, KeywordDeleteInfo> keywordDeleteInfo = keywordRepository.getKeywordDeleteInfo(shard, ids);
        return deleteKeywordValidationService.validate(ids, keywordDeleteInfo, operatorUid, clientId);
    }
}
