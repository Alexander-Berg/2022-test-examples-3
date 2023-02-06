const popups = require('../page-objects/client-popups');
const { consts } = require('../config');
const {
    photoItemByName,
    photoItem,
    selectedPhotoItem,
} = require('../page-objects/client');
const { assert } = require('chai');
const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('../config').login);

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

const yaWaitPageReady = async function () {
    await this.executeAsync((done) => {
        if (document.readyState === 'complete') {
            done();
        } else {
            document.addEventListener('readystatechange', () => {
                if (document.readyState === 'complete') {
                    done();
                }
            });
        }
    });
};

/**
 * Пропускает попап с приветствием при первом посещении
 *
 * @this Browser
 * @returns {Promise<void>}
 */
const yaSkipWelcomePopup = async function () {
    await this.pause(1000);

    if (await this.isVisible(popups.common.welcomePopup())) {
        await this.click(popups.common.welcomePopup.closeButton());
        await this.yaWaitForHidden(popups.common.welcomePopup());
    }

    if (await this.isVisible(popups.touch.promoAppPopup())) {
        await this.click(popups.touch.promoAppPopup.skip());
        await this.yaWaitForHidden(popups.touch.promoAppPopup());
    }
};

/**
 * @returns {Promise<void>}
 */
const yaSkipFacesOnboarding = async function () {
    await yaWaitPageReady.call(this);

    await this.executeAsync((done) => {
        if (typeof ns !== 'object') {
            done();
        } else {
            ns.Model.get('settings')
                .save({ key: 'facesAlbumsOnboardingClosed', value: '1' })
                .then(done, done);
        }
    });
};

/**
 * Пропускает полноформатное промо
 *
 * @this Browser
 * @returns {Promise<void>}
 */
const yaSkipSubscriptionOnboarding = async function () {
    const isMobile = await this.yaIsMobile();

    if (isMobile) {
        return;
    }

    await yaWaitPageReady.call(this);

    await this.executeAsync((done) => {
        if (typeof ns !== 'object') {
            done();
        } else {
            const preloadData = JSON.parse(
                document.getElementById('preloaded-data').innerHTML
            );
            if (preloadData.billingProducts.active_promo) {
                ns.Model.get('settings')
                    .save({
                        key: 'lastClosedSubscriptionOnboardingId',
                        value: preloadData.billingProducts.active_promo.key,
                    })
                    .then(done, done);
            } else {
                done();
            }
        }
    });
};

/**
 * Пропускает все стартовые онбординги
 *
 * @returns {Promise<void>}
 */
const yaSkipAllOnboardings = async function () {
    const skipFuncs = [
        yaSkipWelcomePopup,
        yaSkipFacesOnboarding,
        yaSkipSubscriptionOnboarding,
    ];

    for (const func of skipFuncs) {
        await func.call(this);
    }
};

/**
 * Открыть новую вкладку
 *
 * @param {Object} [options]
 * @param {string} [options.url]
 * @param {boolean} [options.skipOnboardings]
 *
 * @returns {Promise<string>}
 */
const yaOpenNewTab = async function ({ url, skipOnboardings = true } = {}) {
    const newUrl = url || (await this.getUrl());

    await this.newWindow(newUrl);
    await this.waitUntil(
        async function () {
            const realUrl = await this.getUrl();
            return realUrl.startsWith(newUrl);
        },
        5000,
        `Не дождались открытия нужного URL: ${newUrl}`
    );

    await yaWaitPageReady.call(this);
    const tabId = await this.getCurrentTabId();

    if (skipOnboardings) {
        await yaSkipAllOnboardings.call(this);
    }

    return tabId;
};

/**
 * Закрываем промо-нотифайку
 *
 * @this Browser
 * @returns {Promise<void>}
 */
const yaClosePromoNotification = async function () {
    if (await this.isVisible(popups.common.promoNotification())) {
        await this.click(popups.common.promoNotificationClose());
        await this.yaWaitForHidden(popups.common.promoNotification());
    }
};

/**
 * Авторизует пользователя и скрывает различные модальные окна
 *
 * @this Browser
 * @param {string} login
 * @param {string} [tld]
 * @returns {Promise<Browser>}
 */
