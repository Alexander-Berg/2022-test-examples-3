import {assert} from 'chai';
import {order} from 'suites/trains';

import {MINUTE} from 'helpers/constants/dates';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';

describe(order.steps.order, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Покупка со страховкой и успешный возврат', async function () {
        const app = new TestTrainsApp(this.browser);
        const {
            orderPlacesStepPage,
            orderConfirmationStepPage,
            orderPassengersStepPage,
            genericOrderPage,
            paymentPage,
            happyPage,
        } = app;

        await orderPlacesStepPage.browseToPageWithoutTransfer();
        await orderPlacesStepPage.waitTrainDetailsLoaded();

        await orderPlacesStepPage.selectPassengers({adults: 1});
        await orderPlacesStepPage.selectAnyPlacesInPlatzkarte();
        await orderPlacesStepPage.goNextStep();

        await app.setTestContext();
        await app.paymentTestContextHelper.setPaymentTestContext();

        await app.setFirstPassengerViaFields(PASSENGER, CONTACTS);
        await orderPassengersStepPage.layout.goNextStep();

        await orderConfirmationStepPage.waitOrderLoaded();
        await orderConfirmationStepPage.addInsurance();

        const {price: priceFromConfirmation} =
            await app.getDataFromConfirmationPage();

        await orderConfirmationStepPage.goNextStep();

        await paymentPage.waitUntilLoaded();

        await happyPage.waitUntilLoaded();
        await happyPage.orderActions.detailsLink.click();

        await genericOrderPage.waitOrderLoaded();

        const {orderOrchActionModal} = genericOrderPage;
        const firstPassenger =
            await genericOrderPage.passengers.passengers.first();
        const firstTicket = await firstPassenger.tickets.tickets.first();

        await firstPassenger.info.name.scrollIntoView();

        assert.isTrue(
            await firstTicket.insurance.price.isDisplayed(),
            'На странице заказа не отображается страховка',
        );

        assert.equal(
            priceFromConfirmation,
            await genericOrderPage.passengers.totalPrice.total.getPriceValue(),
            'Различаются цены со страницы подтверждения и заказа',
        );

        await firstPassenger.refundTicket();

        await orderOrchActionModal.loader.waitUntilLoaded();

        const priceInModal = await orderOrchActionModal.price.getPriceValue();

        await orderOrchActionModal.submitButton.click();
        await orderOrchActionModal.loader.waitUntilLoaded(2 * MINUTE);

        assert.equal(
            await firstPassenger.refundTicketStatus.getText(),
            'Возврат',
            'Отображается некорректный статус билета',
        );
        assert.isFalse(
            await firstPassenger.isRefundButtonDisplayed(),
            'Кнопка сдать билет не должна отображаться',
        );
        assert.equal(
            priceInModal,
            await firstTicket.refund.price.getPriceValue(),
            'Сумма к возврату не совпадает с суммой в модальном окне',
        );
        assert.isTrue(
            await firstTicket.refund.downloadTicket.isDisplayed(),
            'Не появилась квитанция на возврат',
        );
        assert.isTrue(
            await firstTicket.refund.downloadReceipt.isDisplayed(),
            'Не появился чек на возврат',
        );
    });
});
