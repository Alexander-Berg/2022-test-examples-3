const consts = require('../config').consts;
const PageObjects = require('../page-objects/public');

describe('Проверка баннеров на мобильных устройствах -> ', () => {
    hermione.only.in('chrome-phone-4.4.2', 'Актуально только для Android 4.4+');
    it('diskpublic-2268: Баннер "Открыть в приложении" - Переход по ссылке', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FB2_FILE_URL);
        await bro.yaWaitForVisible(PageObjects.mobileBannerOpenInApp(), 'Баннер "Открыть в приложении" не отобразился');
        await bro.yaClickAndAssertNewTabUrl(
            PageObjects.mobileBannerOpenInApp(),
            { linkShouldContain: 'appmetrica_deep_link' }
        );
    });

    hermione.only.in('chrome-phone-4.4.2', 'Актуально только для Android 4.4+');
    it('AssertView: diskpublic-2267: Проверка отображения баннера "Открыть в приложении"', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FB2_FILE_URL);
        await bro.yaWaitForVisible(PageObjects.mobileBannerOpenInApp(), 'Баннер "Открыть в приложении" не отобразился');
        await bro.assertView('banner-open-in-app', PageObjects.mobileBannerOpenInApp());
    });
});