const yaClientLoginFast = async function (login, tld) {
    await this.yaClientLoginFastUser(getUser(login), tld);
};

/**
 * @param {{ login: string, password: string }} user
 * @param {string} [tld]
 * @returns {Promise<void>}
 */
const yaClientLoginFastUser = async function (user, tld) {
    await this.loginFast(user, tld);
    await this.yaSkipWelcomePopup();
    await this.yaSkipSubscriptionOnboarding();
    await this.yaClosePromoNotification();
    await this.yaEnableUnlimitedNotifications();
};

/**
 * @param {string} selector
 * @param {string} eventName
 * @returns {Promise<void>}
 */
const yaExecuteEvent = async function (selector, eventName) {
    await this.execute(
        (selector, eventName) => {
            const element = document.querySelector(selector);
            const event = document.createEvent('Events');
            event.initEvent(eventName, true, false);
            element.dispatchEvent(event);
        },
        selector,
        eventName
    );
};

/**
 * Выполнить клик программно через браузер
 * Стабильнее работает когда обычный .click не срабатывает, попадая не на тот элемент
 *  - если элемент двигается
 *  - если элемент частично перекрыт другим элементом
 *
 * @this Browser
 * @param {string} selector
 * @returns {Promise<Browser>}
 */
const yaExecuteClick = async function (selector) {
    await this.yaExecuteEvent(selector, 'click');
};

/**
 * Выполнить клик программно через браузер
 * Стабильнее работает когда обычный .click не срабатывает, попадая не на тот элемент
 *  - если элемент двигается
 *  - если элемент частично перекрыт другим элементом
 *
 * @this Browser
 * @param {Object} webDriverElement - элемент типа WebDriverJSON
 * @returns {Promise<Browser>}
 */
const yaExecuteClickOnElement = function (webDriverElement) {
    return this.execute((element) => {
        if (!element || !(element instanceof Element)) {
            return;
        }

        const event = document.createEvent('Events');
        event.initEvent('click', true, false);
        element.dispatchEvent(event);
    }, webDriverElement);
};

/**
 * Вызывает скролл окна в конец, чтобы проскроллить все подгружающиеся элементы.
 *
 * @this Browser
 * @returns {Promise<*>}
 */
const yaScrollToEnd = async function () {
    let currentPageY = 0;

    const pageYLimit = 1000000;
    const checkInterval = 500;

    // скроллит страницу вниз, пока проверка проскроллености
    // говорит о том что страница еще не проскроллена
    while (true) {
        if (currentPageY > pageYLimit) {
            throw new Error('Страница слишком длинная, или неверно сработал скролл вниз');
        }

        const targetPageY = await this.execute(() => document.body.scrollHeight - window.innerHeight);

        currentPageY += targetPageY;

        // подскролливаем в 2 этапа, т.к. на програмнный фуллскролл плохо реагирует Хром,
        // и подгрузка следующей порции не происходит
        await this.execute((y) => window.scrollTo(0, y), targetPageY - 10);
        await this.execute((y) => window.scrollTo(0, y), targetPageY + 10);

        let checkAttempts = 7;
        let scrolled = true;

        while (true) {
            scrolled = await this.execute(
                () => document.body.scrollHeight - window.innerHeight - window.pageYOffset < 5
            );

            if (scrolled && checkAttempts-- > 0) {
                await this.pause(checkInterval);
            } else {
                break;
            }
        }

        if (scrolled) {
            break;
        }
    }
};

/**
 * Вызывает скролл окна до элемента и центрирует его в окне.
 *
 * @this Browser
 * @param {string} selector - селектор элемента, до которого нужно проскроллить страницу
 * @returns {Promise<void>}
 */
const yaScrollIntoView = async function (selector) {
    const { height } = await this.getViewportSize();
    await this.scroll(selector, 0, -(height / 2));
};

/**
 * Вызывает нативный scrollIntoView на элементе
 *
 * @param {string} selector
 * @returns {Promise<void>}
 */
const yaNativeScrollIntoView = async function (selector) {
    await this.selectorExecute(selector, (elements) => {
        elements[0].scrollIntoView();
    });
};

