const PageObjects = require('../page-objects/public');
const WAITING_AUTH_TIMEOUT = 15000;

/**
 * Ожидаем что по окончанию сбора авторизации пользователь будет НЕавторизован
 */
async function yaPublicWaitUnauthorizedInAllDomains() {
    await this.yaWaitForVisible(
        PageObjects.loginLinkButton(),
        WAITING_AUTH_TIMEOUT,
        'Сбор авторизации не завершился за отведённое время'
    );
}

/**
 * авторизация первым юзером
 */
async function yaPublicFirstUserAuthorize({ login, password }) {
    await this.yaClick(PageObjects.loginLinkButton());
    await this.login({ login, password });
    await this.yaWaitForVisible(
        PageObjects.legoUser.userPic(),
        WAITING_AUTH_TIMEOUT,
        'Не дождались появления аватарки пользователя после авторизации'
    );
}

module.exports = {
    yaPublicWaitUnauthorizedInAllDomains,
    yaPublicFirstUserAuthorize
};
