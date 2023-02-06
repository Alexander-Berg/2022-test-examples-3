const landingsObjects = require('../page-objects/landings');

describe('Незалогиновая страница почты', function() {
    beforeEach(async function() {
        const bro = this.browser;

        await bro.url('/mail-landing');
        await bro.yaWaitForVisible(landingsObjects.mailLanding.root());
    });

    hermione.only.notIn('chrome-phone', 'убрать, когда будет писаться полноценный тест');
    it('Внешний вид', async function() {
        const bro = this.browser;

        await bro.assertView('page', 'body', {
            invisibleElements: landingsObjects.mailLanding.stickyButtons()
        });
    });
});
