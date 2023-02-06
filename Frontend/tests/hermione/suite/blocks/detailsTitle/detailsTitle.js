const selectors = require('../../../page-objects');

const testTitle = async function({ url, mock }) {
    await this.browser
        .ywOpenPage(url, {
            lang: this.lang,
            query: {
                usemock: mock,
            },
        })
        .ywWaitForVisible(selectors.month.Title, 10000)
        .assertView('Title', selectors.month.Title);
};

describe('Блоки', function() {
    describe('Страница детального прогноза', function() {
        it.langs.full();
        it('Заголовок', function() {
            return testTitle.call(this, {
                url: 'moscow/details',
                mock: `turboapp_moscow${this.lang.tld !== 'ru' ? `_${this.lang.tld}` : ''}`,
            });
        });
    });
    describe('Страница месяца', function() {
        it.langs.full();
        it('Заголовок', function() {
            return testTitle.call(this, {
                url: 'month/april',
                mock: `turboapp-month${this.lang.tld !== 'ru' ? `-${this.lang.tld}` : ''}`,
            });
        });
    });
});
