const { setAjaxHash } = require('../../../../../helpers');

const PO = require('../../../../../page-objects/pages/offer');

const suggestResults = require('./.mockSuggest');

function prepare(browser, page) {
    return browser
        .conditionalLogin('marat')
        .preparePage('', page)
        .disableAnimations('*')
        .waitForVisible(PO.pageOffer.actions())
        .setFixedDateTime()
        .waitForPageLoad()
        .waitForLoad()
        .assertView('offer_actions', PO.pageOffer.actions())
        .click(PO.pageOffer.actions.update())
        .waitForPageLoad()
        .waitForVisible(PO.pageForm.form())
        .waitForVisible(PO.pageForm.form.groupWorkPlace.arrow())
        .waitForLoad()
        .assertView('offer_form_update', PO.pageForm.form());
}

describe('Внутренний оффер / Редактирование', function() {
    it('Обязательные поля оффера на ротацию', function() {
        return this.browser
            .then(() => prepare(this.browser, '/offers/81731/'))
            .click(PO.pageForm.form.submit())
            .waitForPageLoad()
            .waitForHidden(PO.pageForm.form.submitDisabled())
            .assertView('offer_form_update_validation', PO.pageForm.form());
    });

    it('Редактирование оффера на ротацию', function() {
        const page = '/offers/81731/';

        return this.browser
            .then(() => prepare(this.browser, page))
            .setMockableSuggestValue({
                data: suggestResults,
                input: PO.pageForm.form.fieldDepartment.input(),
                items: PO.departmentSuggest.items(),
                item: PO.departmentSuggest.yandex.item(),
                text: 'yandex_main_searchadv',
            })
            .waitForVisible(PO.pageForm.form.groupWorkPlace.cat())
            .click(PO.pageForm.form.groupWorkPlace.cat())
            .assertView('group_workplace_collapsed', PO.pageForm.form.groupWorkPlace())
            .setSelectDynamicValue({
                block: PO.pageForm.form.fieldOrg.button(),
                menu: PO.orgSelect(),
                text: 'Яндекс',
            })
            .setSuggestValue({
                block: PO.pageForm.form.fieldPosition.input(),
                menu: PO.positionSuggest(),
                item: PO.positionSuggest.developer(),
                text: 'Разработчик',
            })
            .setValue(PO.pageForm.form.fieldStaffPositionName.input(), 'Хороший разработчик')
            .setSelectValue({
                block: PO.pageForm.form.fieldProfessionSphere(),
                menu: PO.professionSphereSelect(),
                item: PO.professionSphereSelect.hr(),
            })
            .waitUntil(() => {
                return this.browser
                    .getValue(PO.pageForm.form.fieldProfession.select())
                    .then(value => value == 63);
            }, 1000, 'Ожидалось что значение в селекторе профессии значение равно 63 - \'Специалист по обучению\'', 100)
            .setSelectValue({
                block: PO.pageForm.form.fieldProfession(),
                menu: PO.professionSelect(),
                item: PO.professionSelect.learnSpecialist(),
            })
            .setSelectValue({
                block: PO.pageForm.form.fieldWorkPlace(),
                menu: PO.workPlaceSelect(),
                item: PO.workPlaceSelect.office(),
            })
            .waitForVisible(PO.pageForm.form.fieldOffice())
            .setSelectDynamicValue({
                block: PO.pageForm.form.fieldOffice.button(),
                menu: PO.officeSelect(),
                text: 'Москва, БЦ Морозов',
            })
            .assertView('group_workplace_with_office', PO.pageForm.form.groupWorkPlace())
            .setSFieldValue(PO.pageForm.form.fieldJoinAt(), '01-01-2019')
            .setValue(PO.pageForm.form.fieldGrade.input(), '11')
            .click(PO.pageForm.form.groupProcessingConditions.cat())
            .assertView('group_processing_conditions_collapsed', PO.pageForm.form.groupProcessingConditions())
            .setSelectValue({
                block: PO.pageForm.form.fieldPaymentType(),
                menu: PO.paymentTypeSelect(),
                item: PO.paymentTypeSelect.hourly(),
            })
            .waitForVisible(PO.pageForm.form.fieldHourlyRate.input())
            .waitForVisible(PO.pageForm.form.fieldEmploymentType())
            .assertView('group_processing_conditions_with_hourly_rate', PO.pageForm.form.groupProcessingConditions())
            .setValue(PO.pageForm.form.fieldHourlyRate.input(), '10')
            .setValue(PO.pageForm.form.fieldSalary.input(), '15')
            .setSelectValue({
                block: PO.pageForm.form.fieldEmploymentType(),
                menu: PO.employmentTypeSelect(),
                item: PO.employmentTypeSelect.full(),
            })
            .setSelectValue({
                block: PO.pageForm.form.fieldContractType(),
                menu: PO.contractTypeSelect(),
                item: PO.contractTypeSelect.project(),
            })
            .click(PO.pageForm.form.groupBonus.cat())
            .click(PO.pageForm.form.groupExtraBonus.cat())
            .waitForVisible(PO.pageForm.form.fieldVmi())
            .setSFieldValue(PO.pageForm.form.fieldVmi(), 'true')
            .waitForVisible(PO.pageForm.form.fieldNeedRelocation())
            .setSFieldValue(PO.pageForm.form.fieldNeedRelocation(), 'true')
            .assertView('offer_form_update_filled', PO.pageForm.form())
            .execute(setAjaxHash, 'after_offer_edited')
            .click(PO.pageForm.form.submit())
            .waitUntil(() => {
                return this.browser
                    .getUrl()
                    .then(url => url.endsWith(page));
            }, 5000, 'Не произошла навигация на страницу оффера', 100)
            .waitForPageLoad()
            .waitForLoad()
            .waitForHidden(PO.pageOffer.progress())
            .assertView('offer_form_update_success', PO.pageOffer());
    });

    it('Обязательные поля оффера на перевод стажера', function() {
        return this.browser
            .then(() => prepare(this.browser, '/offers/81733/'))
            .setSelectValue({
                block: PO.pageForm.form.fieldEmployeeType(),
                menu: PO.employeeTypeSelect(),
                item: PO.employeeTypeSelect.intern(),
            })
            .waitForVisible(PO.pageForm.form.fieldUserName.messageTypeSuccess())
            .click(PO.pageForm.form.submit())
            .waitForPageLoad()
            .waitForHidden(PO.pageForm.form.submitDisabled())
            .assertView('offer_form_update_validation', PO.pageForm.form());
    });

    it('Редактирование оффера на перевод стажера', function() {
        const page = '/offers/81733/';

        return this.browser
            .then(() => prepare(this.browser, page))
            .setSelectValue({
                block: PO.pageForm.form.fieldEmployeeType(),
                menu: PO.employeeTypeSelect(),
                item: PO.employeeTypeSelect.intern(),
            })
            .setSelectDynamicValue({
                block: PO.pageForm.form.fieldOrg.button(),
                menu: PO.orgSelect(),
                text: 'Яндекс',
            })
            .setMockableSuggestValue({
                data: suggestResults,
                input: PO.pageForm.form.fieldDepartment.input(),
                items: PO.departmentSuggest.items(),
                item: PO.departmentSuggest.yandex.item(),
                text: 'yandex_main_searchadv',
            })
            .setSuggestValue({
                block: PO.pageForm.form.fieldPosition.input(),
                menu: PO.positionSuggest(),
                item: PO.positionSuggest.developer(),
                text: 'Разработчик',
            })
            .setValue(PO.pageForm.form.fieldStaffPositionName.input(), 'Хороший разработчик')
            .setSelectValue({
                block: PO.pageForm.form.fieldProfessionSphere(),
                menu: PO.professionSphereSelect(),
                item: PO.professionSphereSelect.hr(),
            })
            .waitUntil(() => {
                return this.browser
                    .getValue(PO.pageForm.form.fieldProfession.select())
                    .then(value => value == 63);
            }, 1000, 'Ожидалось что значение в селекторе профессии значение равно 63 - \'Специалист по обучению\'', 100)
            .setSelectValue({
                block: PO.pageForm.form.fieldProfession(),
                menu: PO.professionSelect(),
                item: PO.professionSelect.learnSpecialist(),
            })
            .click(PO.pageForm.form.groupWorkPlace.cat())
            .setSelectValue({
                block: PO.pageForm.form.fieldWorkPlace(),
                menu: PO.workPlaceSelect(),
                item: PO.workPlaceSelect.office(),
            })
            .waitForVisible(PO.pageForm.form.fieldOffice())
            .setSelectDynamicValue({
                block: PO.pageForm.form.fieldOffice.button(),
                menu: PO.officeSelect(),
                text: 'Москва, БЦ Морозов',
            })
            .setSFieldValue(PO.pageForm.form.fieldJoinAt(), '01-01-2019')
            .setValue(PO.pageForm.form.fieldGrade.input(), '11')
            .setSelectValue({
                block: PO.pageForm.form.fieldPaymentType(),
                menu: PO.paymentTypeSelect(),
                item: PO.paymentTypeSelect.hourly(),
            })
            .waitForVisible(PO.pageForm.form.fieldHourlyRate.input())
            .waitForVisible(PO.pageForm.form.fieldEmploymentType())
            .assertView('group_processing_conditions_with_hourly_rate', PO.pageForm.form.groupProcessingConditions())
            .setValue(PO.pageForm.form.fieldHourlyRate.input(), '10')
            .setValue(PO.pageForm.form.fieldSalary.input(), '15')
            .setSelectValue({
                block: PO.pageForm.form.fieldEmploymentType(),
                menu: PO.employmentTypeSelect(),
                item: PO.employmentTypeSelect.full(),
            })
            .setSelectValue({
                block: PO.pageForm.form.fieldContractType(),
                menu: PO.contractTypeSelect(),
                item: PO.contractTypeSelect.project(),
            })
            .click(PO.pageForm.form.groupBonus.cat())
            .click(PO.pageForm.form.groupExtraBonus.cat())
            .waitForVisible(PO.pageForm.form.fieldVmi())
            .setSFieldValue(PO.pageForm.form.fieldVmi(), 'true')
            .waitForVisible(PO.pageForm.form.fieldNeedRelocation())
            .setSFieldValue(PO.pageForm.form.fieldNeedRelocation(), 'true')
            .assertView('offer_form_update_filled', PO.pageForm.form())
            .execute(setAjaxHash, 'after_offer_edited')
            .click(PO.pageForm.form.submit())
            .waitForHidden(PO.pageOffer.progress())
            .waitUntil(() => {
                return this.browser
                    .getUrl()
                    .then(url => url.endsWith(page));
            }, 5000, 'Не произошла навигация на страницу оффера', 100)
            .waitForPageLoad()
            .waitForHidden(PO.pageOffer.progress())
            .assertView('offer_form_update_success', PO.pageOffer());
    });
});
