const clientObjects = require('../page-objects/client');

/**
 * Открывает чат из меню «Показать ещё»
 *
 * @param {Object} bro
 */
async function openChatFromPopup(bro) {
    await bro.click(clientObjects.common.showMorePopup.chatButton());
    await bro.yaWaitForVisible(clientObjects.common.chatWidget());
}

/**
 * Открывает всплывающее меню «Показать ещё»
 *
 * @param {Object} bro
 */
async function openShowMorePopup(bro) {
    await bro.click(clientObjects.common.toolbar.moreButton());
    await bro.yaWaitForVisible(clientObjects.common.showMorePopup());
}

/**
 * Включает запись экрана
 *
 * @param {Object} bro
 */
async function startRecording(bro) {
    await bro.click(clientObjects.common.showMorePopup.startRecording());
    await bro.yaWaitForHidden(clientObjects.common.showMorePopup());
}

/**
 * Ждёт появления тултипа об ограниченной записи
 *
 * @param {Object} bro
 */
async function waitShowLimitationRecord(bro) {
    await bro.yaWaitForVisible(clientObjects.common.showLimitationRecord());
}

/**
 * Закрывает тултип об ограниченной записи
 *
 * @param {Object} bro
 */
async function closeLimitationRecord(bro) {
    await bro.click(clientObjects.common.showLimitationRecord.closeButton());
    await bro.yaWaitForHidden(clientObjects.common.showLimitationRecord());
}

/**
 * Установить приложение
 *
 * @param {Object} bro
 */
async function downloadApp(bro) {
    await bro.click(clientObjects.common.showLimitationRecord.downloadButton());
    await bro.yaWaitForHidden(clientObjects.common.showLimitationRecord());
}

