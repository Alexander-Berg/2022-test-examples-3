const pageObjects = require('../page-objects/landings').businessTariff;

describe('Тарифы для бизнеса', function() {
    beforeEach(async function() {
        const bro = this.browser;

        await bro.url('/business/tariff');
    });

    hermione.only.notIn('chrome-phone', 'убрать, когда будет писаться полноценный тест');
    it('Внешний вид', async function() {
        const bro = this.browser;

        await bro.yaWaitForVisible(pageObjects.cards());
        await bro.assertView('page', 'body', {
            hideElements: pageObjects.headerBackdrop()
        });
    });
});
