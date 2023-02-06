const indexPage = require('../../page-objects/index');

describe('tap-taxi-293: Адрес. Отсутствие возможности ввода значения в поле ввода подъезда отличные от цифр', function() {
    it('Поле ввода должно отображаться пустым', async function() {
        const bro = this.browser;
        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await indexPage.porchForm.open(bro);
        await bro.keys('QweТес!"№;%:?*()__+@#$%^&*()_~');
        await bro.assertViewAfterLockFocusAndHover('porchForm', indexPage.root, { ignoreElements: [
            indexPage.map.container,
            indexPage.tariff.tariffsSelector,
            indexPage.addressPoint.fromInput,
            indexPage.addressPoint.toInput
        ] });
    });
});
