const PO = require('../../../../../page-objects/pages/publications');

/**
 * Открывает страницу ротации
 * @param {Object} browser
 * @returns {Object}
 */
function openUrl(browser) {
    return browser
        .conditionalLogin('marat')
        .preparePage('', '/vacancies/publications/rotate/')
        .waitForVisible(PO.rotationForm())
        .staticElement(PO.pubsTabs())
        .assertView('default_view', [
            PO.header(),
            PO.rotationForm(),
        ]);
}
describe('Объявления о вакансиях / Ротация', function() {
    it('Проверка обязательных полей', function() {
        return this.browser
            .then(() => openUrl(this.browser))
            .click(PO.rotationForm.submit())
            .waitForVisible(PO.rotationForm.error())
            .assertView('form_with_error', PO.rotationForm());
    });
    it('Проверка ссылки процесс ротаций', function() {
        return this.browser
            .then(() => openUrl(this.browser))
            .assertAttribute(PO.rotatePane.infoblock.rotationRulesLink(), 'href', 'https://wiki.yandex-team.ru/HR/grupparekrutmenta/rotation/#praviladopuskakrotacii');
    });
    it('Проверка ротации', function() {
        return this.browser
            .then(() => openUrl(this.browser))
            .setSelectValue({
                block: PO.rotationForm.fieldReason(),
                menu: PO.reasonSelect(),
                item: PO.reasonSelect.second(),
            })
            .click(PO.rotationForm.publications.button())
            .assertUrl('/vacancies/publications/?filter=yes')
            .waitForVisible(PO.pubsList.list())
            .staticElement(PO.pubsTabs())
            .click(PO.pubsList.list.publication1.toRotation())
            .staticElement('.m-notification')
            .assertView('selected_publications1', PO.pubsList.list.publication1.toRotation())
            .click(PO.pubsList.list.publication2.toRotation())
            .assertView('selected_publications2', PO.pubsList.list.publication2.toRotation())
            .click(PO.pubsList.list.publication3.toRotation())
            .assertView('selected_publications3', PO.pubsList.list.publication3.toRotation())
            .assertView('rotation_counter', PO.pubsTabs.rotate())
            .click(PO.pubsList.list.publication3.toRotation())
            .assertView('unselected_publication', PO.pubsList.list.publication3.toRotation())
            .click(PO.pubsTabs.rotate())
            .assertUrl('/vacancies/publications/rotate/')
            .assertView('filled_publications', PO.rotationForm.publications())
            .click(PO.rotationForm.publications.selected1.delete())
            .setValue(PO.rotationForm.fieldComment.input(), 'Комментарий для нанимающих менеджеров')
            .click(PO.rotationForm.fieldIsAgree.checkbox())
            .click(PO.rotationForm.fieldIsPrivacyNeeded.checkbox())
            .assertView('form_filled', PO.rotationForm())
            .click(PO.rotationForm.submit())
            .waitForVisible(PO.successMessage())
            .assertView('after_form_submission', [
                PO.rotationForm(),
                PO.successMessage(),
            ]);
    });
});
