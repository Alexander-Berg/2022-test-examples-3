describe('webdriver.io', () => {
    it('Telemost Tests', () => {
        const { conf, mute, mutevideo, zombname } = process.env;
        console.log(browser.sessionId);
        browser.url(`https://telemost.yandex.ru/j/${conf}`);
        browser.pause(3000);

        //костыль из-за сломанного protocol_handler в chrome 84
        // browser.newWindow(`https://${stand}.yandex.ru/j/${conf}${confSearch ? '?' + confSearch : ''}`);
        // browser.pause(5000);

        $('input').setValue(['test-', zombname]);

        browser.waitUntil(
            () => $('.Button2_view_accent').isEnabled() === true,
            {
                timeout: 10000,
                timeoutMsg: 'expected connect button to be enabled in 10s'
            }
        );

        if (mute === 'yes') {
            $("//button[contains(@title, 'Выключить микрофон')]").click();
        }
        if (mutevideo === 'yes') {
            $("//button[contains(@title, 'Выключить камеру')]").click();
        }
        $('.Button2_view_accent').click();

        browser.pause(60000 * process.env.lifetime);

        //выходим из конфы
        $("//button[contains(@title, 'Выйти')]").click();
        browser.pause(5000);
    });
});
