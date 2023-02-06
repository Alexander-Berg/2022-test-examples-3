const indexPage = require('../../page-objects/index');

describe('tap-taxi-194: Главная. Вызов такси по нажатию на кнопку "Заказать" в информационной модалке тарифа', function() {
    it('Должен появится экран поиска такси', async function() {
        const bro = this.browser;
        await bro.auth('taxi-194');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 30000);

        // Ожидаем завершения анимации появления стоимости тарифа
        await bro.pause(2000);

        await indexPage.tariff.clickTariffActiveAndWaitRequirementsModal(bro);

        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);

        await bro.waitForVisible(indexPage.orderStatus.cancelButton, 5000);
        await bro.click(indexPage.orderStatus.cancelButton);
        await bro.waitForVisible(indexPage.commonModalContent, 5000);

        await bro.click(indexPage.commonModalConfirmButton);
        await bro.waitForVisible(indexPage.commonModalContent, 10000);
        await bro.click(indexPage.commonModalConfirmButton);
        await bro.waitForVisible(indexPage.tariff.requirementsModalTariffInfo, 5000, true);
    });
});
