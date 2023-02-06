const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('../config/index').login);
const consts = require('../config/index').folder;
const albums = require('../config/index').album;
const PageObjects = require('../page-objects/public');
const { videoPlayer } = require('@ps-int/ufo-hermione/page-objects/video-player').common;
const { assert } = require('chai');
const { wowGridGroup, wowGridItem, wowGridItemByTitle, listingItem } = require('../helpers/selectors');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

/**
 * Возвращает список групп фоток с именами и координатами
 *
 * @param {Browser} bro
 * @returns {Promise<Array<Array<{ title: string, left: number, top: number, width: number, height: number }>>>}
 */
const getItemsRects = async(bro) => {
    return (await bro.execute((groupSelector, itemSelector) => {
        const groups = document.querySelectorAll(groupSelector);
        const groupArray = [];
        groups.forEach((group) => {
            const elements = group.querySelectorAll(itemSelector);
            groupArray.push(Array.from(elements).map((element) => {
                const rect = element.getBoundingClientRect();
                return {
                    title: element.getAttribute('title'),
                    left: rect.left,
                    top: rect.top + document.scrollingElement.scrollTop,
                    width: rect.width,
                    height: rect.height
                };
            }));
        });
        return groupArray;
    }, PageObjects.wowGrid.group(), PageObjects.wowGrid.item()));
};

/**
 * @param {Browser} bro
 * @param {boolean} shouldBeAvailable
 * @returns {Promise<void>}
 */
const checkWowViewIsAvailable = async(bro, shouldBeAvailable) => {
    const isMobile = await bro.yaIsMobile();
    const errorMessage = `В подпапке с фотками ${shouldBeAvailable ? 'не' : ''}доступен тип "Умная сетка"`;

    if (isMobile) {
        await bro.yaOpenMore();

        const wowItem = await bro.$(PageObjects.fileMenuPane.wow());

        assert(await wowItem.isDisplayed() === shouldBeAvailable, errorMessage);

        await bro.yaClick(PageObjects.modalCell());
        await bro.yaWaitForHidden(PageObjects.fileMenuPane());
    } else {
        await bro.yaClick(PageObjects.desktopListingTypeButton());
        await bro.yaWaitForVisible(PageObjects.desktopListingTypeMenu());

        const wowItem = await bro.$(PageObjects.desktopListingTypeMenu.wow());

        assert(await wowItem.isDisplayed() === shouldBeAvailable, errorMessage);
    }
};

