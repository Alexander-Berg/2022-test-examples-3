const popups = require('../page-objects/client-popups');
const consts = require('../config').consts;

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

/**
 * Проверяет, что появился стандартный прогрессбар
 *
 * @this Browser
 * @returns {Promise<Browser>}
 */
const yaAssertProgressBarAppeared = function() {
    return this
        .yaWaitForVisible(popups.common.operationsProgress(), 'Прогрессбар удаления не появился');
};

/**
 * Проверяет, что стандартный прогрессбар исчез
 *
 * @this Browser
 * @returns {Promise<Browser>}
 */
const yaAssertProgressBarDisappeared = async function() {
    await this.$(popups.common.operationsProgress()).waitForDisplayed({
        timeout: consts.FILE_OPERATIONS_TIMEOUT,
        timeoutMsg: 'Прогрессбар удаления не исчез',
        reverse: true
    });
};

module.exports = {
    yaAssertProgressBarAppeared,
    yaAssertProgressBarDisappeared
};
