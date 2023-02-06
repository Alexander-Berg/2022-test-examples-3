describe('Домашний экран', function() {
    // Предзагрузка нескольких каруселей для тестов создает дополнительные ненужные дампы
    const expFlags = '';

    it('Карусель', async function() { // c471dec
        const { browser } = this;

        await browser.yaOpenPage('home/?offset=0' + expFlags);

        await browser.yaWaitForNavigationState({ down: true });

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');

        await browser.yaQuasarMove('right');
        await browser.yaQuasarMove('right');
        await browser.yaQuasarMove('right');
        await browser.yaQuasarMove('left');

        await browser.assertView('3th-screen', 'body');
        await browser.yaAssertQuasarState('3th-screen');

        // Проверка кнопки назад
        await browser.yaQuasarMove('right');
        await browser.yaQuasarMove('right');
        await browser.yaQuasarRCMove('back');
        await browser.yaWaitForNavigationState({ left: false });
        await browser.yaAssertQuasarState('plain');

        await browser.yaQuasarMove('right');
        await browser.yaQuasarMove('right');
        await browser.yaQuasarMove('up');

        await browser.assertView('home-screen', 'body');
        await browser.yaAssertQuasarState('home-screen');
    });

    // it('Двурядная Карусель', async function() {
    //     const { browser } = this;
    //
    //     await browser.yaOpenPage('home/?offset=8' + expFlags);
    //
    //     await browser.yaWaitForNavigationState({ down: true });
    //
    //     await browser.assertView('plain', 'body');
    // });

    it('Навигация пультом в однорядной карусели', async function() { // 36ccc15
        const { browser } = this;

        await browser.yaOpenPage('home/?offset=0' + expFlags);

        await browser.yaWaitForNavigationState({ down: true, up: true });

        async function check(direction, number) {
            await browser.yaQuasarRCMove(direction);
            await browser.yaAssertRCActiveElement('.VideoItem', 'VideoItem_active', number);
        }

        await check('right', 1);
        // проверка 1 карточки вверх, вниз, направо
        await check('up', 4);
        await check('down', 1);
        await check('down', 1);
        await check('up', 1);
        await check('right', 2);
        // проверка 2 карточки вверх, вниз, направо
        await check('up', 5);
        await check('down', 2);
        await check('down', 2);
        await check('up', 2);
        await check('right', 3);
        // проверка 3 карточки вверх, вниз, направо
        await check('up', 6);
        await check('down', 3);
        await check('down', 3);
        await check('up', 3);
        await check('right', 4);
        // проверка 4 карточки вверх, вниз, направо
        await check('up', 7);
        await check('down', 4);
        await check('down', 4);
        await check('up', 4);
        await check('right', 4);
        // проверка 1 - 4 карточки налево
        await check('left', 4);
        await check('left', 3);
        await check('left', 2);
        await check('left', 1);
        await check('left', 1);
    });

    // it('Навигация пультом в многорядной карусели', async function() {
    //     // 22fed37
    //     const { browser } = this;
    //
    //     await browser.yaOpenPage('home/0/?offset=8' + expFlags);
    //
    //     await browser.yaWaitForNavigationState({ down: true, up: true });
    //
    //     async function check(direction, number, multirow = true) {
    //         await browser.yaQuasarRCMove(direction);
    //         await browser.yaAssertRCActiveElement('.VideoItem', 'VideoItem_active', number, multirow);
    //     }
    //
    //     await check('right', 1);
    //     await browser.yaQuasarMove('right');
    //     await browser.yaQuasarMove('right');
    //     await browser.yaQuasarMove('right');
    //     await browser.yaQuasarMove('right');
    //     await browser.yaQuasarMove('right');
    //     await browser.yaQuasarMove('right');
    //     await browser.yaQuasarMove('right');
    //     await check('up', 1, false);
    //     await check('right', 2, false);
    //     await check('right', 3, false);
    //     await check('right', 4, false);
    //     await check('right', 4, false);
    //     await check('down', 4);
    //     await check('up', 4, false);
    //     await check('left', 3, false);
    //     await check('left', 2, false);
    //     await check('left', 1, false);
    //     await check('left', 4);
    //     await check('down', 8);
    //     await check('right', 1, false);
    // });
});
