const PO = require('../../../../page-objects');
const DOCUMENTATION_URL = 'https://doc.yandex-team.ru/goals/';

describe('Неизменные элементы сайта', function() {
    beforeEach(function() {
        return this.browser
            .setViewportSize({ width: 1700, height: 2000 })
            .loginToGoals();
    });

    it('Скриншот главной шапки', async function() {
        const browser = this.browser;

        await browser.preparePage('site-header', '/');
        // Ждём самого позднего элемента (кнопка создания рендерится асинхронно)
        await browser.waitForVisible(PO.header.newGoalButton());
        await browser.assertView('site-header', PO.header());
    });

    it('Корректная ссылка на документацию', async function() {
        const browser = this.browser;

        await browser.preparePage('site-header', '/');
        await browser.waitForVisible(PO.header.questionMarkButton());
        await browser.yaCheckHref(
            PO.header.questionMarkButton(),
            { href: DOCUMENTATION_URL },
            'Wroing documentation url',
        );
    });
});
