const indexPage = require('../../page-objects/index');

describe('tap-taxi-292: Адрес. Закрытие модалки ввода подъезда c закрытой клавиатурой', function() {
    it('Модалка должна закрыться по нажатию в область вне модалки', async function() {
        const bro = this.browser;
        await openPageAndPorchForm(bro);

        await bro.leftClick(indexPage.map.container);
        await bro.waitForVisible(indexPage.porchForm.container, 5000, true);
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 5000);
    });

    it('Модалка должна закрыться по нажатию на системную кнопку back', async function() {
        const bro = this.browser;
        await openPageAndPorchForm(bro);

        await bro.back();
        await bro.waitForVisible(indexPage.porchForm.container, 5000, true);
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 5000);
    });

    it('Модалка должна закрыться смахиванием вниз', async function() {
        const bro = this.browser;
        await openPageAndPorchForm(bro);

        await bro.swipeDown(indexPage.porchForm.container, 1000);
        await bro.waitForVisible(indexPage.porchForm.container, 5000, true);
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 5000);
    });

    it('Модалка должна по нажатию на кнопку "Отмена"', async function() {
        const bro = this.browser;
        await openPageAndPorchForm(bro);

        await bro.click(indexPage.porchForm.closeButton, 1000);
        await bro.waitForVisible(indexPage.porchForm.container, 5000, true);
        await bro.waitForVisible(indexPage.addressPoint.porchButton, 5000);
    });

    async function openPageAndPorchForm(bro) {
        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await indexPage.porchForm.open(bro);
    }
});
