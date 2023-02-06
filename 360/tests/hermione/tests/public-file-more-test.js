const consts = require('../config/index').consts;
const folders = require('../config/index').folder;
const albums = require('../config/index').album;
const PageObjects = require('../page-objects/public');
const assert = require('chai').assert;

const TEST_ID_ALBUM = '?test-id=239414';

describe('Выпадающее меню ЕЩЁ -> ', () => {
    it('diskpublic-2085: diskpublic-2279: Кнопка "Открыть" для книги', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FB2_FILE_URL);
        await bro.yaWaitForVisibleDocPreview(consts.PUBLIC_FB2_FILE_TYPE);
        await bro.yaOpenMore();
        await bro.yaClickAndAssertNewTabUrl(PageObjects.fileMenuOpenDV(), { linkShouldContain: 'docviewer' });
    });

    it('diskpublic-2280: diskpublic-2281: Отсутствие кнопки "Открыть" для HTML файла', async function() {
        await this.browser.url(consts.PUBLIC_HTML_FILE_URL);
        await this.browser.yaWaitForVisibleIcon(consts.PUBLIC_HTML_FILE_TYPE);
        await this.browser.yaOpenMore();
        await this.browser.yaWaitForHidden(
            PageObjects.slider(),
            `Отображается кнопка "Открыть" для файла ${consts.PUBLIC_HTML_FILE_TYPE}`
        );
    });

    it('diskpublic-2283: diskpublic-2282: Кнопка "Информация о файле" (русский язык)', async function() {
        await this.browser.url(consts.PUBLIC_HTML_FILE_URL);
        await this.browser.yaOpenMore();
        await this.browser.yaAssertFileInfo(
            consts.PUBLIC_HTML_FILE_OWNER,
            consts.PUBLIC_HTML_FILE_SIZE,
            consts.PUBLIC_HTML_FILE_MODIFIED,
            consts.PUBLIC_HTML_FILE_VIRUSES
        );
    });

    it('diskpublic-2284: diskpublic-2285: Кнопка "Информация о файле" (английский язык)', async function() {
        await this.browser.url(consts.PUBLIC_HTML_FILE_URL + '?lang=en');
        await this.browser.yaOpenMore();
        await this.browser.yaAssertFileInfo(
            consts.PUBLIC_HTML_FILE_OWNER_EN,
            consts.PUBLIC_HTML_FILE_SIZE_EN,
            consts.PUBLIC_HTML_FILE_MODIFIED_EN,
            consts.PUBLIC_HTML_FILE_VIRUSES_EN
        );
    });

    it('diskpublic-2287: diskpublic-2286: Кнопка "Информация о папке" (английский язык)', async function() {
        await this.browser.url(folders.PUBLIC_FOLDER_URL + '?lang=en');
        await this.browser.yaOpenMore();
        await this.browser.yaClick(PageObjects.fileMenuFileInfo());
        await this.browser.yaWaitForVisible(PageObjects.infoBlock());

        const ownerElement = await this.browser.$(PageObjects.infoBlock.owner());
        const owner = await ownerElement.getText();
        assert.equal(owner, consts.PUBLIC_HTML_FILE_OWNER_EN);

        const sizeElement = await this.browser.$(PageObjects.infoBlock.size());
        const size = await sizeElement.getText();
        assert.equal(size, folders.PUBLIC_FOLDER_SIZE_EN);

        const modifiedElement = await this.browser.$(PageObjects.infoBlock.modified());
        const modified = await modifiedElement.getText();
        assert.equal(modified, folders.PUBLIC_FOLDER_MODIFIED_EN);

        const fileCountElement = await this.browser.$(PageObjects.infoBlock.fileCount());
        const fileCount = await fileCountElement.getText();
        const splitCount = fileCount.split(':');
        assert.equal(splitCount[0], folders.PUBLIC_FOLDER_FILE_COUNT_TITLE_EN);
        // число файлов не очень стабильно - проверяем что нам там просто приехало какое-то корректное число
        assert(parseInt(splitCount[1], 10) > -1, 'Отображается некорректное число файлов в папке');

        const viewsElement = await this.browser.$(PageObjects.infoBlock.views());
        const views = await viewsElement.getText();
        const splitViews = views.split(':');
        assert.equal(splitViews[0], folders.PUBLIC_FOLDER_VIEWS_TITLE_EN);
        assert(parseInt(splitViews[1], 10) > -1, 'Отображается некорректное число просмотров');

        const downloadsElement = await this.browser.$(PageObjects.infoBlock.downloads());
        const downloads = await downloadsElement.getText();
        const splitDownloads = downloads.split(':');
        assert.equal(splitDownloads[0], folders.PUBLIC_FOLDER_DOWNLOADS_TITLE_EN);
        assert(parseInt(splitDownloads[1], 10) > -1, 'Отображается некорректное число скачиваний');
    });

    it('diskpublic-1753: diskpublic-257: AssertView: Проверка отображения "Информации о папке" (русский язык)', async function() {
        await this.browser.url(folders.PUBLIC_FOLDER_URL);
        await this.browser.yaOpenMore();
        await this.browser.yaClick(PageObjects.fileMenuFileInfo());
        await this.browser.yaWaitForVisible(PageObjects.infoBlock());
        await this.browser.pause(300); //пауза нужна, чтобы всплывашка успела отобразиться полностью
        await this.browser.yaAssertView(
            'info-block',
            PageObjects.infoBlock(),
            {
                ignoreElements: [
                    PageObjects.infoBlock.views(),
                    PageObjects.infoBlock.downloads(),
                ]
            }
        );
    });

    it('AssertView: Проверка отображения "Информации о файле" (русский язык)', async function() {
        await this.browser.url(consts.PUBLIC_HTML_FILE_URL);
        await this.browser.yaOpenMore();
        await this.browser.yaClick(PageObjects.fileMenuFileInfo());
        await this.browser.yaWaitForVisible(PageObjects.infoBlock());
        await this.browser.pause(300); //пауза нужна, чтобы всплывашка успела отобразиться полностью
        await this.browser.yaAssertView(
            'info-block',
            PageObjects.infoBlock(),
            {
                ignoreElements: [
                    PageObjects.infoBlock.views(),
                    PageObjects.infoBlock.downloads(),
                ]
            }
        );
    });

    it('diskpublic-2812, diskpublic-2830: Кнопка "Информация об альбоме"', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2830' : 'diskpublic-2812';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);
        await bro.yaOpenMore();
        await bro.yaClick(PageObjects.fileMenuFileInfo());
        await bro.yaWaitForVisible(PageObjects.infoBlock());

        const ownerElement = await bro.$(PageObjects.infoBlock.owner());
        const owner = await ownerElement.getText();
        assert.equal(owner, albums.PUBLIC_ALBUM_OWNER);

        const modifiedElement = await bro.$(PageObjects.infoBlock.modified());
        const modified = await modifiedElement.getText();
        assert.equal(modified, albums.PUBLIC_ALBUM_MODIFIED);

        const viewsElement = await bro.$(PageObjects.infoBlock.views());
        const views = await viewsElement.getText();
        const n = views.split(':').pop();
        assert(parseInt(n, 10) > -1, 'Отображается некорректное число просмотров');
    });

    it('diskpublic-2813, diskpublic-2831: AssertView: Проверка отображения "Информации об альбоме"', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2831' : 'diskpublic-2813';

        await bro.url(albums.PUBLIC_ALBUM_URL + TEST_ID_ALBUM);
        await bro.yaOpenMore();
        await bro.yaClick(PageObjects.fileMenuFileInfo());
        await bro.yaWaitForVisible(PageObjects.infoBlock());
        await bro.pause(300); //пауза нужна, чтобы всплывашка успела отобразиться полностью
        await bro.yaAssertView(
            this.testpalmId,
            PageObjects.infoBlock(),
            {
                ignoreElements: [
                    PageObjects.infoBlock.views(),
                ]
            }
        );
    });
});
