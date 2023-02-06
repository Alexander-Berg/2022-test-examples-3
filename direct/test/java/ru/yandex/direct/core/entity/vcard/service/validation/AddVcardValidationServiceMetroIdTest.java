package ru.yandex.direct.core.entity.vcard.service.validation;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestVcards;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.vcard.service.validation.AddVcardValidationService.DefectDefinitions.contactEmailIsEmpty;
import static ru.yandex.direct.core.entity.vcard.service.validation.AddVcardValidationService.DefectDefinitions.contactEmailTooLong;
import static ru.yandex.direct.core.entity.vcard.service.validation.AddVcardValidationService.DefectDefinitions.invalidContactEmailFormat;
import static ru.yandex.direct.core.entity.vcard.service.validation.MetroIdValidator.DefectDefinitions.invalidMetro;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.matchesWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class AddVcardValidationServiceMetroIdTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Autowired
    private CampaignSubObjectAccessCheckerFactory campaignSubObjectAccessCheckerFactory;

    @Autowired
    private CampaignSteps campaignSteps;

    @Mock
    private Constraint<Long, Defect> metroIdConstraint;
    @Mock
    private MetroIdValidator metroIdValidator;

    private AddVcardValidationService validationService;

    private CampaignInfo campaignInfo;
    private Long operatorUid;
    private UidClientIdShard client;

    @Before
    public void setUp() {
        when(metroIdValidator.createConstraintFor(anyString())).thenReturn(metroIdConstraint);

        validationService = new AddVcardValidationService(campaignSubObjectAccessCheckerFactory, metroIdValidator);

        campaignInfo = campaignSteps.createActiveTextCampaign();

        operatorUid = campaignInfo.getUid();

        client = UidClientIdShard.of(campaignInfo.getUid(), campaignInfo.getClientId(), campaignInfo.getShard());
    }

    private ValidationResult<List<Vcard>, Defect> validateMetro() {
        return validationService.validate(
                operatorUid,
                false,
                singletonList(
                        TestVcards.fullVcard(campaignInfo.getUid(), campaignInfo.getCampaignId())),
                client
        );
    }

    @Test
    public void validateMetroIdSuccess() {
        when(metroIdConstraint.apply(anyLong())).thenReturn(null);

        ValidationResult<List<Vcard>, Defect> vr = validateMetro();

        assertThat(vr.getSubResults().get(index(0)), hasNoDefectsDefinitions());
    }

    @Test
    public void validateMetroIdFailed() {
        when(metroIdConstraint.apply(anyLong())).thenReturn(invalidMetro());

        ValidationResult<List<Vcard>, Defect> vr = validateMetro();

        assertThat(
                vr.getSubResults().get(index(0)),
                hasDefectDefinitionWith(
                        validationError(path(field(Vcard.METRO_ID.name())), invalidMetro())));
    }


    @Parameterized.Parameters(name = "email: {0}, result: {1}")
    public static Collection<Object[]> emailCaseParameters() {
        StringBuilder tooLongEmail = new StringBuilder("@");
        IntStream.range(0, 300).forEach(i -> tooLongEmail.append((char) i));
        return List.of(new Object[][]{
                {"name@domain.ru", null},
                {"namedoamin.ru", invalidContactEmailFormat()},
                {"имя@домен.ру", null},
                {"@", null},
                {"@домен", null},
                {"имя@", null},
                {"", contactEmailIsEmpty()},
                {tooLongEmail.toString(), contactEmailTooLong()},
        });
    }

    @Test
    @Parameters(method = "emailCaseParameters")
    public void validateEmail(String email, Defect<Void> result) {
        Vcard vcards = TestVcards.fullVcard(campaignInfo.getUid(), campaignInfo.getCampaignId());
        vcards.withEmail(email);
        ValidationResult<List<Vcard>, Defect> vr = validationService.validate(
                operatorUid,
                false,
                singletonList(vcards),
                client
        );
        assertThat(vr.getSubResults().get(index(0)), result == null ?
                hasNoDefectsDefinitions() :
                hasDefectDefinitionWith(matchesWith(result)));
    }
}
