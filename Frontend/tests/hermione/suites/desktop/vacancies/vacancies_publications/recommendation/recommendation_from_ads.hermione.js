const ListPO = require('../../../../../page-objects/pages/publications');
const PubPO = require('../../../../../page-objects/pages/publication');

describe('Объявления о вакансиях / Порекомендовать со страницы объявления', function() {
    it('Проверка рекомендации со страницы объявления', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/vacancies/publications/53112')
            .waitForVisible(PubPO.pagePublication.recommendationAction())
            .assertView('default_view', PubPO.pagePublication())
            .click(PubPO.pagePublication.recommendationAction())
            .getTabIds().then(ids => {
                return this.browser.switchTab(ids[1]);
            })
            .assertUrl('/vacancies/publications/recommend/?publications=53112')
            .staticElement(ListPO.pubsTabs())
            .waitForVisible(ListPO.recommendationForm())
            .assertView('recommendation_form', ListPO.recommendationForm());
    });
});
