const listing = require('../page-objects/client-content-listing');
const popups = require('../page-objects/client-popups');
const consts = require('../config').consts;
const clientNavigation = require('../page-objects/client-navigation');
const { retriable } = require('@ps-int/ufo-hermione/helpers/retries');
const { assert } = require('chai');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

const actions = {
    //desktop-имплементации
    desktop: {
        /**
         * Выбирает все элементы в открытой директории
         *
         * @returns {Promise<boolean>}
         */
        async yaSelectAll() {
            const listingBody = await this.$(listing.common.listingBody());

            await listingBody.waitForDisplayed();
            await this.execute(() => window.scrollTo(0, 0));

            const elements = await this._yaGetListingElements();

            if (elements.length === 0) {
                return false;
            }

            const firstElement = elements[0];

            await firstElement.click();

            if (elements.length === 1) {
                return true;
            }

            await this.pause(2000);

            return this.actions([{
                type: 'key',
                id: 'pressShift',
                actions: [{ type: 'keyDown', value: consts.KEY_SHIFT }]
            }, {
                type: 'pointer',
                id: 'clickOnElementById',
                actions: [
                    { type: 'pointerMove', origin: elements[elements.length - 1], x: 0, y: 0 },
                    { type: 'pointerDown', button: 0 },
                    { type: 'pointerUp', button: 0 },
                ]
            }]);
        },
        /**
         * Выбирает элемент по его имени в открытой директории
         *
         * @param {string} resourceName
         * @param {string} [withPressedKey]
         * @returns {Promise<void>}
         */
        async yaSelectResource(resourceName, withPressedKey) {
            const elementSelector = listing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, resourceName);
            const selectedElementSelector =
                listing.common.listingBodySelectedItemsInfoXpath().replace(/:titleText/g, resourceName);

            const listingElement = await this.$(listing.common.listingBody());

            const select = async() => {
                if (!(await this.isVisible(elementSelector))) {
                    await this.yaScrollToEnd();
                }
                if (withPressedKey !== undefined) {
                    await this.yaClickWithPressedKey(elementSelector, withPressedKey);
                } else {
                    await this.yaScrollIntoView(elementSelector);
                    await this.click(elementSelector);
                }
            };

            await listingElement.waitForDisplayed();
            await retriable(async() => {
                await select();
                await this.waitForExist(selectedElementSelector);
            }, 5, 500);
        },
        /**
         * Выбирает элементы по переданным именам в открытой директории
         *
         * @param {string[]} resourceNames
         * @returns {Promise<void>}
         */
        async yaSelectResources(resourceNames) {
            for (const name of resourceNames) {
                await this.yaSelectResource(name, consts.KEY_CTRL);
            }
        },

        /**
         * Функция для выбора типа листинга
         *
         * @param {'icons'|'tile'|'list'} type
         * @param {boolean} [fast=false]
         * @returns {Promise<void>}
         */
        async yaSetListingType(type, fast = false) {
            if (fast) {
                await this._yaSetListingTypeFast(type);
            } else {
                await this.yaWaitForVisible(listing.desktop.listingType());
                await this.click(listing.desktop.listingType());
                await this.yaWaitForVisible(listing.desktop.listingType.popup());
                await this.pause(200);

                await this.yaExecuteClick(listing.desktop.listingType[type]());
            }
        },
        /**
         * Функция для выставления типа сортировки
         *
         * @param {boolean} isAscending
         * @returns {Promise<void>}
         */
        async yaSetSortingType(isAscending) {
            await this.click(listing.desktop.listingSortButton());
            await this.yaWaitForVisible(popups.desktop.sortPopup());
            await this.click(popups.desktop.sortPopup[`${isAscending ? 'ascending' : 'descending'}SortButton`]());
        },

        /**
         * Открывает контекстное меню, которое появляется по клику на пустую область листинга
         *
         * @returns {Promise<void>}
         */
        async yaOpenListingContextMenu() {
            await this.yaWaitForVisible(listing.common.listing.head());
            await this.rightClick(listing.common.listing.head());
            await this.pause(500);
            return this.yaWaitForVisible(popups.desktop.contextMenuCreatePopup());
        }
    },
    //touch-имплементации
    touch: {
        /**
         * Выбирает все элементы в открытой директории
         *
         * @returns {Promise<boolean>}
         */
        async yaSelectAll() {
            await this.yaWaitForHidden(listing.common.listingSpinner());
            await this.scroll(0, 0);

            const elements = await this._yaGetListingElements();

            if (elements && elements.length) {
                if (elements.length === 1) {
                    await this.yaLongPress(elements[0]);
                    return true;
                }

                await this.yaLongPress(elements.shift());

                for (const element of elements) {
                    await this.yaTap(element);
                }

                return true;
            }

            return false;
        },
        /**
         * Выбирает элемент по его имени в открытой директории
         *
         * @param {string} resourceName
         * @param {Object} opts
         * @param {boolean} [opts.doTap=false]
         * @returns {Promise<void>}
         */
        async yaSelectResource(resourceName, opts) {
            const options = Object.assign({}, { doTap: false }, opts);
            const elementSelector = listing.common.listingBodyItemsInfoXpath()
                .replace(/:titleText/g, resourceName);

            const select = async() => {
                await this.yaWaitForVisible(listing.common.listingBody());
                await this.yaScrollToEnd();
                await this.yaScrollIntoView(elementSelector);

                if (options.doTap) {
                    await this.yaTap(elementSelector);
                } else {
                    await this.keys('Escape');
                    await this.yaLongPress(elementSelector);
                }
            };

            await retriable(async() => {
                await select();

                await this.waitForExist(
                    listing.common.listingBodySelectedItemsInfoXpath().replace(/:titleText/g, resourceName)
                );
            }, 5, 500);
        },
        /**
         * Выбирает элементы по переданным именам в открытой директории
         *
         * @param {string[]} resourceNames
         * @returns {Promise<Browser>}
         */
        async yaSelectResources(resourceNames) {
            for (let i = 0; i < resourceNames.length; i++) {
                await this.yaSelectResource(resourceNames[i], { doTap: !!i });
            }
        },

        /**
         * Функция для выбора типа листинга
         *
         * @param {'tile'|'icons'|'list'} type
         * @param {boolean} [fast=false]
         * @returns {Promise<void>}
         */
        async yaSetListingType(type, fast = false) {
            if (fast) {
                await this._yaSetListingTypeFast(type);
            } else {
                await this.yaWaitForVisible(clientNavigation.touch.touchListingSettings.settings());
                await this.click(clientNavigation.touch.touchListingSettings.settings());
                await this.yaWaitForVisible(popups.touch.settingsPopup[type]());
                await this.pause(200);
                await this.click(popups.touch.settingsPopup[type]());
                await this.yaCloseVisibleMobilePane();
            }
        },
        /**
         * Функция для выставления типа сортировки
         *
         * @param {boolean} isAscending
         * @returns {Promise<void>}
         */
        async yaSetSortingType(isAscending) {
            await this.click(clientNavigation.touch.touchListingSettings.settings());
            await this.yaWaitForVisible(popups.touch.settingsPopup());
            await this.click(popups.touch[`settingsPopup${isAscending ? 'Ascending' : 'Descending'}SortButton`]());
            await this.yaCloseVisibleMobilePane();
        }
    },
    common: {
        /**
         * Находит все элементы листинга, которые в данный момент отображены
         * на экране
         *
         * @private
         * @returns {Promise<Browser>}
         */
        async _yaGetListingElements() {
            const elements = await this.$$(listing.common.listingBody.items());

            if (elements.length === 0) {
                return [];
            }

            const lastElement = elements[elements.length - 1];
            const lastElementIcon = await lastElement.$(listing.common.listingBody.items.icon());

            if (
                await lastElementIcon.isExisting() &&
                (await lastElementIcon.getAttribute('class'))
                    .includes(listing.common.listing_body_items_trashIconClass())
            ) {
                elements.pop();
            }

            return elements;
        },
        /**
         * Функция для установки настройки отображения листинга
         *
         * @this Browser
         * @param {'tile'|'icons'|'list'} type
         * @returns {Promise<Browser>}
         */
        _yaSetListingTypeFast(type) {
            return this.execute((type) => {
                ns.Model.get('statesContext').setView(type);
            }, type);
        },
        /**
         * Находит все элементы листинга и возвращает их названия
         *
         * @returns {Promise<string>}
         */
        async yaGetListingElementsTitles() {
            await this.yaScrollToEnd();
            return (await this.execute((selector) => {
                return Array.from(document.querySelectorAll(selector))
                    .map((element) => element.getAttribute('title') || element.innerText);
            }, listing.common.listingBody.items.title()));
        },
        /**
         * Делает проверку, находится ли в списке листинга элемент с заданным именем,
         * возвращает результат
         *
         * @param {string} titleText - имя проверяемого элемента
         * @returns {Promise<boolean>}
         */
        yaGetContentListingHas(titleText) {
            return this
                .element(listing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, titleText))
                .then((result) => {
                    return !!result.value;
                });
        },
        /**
         * Проверяет, находится ли в списке листинга элемент с заданным именем,
         * бросает исключение если его нет
         *
         * @param {string} titleText - имя проверяемого элемента
         * @param {string} message - сообщение об ошибке
         * @returns {Promise<Browser>}
         */
        async yaAssertListingHas(titleText, message = 'Листинг не содержит элемента ' + titleText) {
            const listingBody = await this.$(listing.common.listingBody());

            await listingBody.waitForDisplayed({ timeoutMsg: 'Листинг файлов не открыт' });

            const target = await this.findElement(
                'xpath',
                listing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, titleText)
            );
            const targetElement = await this.$(target);

            await targetElement.waitForDisplayed({ timeoutMsg: message });

            return await targetElement.isDisplayed();
        },
        /**
         * Проверяет, что в списке листинга не отображаетсяэлемент с заданным именем,
         * бросает исключение если он присутствует
         *
         * @param {string} titleText - имя проверяемого элемента
         * @param {boolean} [isDocs]
         * @returns {Promise<Browser>}
         */
        async yaAssertListingHasNot(titleText, isDocs) {
            const listingElement = await this.$(listing.common[isDocs ? 'listingBody' : 'listing']());
            const itemSelector = listing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, titleText);

            const item = await this.findElement('xpath', itemSelector);

            await listingElement.waitForDisplayed({ timeoutMsg: 'Листинг файлов не открыт' });
            assert(item.error && item.error === 'no such element', `Листинг содержит элемент ${titleText}`);
        },
        /**
         * Открывает элемент с заданным именем,
         * бросает исключение если его нет
         *
         * @param {string} titleText - имя проверяемого элемента
         * @param {string} [message] - сообщение об ошибке
         * @returns {Promise<Browser>}
         */
        async yaOpenListingElement(titleText, message) {
            message = message || 'Листинг не содержит элемента ' + titleText;

            const selector = listing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, titleText);

            const target = await this.findElement('xpath', selector);
            const targetElement = await this.$(target);

            await targetElement.waitForDisplayed({ timeoutMsg: message });
            await targetElement.scrollIntoView({ block: 'center' });

            if (await this.yaIsMobile()) {
                // Если выделен какой-либо элемент листинга, то клик по папке
                // просто изменит выделение, а не откроет папку.
                // Поэтому сначала закроем топбар, и только потом зайдём кликом в папку
                if (await this.yaIsActionBarDisplayed()) {
                    const closeButton = await this.$(popups.common.actionBar.closeButton());

                    await closeButton.click();
                    await this.yaWaitActionBarHidden();
                }

                const target = await this.findElement('xpath', selector);
                const targetElement = await this.$(target);

                await targetElement.click();
            } else {
                const target = await this.findElement('xpath', selector);
                const targetElement = await this.$(target);

                await targetElement.doubleClick();
            }
        },
        /**
         * Закрывает action-bar
         *
         * @returns {Promise<Browser>}
         */
        async yaCloseActionBar() {
            await this.click(popups.common.actionBar.closeButton());
            await this.yaWaitActionBarHidden();
        },
        /**
         * Возвращает название ресурса из инфо попапа
         *
         * @param {string} infoButtonSelector - селектор для кнопки вызова инфопоапа
         * @returns {Promise<string>}
         */
        async yaGetResourceNameFromInfoDropdown(infoButtonSelector) {
            await this.yaWaitForVisible(infoButtonSelector);
            await this.click(infoButtonSelector);
            await this.yaWaitForVisible(popups.common.resourceInfoDropdownContent());
            await this.pause(500);

            const fileName = await this.getText(popups.common.resourceInfoDropdownContent.fileName());

            if (await this.yaIsMobile()) {
                await this.click(clientNavigation.touch.modalCell());
                await this.yaWaitForHidden(popups.touch.mobilePaneVisible());
            } else {
                await this.click(infoButtonSelector);
            }

            return fileName;
        },
        /**
         * Проверяет отсутствие элемента на странице
         *
         * @param {string} selector - селектор элемента
         *
         * @returns {boolean}
         */
        async yaElementIsNotDisplayed(selector) {
            const element = await this.$(selector);

            return (await element.isDisplayed({ reverse: true }));
        }
    }
};

module.exports = exports = actions;
