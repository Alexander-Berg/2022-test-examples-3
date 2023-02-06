describe('Онбординги', function() {
    it('Фича субтитров. Экран 1', async function() {
        const { browser } = this;

        await browser.yaOpenPage('onboardings/subs/?clientX=0');

        await browser.yaWaitForNavigationState({ down: false, up: false, left: false, right: true });

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
    it('Фича субтитров. Экран 2', async function() {
        const { browser } = this;

        await browser.yaOpenPage('onboardings/subs/?clientX=1');

        await browser.yaWaitForNavigationState({ down: false, up: false, left: true, right: false });

        await browser.assertView('plain', 'body');
        await browser.yaAssertQuasarState('plain');
    });
    it('4k', async function() {
        const { browser } = this;

        await browser.yaOpenPage('onboardings/uhd');

        await browser.assertView('plain', 'body');
    });
});
