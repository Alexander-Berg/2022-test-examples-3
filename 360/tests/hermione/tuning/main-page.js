const pageObjects = require('../page-objects/tuning').objects;

hermione.only.notIn('chrome-phone', 'убрать, когда будет писаться полноценный тест');
describe('Главная страница', function() {

    it('Внешний вид', async function() {
        const bro = this.browser;

        await bro.url('/?theme=default');

        await bro.yaWaitForVisible(pageObjects.tariffsGrid());
        await bro.assertView('page', 'body', {
            invisibleElements: [
                pageObjects.tariffsGrid.header(),
                pageObjects.tariffsGrid.arrowButtons()
            ]
        });
    });

    it('Темная тема', async function () {
        const bro = this.browser;

        await bro.url('/?theme=test-dark-link');

        await bro.yaWaitForVisible(pageObjects.tariffsGrid());
        await bro.assertView('page', 'body', {
            invisibleElements: [
                pageObjects.tariffsGrid.header(),
                pageObjects.tariffsGrid.arrowButtons()
            ]
        });
    });
});
