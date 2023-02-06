const photo = require('../page-objects/client-photo2-page');
const clientPopups = require('../page-objects/client-popups');
const { assert } = require('chai');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

const actions = {
    common: {
        /**
         * Ждёт пока прогрузятся все превью во вьюпорте в фотосрезе
         *
         * @this Browser
         * @returns {Promise<void>}
         */
        async yaWaitPhotoSliceItemsInViewportLoad() {
            await this.yaWaitPreviewsLoaded(photo.common.photo.item.preview(), true, 10000);
        },

        /**
         * @param {string} itemName
         * @returns {Promise<void>}
         */
        async yaWaitPhotoSliceItemInViewport(itemName) {
            const itemSelector = photo.common.photo.itemByName().replace(':title', itemName);
            await this.yaWaitForVisible(itemSelector);
            await this.yaAssertInViewport(itemSelector);
        },

        /**
         * Выбирает фотографию в фотосрезе по селектору
         *
         * @this Browser
         * @param {string} itemSelector - селектор фотографии
         * @param {boolean} [hover] - нужно ли делать ховер и дожидаться появления чекбокса
         * @param {boolean} [multiselect]
         * @returns {Promise<void>}
         */
        async yaSelectPhotosliceItem(itemSelector, hover, multiselect = false) {
            const isMobile = await this.yaIsMobile();
            await this.yaWaitForVisible(itemSelector);

            if (isMobile) {
                if (!multiselect) {
                    await this.keys('Escape'); // снять выделение, иначе long tap добавит ресурс к текущему выделению
                }
                await this.yaLongPress(itemSelector);
            } else {
                const checkboxSelector = itemSelector + ' .lite-checkbox';
                if (hover) {
                    await this.moveToObject(itemSelector);
                    await this.yaWaitForVisible(checkboxSelector);
                }
                await this.click(checkboxSelector);
            }
        },

        /**
         * @this Browser
         * @param {string} titleLabelSelector
         * @returns {Promise<void>}
         */
        async yaSelectCluster(titleLabelSelector) {
            await this.yaWaitForVisible(titleLabelSelector);

            if (await this.yaIsMobile()) {
                await this.yaLongPress(titleLabelSelector);
            } else {
                const checkboxSelector = titleLabelSelector + ' .lite-checkbox';

                await this.moveToObject(titleLabelSelector);
                await this.yaWaitForVisible(checkboxSelector);
                await this.click(checkboxSelector);
                // чтобы убрать hover и перекалибровать указатель
                await this.moveToObject('body', 0, 0);
            }
        },

        /**
         * Выбирает фотографию в фотосрезе по имени файла
         *
         * @this Browser
         * @param {string} name - имя файла
         * @param {boolean} [hover] - нужно ли делать ховер и дожидаться появления чекбокса
         * @param {boolean} [multiselect]
         * @returns {Promise<void>}
         */
        async yaSelectPhotosliceItemByName(name, hover, multiselect = false) {
            const selector = photo.common.photo.itemByName().replace(':title', name);
            await this.yaSelectPhotosliceItem(selector, hover, multiselect);
        },

        /**
         * Возвращает имя файла в фотосрезе по селектору
         *
         * @this Browser
         * @param {string} itemSelector
         * @returns {Promise<string>}
         */
        async yaGetPhotosliceItemName(itemSelector) {
            await this.yaWaitForVisible(itemSelector);

            const item = await this.$(itemSelector);

            return await item.getAttribute('title');
        },

        /**
         * Ждём открытие фотосреза с топбаром добавления в альбом
         *
         * @this Browser
         * @returns {Promise<void>}
         */
        async yaWaitPhotosliceWithToolbarOpened() {
            await this.yaWaitForVisible(photo.common.photo.item(), 'Фотосрез не отобразился');
            await this.yaWaitForVisible(photo.common.photoSelecting(), 'Фотосрез не в режиме выделения');
            await this.yaWaitForVisible(photo.common.addToAlbumBar(), 'Топбар "Выберите фото" не появился');
        },

        /**
         * @callback FastscrollPointerPositionCondition
         * @param {DOMRect} containerRect
         * @param {DOMRect} pointerRect
         * @returns {boolean}
         */

        /**
         * @param {FastscrollPointerPositionCondition} condition
         * @returns {Promise<void>}
         */
        async yaAssertFastscrollPointerPosition(condition) {
            const containerRect = await this.yaGetElementRect(photo.desktop.fastScroll.pointerContainer());
            const elementRect = await this.yaGetElementRect(photo.desktop.fastScroll.scrollPointer());
            assert.equal(condition(containerRect, elementRect), true, 'Позиция засечки фастсрола некорректна');
        },

        /**
         * @param {string} month
         * @param {number} [xOffset] дополнительная поправка координаты
         * @param {number} [yOffset] дополнительная поправка координаты
         * @returns {Promise<void>}
         */
        async yaHoverOnFastscrollMonth(month, xOffset = 0, yOffset = 0) {
            const element = await this.$(photo.desktop.fastScroll.month() + `[data-month="${month}"]`);
            await element.moveTo({ xOffset, yOffset });
        },

        /**
         * Выбирает рандомную фотку из фотосреза.
         *
         * @returns {string}
         */
        async yaGetPhotosliceRandomPhoto() {
            return await this.execute((groupSelector, photoSelector) => {
                const getPreviousSiblingsCount = (element) => {
                    let count = 0;
                    let previousElement = element.previousElementSibling;

                    while (previousElement) {
                        count++;
                        previousElement = previousElement.previousElementSibling;
                    }

                    return count;
                };

                const photos = document.querySelectorAll(photoSelector);
                const photo = photos[Math.floor(Math.random() * photos.length)];
                const group = photo.parentElement;

                const groupIndex = getPreviousSiblingsCount(group) + 1;
                const photoIndex = getPreviousSiblingsCount(photo) + 1;

                return `${groupSelector}:nth-child(${groupIndex}) ${photoSelector}:nth-child(${photoIndex})`;
            }, photo.common.photoGroup(), photo.common.photoItem());
        },
        /**
         * Перемещает все фото из текущей директории в папку Photos
         *
         * @this Browser
         * @returns {Promise<void>}
         */
        async yaMoveBackToPhoto() {
            if (await this.yaListingNotEmpty()) {
                await this.yaSelectAll();
                await this.yaMoveSelected('photos');
                await this.yaAssertProgressBarAppeared();
                await this.yaAssertProgressBarDisappeared();
            }
        }
    },
    desktop: {
        /**
         * @param {"tile" | "wow"} type
         * @returns {Promise<void>}
         */
        async yaSetPhotoSliceListingType(type) {
            await this.yaSetListingType(type);
        }
    },
    touch: {
        /**
         * @param {"tile" | "wow"} type
         * @returns {Promise<void>}
         */
        async yaSetPhotoSliceListingType(type) {
            await this.yaWaitForVisible(photo.common.photoHeader.menuButton());
            await this.click(photo.common.photoHeader.menuButton());
            await this.yaWaitForVisible(clientPopups.touch.mobilePaneVisible());
            await this.pause(500);
            await this.click(photo.touch[`${type}ViewRadio`]());
            await this.pause(1000);
            await this.yaCloseVisibleMobilePane();
        }
    }
};

module.exports = actions;
