const content = require('../page-objects/client-content-listing').common;
const { stub, root } = require('../page-objects/client');
const clientNavigation = require('../page-objects/client-navigation');
const albums = require('../page-objects/client-albums-page');
const { appPromoBanner } = require('../page-objects/client');
const { footer } = require('../page-objects/client-footer').common;

/**
 * Ожидает элемент и ждет 500 мс
 *
 * @param {string}  selector
 * @returns {Promise<void>}
 */
async function waitAndPause(selector = stub()) {
    await this.browser.yaWaitForVisible(selector);
    await this.browser.pause(500); // завершение анимации
}

const ignoredElements = [
    clientNavigation.desktop.spaceInfoSection.infoSpaceButton(),
    clientNavigation.desktop.sidebarNavigation(),
    footer.copyright()
];

hermione.only.notIn(['chrome-phone-6.0', 'chrome-desktop-win', 'chrome-desktop-mac']);
describe('Заглушки -> ', () => {
    it('diskclient-4279, 4345: [Заглушки] - Фото', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4345' : 'diskclient-4279';
        await bro.yaClientLoginFast('yndx-ufo-test-169');

        await bro.yaOpenSection('photo', true);
        await waitAndPause.call(this);
        await bro.yaAssertView(this.testpalmId, root.content(), { ignoreElements: ignoredElements });
    });

    it('diskclient-5223, diskclient-5543: Отображение заглушки автоальбома', async function() {
        const bro = this.browser;
        const isMobile = await this.browser.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5543' : 'diskclient-5223';
        await bro.yaClientLoginFast('yndx-ufo-test-169');

        await bro.url('/client/photo?filter=beautiful');
        await waitAndPause.call(this);
        await bro.yaAssertView(this.testpalmId, root.content(), { ignoreElements: ignoredElements });
    });

    hermione.only.notIn('firefox-desktop', 'https://st.yandex-team.ru/CHEMODAN-72810');
    it('diskclient-697, 5422: [Заглушки] - Загрузки', async function() {
        const bro = this.browser;

        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5422' : 'diskclient-697';
        await bro.yaClientLoginFast('yndx-ufo-test-169');

        await bro.url('/client/disk/Загрузки');
        await waitAndPause.call(this);
        await bro.yaAssertView(this.testpalmId, root.content(), { ignoreElements: ignoredElements });

        if (!isMobile) { // на "мобильном" хроме вызывает падение селениума
            await bro.yaClickAndAssertNewTabUrl(stub.actionButton(), {
                linkShouldContain: 'https://browser.yandex.ru'
            }, 'Не открылась страница скачивания Яндекс браузера');
        }
    });

    it('diskclient-1962, 1985: [Заглушки] - Общий доступ', async function() {
        const bro = this.browser;

        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1985' : 'diskclient-1962';
        await bro.yaClientLoginFast('yndx-ufo-test-169');

        await bro.yaOpenSection('shared', true);
        await waitAndPause.call(this);
        await bro.yaAssertView(this.testpalmId, root.content(), { ignoreElements: ignoredElements });
    });

    it('diskclient-5421, 1983: [Заглушки] - Пустой корень', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1983' : 'diskclient-5421';
        await bro.yaClientLoginFast('yndx-ufo-test-169');

        await bro.yaOpenSection('disk', true); // Технически здесь нет заглушки
        await bro.yaAssertView(this.testpalmId, root.content(), { ignoreElements: ignoredElements });
    });

    it('diskclient-1963, 1128: [Заглушки] - Последние', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1128' : 'diskclient-1963';
        await bro.yaClientLoginFast('yndx-ufo-test-169');

        await bro.yaOpenSection('recent', true);
        await waitAndPause.call(this);
        await bro.yaAssertView(this.testpalmId, root.content(), {
            ignoreElements: ignoredElements.concat(stub.background())
        });
    });

    it('diskclient-1615, 693: [Заглушки] - История', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1615' : 'diskclient-693';
        await bro.yaClientLoginFast('yndx-ufo-test-17');

        await bro.yaOpenSection('journal');
        await bro.yaWaitForVisible(content.journalListing());
        await bro.yaAssertView(this.testpalmId, root.content(), {
            ignoreElements: ignoredElements.concat(content.journalListing.calendarDropdown())
        });
    });

    it('diskclient-5222, diskclient-5491: Отображение заглушки раздела Альбомы', async function() {
        const bro = this.browser;
        const isMobile = await this.browser.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5491' : 'diskclient-5222';
        await bro.yaClientLoginFast('yndx-ufo-test-169');

        await bro.url('/client/albums');
        await waitAndPause.call(this);

        await bro.yaAssertView(this.testpalmId, root.content(),
            { ignoreElements: clientNavigation.desktop.spaceInfoSection.infoSpaceButton() });
    });

    it('diskclient-5990, diskclient-5991: Заглушка пустого личного альбома', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5991' : 'diskclient-5990';

        await bro.yaClientLoginFast('yndx-ufo-test-256');

        await bro.url('/client/albums/5de92d3368c89a5950af92cb');
        await bro.yaWaitForVisible(albums.album2.stub());
        await bro.yaAssertView(this.testpalmId, albums.album2());
    });
});

hermione.only.notIn('chrome-phone-6.0');
describe('Заглушки зависящие от ОС -> ', () => {
    it('diskclient-699, 5423: [Заглушки] - Скриншоты"', async function() {
        const bro = this.browser;

        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5423' : 'diskclient-699';
        await bro.yaClientLoginFast('yndx-ufo-test-169');

        await bro.url('/client/disk/Скриншоты'); // Этого раздела нет в панели навигации
        await waitAndPause.call(this);

        const isPromoVisible = await bro.isVisible(appPromoBanner());
        if (isPromoVisible) {
            await bro.click(appPromoBanner.closeButton());
            await bro.yaWaitForHidden(appPromoBanner(), 'Промо-баннер отобразился после нажатия на кнопку закрытия');
        }

        await bro.yaAssertView(this.testpalmId, root.content(), { ignoreElements: ignoredElements });

        if (this.browserId in ['chrome-desktop-win', 'chrome-desktop-mac']) {
            await bro.yaClickAndAssertNewTabUrl(stub.actionButton(), {
                linkShouldContain: 'https://disk.yandex.ru/download'
            }, 'Не открылась страница скачивания диска');
        }
    });
});
