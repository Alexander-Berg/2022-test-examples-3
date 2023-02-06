const indexPage = require('../../page-objects/index');

describe('tap-taxi-79: Главная. Закрытие шторки "Детали поездки" нажатием на системную кнопку back', function() {
    it('Шторка должна закрыться ', async function() {
        const bro = this.browser;
        await bro.auth('taxi-79');
        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 30000);

        // Ожидаем завершения анимации появления стоимости тарифа
        await bro.pause(3000);

        await bro.click(indexPage.orderFormButton);
        await bro.pause(1000);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderFormButton, 30000, true);
        await bro.waitForVisible(indexPage.orderStatus.container, 30000);
        await bro.swipeUp(indexPage.orderStatus.container, 1000);
        await bro.waitForVisible(indexPage.orderStatus.details, 5000);

        await bro.click(indexPage.orderStatus.details);
        await bro.waitForVisible(indexPage.orderStatus.detailsModal, 5000);

        // Ожидаем полного раскрытия модалки
        // иначе возврат происходит на страницу паспорта
        await bro.pause(2000);

        await bro.back();
        await bro.waitForVisible(indexPage.orderStatus.detailsModal, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.cancelButton, 5000);
    });
});
