const querystring = require('querystring');

describe('ТВ-каналы', function() {
    it('Открытие карусели', async function() {
        const { browser } = this;

        const offset = 0;

        await browser.yaOpenPage(`channels/${offset}/?now=1599513357&end_date__from=1599513357&start_date__to=1599513357&service=ya-station&from=ya-station&srcrwr=SRC_SETUP%3Ashav2.man.yp-c.yandex.net%3A20202&ipreg=213&waitall=da&timeout=9999999&quasarUI=1`);
        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');

        // Проверка кнопки назад
        await browser.yaQuasarMove('down');
        await browser.yaQuasarMove('down');
        await browser.yaQuasarRCMove('back');
        await browser.yaWaitForNavigationState({ up: false });
        await browser.yaAssertQuasarState('plain');
    });

    it('Корректно выдаёт саджесты', async function() {
        const { browser, PO } = this;

        await browser.yaOpenPage('channels/0/?' + querystring.stringify({
            quasarUI: 1,
        }));
        await browser.assertView('suggests', PO.NativeUI.Footer());
        await browser.yaAssertQuasarState('suggests');
    });
});
