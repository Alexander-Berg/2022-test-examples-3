import {assert} from 'chai';
import {order} from 'suites/trains';

import {MINUTE} from 'helpers/constants/dates';
import {TRAINS_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {TestTrainsApiClient} from 'helpers/project/trains/api/TestTrainsApiClient';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';
import {delay} from 'helpers/project/common/delay';

describe(order.steps.order, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Получение билета в кассе', async function () {
        const app = new TestTrainsApp(this.browser);
        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
            genericOrderPage,
            paymentPage,
            happyPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await app.setTestContext({
            ...TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
            officeAcquireDelayInSeconds:
                TestTrainsApiClient.minTimeoutInSeconds,
        });
        await app.paymentTestContextHelper.setPaymentTestContext();

        await app.setFirstPassengerViaFields(PASSENGER, CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();
        await orderConfirmationStepPage.goNextStep();

        await paymentPage.waitUntilLoaded();

        await happyPage.waitUntilLoaded();
        await happyPage.orderActions.detailsLink.click();

        await genericOrderPage.waitOrderLoaded();

        const firstPassenger = await genericOrderPage.passengers.passengers.at(
            0,
        );

        await delay(TestTrainsApiClient.minTimeoutInSeconds * 1000);
        await this.browser.refresh();
        await genericOrderPage.waitOrderLoaded();

        const firstWarning = await genericOrderPage.warnings.warnings.at(0);

        assert.equal(
            await firstWarning.getText(),
            'Билеты, которые вы получили на бланке в кассе или терминале, вернуть онлайн нельзя. Вы можете вернуть их только в кассе.',
            'Отображается некорректный сообщение в предупреждении',
        );

        assert.equal(
            await firstPassenger.refundTicketStatus.getText(),
            'Выдан посадочный купон',
            'Отображается некорректный статус билета',
        );

        assert.isFalse(
            await firstPassenger.isRefundButtonDisplayed(),
            'Кнопка сдать билет не должна отображаться',
        );

        assert.isFalse(
            await firstPassenger.actions.cancelAllCheckinIsDisplayed(),
            'Кнопка отменить ЭР не должна отображаться',
        );

        /* Закрываем dropdown с отменой электронной регистрации */
        if (firstPassenger.isDesktop) {
            await firstPassenger.info.name.click();
        }

        assert.isTrue(
            await genericOrderPage.orderActions.downloadButton.isDisplayed(),
            'Кнопка скачать должна отображаться',
        );

        if (genericOrderPage.orderActions.printButton.isTouch) {
            assert.isFalse(
                await genericOrderPage.orderActions.printButton.isDisplayed(),
                'Кнопка распечатать не должна отображаться в тачах',
            );
        } else {
            assert.isTrue(
                await genericOrderPage.orderActions.printButton.isDisplayed(),
                'Кнопка распечатать должна отображаться в десктопе',
            );
        }
    });
});
