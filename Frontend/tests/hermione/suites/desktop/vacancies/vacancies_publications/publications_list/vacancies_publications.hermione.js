const PO = require('../../../../../page-objects/pages/publications');

const FIRST_PUBLICATION_ID = '52612';

/**
 * Открывает страницу списка публикаций
 * @param {Object} browser
 * @returns {Object}
 */
function openUrl(browser) {
    return browser
        .conditionalLogin('marat')
        .preparePage('', '/vacancies/publications/')
        .waitForVisible(PO.pubsList.list())
        .staticElement(PO.pubsTabs())
        .assertView('default_view', [
            PO.header(),
            PO.pubsList(),
        ]);
}
describe('Объявления о вакансиях / Список вакансий', function() {
    it('Проверка пагинации', function() {
        return this.browser
            .then(() => openUrl(this.browser))
            .click(PO.pubsList.pager.page2())
            .waitUntil(() => {
                return this.browser
                    .getText(PO.pubsList.list.publication1.vacancyId())
                    .then(text => {
                        return text !== `VAC ${FIRST_PUBLICATION_ID}`;
                    });
            }, 5000, 'Не дождались смены первой публикации', 100)
            .assertUrl('/vacancies/publications/?page=2')
            .pause(10000)
            .assertView('publications_list_page2', [
                PO.pubsList.list(),
            ]);
    });

    it('Проверка перехода на объявление', function() {
        const FIRST_PUBLICATION_ID = '52741';

        return this.browser
            .then(() => openUrl(this.browser))
            .click(PO.pubsList.list.publication1.title())
            .assertUrlPath(`/vacancies/publications/${FIRST_PUBLICATION_ID}/`);
    });
});
