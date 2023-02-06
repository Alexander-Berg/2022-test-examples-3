const indexPage = require('../../page-objects/index');

describe('tap-taxi-221: Главная. Отображение информационной модалки тарифов', function() {
    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должна отображаться информационная модалка тарифа "Комфорт"', async function() {
        const bro = this.browser;
        await openPageAndTariffInfoModal(bro, 'Комфорт');

        await bro.assertView('tariffComfortInfoModal', indexPage.tariff.infoModal);
        await bro.click(indexPage.tariff.infoModalCancelButton);
        await bro.waitForVisible(indexPage.tariff.infoModal, 5000, true);
    });

    async function openPage(bro) {
        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActivePriceNonEmpty, 15000);
    }

    async function openPageAndTariffInfoModal(bro, tariffName) {
        await openPage(bro);
        await bro.pause(1000);
        await indexPage.tariff.clickButton(bro, tariffName);

        await bro.hideElement(indexPage.map.container);
        await indexPage.tariff.clickTariffActiveAndWaitInfoModal(bro);
        await bro.waitModalOpen();
    }
});
