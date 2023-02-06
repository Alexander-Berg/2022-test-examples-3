const PO = require('../../../../page-objects/pages/form');
const POCandidate = require('../../../../page-objects/pages/candidate');
const options = {
    tolerance: 5,
    antialiasingTolerance: 5,
}

function openForm(browser) {
    return browser
        .conditionalLogin('marat')
        .preparePageExtended('/candidates/create/', [2019, 10, 24, 0, 0, 0], '/candidates/create/')
        .waitForPageLoad()
        .waitForVisible(PO.pageForm.candidateForm())
        .assertView('candidat_create_form_empty', PO.pageForm.candidateForm());
}

describe('Кандидат / Создание', function() {
    it('Проверка обязательных полей', function() {
        return this.browser
            .then(() => openForm(this.browser))
            .click(PO.pageForm.candidateForm.submit())
            .waitForHidden(PO.pageForm.candidateForm.submitDisabled())
            .assertView('candidate_create_form_validation', PO.pageForm.candidateForm());
    });
    it('Новый кандидат', function() {
        return this.browser
            .then(() => openForm(this.browser))
            .setValue(PO.pageForm.candidateForm.fieldFirstName.input(), 'Кандидат')
            .setValue(PO.pageForm.candidateForm.fieldLastName.input(), 'Тестовый')
            .setSFieldValue(PO.pageForm.candidateForm.fieldBirthday(), '10-10-1995')
            .setSFieldValue(PO.pageForm.candidateForm.fieldGender(), 'male')
            .setValue(PO.pageForm.candidateForm.fieldCountry.input(), 'Россия')
            .setSuggestValue({
                block: PO.pageForm.candidateForm.fieldTargetCities.input(),
                menu: PO.targetCities.items(),
                text: 'Москва',
                item: PO.targetCities.moscow(),
            })
            .setSuggestValue({
                block: PO.pageForm.candidateForm.fieldTargetCities.input(),
                menu: PO.targetCities.items(),
                text: 'Санкт',
                item: PO.targetCities.peter(),
            })
            .setSuggestValue({
                block: PO.pageForm.candidateForm.fieldRecruiters.input(),
                menu: PO.responsibles.items(),
                text: 'user3993',
                item: PO.responsibles.user3993(),
            })
            .setSuggestValue({
                block: PO.pageForm.candidateForm.fieldRecruiters.input(),
                menu: PO.responsibles.items(),
                text: 'olgakozlova',
                item: PO.responsibles.olgakozlova(),
            })
            // Кликаем по хедеру чтобы гарантированно закрыть саджест с пользователями
            .click('.f-layout__header')
            .setSelectValue({
                block: PO.pageForm.candidateForm.fieldSource(),
                item: PO.sourceSelect.second(),
            })
            .setValue(PO.pageForm.candidateForm.fieldMainContact.input(), 'test@yandex-team.ru')
            .assertView('candidate_form_filled', PO.pageForm.candidateForm())
            .click(PO.pageForm.candidateForm.submit())
            .waitForVisible(POCandidate.pageCandidate.tabs())
            .waitForVisible(POCandidate.pageCandidate.status())
            .assertView('candidate_page', POCandidate.pageCandidate(), options);
    });
});