describe('Паблик Папки -> ', () => {
    it('diskpublic-540: diskpublic-1820: Смоук: Просмотр Папки в неавторизованном состоянии', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaWaitForVisible(PageObjects.listing());
        const folder = await bro.$(PageObjects.content.folderName());
        const folderName = await folder.getText();
        assert.equal(folderName, consts.PUBLIC_FOLDER_FILE_NAME);
        await bro.yaWaitForVisibleToolbarButtons('Папки');
    });

    it('diskpublic-1675: diskpublic-226: Сохранение Папки на Диск из неавторизованного состояния', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaSaveToDiskWithAuthorization(getUser('test'));
    });

    it('diskpublic-2288: diskpublic-227: Отображение листинга файлов на паблике папки', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaWaitForVisible(listingItem(3));

        const item = await bro.$(listingItem(3));
        assert.equal(
            (await item.getText()),
            consts.PUBLIC_FOLDER_ITEM_3
        );
        return bro;
    });

    it('diskpublic-2289: diskpublic-2290: Переход в подпапку и проверка отображения файлов в листинге подпапки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const selector = await PageObjects.listing.subfolder();

        await bro.url(consts.PUBLIC_FOLDER_URL);
        const subfolder = await bro.$(selector);

        if (isMobile) {
            await subfolder.click();
        } else {
            await subfolder.doubleClick();
        }

        await bro.yaWaitForHidden(PageObjects.listing.subfolder());
        await bro.yaWaitForVisible(listingItem(4));

        const item = await bro.$(listingItem(4));
        assert.equal(
            (await item.getText()),
            consts.PUBLIC_SUBFOLDER_ITEM_4
        );
        return bro;
    });

    it('diskpublic-2292: diskpublic-2293: Открытие слайдера с файлом из Папки по ссылке', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2293' : 'diskpublic-2292';

        await bro.url(consts.PUBLIC_FOLDER_SLIDER_URL);
        await bro.yaWaitForVisible(PageObjects.slider(), 'Слайдер не открылся');
        await bro.yaAssertView(this.testpalmId, 'body', { ignoreElements: [] });
        return bro;
    });

    it('diskpublic-601: diskpublic-228: Открытие слайдера изображения из Папки', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FOLDER_URL);

        const item = await bro.$(listingItem(11));
        await item.scrollIntoView();

        if (await bro.yaIsMobile()) {
            await item.click();
        } else {
            await item.doubleClick();
        }

        await bro.yaWaitForVisible(PageObjects.slider(), 'Слайдер не открылся');
        return bro;
    });

    it('diskpublic-2295: diskpublic-230: Открытие документа в DV из Папки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(consts.PUBLIC_FOLDER_URL);

        const item = await bro.$(listingItem(10));
        await item.scrollIntoView();

        await bro.yaOpenFileFromListing(
            listingItem(10),
            isMobile ? PageObjects.slider.activeItem.resourcePreview() : null,
            {
                shouldBeOpenedInDocs: !isMobile
            }
        );
        return bro;
    });

    hermione.skip.in('chrome-desktop', 'Актуально только для тача');
    it('diskpublic-2262: Открытие документа в DV по "Открыть" в слайдере из Папки', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FOLDER_URL);

        const item = await bro.$(listingItem(10));
        await item.scrollIntoView();

        await bro.yaOpenFileFromListing(listingItem(10), PageObjects.slider.activeItem.resourcePreview.openButton());
        return bro;
    });

    it('diskpublic-2296: diskpublic-2297:Открытие архива в DV из Папки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(consts.PUBLIC_FOLDER_URL);

        const item = await bro.$(listingItem(9));
        await item.scrollIntoView();

        await bro.yaOpenFileFromListing(
            listingItem(9),
            isMobile ? PageObjects.slider.activeItem.resourcePreview.openButton() : null
        );
        return bro;
    });

    hermione.skip.in('chrome-desktop', 'Актуально только для тача');
    it('diskpublic-2297: Открытие архива в DV по "Открыть" в слайдере из Папки', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FOLDER_URL);

        const item = await bro.$(listingItem(9));
        await item.scrollIntoView();

        await bro.yaOpenFileFromListing(listingItem(9), PageObjects.slider.activeItem.resourcePreview.openButton());
        return bro;
    });

    it('diskpublic-2298: diskpublic-2299: Открытие книги в DV из Папки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(consts.PUBLIC_FOLDER_WITH_MANY_SUBFOLDERS_URL);
        await bro.yaOpenFileFromListing(
            listingItem(3),
            isMobile ? PageObjects.slider.activeItem.resourcePreview() : null,
            {
                shouldBeOpenedInDocs: !isMobile
            }
        );
        return bro;
    });

    hermione.skip.in('chrome-desktop', 'Актуально только для тача');
    it('diskpublic-2300: Открытие книги в DV по "Открыть" в слайдере из Папки', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FOLDER_WITH_MANY_SUBFOLDERS_URL);
        await bro.yaOpenFileFromListing(listingItem(3), PageObjects.slider.activeItem.resourcePreview.openButton());
        return bro;
    });

    it('diskpublic-547: diskpublic-268: Смоук: Подгрузка порций в большой Папке', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FOLDER_WITH_MANY_FILES_URL);
        await bro.yaWaitForHidden(listingItem(41), 'Отображается больше 40 файлов в папке');

        const item = await bro.$(listingItem(40));
        await item.scrollIntoView();

        await bro.yaWaitForVisible(PageObjects.listing.spinner());
        await bro.yaWaitForVisible(listingItem(41));
        await bro.yaWaitForHidden(
            listingItem(81),
            'Отображается больше 80 файлов в папке после подгрузки новой порции'
        );

        const newItem = await bro.$(listingItem(80));
        await newItem.scrollIntoView();

        await bro.yaWaitForVisible(PageObjects.listing.spinner());
        await bro.yaWaitForVisible(listingItem(81));
        return bro;
    });

    hermione.only.in('', 'Мигает CHEMODAN-73618');
    it('diskpublic-541: diskpublic-236: Переходы в подпапки на разные уровни и назад', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const selector = await PageObjects.listing.subfolder();

        await bro.url(consts.PUBLIC_FOLDER_WITH_MANY_SUBFOLDERS_URL);

        const item = await bro.$(selector);

        if (isMobile) {
            await item.click();
        } else {
            await item.doubleClick();
        }

        await bro.yaWaitForVisible(PageObjects.listing.spinner());
        await bro.yaWaitForVisible(listingItem(1));

        if (isMobile) {
            await item.click();
        } else {
            await item.doubleClick();
        }

        await bro.yaWaitForHidden(listingItem(1));
        await bro.yaClick(PageObjects.content.header.back());
        await bro.yaWaitForVisible(listingItem(1));
        return bro;
    });

    it('diskpublic-1642: diskpublic-1796: Смоук: Скачивание публичной папки целиком', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(consts.PUBLIC_FOLDER_URL);

        const buttonSelector = isMobile ?
            PageObjects.toolbar.downloadButton() :
            PageObjects.desktopToolbar.downloadButton();

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.yaClick(buttonSelector);
        });

        assert(/downloader\.disk\.yandex\.ru\/zip\//.test(url), 'Некорректный url для скачивания');

        const button = await bro.$(buttonSelector);
        const isDisabled = await button.getAttribute('disabled');
        assert.equal(isDisabled, 'true', 'Кнопка "Скачать всё" не задизейблилась');
    });

    hermione.skip.in('chrome-phone', 'https://st.yandex-team.ru/CHEMODAN-77646');
    it('diskpublic-598: diskpublic-2301: Попытка Сохранения Папки на Диск под переполненным юзером', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaClick((isMobile ? PageObjects.toolbar.saveButton() : PageObjects.desktopToolbar.saveButton()));
        await bro.login(getUser('fullOfUser'));
        await bro.yaWaitForVisible(PageObjects.toolbar.snackbarError(), 'Сообщение об ошибке не отобразилось');
    });

    it('AssertView: Проверка отображения содержимого папки', async function() {
        const bro = this.browser;
        await bro.url(consts.PUBLIC_FOLDER_URL);
        await bro.yaWaitForVisible(PageObjects.listing());
        await bro.pause(300);
        await bro.yaAssertView('folder-content', PageObjects.content());
    });

    /**
     * @param {('photos'|'weird')} folder
     * @param {('tile'|'icons'|'list')} listingType
     * @returns {Promise<void>}
     */
    async function testClampedText(folder, listingType) {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2706' : 'diskpublic-2707';

        await bro.url(consts[`PUBLIC_FOLDER_CLAMPED_TEXT_${folder.toUpperCase()}_URL`]);
        await bro.yaChangeListingType(listingType, isMobile);
        if (!isMobile) {
            await bro.yaResetPointerPosition();
        }
        const listingTypeCapitalized = listingType[0].toUpperCase() + listingType.slice(1);
        await bro.yaWaitForVisible(PageObjects.listing[`listingItem${listingTypeCapitalized}`]());
        await bro.pause(500); // подгрузка превью
        await bro.assertView(`${this.testpalmId}-${folder}-${listingType}`, PageObjects.listing.listingItems());
    }

    hermione.only.in('chrome-desktop');
    it('diskpublic-2706, 2707: Перенос и заточивание наименования ресурса (крупная плитка)', async function() {
        await testClampedText.call(this, 'photos', 'tile');
        await testClampedText.call(this, 'weird', 'tile');
    });

    it('diskpublic-2706, 2707: Перенос и заточивание наименования ресурса (мелкая плитка)', async function() {
        await testClampedText.call(this, 'photos', 'icons');
        await testClampedText.call(this, 'weird', 'icons');
    });

    it('diskpublic-2706, 2707: Перенос и заточивание наименования ресурса (список)', async function() {
        await testClampedText.call(this, 'photos', 'list');
        await testClampedText.call(this, 'weird', 'list');
    });

    hermione.skip.in(['chrome-desktop', 'chrome-phone'], 'https://st.yandex-team.ru/CHEMODAN-84334');
    it('diskpublic-2680: Автовоспроизведение видео', async function() {
        const bro = this.browser;

        await bro.url(consts.PUBLIC_FOLDER_WITH_VIDEO_URL);

        const gridItem = await bro.$(wowGridItem(6));

        await gridItem.doubleClick();
        await bro.yaAssertVideoIsPlaying();

        await bro.keys('Left arrow');

        const preview = await bro.$(PageObjects.slider.activeItem.previewInSlider());
        await preview.waitForDisplayed();

        await bro.keys('Right arrow');

        await bro.waitForVisible(videoPlayer.preview());
    });

    it('diskpublic-2886, diskpublic-2884: Смена типа листинга в пустой публичной папки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2886' : 'diskpublic-2884';

        await bro.url(consts.PUBLIC_FOLDER_EMPTY_URL);

        if (isMobile) {
            await bro.yaOpenMore();
            await bro.yaWaitForHidden(PageObjects.fileMenuPane.wow());
            await bro.yaClick(PageObjects.fileMenuPane.list());
        } else {
            await bro.yaWaitForVisible(PageObjects.desktopListingTypeButton());
            await bro.yaClick(PageObjects.desktopListingTypeButton());
            await bro.yaWaitForHidden(PageObjects.desktopListingTypeMenu.wow());
            await bro.yaClick(PageObjects.desktopListingTypeMenu.list());
        }
        await bro.yaWaitForHidden(PageObjects.listing());
        await bro.yaWaitForVisible(PageObjects.content.empty());
    });

    it('diskpublic-2855, diskpublic-2854: Открытие слайдера в публичной папке в режиме вау-сетки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2855' : 'diskpublic-2854';

        await bro.url(consts.PUBLIC_FOLDER_WITH_10_PHOTOS_URL);
        await bro.yaClick(wowGridItem(1));
        await bro.yaWaitForVisible(PageObjects.slider(), 'Слайдер не открылся');
    });

    it('diskpublic-2841, diskpublic-2840: Изменение типа сетки с вау на список и обратно', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2841' : 'diskpublic-2840';

        await bro.url(consts.PUBLIC_FOLDER_WITH_10_PHOTOS_URL);
        await bro.yaChangeListingType('icons', isMobile);
        await bro.yaWaitForVisible(PageObjects.listing());
        await bro.yaChangeListingType('wow', isMobile);
        await bro.yaWaitForVisible(PageObjects.wowGrid());
    });

    it('diskpublic-2878, diskpublic-2873: Сохранение типа листинга для публичного альбома и публичной папки с фото и видео для неавторизованного юзера', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2878' : 'diskpublic-2873';

        // заходим в публичную папку с фото - там вау-сетка
        await bro.url(consts.PUBLIC_FOLDER_WITH_10_PHOTOS_URL);
        await bro.yaWaitForVisible(PageObjects.wowGrid());

        // меняем тип листинга на плитку
        await bro.yaChangeListingType('icons', isMobile);
        await bro.yaWaitForVisible(PageObjects.listing());

        // снова заходим в папку с фото - она отображается плиткой
        await bro.url(consts.PUBLIC_FOLDER_WITH_10_PHOTOS_URL);
        await bro.yaWaitForVisible(PageObjects.listing());

        // заходим в публичный альбом на паблике - он тоже отображается иконками,
        // т.к. настройка одна - для альбома и для папки
        await bro.url(albums.PUBLIC_ALBUM_URL);
        await bro.yaWaitForVisible(PageObjects.listing());
    });

    it('diskpublic-2879, diskpublic-2875: Сохранение типа листинга для публичной папки с документами для неавторизованного юзера', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2879' : 'diskpublic-2875';

        // заходим в публичную папку с документами - там список
        await bro.url(consts.PUBLIC_FOLDER_DOCUMENTS_URL);
        await bro.yaWaitForVisible(PageObjects.listing());
        await bro.yaWaitForVisible(PageObjects.listing.listingItemList());

        // меняем тип листинга на плитку
        await bro.yaChangeListingType('icons', isMobile);
        await bro.yaWaitForVisible(PageObjects.listing.listingItemIcons());

        // снова заходим в папку с фото - она отображается плиткой
        await bro.url(consts.PUBLIC_FOLDER_DOCUMENTS_URL);
        await bro.yaWaitForVisible(PageObjects.listing.listingItemIcons());

        // заходим в публичный альбом на паблике - он отображается вау-сеткой,
        // т.к. настройка для альбома и папки не с фото - разные
        await bro.url(albums.PUBLIC_ALBUM_URL);
        await bro.yaWaitForVisible(PageObjects.wowGrid());
    });

    it('diskpublic-2862, diskpublic-2864: Сохранение типа листинга для публичного альбома и публичной папки с фото и видео (Плитка)', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2862' : 'diskpublic-2864';

        // заходим в публичную папку с фото, логинимся - там последняя настройка юзера
        await bro.url(consts.PUBLIC_FOLDER_WITH_10_PHOTOS_URL);
        await bro.yaWaitForVisible(PageObjects.loginButton());
        await bro.yaClick(PageObjects.loginButton());
        await bro.login(getUser('yndx-ufo-test-762'));
        await bro.yaChangeListingType('wow', isMobile, true);
        await bro.yaWaitForVisible(PageObjects.wowGrid());

        // меняем тип листинга на плитку
        await bro.yaChangeListingType('icons', isMobile);
        await bro.yaWaitForVisible(PageObjects.listing.listingItemIcons());

        // снова заходим в публичную папку с фото - там плитка
        await bro.url(consts.PUBLIC_FOLDER_WITH_10_PHOTOS_URL);
        await bro.yaWaitForVisible(PageObjects.listing.listingItemIcons());

        // заходим в публичную папку с документами - там список
        // т.к. настройка для папки не с фото - другая
        await bro.url(consts.PUBLIC_FOLDER_DOCUMENTS_URL);
        await bro.yaWaitForVisible(PageObjects.listing());
        await bro.yaWaitForVisible(PageObjects.listing.listingItemList());
    });

    it('diskpublic-2843, diskpublic-2842: Отсутствие режима вау-сетки для папки с мелкими фото в первой порции', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2843' : 'diskpublic-2842';

        await bro.url(consts.PUBLIC_FOLDER_WITH_SMALL_PHOTOS_URL);
        await bro.yaWaitForHidden(PageObjects.wowGrid());
        await bro.yaWaitForVisible(PageObjects.listing());

        if (isMobile) {
            await bro.yaOpenMore();
            await bro.yaWaitForHidden(PageObjects.fileMenuPane.wow());
        } else {
            await bro.yaWaitForVisible(PageObjects.desktopListingTypeButton());
            await bro.yaClick(PageObjects.desktopListingTypeButton());
            await bro.yaWaitForHidden(PageObjects.desktopListingTypeMenu.wow());
        }
    });

    it('diskpublic-2889, diskpublic-2888: Отображение фото со сторонами меньше 200px в режима вау-сетки для папки с фото в первой порции (меньше 20 в первой порции)', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2889' : 'diskpublic-2888';

        await bro.url(consts.PUBLIC_FOLDER_WITH_1_SMALL_PHOTO_URL);
        await bro.yaScrollToBeginGrid(isMobile);
        await bro.yaAssertView(this.testpalmId, PageObjects.wowGrid());
    });

    it('diskpublic-2844, diskpublic-2845: Отображение вау-сетки для папки с 81 фото и видео', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2845' : 'diskpublic-2844';

        const LAST_PHOTO_TITLE = '2010-03-26 05-49-31.JPG';

        await bro.url(consts.PUBLIC_FOLDER_WITH_81_PHOTOS_URL);
        await bro.yaWaitForVisible(PageObjects.wowGrid());

        // изначально отрисовано 2 группы по 20+ фоток
        const itemsRectsInitial = await getItemsRects(bro);
        assert.equal(itemsRectsInitial.length, 2, 'Число групп при первоначальной загрузке');
        const allGroups = itemsRectsInitial.slice();

        // после подскролла ко 2-й группе должно отрисоваться 3 группы по 20+ фоток
        const wowGridGroup2 = await bro.$(wowGridGroup(2));
        await wowGridGroup2.scrollIntoView();
        const itemsRectsAfterFirstScroll = await getItemsRects(bro);
        assert.equal(itemsRectsAfterFirstScroll.length, 3, 'Число групп после первого скролла');
        // первые 2 группы не должны были перестроиться
        assert.deepEqual(allGroups, itemsRectsAfterFirstScroll.slice(0, 2), 'Содержимое групп после 1-го скролла');
        const thirdGroup = itemsRectsAfterFirstScroll[2];
        allGroups.push(thirdGroup);

        const thirdGroupMiddlePhotoTitle = thirdGroup[Math.round(thirdGroup.length / 2)].title;

        // после подскролла к середине 3-й группы должны отрисоваться 2-я, 3я и 4-я (последняя) группы
        const gridItem = await bro.$(wowGridItemByTitle(thirdGroupMiddlePhotoTitle));
        await gridItem.scrollIntoView();
        await bro.yaWaitForVisible(wowGridItemByTitle(LAST_PHOTO_TITLE));

        const itemsRectsAfterSecondScroll = await getItemsRects(bro);
        assert.equal(itemsRectsAfterSecondScroll.length, 3, 'Число групп после второго скролла');
        // 2-я и 3-я группы не должны перестроиться
        assert.deepEqual(
            allGroups.slice(1, 3),
            itemsRectsAfterSecondScroll.slice(0, 2),
            'Содержимое групп после 2-го скролла'
        );
        allGroups.push(itemsRectsAfterSecondScroll[2]);

        const newGridItem = await bro.$(wowGridItemByTitle(LAST_PHOTO_TITLE));
        await newGridItem.scrollIntoView();
        const itemsRectsAfterLastScroll = await getItemsRects(bro);
        assert.equal(itemsRectsAfterLastScroll.length, 2, 'Число групп после третьего скролла');
        // 3-я и 4-я группы не должны перестроиться
        assert.deepEqual(allGroups.slice(2, 4), itemsRectsAfterLastScroll, 'Содержимое групп после 3-го скролла');

        const totalItemsCount = allGroups.reduce((accu, items) => accu + items.length, 0);
        assert.equal(totalItemsCount, 81, 'Общее число фоток в папке');

        // дополнительно проверим скриншотом, что последняя фотка красиво встала в сетку
        await bro.yaAssertView(this.testpalmId, PageObjects.wowGrid());
    });

    it('diskpublic-2846, diskpublic-2847: Отображение вау-сетки для папки с "лже" фото или видео', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2847' : 'diskpublic-2846';

        await bro.url(consts.PUBLIC_FOLDER_WITH_40_PHOTOS_AND_2_FILES_URL + '&forceWow=1');
        await bro.yaWaitForVisible(PageObjects.wowGrid());
        // перед тем как заснять вау-сетку - отскроллим страницу в самый конец (там будут два файла в вау-сетке)
        await bro.yaScrollAndGetItems(PageObjects.wowGrid.item(), PageObjects.wowGrid.spinner());
        await bro.yaAssertView(this.testpalmId, PageObjects.wowGrid());
    });

    it('diskpublic-2893, diskpublic-2894: Отображение вау-сетки внутри подпапки с фото и видео после перехода из корня', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2894' : 'diskpublic-2893';

        await bro.url(consts.PUBLIC_FOLDER_WITH_PHOTO_SUBFOLDERS);
        await bro.yaWaitForVisible(PageObjects.listing());
        await bro.yaWaitForVisible(PageObjects.listing.listingItemList());

        await checkWowViewIsAvailable(bro, false);

        const photoSubfolderSelector = PageObjects.listingItemXpath().replace(/:titleText/g, 'PhotoSubfolder');
        const photoSubfolder = await bro.$(photoSubfolderSelector);

        if (isMobile) {
            await photoSubfolder.click();
        } else {
            await photoSubfolder.doubleClick();
        }

        await bro.yaWaitForHidden(PageObjects.listing());
        await bro.yaWaitForVisible(PageObjects.wowGrid());
        await bro.yaWaitForVisible(PageObjects.wowGrid.item());

        await checkWowViewIsAvailable(bro, true);

        await bro.yaClick(PageObjects.content.header.backButton());
        await bro.yaWaitForHidden(PageObjects.wowGrid());
        await bro.yaWaitForVisible(PageObjects.listing());
    });

    it('diskpublic-2895, diskpublic-2896: Отображение вау-сетки внутри подпапки с фото и видео при переходе по прямой ссылке', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2896' : 'diskpublic-2895';

        await bro.url(consts.PUBLIC_FOLDER_WITH_PHOTO_SUBFOLDERS + '/PhotoSubfolder');
        await bro.yaWaitForVisible(PageObjects.wowGrid());
        await bro.yaWaitForVisible(PageObjects.wowGrid.item());

        await checkWowViewIsAvailable(bro, true);

        await bro.yaClick(PageObjects.content.header.backButton());
        await bro.yaWaitForHidden(PageObjects.wowGrid());
        await bro.yaWaitForVisible(PageObjects.listing());

        await checkWowViewIsAvailable(bro, false);
    });

    it('diskpublic-2891, diskpublic-2892: Подгрузка порций в режиме вау-сетки для подпапки с фото и видео', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2892' : 'diskpublic-2891';

        await bro.url(consts.PUBLIC_FOLDER_WITH_PHOTO_SUBFOLDERS + '/PhotoSubfolder');
        await bro.yaWaitForVisible(PageObjects.wowGrid());

        const loadedItems = await bro.yaScrollAndGetItems(PageObjects.wowGrid.item(), PageObjects.wowGrid.spinner());
        await assert.equal(84, loadedItems.length, 'Количество фоток отличается от ожидаемого');
    });
});
