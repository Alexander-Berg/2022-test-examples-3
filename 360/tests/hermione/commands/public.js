const PageObjects = require('../page-objects/public');
const assert = require('chai').assert;
const { WAITING_AUTH_TIMEOUT } = require('../config').consts;
const { wowGridItem } = require('../helpers/selectors');

const actions = {
    //общие имплементации для desktop и touch
    common: {
        // Открытие слайдера одиночной картинки
        async yaOpenSlider(typeOrName) {
            await this.yaClick(PageObjects.imagePreview());
            await this.yaWaitForVisible(PageObjects.slider(), `Слайдер картинки ${typeOrName} не открылся`);
        },

        async yaWaitForVisibleImagePreview(typeOrName) {
            await this.yaWaitForVisible(PageObjects.imagePreview(), `Превью картинки ${typeOrName} не отобразилось`);
        },

        async yaWaitForVisibleDocPreview(typeOrName) {
            await this.yaWaitForVisible(PageObjects.docPreview(), `Превью документа ${typeOrName} не отобразилось`);
        },

        async yaWaitForVisibleIcon(typeOrName, icon = typeOrName) {
            await this.yaWaitForVisible(`.file-icon_${icon}`, `Иконка файла ${typeOrName} не отобразилось`);
        },

        async yaAssertFileName(name, fileNameSelector = PageObjects.fileName()) {
            const element = await this.$(fileNameSelector);
            const fileName = await element.getText();
            assert.equal(fileName, name);
        },

        // Проверка информации о файле
        async yaAssertFileInfo(fileOwner, fileSize, fileModified, fileViruses) {
            await this.yaClick(PageObjects.fileMenuFileInfo());
            await this.yaWaitForVisible(PageObjects.infoBlock());

            const ownerElement = await this.$(PageObjects.infoBlock.owner());
            const owner = await ownerElement.getText();
            assert.equal(owner, fileOwner);

            const sizeElement = await this.$(PageObjects.infoBlock.size());
            const size = await sizeElement.getText();
            assert.equal(size, fileSize);

            const modifiedElement = await this.$(PageObjects.infoBlock.modified());
            const modified = await modifiedElement.getText();
            assert.equal(modified, fileModified);

            const virusesElement = await this.$(PageObjects.infoBlock.viruses());
            const viruses = await virusesElement.getText();
            assert.equal(viruses, fileViruses);

            const viewsElement = await this.$(PageObjects.infoBlock.views());
            const views = await viewsElement.getText();
            const n = views.split(':').pop();
            assert(parseInt(n, 10) > -1, 'Отображается некорректное число просмотров');
        },

        async yaWaitForSliderClosed(type) {
            await this.yaWaitForHidden(PageObjects.slider(), `Слайдер картинки ${type} не закрылся`);
        },

        async yaChangeListingType(listingType, isMobile) {
            if (isMobile) {
                await this.yaOpenMore();
                await this.yaClick(PageObjects.fileMenuPane[listingType]());
                await this.yaWaitForHidden(PageObjects.fileMenuPane());
            } else {
                await this.yaWaitForVisible(PageObjects.desktopListingTypeButton());
                await this.yaClick(PageObjects.desktopListingTypeButton());
                await this.yaWaitForVisible(PageObjects.desktopListingTypeMenu[listingType]());
                await this.yaClick(PageObjects.desktopListingTypeMenu[listingType]());
                await this.keys('Escape');
                await this.yaWaitForHidden(PageObjects.desktopListingTypeMenu());
            }
        },

        async yaGetDownloadUrlFromAction(downloadAction) {
            await downloadAction();
            await this.pause(500);
            return await this.retrieveUrlFromIframe();
        },

        /**
         * Возвращает ссылку на скачиваемый файл из iframe (вызывается после клика на скачать)
         * извлекая ее из невидимого iframe
         *
         * @returns {Promise<string>}
         * @private
         */
        async retrieveUrlFromIframe() {
            const waitTimeout = 2000;
            const waitCheckInterval = 100;

            let element;
            await this.waitUntil(async() => {
                element = await this.execute((selector) => {
                    return document.querySelector(selector);
                }, PageObjects.downloadIframe());
                return !!element;
            }, waitTimeout, 'IFrame для скачивания не создался', waitCheckInterval);

            return (await this.execute((element) => element.getAttribute('src'), element));
        },

        /**
         * Подскролл страницы для корректного снятия скрина с вау-сеткой
         *
         * @param {boolean} [isMobile]
         * @param {boolean} [isPaidUser]
         */
        async yaScrollToBeginGrid(isMobile = false, isPaidUser = false) {
            // для тачей страницу нужно отскролить к первому элементу сетки,
            // а на десктопе к контекнту, чтобы шапка не перекрывала сетку
            if (isMobile && isPaidUser) {
                await this.yaScroll(0);
            } else {
                const item = await this.$(isMobile ? wowGridItem(1) : PageObjects.content());
                await item.scrollIntoView();
            }
        },

        /**
         * Перелистывает слайдер
         *
         * @param {number} times - сколько раз надо перелистнуть слайдер
         * @param {('left'|'right')} direction - в каком направлении листать слайдер
         * @returns {Promise<void>}
         */
        async yaChangeSliderActiveImage(times = 1, direction = 'right') {
            const isMobile = await this.yaIsMobile();
            if (isMobile) {
                // чтобы избежать asynchronous script timeout: result was not received in 0 seconds
                // при executeAsync в yaPointerPanX
                this.setTimeout({ script: 3000 });
            }
            for (let i = 0; i < times; i++) {
                if (isMobile) {
                    await this.yaPointerPanX(PageObjects.slider.items(), direction === 'right' ? -1 : 1);
                } else {
                    await this.yaClick(direction === 'right' ?
                        PageObjects.slider.nextImage() :
                        PageObjects.slider.previousImage());
                }
            }
        },

        /**
         * @this Browser
         * @returns {Promise<string>}
         */
        async yaGetActiveSliderImageName() {
            return (await this.execute((itemSelector) => {
                const src = document.querySelector(itemSelector).getAttribute('src');
                return decodeURIComponent(src.match(/&filename=([^&]+)/)[1]).replace(/\+/g, ' ');
            }, PageObjects.slider.activePreview.image()));
        }
    },
    //desktop-имплементации
    desktop: {
        async yaSaveToDiskWithAuthorization(login) {
            await this.yaWaitForVisible(PageObjects.desktopToolbar.saveButton());
            await this.yaClick(PageObjects.desktopToolbar.saveButton());
            await this.login(login);
            await this.yaWaitForVisible(
                PageObjects.snackbarText(),
                10000,
                'Сообщение "Сохранено в загрузки" не отобразилось'
            );
            await this.yaWaitForVisible(
                PageObjects.desktopToolbar.openDiskButton(),
                WAITING_AUTH_TIMEOUT,
                'Кнопка "Открыть Диск" не отобразилось'
            );
        },

        async yaSaveToDisk() {
            await this.yaWaitForVisible(PageObjects.desktopToolbar.saveButton());
            await this.yaClick(PageObjects.desktopToolbar.saveButton());
            await this.yaWaitForVisible(
                PageObjects.snackbarText(),
                10000,
                'Сообщение "Сохранено в загрузки" не отобразилось'
            );
            await this.yaWaitForVisible(
                PageObjects.desktopToolbar.openDiskButton(),
                WAITING_AUTH_TIMEOUT,
                'Кнопка "Открыть Диск" не отобразилось'
            );
        },

        async yaWaitForVisibleToolbarButtons(type) {
            await this.yaWaitForVisible(
                PageObjects.desktopToolbar.saveButton(),
                `Кнопка "Сохранить на Диск" для ${type} не отобразилось`
            );
            await this.yaWaitForVisible(
                PageObjects.desktopToolbar.downloadButton(),
                `Кнопка "Скачать" для ${type} не отобразилось`
            );
        },

        // Открытие выпадушки Ещё
        async yaOpenMore() {
            await this.yaClick(PageObjects.moreButton());

            const fileMenu = await this.$(PageObjects.fileMenu());

            await this.waitUntil(async() => {
                return (await fileMenu.isExisting());
            }, { timeoutMsg: 'Выпадушка Еще не существует' });

            await fileMenu.waitForDisplayed({
                timeout: 3000,
                timeoutMsg: 'Выпадушка Ещё не открылась'
            });
        },

        // на десктопе файл из листинг открывается по дабл-клику
        async yaOpenFileFromListing(selector, _selectorInSlider, { shouldBeOpenedInDocs } = {}) {
            await this.yaWaitForVisible(selector);
            await this.yaClickAndAssertNewTabUrl(selector, {
                doDoubleClick: true,
                linkShouldContain: shouldBeOpenedInDocs ? '/docs/view?url=ya-disk-public' : 'docviewer'
            });
        },

        async yaSaveAndDownloadWithAuthorization(login) {
            await this.yaClick(PageObjects.desktopToolbar.saveAndDownloadButton());
            await this.login(login);
            await this.yaWaitForHidden(PageObjects.antiFoTooltip(), 15000);
            let url = await this.getUrl();
            url = decodeURI(url);
            assert.include(url, '/client/recent');
        }
    },

    //touch-имплементации
    touch: {
        async yaSaveToDiskWithAuthorization(login) {
            await this.yaWaitForVisible(PageObjects.toolbar.saveButton());
            await this.yaClick(PageObjects.toolbar.saveButton());
            await this.login(login);
            await this.yaWaitForVisible(
                PageObjects.toolbar.openDiskButton(),
                WAITING_AUTH_TIMEOUT,
                'Кнопка "Открыть Диск" не отобразилось'
            );
            await this.yaWaitForVisible(PageObjects.toolbar.snackbarText(), 'Снэкбар "Сохранено в загрузки" не отобразился');
        },

        async yaSaveToDisk() {
            await this.yaWaitForVisible(PageObjects.toolbar.saveButton());
            await this.yaClick(PageObjects.toolbar.saveButton());
            await this.yaWaitForVisible(
                PageObjects.toolbar.openDiskButton(),
                WAITING_AUTH_TIMEOUT,
                'Кнопка "Открыть Диск" не отобразилось'
            );
            await this.yaWaitForVisible(
                PageObjects.toolbar.snackbarText(),
                'Снэкбар "Сохранено в загрузки" не отобразился'
            );
        },

        async yaSaveAndDownloadWithAuthorization(login) {
            await this.yaClick(PageObjects.toolbar.saveAndDownloadButton());
            await this.login(login);
            await this.yaWaitForVisible(PageObjects.toolbar.openDiskButton(), 'Кнопка "Открыть Диск" не отобразилось');
            await this.yaWaitForVisible(
                PageObjects.toolbar.snackbarText(),
                10000,
                'Сообщение "Сохранено в загрузки" не отобразилось'
            );
        },

        async yaWaitForVisibleToolbarButtons(type) {
            await this.yaWaitForVisible(
                PageObjects.toolbar.saveButton(),
                `Кнопка "Сохранить на Диск" для ${type} не отобразилось`
            );
            await this.yaWaitForVisible(
                PageObjects.toolbar.downloadButton(),
                `Кнопка "Скачать" для ${type} не отобразилось`
            );
        },

        // Открытие выпадушки Ещё
        async yaOpenMore() {
            await this.yaClick(PageObjects.moreButton());

            const fileMenuPane = await this.$(PageObjects.fileMenuPane());

            await this.waitUntil(async() => {
                return (await fileMenuPane.isExisting());
            }, { timeoutMsg: 'Всплывающая панель не существует' });

            await fileMenuPane.waitForDisplayed({
                timeout: 3000,
                timeoutMsg: 'Всплывающая панель "Ещё" не открылась'
            });

            await this.pause(500); // завершение анимации панели
        },

        // на таче по первому тапу открывается слайдер, при клике на файл в слайдере - открывается DV
        async yaOpenFileFromListing(selector, selectorInSlider) {
            await this.yaWaitForVisible(selector);
            await this.yaClick(selector);

            await this.yaWaitForVisible(selectorInSlider);
            await this.yaClickAndAssertNewTabUrl(selectorInSlider, { linkShouldContain: 'docviewer' });
        }
    }
};

module.exports = exports = actions;
