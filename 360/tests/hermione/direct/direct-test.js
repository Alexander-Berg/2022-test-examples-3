const client = require('../page-objects/client');
const clientContentListing = require('../page-objects/client-content-listing');
const clientCommon = require('../page-objects/client-common');
const { NAVIGATION } = require('../config').consts;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const listing = require('../page-objects/client-content-listing.js');
const { assert } = require('chai');

describe('Реклама -> ', () => {
    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-1679: [Директ] Кнопка "Отключить рекламу"', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-267');

        await bro.yaWaitForVisible(clientCommon.desktop.removeAdsButton());
        await bro.click(clientCommon.desktop.removeAdsButton());

        const url = await bro.getUrl();
        assert.equal(url, 'https://mail360.yandex.ru/premium-plans?from=disk_direct');
    });

    hermione.only.in(clientDesktopBrowsersList);
    hermione.only.notIn('firefox-desktop', 'Таймаутит - https://st.yandex-team.ru/CHEMODAN-79460');
    it('diskclient-1673: [Директ] Отображение рекламы бесплатным пользователям', async function() {
        const bro = this.browser;

        const domains = ['com', 'com.ge', 'com.tr', 'by'];
        const sectionsWithDirect = ['disk', 'shared', 'published', 'archive'];
        const sectionsWithoutDirect = ['recent', 'albums', 'photo', 'journal'];

        await bro.yaClientLoginFast('yndx-ufo-test-549');

        for (const section of sectionsWithDirect) {
            await bro.url(NAVIGATION[section].url);
            await bro.yaWaitForVisible(
                clientCommon.common.directFrame(),
                `Должна быть реклама в разделе: ${section}`
            );
        }

        for (const section of sectionsWithoutDirect) {
            await bro.url(NAVIGATION[section].url);
            await bro.yaWaitForHidden(client.stub(), `Для кейса не должно быть заглушки в разделе: ${section}`);
            await bro.yaWaitForHidden(
                clientCommon.common.directFrame(),
                `Не должно быть рекламы в разделе: ${section}`
            );
        }

        for (const domain of domains) {
            await bro.yaClientLoginFast('yndx-ufo-test-549', domain);
            await bro.yaWaitForVisible(
                clientCommon.common.directFrame(),
                `Должна быть реклама для домена: ${domain}`
            );
        }
    });

    hermione.only.in(clientDesktopBrowsersList);
    hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: 'diskclient-6316' });
    it('diskclient-6316: [Директ] Отображение рекламы в Корзине', async function() {
        const bro = this.browser;

        const file = await bro.yaUploadFiles('test-file.txt', { uniq: true });

        await bro.yaDeleteResource(file);
        await bro.url(NAVIGATION.trash.url);
        await bro.yaWaitForVisible(listing.common.listing.item());
        await bro.yaWaitForVisible(
            clientCommon.common.directFrame(),
            'Должна быть реклама в Корзине, если в ней есть файлы'
        );

        await bro.yaCleanTrash();
        await bro.yaWaitForHidden(listing.common.listing.item());
        await bro.yaWaitForHidden(
            clientCommon.common.directFrame(),
            'Не должно быть рекламы в пустой Корзине'
        );
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-7081: Содержимое рекламы должно загрузиться', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-634');

        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForVisible(clientCommon.common.directFrame(), 'Должна быть реклама');

        const iframe = await bro.element(clientCommon.common.directFrame());

        await bro.frame(iframe);
        // Тут мы проверяем что реклама загрузилась из директа и отобразилась.
        // Фактически реклама находится внутри shadow-dom (closed)
        await bro.yaWaitForVisible(clientCommon.common.directInner(), 'Должно быть содержимое рекламы');
        await bro.frameParent();
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-7086, 7087, 7088, 7089: Рекламная полоска отсутствует в заглушках разделов', async function() {
        const bro = this.browser;

        const sectionsWithDirectTop = [
            'photo',
            'albums',
            'downloads',
            'scans'
        ];

        await bro.yaClientLoginFast('yndx-ufo-test-634');

        for (const section of sectionsWithDirectTop) {
            await bro.url(NAVIGATION[section].url);
            await bro.yaWaitForHidden(
                clientCommon.common.directFrame(),
                `Не должно быть рекламы в разделе: ${section}`
            );
        }
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-7090: Рекламная полоска отсутствует в заглушке пустой страницы поиска по файлам', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-549');
        await bro.url('/client/search?querySearch=qwqwqwqw&scopeSearch=%2Fdisk');

        await bro.yaWaitForVisible(
            clientContentListing.common.listing.searchStub(),
            'Должна быть заглушка пустого поиска по файлам'
        );
        await bro.yaWaitForHidden(clientCommon.common.directFrame(), 'Не должно быть рекламного блока');
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-7091: Рекламная полоска отсутствует в заглушке пустой страницы поиска по Корзине', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-549');

        await bro.url('/client/search?querySearch=qwqwqwqw&scopeSearch=%2Ftrash');

        await bro.yaWaitForVisible(
            clientContentListing.common.listing.searchStub(),
            'Должна быть заглушка пустого поиска по корзине'
        );
        await bro.yaWaitForHidden(clientCommon.common.directFrame(), 'Не должно быть рекламного блока');
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-7079: Отсутствие рекламной полоски для платного пользователя', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-oligarh');
        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForVisible(
            clientContentListing.common.listing.inner(),
            'Должен отобразиться список файлов'
        );
        await bro.yaWaitForHidden(clientCommon.common.directFrame(), 'Не должно быть рекламы для платника');
    });

    hermione.only.in('chrome-phone');
    it('diskclient-7085: Отсутствие рекламной плоски в таче', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-634');

        await bro.url(NAVIGATION.disk.url);
        await bro.yaWaitForHidden(clientCommon.common.directFrame(), 'Не должно быть рекламы');
    });

    // <-- Рекламная полоска в Диске https://st.yandex-team.ru/CHEMODAN-81327
});
