const rememberBlock = require('../page-objects/client-remember-block').common;
const { assert } = require('chai');

const actions = {
    common: {
        /**
         * Проверяет, есть ли ресурс с именем resourceName в списке фотографий блока воспоминаний.
         * Проверяется по ссылке на downloader
         *
         * @param {string} resourceName - имя ресурса
         * @returns {Promise<boolean>}
         */
        async yaRememberBlockHasResource(resourceName) {
            const resources = (await this.yaRememberBlockGetResourcesNames());
            return resources.includes(resourceName);
        },
        /**
         * Возвращает массив имён ресурсов в блоке
         *
         * @returns {Promise<string[]>}
         */
        async yaRememberBlockGetResourcesNames() {
            return (await this.execute((selector) => {
                return Array.from(document.querySelectorAll(selector)).map((resource) => resource.title);
            }, rememberBlock.resource()));
        },
        /**
         * Проверка на наличие ресурса в списке фото блока воспоминаний
         *
         * @param {string} resourceName
         * @returns {Promise<void>}
         */
        async yaAssertRememberBlockHasResource(resourceName) {
            const hasResource = await this.yaRememberBlockHasResource(resourceName);
            assert(hasResource === true, `Ресурса ${resourceName} нет в блоке воспоминаний`);
        },
        /**
         * Проверка на отсутсвие ресурса в списке фото блока воспоминаний
         *
         * @param {string} resourceName
         * @returns {Promise<void>}
         */
        async yaAssertRememberBlockHasNotResource(resourceName) {
            const hasResource = await this.yaRememberBlockHasResource(resourceName);
            assert(hasResource === false, `Ресурс ${resourceName} есть в блоке воспоминаний`);
        },

        /**
         * @param {string} touchTestPalmId
         * @param {string} desktoptestPalmId
         * @param {Object} block
         */
        async loginAndGoToUrl(touchTestPalmId, desktoptestPalmId, block) {
            const isMobile = await this.yaIsMobile();
            this.testpalmId = isMobile ? touchTestPalmId : desktoptestPalmId;

            await this.yaClientLoginFast(block.user);
            await this.url(block.url);
            await this.yaWaitPreviewsLoaded(rememberBlock.wowResource.preview());
        }
    }
};

module.exports = exports = actions;
