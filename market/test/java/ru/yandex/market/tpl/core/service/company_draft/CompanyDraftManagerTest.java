package ru.yandex.market.tpl.core.service.company_draft;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftManager;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftFormDescriptionService.BUSINESS_ID;
import static ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftFormDescriptionService.CITY;
import static ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftFormDescriptionService.HOUSE;
import static ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftFormDescriptionService.LATITUDE;
import static ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftFormDescriptionService.LOGIN;
import static ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftFormDescriptionService.LONGITUDE;
import static ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftFormDescriptionService.SC_NAME;
import static ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftFormDescriptionService.STREET;

/**
 * @author zaxarello
 */
@RequiredArgsConstructor
public class CompanyDraftManagerTest extends TplAbstractTest {
    private static final String FORM_BUSINESS_ID = "111340";
    private static final String FORM_SC_NAME = "ИМЯ";
    private static final String FORM_LOGIN = "УНИКАЛЬНЫЙ ЛОГИН 6789";
    private static final String FORM_CITY = "ГОРОД";
    private static final String FORM_STREET = "УЛИЦА";
    private static final String FORM_HOUSE = "ДОМ";
    private static final String FORM_LATITUDE = "1.1";
    private static final String FORM_LONGITUDE = "2.2";
    private final CompanyDraftManager companyDraftManager;
    private final CompanyDraftRepository companyDraftRepository;

    @Test
    void testCreateCompanyDraftFromYandexForm() {
        companyDraftManager.createCompanyDraftFromYandexForm(getDbsAnswersWithMandatoryFields());
        Optional<CompanyDraft> companyDraftOptional = companyDraftRepository.findByLogin(FORM_LOGIN);
        Assertions.assertThat(companyDraftOptional).isPresent();
        CompanyDraft companyDraft = companyDraftOptional.get();
        Assertions.assertThat(companyDraft.getBusinessId()).isEqualTo(Long.parseLong(FORM_BUSINESS_ID));
        Assertions.assertThat(companyDraft.getScName()).isEqualTo(FORM_SC_NAME);
        Assertions.assertThat(companyDraft.getScAddress()).isEqualTo(String.join(" ", FORM_CITY, FORM_STREET,
                FORM_HOUSE));
        Assertions.assertThat(companyDraft.getScLatitude()).isEqualTo(Double.parseDouble(FORM_LATITUDE));
        Assertions.assertThat(companyDraft.getScLongitude()).isEqualTo(Double.parseDouble(FORM_LONGITUDE));
    }


    private LinkedMultiValueMap<String, String> getDbsAnswersWithMandatoryFields() {
        LinkedMultiValueMap<String, String> surveyAnswers = new LinkedMultiValueMap<>();
        surveyAnswers.add(BUSINESS_ID, FORM_BUSINESS_ID);
        surveyAnswers.add(SC_NAME, FORM_SC_NAME);
        surveyAnswers.add(LOGIN, FORM_LOGIN);
        surveyAnswers.add(CITY, FORM_CITY);
        surveyAnswers.add(STREET, FORM_STREET);
        surveyAnswers.add(HOUSE, FORM_HOUSE);
        surveyAnswers.add(LATITUDE, FORM_LATITUDE);
        surveyAnswers.add(LONGITUDE, FORM_LONGITUDE);
        return surveyAnswers;
    }

}
