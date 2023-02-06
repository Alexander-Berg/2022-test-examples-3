const assert = require('chai').assert;
const url = require('url');

// TODO SBSDEV-8895: Падают тесты на ExpMeta из-за задержки исчезнования паранджи
describe.skip('ExpMeta:', function() {
    afterEach(function() {
        return this.browser.yaCheckClientErrors();
    });

    it('законченный эксперимент:', function() {
        return this.browser
            .openSbs('/experiment/62967')
            .waitForVisible('.ExpMeta-Header', 4000)
            .waitForVisible('.Overlay', 'Паранджа не скрылась', true)
            .assertView('completed', '.ExpMeta');
    });

    it('незапущенный эксперимент:', function() {
        return this.browser
            .openSbs('/experiment/1583')
            .waitForVisible('.ExpMeta .BarSection-Title_visited', 4000)
            .getAttribute('.ExpMeta-UserName .Username .link', 'href')
            .then((actualHref) => {
                const expectedHref = 'https://staff.yandex-team.ru/konovodov';
                assert.strictEqual(actualHref, expectedHref, 'Некорректная ссылка на Стафф автора эксперимента');
                return this.browser;
            })
            // @TODO use yaCheckUrl
            .getAttribute('.IconLink_parent', 'href')
            .then((actualHref) => {
                const expectedPath = '/experiment/8850';
                assert.strictEqual(url.parse(actualHref).pathname, expectedPath, 'Некорректная ссылка на эксперимент-родитель');
                return this.browser;
            })
            .getAttribute('.IconLink_nirvana', 'href')
            .then((actualHref) => {
                const expectedHref = 'https://nirvana.yandex-team.ru/flow/f1a92012-3894-45f5-97eb-44b74d30838a/graph';
                assert.strictEqual(actualHref, expectedHref, 'Некорректная ссылка на актуальный граф');
                return this.browser;
            })
            .getAttribute('.IconLink_st', 'href')
            .then((actualHref) => {
                const expectedHref = 'https://st.test.yandex-team.ru/SIDEBYSIDE-1583';
                assert.strictEqual(actualHref, expectedHref, 'Некорректная ссылка на тикет эксперимента');
                return this.browser;
            })
            .getAttribute('.ExpMeta-Pool_preview .link', 'href')
            .then((actualHref) => {
                const expectedHref = 'https://sandbox.toloka.yandex.com/requester/assignment-preview/pool/73389';
                assert.strictEqual(actualHref, expectedHref, 'Некорректная ссылка на предпросмотр эксперимента');
                return this.browser;
            })
            .then(() => this.browser.assertView('not-started', '.ExpMeta'));
    });
});
