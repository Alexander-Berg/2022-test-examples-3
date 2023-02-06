/**
 * Открытие мессенджера и групового чата
 *
 * @returns {Promise}
 */

module.exports = async function yaOpenMessengerAndGroupChat(this: WebdriverIO.Browser) {
    await this.yaOpenMessenger({
        build: 'yamb',
    });

    // Открываем окно создания чата
    await this.waitForVisible(PO.chatListWriteBtn());
    await this.click(PO.chatListWriteBtn());

    // Открываем окно создания групового чата
    await this.waitForVisible(PO.createGroupChatBtn());
    await this.click(PO.createGroupChatBtn());

    await this.waitForVisible(PO.createGroupChat(), 'Окно создания группового чата не появилось');
};