/**
 * @this Browser
 * @param {string} key
 * @param {string} value
 * @returns {Promise<Browser>}
 */
const yaSaveSettings = function (key, value) {
    return this.executeAsync(
        (key, value, done) => {
            return ns.Model.get('settings')
                .save({ key, value })
                .then(() => done());
        },
        key,
        value
    );
};

/**
 * Функция для сброса настроек показа welcome-popup
 *
 * @this Browser
 * @returns {Promise<Browser>}
 */
const yaResetWelcomePopupSettings = function () {
    return this.executeAsync((done) => {
        const settings = ns.Model.get('settings');

        settings
            .save({ key: 'countDisplaysDialogWelcome', value: '0' })
            .then(() =>
                settings
                    .save({
                        key: 'timestampLastDisplayedDialogWelcome',
                        value: '0',
                    })
                    .then(() => done())
            );
    });
};

/**
 * Функция для редактирования по ключу настроек пользователя
 *
 * @this Browser
 * @param {string} key название настройки
 * @param {string} [value=0] значение
 * @returns {Promise<Browser>}
 */
const yaSetUserSettings = function (key, value = '0') {
    return this.executeAsync(
        (key, value, done) => {
            ns.Model.get('settings')
                .save({ key, value })
                .then(() => done());
        },
        key,
        value
    );
};

/**
 * Делает выделение мышкой от начальной координаты (x, y) до (x + deltaX, y + deltaY)
 *
 * @this Browser
 * @param {string} selector      - элемент на котором делаем dispatchEvent
 * @param {Object} params
 * @param {number} params.startX - координата начала выделения по оси Х
 * @param {number} params.startY - координата начала выделения по оси Y
 * @param {number} params.deltaX - координата конца выделения по оси X
 * @param {number} params.deltaY - координата конца выделения по оси Y
 * @returns {Promise<Browser>}
 */
const yaMouseSelect = function (
    selector,
    { startX = 0, startY = 0, deltaX = 0, deltaY = 0, releaseMouse = true }
) {
    return this.executeAsync(
        (selector, startX, startY, deltaX, deltaY, releaseMouse, done) => {
            const element = document.querySelector(selector);
            element.dispatchEvent(
                new MouseEvent('mousedown', {
                    bubbles: true,
                    clientX: startX,
                    clientY: startY,
                })
            );

            document.dispatchEvent(
                new MouseEvent('mousemove', {
                    clientX: startX + deltaX,
                    clientY: startY + deltaY,
                })
            );

            if (releaseMouse) {
                element.dispatchEvent(
                    new MouseEvent('mouseup', {
                        bubbles: true,
                    })
                );
            }

            done();
        },
        selector,
        startX,
        startY,
        deltaX,
        deltaY,
        releaseMouse
    );
};

/**
 * @this Browser
 * @param {string} selector - селектор элемента на котором нужно выполнить жест
 * @param {number} stepsCount - количество итераций жеста
 * @returns {Promise<Browser>}
 */
const yaPointerPinch = function (selector, stepsCount = 20) {
    return this.executeAsync(
        (selector, stepsCount, done) => {
            const element = document.querySelector(selector);

            const startX = window.innerWidth / 2;
            const startY = window.innerHeight / 2;

            const pointers = [
                { x: startX - 10, y: startY - 10, pointerId: 1 },
                { x: startX + 10, y: startY + 10, pointerId: 2 },
            ];

            pointers.forEach((pointer) => {
                element.dispatchEvent(
                    new PointerEvent('pointerdown', {
                        pointerId: pointer.pointerId,
                        pointerType: 'touch',
                        clientX: pointer.x,
                        clientY: pointer.y,
                    })
                );
            });

            const step = startX / 10;
            let currentStep = 0;

            /**
             *
             */
            function inc() {
                currentStep++;
                pointers.forEach((pointer, index) => {
                    const direction = index ? -1 : 1;
                    pointer.x += direction * step;
                    pointer.y += direction * step;

                    window.dispatchEvent(
                        new PointerEvent('pointermove', {
                            pointerType: 'touch',
                            pointerId: pointer.pointerId,
                            clientX: pointer.x,
                            clientY: pointer.y,
                        })
                    );
                });

                if (currentStep < stepsCount) {
                    requestAnimationFrame(inc);
                } else {
                    pointers.forEach((pointer) => {
                        window.dispatchEvent(
                            new PointerEvent('pointerup', {
                                pointerType: 'touch',
                                pointerId: pointer.pointerId,
                            })
                        );
                    });
                    done();
                }
            }

            inc();
        },
        selector,
        stepsCount
    );
};

