const clientObjects = require('../page-objects/client');

module.exports = {
    async closeSettings(bro) {
        await bro.click(clientObjects.common.settingsModal.closeButton());
        await bro.yaWaitForHidden(clientObjects.common.settingsModal());
    },

    async openMenuTab(bro, tabButton) {
        await bro.click(tabButton);
        await bro.pause(300);
    },

    async openSelect(bro, selectButton) {
        await bro.click(selectButton);
        // У компонента Select много ховеров. Чтобы не было проблем со скринами,
        // переводим курсор на заголовок и ждём, пока пропадут все ховеры.
        await bro.moveToObject(clientObjects.common.settingsModal.title());
        await bro.yaWaitForVisible(clientObjects.common.select());
        await bro.pause(1000);
    },

    async setSelectValue(bro, valueButton) {
        await bro.click(valueButton);
        await bro.yaWaitForHidden(clientObjects.common.select());
    },

    async changeTumblerValue(bro) {
        await bro.click(clientObjects.common.settingsModal.tumbler());
        await bro.pause(300);
    },

    async assertView(bro, assertName) {
        await bro.yaAssertView(assertName, 'body', {
            hideElements: [
                clientObjects.common.psHeader.user.unreadTicker(),
                clientObjects.common.psHeader.services(),
                clientObjects.common.settingsModal.cameraVideo(),
                clientObjects.common.videoOfParticipant.video(),
                clientObjects.common.videoOfParticipant.avatar()
            ]
        });
    },

    async clickAndAssertUrl(bro, element, expectedUrl, { newTab = false } = {}) {
        await bro.yaWaitForVisible(element);

        if (newTab) {
            await bro.yaClickAndAssertNewTabUrl(element, { linkShouldContain: expectedUrl });
        } else {
            await bro.click(element);
            await bro.waitUntil(
                () => bro.getUrl().then((url) => url.startsWith(expectedUrl)),
                5000,
                'Переход не произошёл'
            );
        }
    }
};
