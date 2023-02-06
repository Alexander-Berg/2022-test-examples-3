const selectors = require('../page-objects');

describe('a11y', function() {
    describe('Главная', function() {
        it('Страница', async function() {
            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: { axe: 1 } // принудительно подключаем axe-чанк, даже в прод/rc окружениях
                })
                .ywWaitForVisible(selectors.mainScreen, 10000)
                .ywCheckElementA11y();
        });
    });
});
