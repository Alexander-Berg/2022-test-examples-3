import {assert} from 'chai';
import {order} from 'suites/trains';

import {MINUTE, SECOND} from 'helpers/constants/dates';
import {TRAINS_SUCCESS_TEST_CONTEXT_PARAMS} from 'helpers/constants/testContext';

import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';
import {TestTrainsApiClient} from 'helpers/project/trains/api/TestTrainsApiClient';
import {PASSENGER} from 'helpers/project/trains/data/passengers';
import {CONTACTS} from 'helpers/project/trains/data/contacts';
import {TestIndexAviaPage} from 'helpers/project/avia/pages';
import {delay} from 'helpers/project/common/delay';

describe(order.steps.confirmation, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('ТК ЖД Ошибка получения данных о заказе', async function () {
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

        await app.setTestContext({
            ...TRAINS_SUCCESS_TEST_CONTEXT_PARAMS,
            alwaysTimeoutAfterConfirmingInSeconds:
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

        await delay(TestTrainsApiClient.minTimeoutInSeconds * SECOND * 1.5);

        await this.browser.refresh();

        const {errorModal} = genericOrderPage;

        assert.isTrue(
            await errorModal.isDisplayed(MINUTE),
            'Отображается ошибка оплаты после перезагрузки страницы вручную',
        );

        assert.include(
            await errorModal.text.getText(),
            'Не удалось загрузить данные о заказе',
            'Отображается текст ошибки "Не удалось загрузить данные о заказе ..."',
        );

        await errorModal.retryButton.click();

        assert.isTrue(
            await errorModal.isDisplayed(MINUTE),
            'Отображается ошибка оплаты после нажатия на кнопку Перезагрузить',
        );

        await errorModal.goToMainButton.click();

        const aviaIndexPage = new TestIndexAviaPage(this.browser);

        await aviaIndexPage.indexTabs.isDisplayed(25000);

        const {pathname} = await aviaIndexPage.indexTabs.getActiveTabInfo();

        assert.include(
            pathname,
            '/hotels/',
            'Произошел переход на главную, активна вкладка Отели',
        );
    });
});
