package ru.yandex.autotests.direct.cmd.steps.retargeting;

import java.math.BigInteger;
import java.util.Random;

import org.apache.commons.lang3.RandomUtils;

import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxSaveRetargetingCondResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.enums.TargetingCategoriesState;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.enums.TargetingCategoriesTargetingType;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.tables.records.TargetingCategoriesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.data.retargeting.CommonAjaxRetConditionResponse.RESULT_OK;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class RetargetingHelper {
    private static final String RET_COND_TEMPLATE = "cmd.common.request.retargetingCondition.AjaxDeleteRetargetingCondValidationNegativeTest";

    // тестовые интересы добавляются скриптом prepare_test_db.pl при переналивке базы ТС
    public static final Long BASE_IMPORT_ID = 1000000000L;
    public static final Long PARENT_IMPORT_ID = 1000000008L;
    public static final Long CHILD_IMPORT_ID = 1000000009L;

    private RetargetingHelper() {}

    // В репозитории получения интересов из БД есть кеширования - созданые интересы сразу не подхватываются и
    // это приводит к миганию тестов в регресии. Подробнее см. DIRECT-97214
    @Deprecated
    public static TargetingCategoriesRecord createTargetCategory(String client, Long parentCategory) {
        TargetingCategoriesRecord targetingCategoriesRecord = new TargetingCategoriesRecord();
        targetingCategoriesRecord.setTargetingType(TargetingCategoriesTargetingType.rmp_interest);
        targetingCategoriesRecord.setOriginalName("testOriginalName");
        targetingCategoriesRecord.setName("testName");
        targetingCategoriesRecord.setImportId(BigInteger.valueOf(RandomUtils.nextLong(1,  Integer.MAX_VALUE)));
        targetingCategoriesRecord.setState(TargetingCategoriesState.Submitted);
        targetingCategoriesRecord.setOrderNum(0L);
        targetingCategoriesRecord.setParentCategoryId(parentCategory);

        TestEnvironment.newDbSteps().interestSteps()
                .saveTargetingCategoriesRecords(targetingCategoriesRecord);
        return targetingCategoriesRecord;
    }

    public static Long createDefaultRetargetingCondition(DirectCmdRule cmdRule, String client) {
        RetargetingCondition retCondition = BeanLoadHelper.loadCmdBean(RET_COND_TEMPLATE, RetargetingCondition.class);
        AjaxSaveRetargetingCondResponse response =
                cmdRule.cmdSteps().retargetingSteps().saveRetargetingCondition(retCondition, client);
        assumeThat("получен положительный ответ на создание условия", response.getResult(), equalTo(RESULT_OK));
        return response.getRetCondId();
    }

    // получаем Id одного случайного интереса из предварительно добавленных в базу скриптом prepare_test_db.pl
    public static Long getRandomTargetCategoryId() {
        Long importId = new Random().nextInt(7) + BASE_IMPORT_ID;
        TargetingCategoriesRecord targetingCategoriesRecord = TestEnvironment.newDbSteps().interestSteps().getTargetingCategoriesRecord
                (TargetingCategoriesTargetingType.rmp_interest, importId);
        return targetingCategoriesRecord.getCategoryId();
    }

    public static Long getParentCategoryId() {
        return TestEnvironment.newDbSteps().interestSteps().getTargetingCategoriesRecord
                (TargetingCategoriesTargetingType.rmp_interest, PARENT_IMPORT_ID).getCategoryId();
    }

    public static Long getChildCategoryId() {
        return TestEnvironment.newDbSteps().interestSteps().getTargetingCategoriesRecord
                (TargetingCategoriesTargetingType.rmp_interest, CHILD_IMPORT_ID).getCategoryId();
    }

    public static void setParentCategoryId(Long childCategoryId, Long parentCategoryId) {
        TestEnvironment.newDbSteps().interestSteps().saveParentCategoryIdforChild(childCategoryId, parentCategoryId);
    }
}
