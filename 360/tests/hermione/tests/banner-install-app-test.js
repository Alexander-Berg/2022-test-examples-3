const consts = require('../config/index').consts;
const PageObjects = require('../page-objects/public');

describe('Проверка баннеров на мобильных устройствах -> ', () => {
    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-84522');
    it('AssertView: diskpublic-271: Баннер "Установить приложение"', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FB2_FILE_URL);
        await bro.yaWaitForVisible(
            PageObjects.mobileBannerInstallApp(),
            'Баннер "Установить приложение" не отобразился'
        );
        await bro.assertView('banner-install-app', PageObjects.mobileBannerInstallApp());
    });
});
