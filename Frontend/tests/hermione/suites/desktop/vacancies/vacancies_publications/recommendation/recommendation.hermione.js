const PO = require('../../../../../page-objects/pages/publications');

/**
 * Открывает страницу списка публикаций
 * @param {Object} browser
 * @returns {Object}
 */
function openUrl(browser) {
    return browser
        .conditionalLogin('marat')
        .preparePage('', '/vacancies/publications/recommend/')
        .waitForVisible(PO.recommendationForm())
        .staticElement(PO.pubsTabs())
        .assertView('default_view', [
            PO.header(),
            PO.recommendationForm(),
        ]);
}
describe('Объявления о вакансиях / Порекомендовать', function() {
    it('Проверка обязательных полей', function() {
        return this.browser
            .then(() => openUrl(this.browser))
            .click(PO.recommendationForm.submit())
            .waitForVisible(PO.recommendationForm.error())
            .assertView('form_with_error', PO.recommendationForm());
    });
    it('Проверка ссылки программа рекомендаций', function() {
        return this.browser
            .then(() => openUrl(this.browser))
            .assertAttribute(PO.recommendPane.infoblock.recommendationProgramLink(), 'href', 'https://wiki.yandex-team.ru/HR/grupparekrutmenta/recommend/');
    });
    it('Проверка рекомендации кандидата', function() {
        return this.browser
            .then(() => openUrl(this.browser))
            .setValue(PO.recommendationForm.fieldFirstName.input(), 'Имя')
            .setValue(PO.recommendationForm.fieldLastName.input(), 'Фамилия')
            .setValue(PO.recommendationForm.fieldEmail.input(), 'test@yandex.ru')
            .setValue(PO.recommendationForm.fieldPhone.input(), '+79268867807')
            .uploadFileWithFAttachUploader(PO.recommendationForm.fieldResume.uploader(), [
                {
                    content: ['a'],
                    name: 'a.txt',
                },
            ])
            .click(PO.recommendationForm.publications.button())
            .assertUrl('/vacancies/publications/?filter=yes')
            .waitForVisible(PO.pubsList.list())
            .staticElement(PO.pubsTabs())
            .click(PO.pubsList.list.publication1.toRecommendation())
            .staticElement('.m-notification')
            .assertView('selected_publications1', PO.pubsList.list.publication1.toRecommendation())
            .click(PO.pubsList.list.publication2.toRecommendation())
            .assertView('selected_publications2', PO.pubsList.list.publication2.toRecommendation())
            .click(PO.pubsList.list.publication3.toRecommendation())
            .assertView('selected_publications3', PO.pubsList.list.publication3.toRecommendation())
            .assertView('recommendation_counter', PO.pubsTabs.recommend())
            .click(PO.pubsList.list.publication3.toRecommendation())
            .assertView('unselected_publication', PO.pubsList.list.publication3.toRecommendation())
            .click(PO.pubsTabs.recommend())
            .assertUrl('/vacancies/publications/recommend/')
            .assertView('filled_publications', PO.recommendationForm.publications())
            .click(PO.recommendationForm.publications.selected1.delete())
            .setValue(PO.recommendationForm.fieldComment.input(), 'Чем кандидат полезен для Яндекса')
            .click(PO.recommendationForm.fieldIsInformed.checkbox())
            .assertView('form_filled', PO.recommendationForm())
            .click(PO.recommendationForm.submit())
            .waitForVisible(PO.successMessage())
            .assertView('after_form_submission', [
                PO.recommendationForm(),
                PO.successMessage(),
            ]);
    });
});
