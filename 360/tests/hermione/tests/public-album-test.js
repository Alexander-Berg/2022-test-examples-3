const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('../config/index').login);
const albums = require('../config/index').album;
const PageObjects = require('../page-objects/public');
const { assert } = require('chai');
const { wowGridItem, listingItem } = require('../helpers/selectors');
const { WAITING_AUTH_TIMEOUT } = require('../config').consts;

const TEST_ID_ALBUM = '?test-id=239414';

describe('Паблик Альбома -> ', () => {
    it('diskpublic-2814, diskpublic-2832: Просмотр Альбома в неавторизованном состоянии', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2832' : 'diskpublic-2814';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);
        await bro.yaWaitForVisible(PageObjects.wowGrid());

        const folder = await bro.$(PageObjects.content.folderName());
        const albumName = await folder.getText();
        assert.equal(albumName, albums.PUBLIC_ALBUM_NAME);

        await bro.yaWaitForVisibleToolbarButtons('Альбом');
    });

    it('diskpublic-2815, diskpublic-2833: Отображение листинга файлов публичного альбома', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2833' : 'diskpublic-2815';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);
        await bro.yaChangeListingType('icons', isMobile);

        await bro.yaWaitForVisible(listingItem(7));

        const item = await bro.$(listingItem(7));
        assert.equal((await item.getText()), albums.PUBLIC_ALBUM_ITEM_7);

        return bro;
    });

    it('diskpublic-2816, diskpublic-2823: Открытие слайдера изображения в публичном альбоме', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2823' : 'diskpublic-2816';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);
        await bro.yaChangeListingType('icons', isMobile);
        await bro.yaWaitForVisible(listingItem(7));

        const item = await bro.$(listingItem(7));

        await item.scrollIntoView();
        await item.doubleClick();

        await bro.yaWaitForVisible(PageObjects.slider(), 'Слайдер не открылся');
    });

    it('diskpublic-2856, diskpublic-2857: Открытие слайдера в публичном альбоме в режиме вау-сетки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2857' : 'diskpublic-2856';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);

        const item = await bro.$(wowGridItem(7));

        await item.scrollIntoView();
        await item.click();

        await bro.yaWaitForVisible(PageObjects.slider(), 'Слайдер не открылся');
    });

    it('diskpublic-2817, diskpublic-2824: Проверка отображения "Информации о файле"', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2824' : 'diskpublic-2817';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);

        const item = await bro.$(wowGridItem(7));

        await item.scrollIntoView();
        await item.click();

        await bro.yaWaitForVisible(PageObjects.slider(), 'Слайдер не открылся');

        await bro.yaClick(PageObjects.slider.sliderButtonMore());
        await bro.yaWaitForVisible(PageObjects.fileMenuFileInfo());
        await bro.yaClick(PageObjects.fileMenuFileInfo());
        await bro.yaWaitForVisible(PageObjects.infoBlock());
        await bro.pause(300); //пауза нужна, чтобы всплывашка успела отобразиться полностью
        await bro.yaAssertView(this.testpalmId, PageObjects.infoBlock());

        return bro;
    });

    it('diskpublic-2818, diskpublic-2825: Скачивание файла из слайдера публичного альбома', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2825' : 'diskpublic-2818';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);

        const item = await bro.$(wowGridItem(7));

        await item.scrollIntoView();
        await item.click();

        await bro.yaWaitForVisible(PageObjects.slider(), 'Слайдер не открылся');

        if (isMobile) {
            await bro.yaClick(PageObjects.slider.sliderButtonMore());
            await bro.yaWaitForVisible(PageObjects.fileMenuDownload());
        }

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.yaClick(isMobile ?
                PageObjects.fileMenuDownload() :
                PageObjects.slider.sliderButtonDownload()
            );
        });

        assert(
            /downloader\.disk\.yandex\.ru\/disk\/.+&filename=%D0%9C%D0%B8%D1%88%D0%BA%D0%B8.jpg/.test(url),
            'Некорректный url для скачивания'
        );

        return bro;
    });

    it('diskpublic-2819, diskpublic-2826: Скачивание публичного альбома целиком', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2826' : 'diskpublic-2819';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.yaClick(isMobile ?
                PageObjects.toolbar.downloadButton() :
                PageObjects.desktopToolbar.downloadButton()
            );
        });

        assert(
            /downloader\.disk\.yandex\.ru\/zip-album\/.+&filename=TestAlbum.zip/.test(url),
            'Некорректный url для скачивания'
        );
    });

    hermione.skip.in('chrome-phone', 'https://st.yandex-team.ru/CHEMODAN-77646');
    it('diskpublic-2820, diskpublic-2827: Попытка Сохранения публичного альбома на Диск под переполненным юзером', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2827' : 'diskpublic-2820';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);
        await bro.yaClick((isMobile ? PageObjects.toolbar.saveButton() : PageObjects.desktopToolbar.saveButton()));
        await bro.login(getUser('fullOfUser'));
        await bro.yaWaitForVisible(PageObjects.toolbar.snackbarError(), 'Сообщение об ошибке не отобразилось');
    });

    it('diskpublic-2820, diskpublic-2827: Сохранение Папки на Диск из неавторизованного состояния', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2827' : 'diskpublic-2820';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);
        await bro.yaSaveToDiskWithAuthorization(getUser('test'));
    });

    it('diskpublic-2822, diskpublic-2829: Сохранение файла из слайдера публичного альбома на Диск', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2829' : 'diskpublic-2822';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);
        await bro.yaWaitForVisible(PageObjects.loginButton());
        await bro.yaClick(PageObjects.loginButton());
        await bro.login(getUser('test'));
        await bro.yaWaitForVisible(PageObjects.wowGrid(), WAITING_AUTH_TIMEOUT);

        const item = await bro.$(wowGridItem(7));

        await item.scrollIntoView();
        await item.click();

        await bro.yaWaitForVisible(PageObjects.slider(), 'Слайдер не открылся');

        await bro.yaWaitForVisible(PageObjects.slider.sliderButtonSave());
        await bro.yaClick(PageObjects.slider.sliderButtonSave());
        await bro.yaWaitForVisible(
            isMobile ? PageObjects.toolbar.snackbarText() : PageObjects.snackbarText(),
            10000,
            'Сообщение "Сохранено в загрузки" не отобразилось');
        await bro.yaWaitForVisible(PageObjects.slider.openDiskButton(), 'Кнопка "Открыть Диск" не отобразилось');

        return bro;
    });

    it('diskpublic-2885, diskpublic-2887: Смена типа листинга в пустом публичном альбоме', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2887' : 'diskpublic-2885';

        await bro.url(albums.PUBLIC_ALBUM_EMPTY_URL + TEST_ID_ALBUM);

        if (isMobile) {
            await bro.yaOpenMore();
            await bro.yaClick(PageObjects.fileMenuPane.list());
        } else {
            await bro.yaWaitForVisible(PageObjects.desktopListingTypeButton());
            await bro.yaClick(PageObjects.desktopListingTypeButton());
            await bro.yaClick(PageObjects.desktopListingTypeMenu.list());
        }
        await bro.yaWaitForHidden(PageObjects.listing());
        await bro.yaWaitForVisible(PageObjects.content.empty());
    });
});
