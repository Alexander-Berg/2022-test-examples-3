import {assert} from 'chai';
import {order} from 'suites/trains';

import {MINUTE, SECOND} from 'helpers/constants/dates';
import {TRAINS_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {TestTrainsApiClient} from 'helpers/project/trains/api/TestTrainsApiClient';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';
import {delay} from 'helpers/project/common/delay';

describe(order.steps.order, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Кассовый возврат', async function () {
        const app = new TestTrainsApp(this.browser);
        const {
            orderPlacesStepPage,
            orderPassengersStepPage,
            orderConfirmationStepPage,
            paymentPage,
            genericOrderPage,
            happyPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await app.setTestContext({
            ...TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
            officeReturnDelayInSeconds: TestTrainsApiClient.minTimeoutInSeconds,
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
        const firstTicket = await firstPassenger.tickets.tickets.at(0);

        // Задать таймаут ответа для возврата билета через тестовый траст невозможно, поэтому ждём 1-2 минуты
        await delay(TestTrainsApiClient.minTimeoutInSeconds * 1000);
        await this.browser.waitUntil(
            async () => {
                await this.browser.refresh();
                await genericOrderPage.waitOrderLoaded();

                return (
                    (await firstPassenger.refundTicketStatus.getText()) ===
                    'Возврат'
                );
            },
            {
                timeout: 2 * MINUTE,
                timeoutMsg: 'Статус билета должен быть "Возврат"',
                interval: 15 * SECOND,
            },
        );

        assert.isTrue(
            !(await firstPassenger.isRefundButtonDisplayed()),
            'Кнопка сдать билет не должна отображаться',
        );
        assert.equal(
            await firstPassenger.refundTicketStatus.getText(),
            'Возврат',
            'Отображается некорректный статус билета',
        );
        assert.isTrue(
            await firstTicket.refund.downloadReceipt.isDisplayed(),
            'Не отображается чек на возврат',
        );
        assert.isTrue(
            !(await firstTicket.refund.downloadTicket.isDisplayed()),
            'Не должна отображаться квитанция на возврат',
        );
    });
});
