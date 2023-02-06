/**
 * Скрыть экранную клавиатуру
 *
 */
module.exports = async function yaHideDeviceKeyboard(context: WebdriverIO.ItDefinitionCallbackCtx) {
    const { browserId } = context.currentTest.browserId;

    if (browserId !== 'chrome-phone' && browserId !== 'chrome-pad') {
        return;
    }

    await this.hideDeviceKeyboard();
};
