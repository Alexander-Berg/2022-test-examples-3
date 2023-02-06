const landingsObjects = require('../page-objects/landings');

describe('Лендинг для бизнеса', function() {
    beforeEach(async function() {
        const bro = this.browser;

        await bro.url('/business');
        await bro.yaWaitForVisible(landingsObjects.business.main());
    });

    hermione.only.notIn('chrome-phone', 'убрать, когда будет писаться полноценный тест');
    it('Внешний вид', async function() {
        const bro = this.browser;

        await bro.assertView('page', 'body', {
            invisibleElements: landingsObjects.business.stickyTurnOnButton()
        });
    });
});
