const pageObjects = require('../page-objects/landings').businessCorporateMail;

describe('Корпоративная почта в Яндекс 360', function() {
    beforeEach(async function() {
        const bro = this.browser;

        await bro.url('/business/corporate-mail/');
    });

    hermione.only.notIn('chrome-phone', 'убрать, когда будет писаться полноценный тест');
    it('Внешний вид', async function() {
        const bro = this.browser;

        await bro.yaWaitForVisible(pageObjects.root());
        await bro.assertView('page', 'body', {
            invisibleElements: [
                pageObjects.stickyButton(),
                pageObjects.animation()
            ]
        });
    });
});
