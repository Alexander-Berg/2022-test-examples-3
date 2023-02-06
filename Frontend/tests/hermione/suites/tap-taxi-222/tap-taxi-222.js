const indexPage = require('../../page-objects/index');

describe('tap-taxi-222: Главная. Закрытие информационной модалки тарифов', function() {
    it('Модалка должна закрыться по нажатию в область вне модалки', async function() {
        const bro = this.browser;
        await openPageAndModalTariff(bro);

        await bro.click(indexPage.tariff.infoModalSideControl);
        await bro.waitForVisible(indexPage.tariff.infoModalImage, 5000, true);
        await bro.back();
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 5000);
    });

    it('Модалка должна закрыться по нажатию на системную кнопку back', async function() {
        const bro = this.browser;
        await openPageAndModalTariff(bro);

        await bro.back();
        await bro.waitForVisible(indexPage.tariff.infoModalImage, 5000, true);
        await bro.back();
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 5000);
    });

    it('Модалка должна закрыться смахиванием вниз', async function() {
        const bro = this.browser;
        await openPageAndModalTariff(bro);

        await bro.swipeDown(indexPage.tariff.infoModal, 1000);
        await bro.waitForVisible(indexPage.tariff.infoModalImage, 5000, true);
        await bro.back();
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 5000);
    });

    async function openPageAndModalTariff(bro) {
        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonActive, 10000);
        await indexPage.tariff.clickButton(bro, 'Комфорт');

        await indexPage.tariff.clickTariffActiveAndWaitInfoModal(bro);
    }
});
