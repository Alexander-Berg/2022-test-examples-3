const _ = require('lodash');
const popups = require('../page-objects/client-popups');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

const DEFAULT_NOTIFICATION_TIMEOUT = 20000;

/**
 * Включает возможность вывода бесконечного количества нотификаций
 *
 * @this Browser
 * @returns {Promise<Browser>}
 */
const yaEnableUnlimitedNotifications = function() {
    return this.execute(() => {
        if (typeof ns !== 'undefined') {
            ns.Model.get('queueNotifications').limit = Infinity;
        }
    });
};

/**
 * Возвращает текст нотификации
 *
 * @param {string|{name: string, folder: string}} name
 * @param {string} notificationTemplate
 * @returns {Promise<string>}
 */
const getNotificationText = (name, notificationTemplate) => {
    if (typeof name === 'string') {
        return notificationTemplate.replace(':name', name);
    } else {
        return notificationTemplate.replace(':name', name.name).replace(':folder', name.folder);
    }
};

/**
 * Проверяет, что появилась нотификация с текстом о папке/файле
 * и опционально скрывает нотификацию
 *
 * @this Browser
 * @param {string|{name: string, folder: string}} name
 * @param {string} notificationTemplate
 * @param {Object} [options={}]
 * @param {number} [options.timeout=10000]
 * @param {boolean} [options.close=true]
 * @returns {Promise<void>}
 */
const yaWaitNotificationForResource = async function(name, notificationTemplate, options = {}) {
    const params = _.defaults(options, {
        timeout: DEFAULT_NOTIFICATION_TIMEOUT,
        close: true
    });

    const notificationText = getNotificationText(name, notificationTemplate);

    await this.yaWaitNotificationWithText(notificationText, params.timeout);

    if (params.close) {
        await this.yaCloseNotificationWithText(notificationText);
    }
};

/**
 * Клик по нотифайке о действии с ресурсом
 *
 * @param {string|{name: string, folder: string}} name
 * @param {string} notificationTemplate
 * @param {string} [selector]
 */
const yaClickNotificationForResource = async function(name, notificationTemplate,
    selector = popups.common.notifications.link()) {
    await this.yaClickNotificationWithText(getNotificationText(name, notificationTemplate), selector);
};

/**
 * Закрывает нотификацию
 *
 * @param {string} name
 * @param {string} notificationTemplate
 */
const yaCloseNotificationForResource = async function(name, notificationTemplate) {
    await this.yaCloseNotificationWithText(getNotificationText(name, notificationTemplate));
};

/**
 * Проверяет, что появилась нотификация, содержащая переданный текст
 *
 * @this Browser
 * @param {string} expectedText
 * @param {number} timeout
 * @returns {Promise<Browser>}
 */
const yaWaitNotificationWithText = async function(expectedText, timeout = DEFAULT_NOTIFICATION_TIMEOUT) {
    const selector = popups.common.notifications.text();

    await this.waitUntil(async () => {
        const elements = await this.$$(selector);
        const existingElements = await Promise.all(elements.map((element) => element.isExisting()));

        if (!existingElements.some(Boolean)) {
            return false;
        }

        // стандартный метод getText в этом месте давал сбои - возвращал частично урезанный текст
        // нотификации, из-за чего падала проверка на ожидаемую нотификацию.
        // поэтому getText был заменен на execute
        const notifications = await this.execute(
            (selector) => Array.from(document.querySelectorAll(selector)).map((node) => node.innerText),
            selector
        );

        return [].concat(notifications).some((text) => text.includes(expectedText));
    }, { timeout, timeoutMsg: `Нотифайка «${expectedText}» не появилась` });
};

/**
 * Закрывает нотификацию, содержащую переданный текст
 *
 * @this Browser
 * @param {string} text - текст нотификации
 * @returns {Promise<void>}
 */
const yaCloseNotificationWithText = function(text) {
    return this.yaClickNotificationWithText(text);
};

/**
 * Клик по селектору внутри нотифайки
 *
 * @this Browser
 * @param {string} text - текст нотификации
 * @param {string} [selector] - селектор внутри нотификации
 * @returns {Promise<void>}
 */
const yaClickNotificationWithText = async function(text, selector) {
    await this.execute((text, selector, isMobile, innerSelector) => {
        const notifications = Array.from(document.querySelectorAll(selector))
            .filter((node) => node.innerText.includes(text));

        if (!notifications.length) {
            return;
        }

        const [notification] = notifications;

        const element = innerSelector ? notification.querySelector(innerSelector) : notification;

        if (!element) {
            return;
        }

        if (isMobile && innerSelector) {
            $(element).trigger(ns.V.EVENTS.click);
        } else {
            const event = document.createEvent('Events');

            event.initEvent('click', true, false);
            element.dispatchEvent(event);
        }
    }, text, popups.common.notifications.text(), await this.yaIsMobile(), selector);
};

/**
 * Вызывает скрытие всех видимых нотификаций
 *
 * @this Browser
 * @returns {Promise<void>}
 */
const yaCloseAllNotifications = async function() {
    while (true) {
        const notifications = await this.$$(popups.common.notifications());

        const notificationsDisplayState = await Promise.all(
            notifications.map((notification) => notification.isDisplayed())
        );

        const displayedNotifications = _.zip(notifications, notificationsDisplayState)
            .filter((notification, displayState) => displayState)
            .map((notification) => notification);

        if (!displayedNotifications.length) {
            return;
        }

        await Promise.all(displayedNotifications.map(
            (notification) => this.yaExecuteClickOnElement(notification)
        ));

        await this.pause(300);
    }
};

module.exports = {
    yaWaitNotificationForResource,
    yaWaitNotificationWithText,
    yaCloseNotificationWithText,
    yaClickNotificationForResource,
    yaClickNotificationWithText,
    yaCloseNotificationForResource,
    yaCloseAllNotifications,
    yaEnableUnlimitedNotifications
};
