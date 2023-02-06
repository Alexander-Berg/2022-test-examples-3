package ru.yandex.autotests.directintapi.tests.bsfront;

import ru.yandex.autotests.direct.bsapi.BsDbSteps;
import ru.yandex.autotests.direct.bsapi.BsSteps;
import ru.yandex.autotests.direct.bsapi.CreativeGroupResponse;
import ru.yandex.autotests.direct.bsapi.Templates;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontChangeNotifyResponse;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.BsFrontRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bsfront.Creative;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.irt.testutils.ResourceUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/*
 * todo javadoc
 */
public class CreativesHelper {

    private BsSteps bsSteps;
    private ApiSteps apiSteps;
    private DirectJooqDbSteps dbSteps;

    public CreativesHelper(ApiSteps apiSteps) {
        this.apiSteps = apiSteps;
        this.dbSteps = new DirectJooqDbSteps();
    }

    public Integer createSomeCreativeInBS() {
        return createCreativeInBS(Templates.TEMPLATE_240x400, "name", "href");
    }

    public Integer createCreativeInBSWithStatusModerate(BsDbSteps.StatusModerate statusModerate) {
        Integer creativeId = createSomeCreativeInBS();
        getBsSteps().bsDbSteps().setCreativeStatusModerate(creativeId, statusModerate);
        return creativeId;
    }

    public Integer createCreativeInBS(Templates template, String name, String href) {
        getBsSteps().increaseIncrement(dbSteps.shardingSteps().getLastCreativeId());
        return getBsSteps().createCreative(template, name, href, logoFile());
    }

    public Integer createCreative(String client, String operator) {
        Integer creativeId = createSomeCreativeInBS();
        chandeNotify(creativeId, client, operator);
        return creativeId;
    }

    public Integer createCreativeWithStatusModerate(String client, String operator,
                                                    BsDbSteps.StatusModerate statusModerate) {
        Integer creativeId = createSomeCreativeInBS();
        chandeNotify(creativeId, client, operator);
        getBsSteps().bsDbSteps().setCreativeStatusModerate(creativeId, statusModerate);
        return creativeId;
    }

    public CreativeGroupResponse createSomeCreativeGroupInBS() {
        return createCreativeGroupInBS("test_group", Templates.TEMPLATE_240x400, "name", "href");
    }

    public CreativeGroupResponse createCreativeGroupInBS(String groupName, Templates template, String creativeName, String href) {
        return createCreativeGroupInBS(groupName, prepareBsCreative(template, creativeName, href, logoFile()));
    }

    public CreativeGroupResponse createCreativeGroupInBS(String name, ru.yandex.autotests.direct.bsapi.models.Creative... creatives) {
        return getBsSteps().createCreativeGroup(name, creatives);
    }

    public CreativeGroupResponse createCreativeGroupInBSWithStatusModerate(BsDbSteps.StatusModerate statusModerate) {
        CreativeGroupResponse creativeGroup = createSomeCreativeGroupInBS();
        creativeGroup.getCreativeIds().forEach(crId -> getBsSteps().bsDbSteps()
                .setCreativeStatusModerate(crId.intValue(), statusModerate));
        return creativeGroup;
    }

    public ru.yandex.autotests.direct.bsapi.models.Creative prepareBsCreative(Templates template, String name,
                                                                              String href, File logoFile) {
        getBsSteps().increaseIncrement(dbSteps.shardingSteps().getLastCreativeId());
        return getBsSteps().prepareCreative(template, name, href, logoFile);
    }

    /**
     * см protected/Intapi/BsFront.pm
     */
    private void chandeNotify(Integer creativeId, String client, String operator) {
        Integer uid = null;
        if (operator != null) {
            uid = Integer.parseInt(getApiSteps().userSteps
                    .clientFakeSteps().getClientData(operator).getPassportID());
        }

        //нотификация о создании
        BsFrontRequest bsFrontRequest = new BsFrontRequest()
                .withOperatorUid(uid)
                .withClientLogin(client)
                .withCreatives(new Creative().withId(creativeId.longValue()));

        checkResult(getApiSteps().userSteps.getDarkSideSteps().getBsFrontSteps().changeNotify(bsFrontRequest),
                creativeId);
    }

    private File logoFile() {
        return ResourceUtils.getResourceAsFile("ali.png");
    }

    private BsSteps getBsSteps() {
        if (bsSteps == null) {
            String settingsName = getApiSteps().userSteps.getDarkSideSteps().directEnvSteps().getSettingsName();
            bsSteps = new BsSteps(settingsName.replaceAll("^test", "ts"));
        }
        return bsSteps;
    }

    private ApiSteps getApiSteps() {
        return apiSteps;
    }

    private void checkResult(List<BsFrontChangeNotifyResponse> response, Integer creativeId) {
        BsFrontChangeNotifyResponse expectedResponse = new BsFrontChangeNotifyResponse();
        expectedResponse.setId(creativeId.longValue());
        expectedResponse.setResult(1);
        assumeThat("получен правильный ответ от BsFront.change_notify",
                response, beanDiffer(Collections.singletonList(expectedResponse)));
    }
}