/**
 * Изменяет display: table модального окна на display: block
 *
 * @this Browser
 * @param {string} modalSelector - элемент на котором начать pan
 * @returns {Promise<Browser>}
 */
const yaSetModalDisplay = function (modalSelector = '') {
    return this.execute((selector) => {
        document.querySelector(selector).style.display = 'block';
    }, modalSelector + ' .Modal-Table');
};

/**
 * @param {string} selector
 * @returns {Promise<DOMRect>}
 */
const yaGetElementRect = async function (selector) {
    return await this.execute((selector) => {
        return document.querySelector(selector).getBoundingClientRect();
    }, selector);
};

/**
 * @param {number} value
 * @returns {Promise<void>}
 */
const yaAssertScrollEquals = async function (value) {
    const scrollTop = await this.execute(() => window.pageYOffset);
    assert.equal(scrollTop, value);
};

/**
 * @param {number} x
 * @param {number} y
 * @param {number} [deltaX] позволяет отпустить тап в другом месте
 * @param {number} [deltaY] позволяет отпустить тап в другом месте
 * @returns {Promise<void>}
 */
const yaTapOnScreen = async function (x, y, deltaX = 0, deltaY = 0) {
    const pointerActions = [
        { type: 'pointerMove', x, y },
        { type: 'pointerDown', button: 0 },
    ];
    if (deltaX !== 0 || deltaY !== 0) {
        pointerActions.push({
            type: 'pointerMove',
            x: x + deltaX,
            y: y + deltaY,
        });
    }
    pointerActions.push({ type: 'pointerUp', button: 0 });

    await this.actions([
        {
            type: 'pointer',
            id: 'yaTapOnScreen',
            actions: pointerActions,
        },
    ]);
};

/**
 * Выбирает фотографию по селектору
 *
 * @this Browser
 * @param {string} itemSelector - селектор фотографии
 * @param {boolean} [hover=false] - нужно ли делать ховер и дожидаться появления чекбокса
 * @param {boolean} [multiselect=false]
 * @param {string} [withKeyPressed]
 * @returns {Promise<void>}
 */
const yaSelectPhotoItem = async function (
    itemSelector,
    hover = false,
    multiselect = false,
    withKeyPressed = undefined
) {
    const isMobile = await this.yaIsMobile();
    await this.yaWaitForVisible(itemSelector);

    if (isMobile) {
        if (!multiselect) {
            await this.keys('Escape'); // снять выделение, иначе long tap добавит ресурс к текущему выделению
        }
        await this.yaScrollIntoView(itemSelector);
        await this.yaLongPress(itemSelector);
    } else {
        const checkboxSelector = itemSelector + ' .lite-checkbox';
        if (hover) {
            await this.moveToObject(itemSelector);
            await this.yaWaitForVisible(checkboxSelector);
        }
        if (withKeyPressed) {
            await this.yaClickWithPressedKey(checkboxSelector, withKeyPressed);
        } else {
            await this.click(checkboxSelector);
            await this.moveToObject('body', 0, 0); // убрать hover и перекалибровать указатель
        }
    }
};

/**
 * Выбирает фотографию по имени файла
 *
 * @this Browser
 * @param {string} name - имя файла
 * @param {boolean} [hover] - нужно ли делать ховер и дожидаться появления чекбокса
 * @param {boolean} [multiselect]
 * @param {string|undefined} [withKeyPressed]
 * @returns {Promise<void>}
 */
const yaSelectPhotoItemByName = async function (
    name,
    hover,
    multiselect = false,
    withKeyPressed = undefined
) {
    await this.yaSelectPhotoItem(
        photoItemByName().replace(':title', name),
        hover,
        multiselect,
        withKeyPressed
    );
};

