package ru.yandex.market.passport.internal.idm.api.info;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.passport.internal.idm.api.common.LocalizedString;
import ru.yandex.market.passport.utils.ResourceUtils;

import static org.junit.Assert.assertEquals;


/**
 * @author anmalysh
 * @since 10/27/2018
 */
public class RolesInfoResultTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testRolesInfoResult() throws JsonProcessingException {

        RolesInfo rolesInfo = new RolesInfo();
        rolesInfo.setSlug("role");
        rolesInfo.setName(new LocalizedString("Роль"));
        rolesInfo.setValues(ImmutableMap.of(
            "operator", new SimpleRoleInfo("Оператор"),
            "admin", new SimpleRoleInfo("Администратор")
        ));

        AdditionalField additionalField = new AdditionalField();
        additionalField.setName(new LocalizedString("Поле"));
        additionalField.setRequired(true);
        additionalField.setSlug("field");
        additionalField.setType(AdditionalFieldType.CHOICE_FIELD);

        ChoiceFieldOptions options = new ChoiceFieldOptions();
        options.setCustom(true);
        options.setWidget(WidgetType.RADIO);

        ChoiceFieldOption option1 = new ChoiceFieldOption();
        option1.setName(new LocalizedString("Вариант 1"));
        option1.setValue("option1");
        ChoiceFieldOption option2 = new ChoiceFieldOption();
        option2.setName(new LocalizedString("Вариант 2"));
        option2.setValue("option2");
        options.setChoices(ImmutableList.of(
            option1, option2
        ));

        additionalField.setOptions(options);

        RolesInfoResult result = new RolesInfoResult();
        result.setRoles(rolesInfo);
        result.setFields(ImmutableList.of(additionalField));
        result.setCode(0);
        result.setError("error");
        result.setWarning("warning");
        result.setFatal("fatal");

        String serialized = mapper.writer().writeValueAsString(result);
        String expected = ResourceUtils.getJsonResourceAsString("idm/roles-info-result.json");
        assertEquals(expected, serialized);
    }
}
