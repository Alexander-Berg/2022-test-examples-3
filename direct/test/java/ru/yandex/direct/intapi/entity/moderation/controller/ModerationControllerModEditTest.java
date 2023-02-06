package ru.yandex.direct.intapi.entity.moderation.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.moderation.model.modedit.ModerationEditFieldStatus;
import ru.yandex.direct.intapi.entity.moderation.model.modedit.ModerationEditObjectResult;
import ru.yandex.direct.intapi.entity.moderation.model.modedit.ModerationEditObjectsResult;
import ru.yandex.direct.intapi.entity.moderation.model.modedit.ModerationEditReplacement;
import ru.yandex.direct.intapi.entity.moderation.model.modedit.ModerationEditStatus;
import ru.yandex.direct.intapi.entity.moderation.model.modedit.ModerationEditType;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@IntApiTest
@ParametersAreNonnullByDefault
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationControllerModEditTest {

    @Autowired
    private ModerationController moderationController;

    @Autowired
    private Steps steps;

    @Autowired
    private UserService userService;

    @Autowired
    private BannerRepository bannerRepository;

    private MockMvc mockMvc;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;

    private TextBannerInfo bannerInfoBadStatusmoderate;
    private TextBannerInfo bannerInfoChanged;
    private TextBannerInfo bannerInfoNotChanged;

    private static final String NEW_TITLE = "New title";
    private static final String NEW_TITLE_EXTENSION = "New title extension";
    private static final String NEW_BODY = "New body";

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(moderationController).build();

        clientInfo = steps.clientSteps().createClient(defaultClient());
        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        bannerInfoBadStatusmoderate = steps.bannerSteps().createActiveTextBanner(campaignInfo);
        bannerRepository.moderation.updateStatusModerate(bannerInfoBadStatusmoderate.getShard(),
                List.of(bannerInfoBadStatusmoderate.getBannerId()), BannerStatusModerate.READY);

        bannerInfoChanged = steps.bannerSteps().createActiveTextBanner(campaignInfo);
        bannerRepository.moderation.updateStatusModerate(bannerInfoChanged.getShard(),
                List.of(bannerInfoChanged.getBannerId()), BannerStatusModerate.SENT);

        bannerInfoNotChanged = steps.bannerSteps().createActiveTextBanner(campaignInfo);
        bannerRepository.moderation.updateStatusModerate(bannerInfoNotChanged.getShard(),
                List.of(bannerInfoNotChanged.getBannerId()), BannerStatusModerate.SENT);
    }

    @Test
    public void testTopLevelValidationError() {
        List<ModerationEditReplacement> replacements = Collections.emptyList();
        var response = getResponse(replacements);
        Assertions.assertNull(response.getResults());
        var vr = response.getValidationResult();
        Assertions.assertNotNull(vr);
        Assertions.assertEquals(1, vr.getErrors().size());
        Assertions.assertEquals(CollectionDefectIds.Size.INVALID_COLLECTION_SIZE.getCode(),
                vr.getErrors().get(0).getCode());
    }

    @Test
    public void testSeveralReplacements() {
        var replacementValidationError = new ModerationEditReplacement();

        var replacementNotFound = new ModerationEditReplacement();
        replacementNotFound.setType(ModerationEditType.BANNER);
        replacementNotFound.setId(100500L);
        replacementNotFound.setOldData(Map.of("title", "Old title"));
        replacementNotFound.setNewData(Map.of("title", "New title"));

        var replacementBadStatusmoderate = new ModerationEditReplacement();
        replacementBadStatusmoderate.setType(ModerationEditType.BANNER);
        replacementBadStatusmoderate.setId(bannerInfoBadStatusmoderate.getBannerId());
        replacementBadStatusmoderate.setOldData(Map.of("title", "Old title"));
        replacementBadStatusmoderate.setNewData(Map.of("title", "New title"));

        var replacementNotChanged = new ModerationEditReplacement();
        replacementNotChanged.setType(ModerationEditType.BANNER);
        replacementNotChanged.setId(bannerInfoNotChanged.getBannerId());
        replacementNotChanged.setOldData(Map.of("title", "Old title"));
        replacementNotChanged.setNewData(Map.of("title", "New title"));

        var replacementChanged = new ModerationEditReplacement();
        replacementChanged.setType(ModerationEditType.BANNER);
        replacementChanged.setId(bannerInfoChanged.getBannerId());
        replacementChanged.setOldData(
                Map.of(
                        "title", "Old title",
                        "body", bannerInfoChanged.getBanner().getBody()
                )
        );
        replacementChanged.setNewData(
                Map.of(
                        "title", NEW_TITLE,
                        "body", NEW_BODY
                )
        );

        var replacements = List.of(
                replacementValidationError,
                replacementNotFound,
                replacementBadStatusmoderate,
                replacementNotChanged,
                replacementChanged
        );
        var response = getResponse(replacements);

        Assertions.assertEquals(replacements.size(), response.getResults().size());
        Assertions.assertNull(response.getValidationResult());

        checkValidationErrorResult(response.getResults().get(0));
        checkNotFoundResult(response.getResults().get(1));
        checkBadStatusResult(response.getResults().get(2));
        checkNotChangedResult(response.getResults().get(3));
        checkChangedResult(response.getResults().get(4));
    }

    private ModerationEditObjectsResult getResponse(List<ModerationEditReplacement> replacements) {
        String r = "";
        try {
            r = mockMvc
                    .perform(post("/moderation/mod_edit")
                            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                            .content(JsonUtils.toJson(replacements)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            System.err.println(r);
        } catch (Exception e) {
            Assertions.fail("Не должно быть Exception: " + e.getMessage());
        }

        return JsonUtils.fromJson(r, ModerationEditObjectsResult.class);
    }

    private void checkValidationErrorResult(ModerationEditObjectResult result) {
        Assertions.assertEquals(ModerationEditStatus.VALIDATION_ERROR, result.getEditStatus());
        Assertions.assertNotNull(result.getValidationResult());
        Assertions.assertNull(result.getFieldResults());
    }

    private void checkNotFoundResult(ModerationEditObjectResult result) {
        Assertions.assertNull(result.getFieldResults());
        Assertions.assertNull(result.getValidationResult());
        Assertions.assertEquals(ModerationEditStatus.NOT_FOUND, result.getEditStatus());
    }

    private void checkBadStatusResult(ModerationEditObjectResult result) {
        Assertions.assertNull(result.getValidationResult());
        Assertions.assertNull(result.getFieldResults());
        Assertions.assertEquals(ModerationEditStatus.NOT_APPLICABLE_STATUS_MODERATE, result.getEditStatus());
        checkUpdatedBanner(bannerInfoBadStatusmoderate, false, false, false);
    }

    private void checkNotChangedResult(ModerationEditObjectResult result) {
        Assertions.assertNull(result.getValidationResult());
        Assertions.assertEquals(ModerationEditStatus.NOT_CHANGED, result.getEditStatus());
        Assertions.assertNotNull(result.getFieldResults());
        Assertions.assertEquals(1, result.getFieldResults().size());
        Assertions.assertEquals("title", result.getFieldResults().get(0).getField());
        Assertions.assertEquals(ModerationEditFieldStatus.OLD_VALUE_MISMATCH,
                result.getFieldResults().get(0).getStatus());

        var updatedBanner = bannerRepository.type.getSafely(
                bannerInfoNotChanged.getShard(),
                List.of(bannerInfoNotChanged.getBannerId()),
                TextBanner.class
        ).get(0);
        Assertions.assertEquals(bannerInfoNotChanged.getBanner().getTitle(), updatedBanner.getTitle());
        Assertions.assertEquals(bannerInfoNotChanged.getBanner().getTitleExtension(),
                updatedBanner.getTitleExtension());
        Assertions.assertEquals(bannerInfoNotChanged.getBanner().getBody(), updatedBanner.getBody());
        checkUpdatedBanner(bannerInfoNotChanged, false, false, false);
    }

    private void checkChangedResult(ModerationEditObjectResult result) {
        Assertions.assertNull(result.getValidationResult());
        Assertions.assertEquals(ModerationEditStatus.CHANGED, result.getEditStatus());
        Assertions.assertNotNull(result.getFieldResults());
        Assertions.assertEquals(2, result.getFieldResults().size());
        var title = result.getFieldResults().stream().filter(r -> "title".equals(r.getField())).findFirst();
        var body = result.getFieldResults().stream().filter(r -> "body".equals(r.getField())).findFirst();
        Assertions.assertTrue(title.isPresent());
        Assertions.assertTrue(body.isPresent());
        Assertions.assertEquals(ModerationEditFieldStatus.OLD_VALUE_MISMATCH, title.get().getStatus());
        Assertions.assertEquals(ModerationEditFieldStatus.CHANGED, body.get().getStatus());
        checkUpdatedBanner(bannerInfoChanged, false, false, true);
    }

    private void checkUpdatedBanner(TextBannerInfo bannerInfo,
                                    boolean updateTitle,
                                    boolean updateTitleExtension,
                                    boolean updateBody) {
        var updatedBanner = bannerRepository.type.getSafely(
                bannerInfo.getShard(),
                List.of(bannerInfo.getBannerId()),
                TextBanner.class
        ).get(0);
        Assertions.assertEquals(updateTitle ? NEW_TITLE : bannerInfo.getBanner().getTitle(), updatedBanner.getTitle());
        Assertions.assertEquals(updateTitleExtension ? NEW_TITLE_EXTENSION : bannerInfo.getBanner().getTitleExtension(),
                updatedBanner.getTitleExtension());
        Assertions.assertEquals(updateBody ? NEW_BODY : bannerInfo.getBanner().getBody(), updatedBanner.getBody());
    }
}
