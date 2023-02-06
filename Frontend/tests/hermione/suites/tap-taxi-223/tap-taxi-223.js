const indexPage = require('../../page-objects/index');

describe('tap-taxi-223: Главная. Отображение попапа о повышенном спросе с последующим её закрытием', function() {
    it('Модалка о повышенном спросе должны закрыться нажатием на системную кнопку back', async function() {
        const bro = this.browser;

        await bro.auth('taxi-1');
        await indexPage.open(bro, { gfrom: '55.736916,37.641769', center: '55.736916,37.641769' });
        await indexPage.addressPoint.clickPositionFirst(bro);
        await bro.waitForVisible(indexPage.tariff.priceSurge, 30000);

        await indexPage.tariff.clickTariffActiveAndWaitRequirementsModal(bro);

        await bro.click(indexPage.tariff.tariffButtonInRequirementsModal);

        await bro.waitForVisible(indexPage.tariff.infoModal, 5000);
        await bro.waitForVisible(indexPage.tariff.infoModalSurge, 5000);

        await bro.click(indexPage.tariff.infoModalSurge);
        await bro.waitForVisible(indexPage.tariff.surgeModal, 5000);
        await bro.assertView('surgeModal', indexPage.tariff.infoModal);

        await bro.back();
        await bro.waitForVisible(indexPage.tariff.surgeModal, 5000, true);
        await bro.waitForVisible(indexPage.tariff.infoModal, 5000);

        await bro.back();
        await bro.waitForVisible(indexPage.tariff.surgeModal, 5000, true);
        await bro.waitForVisible(indexPage.tariff.infoModal, 5000, true);
    });

    it('Модалка о повышенном спросе должны закрыться нажатием в область вне модалки', async function() {
        const bro = this.browser;

        await bro.auth('taxi-1');
        await indexPage.open(bro, { gfrom: '55.736916,37.641769', center: '55.736916,37.641769' });
        await indexPage.addressPoint.clickPositionFirst(bro);
        await bro.waitForVisible(indexPage.tariff.priceSurge, 30000);

        await indexPage.tariff.clickTariffActiveAndWaitRequirementsModal(bro);

        await bro.click(indexPage.tariff.tariffButtonInRequirementsModal);

        await bro.waitForVisible(indexPage.tariff.infoModal, 5000);
        await bro.waitForVisible(indexPage.tariff.infoModalSurge, 5000);

        await bro.click(indexPage.tariff.infoModalSurge);
        await bro.waitForVisible(indexPage.tariff.surgeModal, 5000);

        await bro.leftClick(indexPage.tariff.infoModalSideControl);
        await bro.waitForVisible(indexPage.tariff.surgeModal, 5000, true);
        await bro.waitForVisible(indexPage.tariff.infoModal, 5000);

        await bro.back();
        await bro.waitForVisible(indexPage.tariff.surgeModal, 5000, true);
        await bro.waitForVisible(indexPage.tariff.infoModal, 5000, true);
    });

    it('Модалка о повышенном спросе должны закрыться смахиванием вниз', async function() {
        const bro = this.browser;

        await bro.auth('taxi-1');
        await indexPage.open(bro, { gfrom: '55.736916,37.641769', center: '55.736916,37.641769' });
        await indexPage.addressPoint.clickPositionFirst(bro);
        await bro.waitForVisible(indexPage.tariff.priceSurge, 30000);

        await indexPage.tariff.clickTariffActiveAndWaitRequirementsModal(bro);

        await bro.click(indexPage.tariff.tariffButtonInRequirementsModal);

        await bro.waitForVisible(indexPage.tariff.infoModal, 5000);
        await bro.waitForVisible(indexPage.tariff.infoModalSurge, 5000);

        await bro.click(indexPage.tariff.infoModalSurge);
        await bro.waitForVisible(indexPage.tariff.surgeModal, 5000);

        await bro.swipeDown(indexPage.tariff.surgeModal, 1000);
        await bro.waitForVisible(indexPage.tariff.surgeModal, 5000, true);
        await bro.waitForVisible(indexPage.tariff.infoModal, 5000);

        await bro.back();
        await bro.waitForVisible(indexPage.tariff.surgeModal, 5000, true);
        await bro.waitForVisible(indexPage.tariff.infoModal, 5000, true);
    });
});
