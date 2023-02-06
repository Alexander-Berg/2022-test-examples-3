import {assert} from 'chai';
import {happyPage} from 'suites/hotels';

import {MINUTE, SECOND} from 'helpers/constants/dates';

import TestApp from 'helpers/project/TestApp';

const {name: suiteName} = happyPage;

describe(suiteName, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Общий вид HP', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {hotelsBookApp} = app;
        const {hotelsPaymentPage, hotelsHappyPage} = hotelsBookApp;

        await hotelsBookApp.paymentTestContextHelper.setPaymentTestContext();

        await hotelsBookApp.book();

        await hotelsPaymentPage.waitUntilLoaded();

        await hotelsHappyPage.loader.waitUntilLoaded(30 * SECOND);
        await hotelsHappyPage.test();

        assert(
            await hotelsHappyPage.orderActions.downloadButton.isVisible(),
            'На Happy page должна отображаться кнопка "Скачать"',
        );

        if (hotelsHappyPage.isDesktop) {
            assert(
                await hotelsHappyPage.orderActions.printButton.isVisible(),
                'На Happy page должна отображаться кнопка "Распечатать"',
            );
        }
    });
});
