describe('Коллекция', function() {
    const entref = '0oEltsc3Quc2xtQ2hzS0JIUmxlSFFTRTNOZmRHRm5PbTVsZDE5NVpXRnlYekl3TWpFS0hnb0ZjbVZzWlhZU0ZXWnZjbTExYkdFOVJqWXdNVEF3TURBd01EQldNdz09GAIra52w';
    const text = 'Новогоднее кино';

    it('Открытие коллекции', async function() {
        const offset = 0;

        const { browser } = this;

        async function check(direction, number) {
            await browser.yaQuasarRCMove(direction);
            await browser.yaAssertRCActiveElement('.VideoItem', 'VideoItem_active', number);
        }

        await browser.yaOpenPage(`collection/${offset}/?entref=${entref}&text=${text}&ny_promo=1`);
        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');

        // фокус устанавливается на первый видимый элемент
        await check('right', 1);
        // проверка 1 карточки налево, вверх, направо
        await check('left', 1);
        await check('up', 1);
        await check('right', 2);
        // Спускаемся до 3 экрана
        await check('down', 2);
        await check('down', 2);

        await browser.assertView('3th-screen', 'body');
        await browser.yaAssertQuasarState('3th-screen');

        // Двигаемся пультом по 3 экрану
        await check('right', 3);
        await check('right', 4);
        // Пультом переходим на 2 экран
        await check('up', 4);
        // Голосом возвращаемся на первый экран
        await browser.yaQuasarMove('up');

        // Скриншот и состояние должны совпадать. К сожалению нельзя проверить на plain, ругается на duplicate
        await browser.assertView('plain2', 'body');
        await browser.yaAssertQuasarState('plain');

        // Проверка кнопки назад
        await browser.yaQuasarMove('down');
        await browser.yaQuasarMove('down');
        await browser.yaQuasarRCMove('back');
        await browser.yaWaitForNavigationState({ up: false });
        await browser.yaAssertQuasarState('plain');
    });

    it('Открытие третьего экрана коллекции', async function() {
        const offset = 2;

        const { browser } = this;

        await browser.yaOpenPage(`collection/${offset}/?entref=${entref}&text=${text}&ny_promo=1`);
        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });

    it('Длинный заголовок', async function() {
        const offset = 0;
        const entref = '0oEgZsc3RueTAYAprPccs';
        const text = 'Спасители и защитники';

        const { browser } = this;

        await browser.yaOpenPage(`collection/${offset}/?entref=${entref}&text=${text}`);
        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
});
