describe('Поиск видео', function() {
    it('Открытие карусели', async function() {
        const text = 'джеки%20чан';
        const offset = 0;

        const { browser } = this;

        await browser.yaOpenPage(`videoSearch/${offset}/?text=${text}`);
        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');

        // Проверка кнопки назад
        await browser.yaQuasarMove('right');
        await browser.yaQuasarMove('right');
        await browser.yaQuasarRCMove('back');
        await browser.yaWaitForNavigationState({ left: false });
        await browser.yaAssertQuasarState('plain');
    });

    it('Открытие карусели со смещением 1', async function() {
        const text = 'видео%20с%20котиками';
        const offset = 1;

        const { browser } = this;

        await browser.yaOpenPage(`videoSearch/${offset}/?text=${text}`);
        await browser.assertView('offset1', 'body');
        await browser.yaAssertQuasarState('offset1');

        await browser.yaQuasarMove('right');
        await browser.assertView('offset4', 'body');
        await browser.yaAssertQuasarState('offset4');
    });

    it('Открытие третьего экрана карусели', async function() {
        const text = 'джеки%20чан';
        const offset = 9;

        const { browser } = this;

        await browser.yaOpenPage(`videoSearch/${offset}/?text=${text}`);
        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
});
