const { setAjaxHash } = require('../../../../../helpers');
const PO = require('../../../../../page-objects/pages/offer');
const suggestResults = require('./.mockSuggest');

function prepare(browser, id) {
    return browser
        .conditionalLogin('marat')
        .preparePage('', `/offers/${id}/`)
        .disableAnimations('*')
        .setFixedDateTime()
        .waitForVisible(PO.pageOffer.actions())
        .assertView('offer_actions', PO.pageOffer.actions())
        .click(PO.pageOffer.actions.update())
        .waitForVisible(PO.pageForm.form())
        .assertView('offer_form_update', PO.pageForm.form());
}

describe('Внешний оффер / Редактирование', function() {
    it('Проверка обязательных полей', function() {
        return this.browser
            .then(() => prepare(this.browser, '81685'))
            .click(PO.pageForm.form.submit())
            .waitForHidden(PO.pageForm.form.submitDisabled())
            .assertView('offer_form_update_validation', PO.pageForm.form());
    });

    it('Редактирование оффера', function() {
        return this.browser
            .then(() => prepare(this.browser, '81591'))
            .setSelectDynamicValue({
                block: PO.pageForm.form.fieldOrg(),
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
                    .then(value => value == 64);
            }, 1000, 'Ожидалось что значение в селекторе профессии значение равно 64 - \'Рекрутер\'', 100)
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
            .setSelectDynamicValue({
                block: PO.pageForm.form.fieldOffice(),
                menu: PO.officeSelect(),
                text: 'Москва, БЦ Морозов',
            })
            .setSFieldValue(PO.pageForm.form.fieldJoinAt(), '01-01-2019')
            .setValue(PO.pageForm.form.fieldGrade.input(), '11')
            .assertView('group_processing_conditions_collapsed', PO.pageForm.form.groupProcessingConditions())
            .setSelectValue({
                block: PO.pageForm.form.fieldPaymentType(),
                menu: PO.paymentTypeSelect(),
                item: PO.paymentTypeSelect.hourly(),
            })
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
            .setSFieldValue(PO.pageForm.form.fieldVmi(), 'true')
            .setSFieldValue(PO.pageForm.form.fieldNeedRelocation(), 'true')
            .execute(setAjaxHash, 'after_offer_edited')
            .click(PO.pageForm.form.submit())
            .waitForHidden(PO.pageOffer.progress())
            .waitForVisible(PO.pageOffer())
            .assertView('offer_form_update_success', PO.pageOffer());
    });
});
