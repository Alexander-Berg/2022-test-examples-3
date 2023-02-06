const indexPage = require('../../page-objects/index');

describe('tap-taxi-16: Тариф. Отображение активного тарифа при закрытии шторки выбора маршрута при условии, что тариф находится вне зоны видимости', function() {
    it('Должен отображаться активный тариф при закрытии модалки по нажатию в область вне модалки', async function() {
        const bro = this.browser;
        await openPageAndSwipeTariff(bro);

        await bro.click(indexPage.addressSuggest.sideBlock);
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 5000);

        // Ожидаем отработки автоматического свайпа к активному тарифу
        await bro.pause(2000);

        await bro.assertView('tariffAfterCloseAddressSuggest', indexPage.tariff.tariffsSelector, { allowViewportOverflow: true });
    });

    it('Должен отображаться активный тариф при закрытии модалки на системную кнопку back', async function() {
        const bro = this.browser;
        await openPageAndSwipeTariff(bro);

        await bro.back();
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 5000);

        // Ожидаем отработки автоматического свайпа к активному тарифу
        await bro.pause(2000);

        await bro.assertView('tariffAfterCloseAddressSuggest', indexPage.tariff.tariffsSelector, { allowViewportOverflow: true });
    });

    it('Должен отображаться активный тариф при закрытии модалки смахиванием вниз', async function() {
        const bro = this.browser;
        await openPageAndSwipeTariff(bro);

        // Ожидаем полного раскрытия модалки выбора адреса
        await bro.pause(2000);

        await bro.swipeDown(indexPage.addressSuggest.container, 1000);
        await bro.waitForVisible(indexPage.addressSuggest.container, 5000, true);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 5000);

        // Ожидаем отработки автоматического свайпа к активному тарифу
        await bro.pause(2000);

        await bro.assertView('tariffAfterCloseAddressSuggest', indexPage.tariff.tariffsSelector, { allowViewportOverflow: true });
    });

    async function openPageAndSwipeTariff(bro) {
        await bro.auth('taxi');

        await indexPage.open(bro);
        await bro.waitForVisible(indexPage.tariff.buttonTitleActive, 30000);

        await bro.swipeLeft(indexPage.tariff.tariffsSelector, 700);
        await bro.assertView('afterSwipeTariff', indexPage.tariff.tariffsSelector, { allowViewportOverflow: true });

        await indexPage.clickAddressAndWaitSuggest(
            bro,
            indexPage.addressPoint.fromInput,
            indexPage.addressSuggest.searchFrom
        );
    }
});
