describe('Домашний экран', function() {
    it('Главный экран', async function() { // 01ac829
        const { browser } = this;
        async function check(direction, number) {
            await browser.yaQuasarRCMove(direction);
            await browser.yaAssertRCActiveElement('.VideoItem', 'VideoItem_active', number);
        }

        async function checkTab(direction, number) {
            await browser.yaQuasarRCMove(direction);
            await browser.yaAssertRCActiveElement('.MenuTab', 'MenuTab_active', number);
        }

        await browser.yaOpenPage('home/');

        await browser.yaWaitForNavigationState({ down: true });

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');

        // фокус устанавливается на первый видимый элемент
        await check('right', 1);
        await checkTab('up', 1); // первый таб

        await browser.assertView('home-active', 'body');

        await checkTab('right', 2); // второй таб
        await browser.yaWaitForNavigationState({ down: true, right: true });

        await browser.assertView('films-active', 'body');
        await browser.yaAssertQuasarState('films');

        await checkTab('left', 1); // первый таб
        await check('down', 1);
        // проверка 1 карточки налево, направо
        await check('left', 1);
        await check('right', 2);
        // проверка 2 карточки вверх, направо
        await checkTab('up', 1); // первый таб
        await check('down', 1);
        await check('right', 2);
        await check('right', 3);
        // проверка 3 карточки вверх, налево, направо
        await checkTab('up', 1); // первый таб
        await check('down', 1);
        await check('right', 2);
        await check('right', 3);
        // проверка 2 и 3 карточки налево, 1 карточки вниз
        await check('left', 2);
        await check('left', 1);
        await check('down', 4);

        // проверка 4 карточки вверх, направо
        await check('up', 1);
        await check('down', 4);
        await check('right', 5);
        // проверка 5 карточки вверх, направо
        await check('up', 2);
        await check('down', 6);
        await check('left', 5);
        await check('right', 6);
        // проверка 6 карточки вверх, направо
        await check('up', 2);
        await check('down', 6);
        await check('right', 7);
        // проверка 7 карточки вверх, направо
        await check('up', 3);
        await check('down', 7);

        // проверка 7, 6, 5, 4 карточек налево
        await check('left', 6);
        await check('left', 5);
        await check('left', 4);
        await check('left', 4);
    });
});