/**
 * @returns {Promise<string[]>}
 */
const yaGetPhotosNames = async function () {
    return (
        await this.execute((selector) => {
            return Array.from(document.querySelectorAll(selector)).map(
                (resource) => resource.title
            );
        }, photoItem())
    );
};

/**
 * Выбыирает диапазон фотографий с помощью shift от первого до последнего фото из filesNames
 *
 * @param {string[]} filesNames
 */
const yaSelectPhotosRange = async function (filesNames) {
    await this.yaSelectPhotoItemByName(filesNames[0], true);
    await this.yaClickWithPressedKey(
        photoItemByName().replace(':title', filesNames[filesNames.length - 1]),
        consts.KEY_SHIFT
    );
};

/**
 * Открывает контексное меню на файле в срезе
 *
 * @param {string} fileName
 */
const yaOpenActionPopupPhoto = async function (fileName) {
    await this.rightClick(photoItemByName().replace(':title', fileName));
    await this.yaWaitForVisible(popups.common.actionPopup());
};

/**
 * Возвращает массив имён выделенных ресурсов
 *
 * @this Browser
 * @returns {Promise<string[]>}
 */
const yaGetSelectedPhotoItemsNames = async function () {
    const elements = await this.$$(selectedPhotoItem());
    return await Promise.all(elements.map((element) => element.getAttribute('title')));
};

/**
 * Проверяет количество открытых вкладок
 *
 * @param {number} n
 * @returns {Promise<void>}
 */
const yaAssertTabsCount = async function (n) {
    const { length: tabsCount } = await this.getTabIds();
    assert.equal(tabsCount, n);
};

/**
 * Возвращает уникальное имя для файла
 *
 * @returns {string}
 */
const yaGetUniqResourceName = () => `tmp-${Date.now()}`;

/**
 * Проверяет текст ошибки в папапе создания и переименования папки
 *
 * @param {string} expectedErrorText
 * @returns {Promise<void>}
 */
const yaAssertFolderPopupError = async function (expectedErrorText) {
    const actualErrorText = await this.getText(
        popups.common.renameDialog.renameError()
    );
    assert.equal(expectedErrorText, actualErrorText);
};

/**
 * Проверяет соответствие оставшегося места в Диске ожидаемому
 *
 * @param {number} bytes
 */
const yaFreeSpaceIsEqual = async function (bytes) {
    const freeSpace = await this.execute(() => {
        return JSON.parse(document.querySelector('#preloaded-data').innerHTML).space.free;
    });

    assert.equal(freeSpace, bytes, 'Объем свободного место отличается от ожидаемого');
};

const yaOpenUrlOnTld = async function (tld, url = consts.NAVIGATION.disk.url) {
    const fullUrl = this.options.baseUrl.replace(/ru$/, tld) + url;
    await this.url(fullUrl);
};

module.exports = {
    yaSkipWelcomePopup,
    yaClosePromoNotification,
    yaSkipFacesOnboarding,
    yaSkipSubscriptionOnboarding,
    yaSkipAllOnboardings,
    yaOpenNewTab,
    yaExecuteEvent,
    yaExecuteClick,
    yaExecuteClickOnElement,
    yaScrollToEnd,
    yaClientLoginFast,
    yaClientLoginFastUser,
    yaMouseSelect,
    yaPointerPinch,
    yaResetWelcomePopupSettings,
    yaSetModalDisplay,
    yaSetUserSettings,
    yaScrollIntoView,
    yaNativeScrollIntoView,
    yaSaveSettings,
    yaGetElementRect,
    yaAssertScrollEquals,
    yaTapOnScreen,
    yaSelectPhotoItem,
    yaSelectPhotoItemByName,
    yaGetPhotosNames,
    yaSelectPhotosRange,
    yaOpenActionPopupPhoto,
    yaGetSelectedPhotoItemsNames,
    yaAssertTabsCount,
    yaGetUniqResourceName,
    yaAssertFolderPopupError,
    yaFreeSpaceIsEqual,
    yaOpenUrlOnTld,
};
