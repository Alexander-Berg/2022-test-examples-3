const pageObjects = require('../page-objects/landings').businessMessenger;

describe('Яндекс Мессенджер для компаний', function() {
    beforeEach(async function() {
        const bro = this.browser;

        await bro.url('/business/messenger/?test-id=599226');
    });

    hermione.only.notIn('chrome-phone', 'убрать, когда будет писаться полноценный тест');
    it('Внешний вид', async function() {
        const bro = this.browser;

        await bro.yaWaitForVisible(pageObjects.root());
        await bro.assertView('page', 'body', {
            invisibleElements: [pageObjects.stickyButton()]
        });
    });
});
