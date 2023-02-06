const { setAjaxHash } = require('../../../../../helpers');

const PO = require('../../../../../page-objects/pages/offer');

describe('Внутренний оффер / Редактирование после согласования', function() {
    it('Редактирование оффера на ротацию после согласования', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/offers/81731/')
            .waitForVisible(PO.pageOffer.actions())
            .assertView('offer_actions', PO.pageOffer.actions())
            .click(PO.pageOffer.actions.update())
            .waitForVisible(PO.pageForm.form())
            .assertView('offer_form_update', PO.pageForm.form())
            .setValue(PO.pageForm.form.fieldFullName.input(), 'Тест тест2')
            .click(PO.pageForm.form.groupWorkPlace.cat())
            .setSuggestValue({
                block: PO.pageForm.form.fieldPosition.input(),
                menu: PO.positionSuggest(),
                item: PO.positionSuggest.developer(),
                text: 'Разработчик',
            })
            .setValue(PO.pageForm.form.fieldStaffPositionName.input(), 'Хороший-прехороший разработчик')
            .setSFieldValue(PO.pageForm.form.fieldJoinAt(), '06-06-2022')
            .assertView('offer_form_filled', PO.pageForm())
            .execute(setAjaxHash, 'after_offer_edited')
            .click(PO.pageForm.form.submit())
            .waitUntil(() => {
                return this.browser
                    .getUrl()
                    .then(url => url.endsWith('/offers/81731/'));
            }, 5000, 'Не произошла навигация на страницу оффера', 100)
            .waitForHidden(PO.pageOffer.progress())
            .assertView('offer_form_update_success', PO.pageOffer());
    });

    it('Редактирование оффера на перевод стажера после согласования', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/offers/81733/')
            .waitForVisible(PO.pageOffer.actions())
            .assertView('offer_actions', PO.pageOffer.actions())
            .click(PO.pageOffer.actions.update())
            .waitForVisible(PO.pageForm.form())
            .assertView('offer_form_update', PO.pageForm.form())
            .setValue(PO.pageForm.form.fieldFullName.input(), 'Тест тест')
            .setSuggestValue({
                block: PO.pageForm.form.fieldPosition.input(),
                menu: PO.positionSuggest(),
                item: PO.positionSuggest.developer(),
                text: 'Разработчик',
            })
            .setValue(PO.pageForm.form.fieldStaffPositionName.input(), 'Хороший разработчик')
            .setSFieldValue(PO.pageForm.form.fieldJoinAt(), '01-01-2019')
            .assertView('offer_form_filled', PO.pageForm())
            .execute(setAjaxHash, 'after_offer_edited')
            .click(PO.pageForm.form.submit())
            .waitUntil(() => {
                return this.browser
                    .getUrl()
                    .then(url => url.endsWith('/offers/81733/'));
            }, 5000, 'Не произошла навигация на страницу оффера', 100)
            .waitForHidden(PO.pageOffer.progress())
            .assertView('offer_form_update_success', PO.pageOffer());
    });
});