module.exports = {
    openShowMorePopup,
    openChatFromPopup,
    startRecording,
    waitShowLimitationRecord,
    closeLimitationRecord,
    downloadApp,

    /**
     * Открывает попап участников конференции
     *
     * @param {Object} bro
     */
    async openParticipantsPopup(bro) {
        await bro.click(clientObjects.common.toolbar.addUsersButton());
        await bro.yaWaitForVisible(clientObjects.common.participantsPopup.icon());
    },

    /**
     * Закрывает попап участников конференции
     *
     * @param {Object} bro
     */
    async closeParticipantsPopup(bro) {
        await bro.click(clientObjects.common.toolbar.addUsersButton());
        await bro.yaWaitForVisible(clientObjects.common.participantsPopup.icon());
    },

    /**
     * Выключает микрофон и камеру, кликая на кнопки в тулбаре
     *
     * @param {Object} bro
     */
    async turnOffDevicesFromToolbar(bro) {
        await bro.click(clientObjects.common.toolbar.audioButtonOff());
        await bro.yaWaitForVisible(clientObjects.common.toolbar.audioButtonOn());
        await bro.click(clientObjects.common.toolbar.videoButtonOff());
        await bro.yaWaitForVisible(clientObjects.common.toolbar.videoButtonOn());
    },

    /**
     * Выключает микрофон и камеру, кликая на кнопки в меню «Показать ещё»
     *
     * @param {Object} bro
     */
    async turnOffDevicesFromPopup(bro) {
        await openShowMorePopup(bro);
        await bro.click(clientObjects.common.showMorePopup.audioButtonOff());
        await bro.yaWaitForHidden(clientObjects.common.showMorePopup());
        await openShowMorePopup(bro);
        await bro.click(clientObjects.common.showMorePopup.videoButtonOff());
        await bro.yaWaitForHidden(clientObjects.common.showMorePopup());
    },

    /**
     * Включает демонстрацию экрана, кликая на кнопку в тулбаре
     *
     * @param {Object} bro
     */
    async startSharingFromToolbar(bro) {
        await bro.click(clientObjects.common.toolbar.sharingButton());
        await bro.yaWaitForVisible(clientObjects.common.toolbar.cancelSharingButton());
    },

    /**
     * Включает демонстрацию экрана, кликая на кнопку в меню «Показать ещё»
     *
     * @param {Object} bro
     */
    async startSharingFromPopup(bro) {
        await bro.click(clientObjects.common.showMorePopup.sharingButton());
        await bro.yaWaitForHidden(clientObjects.common.showMorePopup());
    },

    /**
     * Выключает демонстрацию экрана, кликая на кнопку в тулбаре
     *
     * @param {Object} bro
     */
    async cancelSharingFromToolbar(bro) {
        await bro.click(clientObjects.common.toolbar.cancelSharingButton());
        await bro.yaWaitForVisible(clientObjects.common.toolbar.sharingButton());
    },

    /**
     * Выключает демонстрацию экрана, кликая на кнопку в меню «Показать ещё»
     *
     * @param {Object} bro
     */
    async cancelSharingFromPopup(bro) {
        await bro.click(clientObjects.common.showMorePopup.cancelSharingButton());
        await bro.yaWaitForHidden(clientObjects.common.showMorePopup());
    },

    /**
     * Открывает чат из тулбара вместе с меню «Показать ещё»
     *
     * @param {Object} bro
     */
    async openChatFromToolbarWithShowMore(bro) {
        await bro.click(clientObjects.common.toolbar.chatButton());
        await bro.yaWaitForVisible(clientObjects.common.chatWidget());
        await openShowMorePopup(bro);
    },

    /**
     * Открывает чат из меню «Показать ещё» вместе с меню «Показать ещё»
     *
     * @param {Object} bro
     */
    async openChatFromPopupWithShowMore(bro) {
        await openChatFromPopup(bro);
        await openShowMorePopup(bro);
    },

    /**
     * Закрывает чат, кликая на кнопку закрытия чата
     *
     * @param {Object} bro
     */
    async closeChat(bro) {
        await bro.click(clientObjects.common.chatWidget.closeButton());
        await bro.waitUntil(
            () => bro.isVisibleWithinViewport(clientObjects.common.chatWidget()).then((isVisible) => !isVisible),
            5000,
            'Виджет чата не закрылся'
        );
    },

    /**
     * Включает запись экрана (для больших экранов с таймером)
     *
     * @param {Object} bro
     */
    async startRecordingWithTimer(bro) {
        await startRecording(bro);
        await bro.yaWaitForVisible(clientObjects.common.toolbar.cancelRecording.timer());
    },

    /**
     * Выключает запись экрана из тулбара
     *
     * @param {Object} bro
     */
    async cancelRecordingFromToolbar(bro) {
        await bro.click(clientObjects.common.toolbar.cancelRecording());
    },

    /**
     * Выключает запись экрана из меню «Показать ещё»
     *
     * @param {Object} bro
     */
    async cancelRecordingFromPopup(bro) {
        await bro.click(clientObjects.common.showMorePopup.cancelRecording());
        await bro.yaWaitForHidden(clientObjects.common.showMorePopup());
    },

    /**
     * Включает вид докладчика
     *
     * @param {Object} bro
     */
    async setPresenterView(bro) {
        await bro.click(clientObjects.common.showMorePopup.presenterViewButton());
        await bro.yaWaitForHidden(clientObjects.common.showMorePopup());
    },

    /**
     * Включает вид галереи
     *
     * @param {Object} bro
     */
    async setGalleryView(bro) {
        await bro.click(clientObjects.common.showMorePopup.galleryViewButton());
        await bro.yaWaitForHidden(clientObjects.common.showMorePopup());
    },

    /**
     * Открывает настройки
     *
     * @param {Object} bro
     */
    async openSettings(bro) {
        await bro.click(clientObjects.common.showMorePopup.settingsButton());
        await bro.yaWaitForVisible(clientObjects.common.settingsModal());
        await bro.pause(1000);
    },

    /**
     * Закрывает настройки
     *
     * @param {Object} bro
     */
    async closeSettings(bro) {
        await bro.click(clientObjects.common.settingsModal.closeButton());
        await bro.yaWaitForHidden(clientObjects.common.settingsModal());
        await bro.pause(1000);
    },

    /**
     * Выходит из конференции
     *
     * @param {Object} bro
     */
    async cancelCall(bro) {
        await bro.click(clientObjects.common.toolbar.cancelCallButton());
        await bro.yaWaitForVisible(clientObjects.common.ratingModal());
        await bro.pause(1000);
    },

    /**
     * Открывает модалку с настройками
     *
     * @param {Object} bro
     * @returns {Promise<void>}
     */
    async openSettingsFromAnywhere(bro) {
        // Дожидаемся либо показа кнопки Настроек, либо кнопки Ещё в тулбаре (тогда Настройки скрыты в меню)
        await bro.yaWaitForVisible(
            [clientObjects.common.settings(), clientObjects.common.toolbar.moreButton()].join(',')
        );

        const isSettingsButtonVisible = await bro.isVisible(clientObjects.common.settings());

        if (!isSettingsButtonVisible) {
            // Если кнопка "Настройки" не видна, то попробуем найти ее в тулбаре за кнопкой "Еще" (три точки)
            await bro.click(clientObjects.common.toolbar.moreButton());
            await bro.yaWaitForVisible(clientObjects.common.settings());
        }

        await bro.click(clientObjects.common.settings());
        await bro.yaWaitForVisible(clientObjects.common.settingsModal());
        await bro.pause(1000);
    }
};
