const indexPage = require('../../page-objects/index');

describe('desktop-taxi-24: Главная. Отмена заказа на этапе поиска такси', function() {
    it('Заказ должен отмениться', async function() {
        const bro = this.browser;
        await bro.auth('taxi-24');
        await indexPage.open(bro, { gfrom: '55.736916,37.641769', center: '55.736916,37.641769' });
        await bro.waitForVisible(indexPage.tariff.buttonActivePrice, 30000);

        // Ожидаем завершения анимации появления стоимости тарифа
        await bro.pause(1000);

        //Ждем доступность кнопки Заказать
        await bro.waitUntil(async function() {
            return await bro.getAttribute(indexPage.orderFormButtonState, 'aria-disabled') === 'false';
        });

        await bro.hideElement(indexPage.map.container);

        await bro.click(indexPage.orderFormButton);
        await bro.waitForVisible(indexPage.orderFormHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderStatus.containerHidden, 5000, true);
        await bro.waitForVisible(indexPage.orderFormButton, 50000, true);
        await bro.waitForVisible(indexPage.orderStatus.searchViewInfo, 50000);
        await bro.pause(2000);
        await bro.assertView('formSearch', indexPage.orderForm, { ignoreElements: [indexPage.headerContainer] });

        await bro.click(indexPage.orderStatus.cancelButton);
        await bro.waitForVisible(indexPage.commonModalContent, 5000);
        await bro.assertView('cancelPopup', indexPage.commonModalContent);

        await bro.click(indexPage.commonModalConfirmButton);
        await bro.waitForVisible(indexPage.commonModalContainerLoading, 5000);
        await bro.waitForVisible(indexPage.commonModalContainerLoading, 10000, true);

        await bro.hideElement(indexPage.orderForm);
        await bro.assertView('cancelInfoPopup', indexPage.commonModalContent);
    });
});
