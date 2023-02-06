package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.intapi.entity.metrika.model.MetrikaGroupsParam;
import ru.yandex.direct.validation.result.PathHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.intapi.validation.ValidationUtils.getErrorText;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class MetrikaGroupsValidationTest {

    private static final Long CORRECT_ID = 1L;
    private static final Long INCORRECT_ID = 0L;

    @SuppressWarnings("WeakerAccess")
    @Parameterized.Parameter(value = 0)
    public List<MetrikaGroupsParam> params;

    @Parameterized.Parameter(value = 1)
    @SuppressWarnings("WeakerAccess")
    public String expectedMessage;

    @Autowired
    private MetrikaGroupsService metrikaGroupsService;

    @SuppressWarnings("RedundantArrayCreation")
    @Parameterized.Parameters(name = "message {1} ")
    public static Collection params() {
        return Arrays.asList(new Object[][]{
                {Collections.singletonList(null), "params[0] cannot be null"},
                {Collections.emptyList(), "params cannot be empty"},
                {Collections.singletonList(new MetrikaGroupsParam()
                        .withOrderId(CORRECT_ID)), "params[0].group_id cannot be null"},
                {Collections.singletonList(new MetrikaGroupsParam()
                        .withGroupId(CORRECT_ID)), "params[0].order_id cannot be null"},
                {Collections.singletonList(new MetrikaGroupsParam()
                        .withGroupId(INCORRECT_ID)
                        .withOrderId(CORRECT_ID)), "params[0].group_id must be greater than 0"},
                {Collections.singletonList(new MetrikaGroupsParam()
                        .withGroupId(CORRECT_ID)
                        .withOrderId(INCORRECT_ID)), "params[0].order_id must be greater than 0"},
                {Arrays.asList(new MetrikaGroupsParam()
                        .withGroupId(CORRECT_ID)
                        .withOrderId(CORRECT_ID), null), "params[1] cannot be null"},
        });
    }

    @Before
    public void setUp() throws Exception {
        metrikaGroupsService = new MetrikaGroupsService();
    }

    @Test
    public void expectException() {
        String validationError =
                getErrorText(metrikaGroupsService.validate(params), path(PathHelper.field("params")));
        assertThat("должна быть ошибка валидации", validationError, equalTo(expectedMessage));
    }
}
