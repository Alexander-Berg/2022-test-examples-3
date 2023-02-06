const cookiesHelper = require('./cookies-helper');
const url = require('url');
const ask = require('asker-as-promised');

/**
 * Модуль инкапсулирует логику запроса и получения моделей от фронтбека.
 */

class DiskClient {
    /**
     * @param {Object} config
     * @param {string} config.modelsUrl - URL для запроса клиентских носкриптовых моделей
     * @param {Object} config.authCookies - Объект с куки для авторизации (ключ - имя куки, значение - значение куки)
     * @param {Object} config.sk - параметр secretKey, полученный от сервера
     * @param {string} [config.idClient] - идентификатор клиента, по умолчанию генерируется рандомно
     */
    constructor(config = {}) {
        if (!config.modelsUrl) {
            throw new Error('not passed config.modelsUrl');
        }
        if (!config.authCookies) {
            throw new Error('not passed config.authCookies');
        }
        if (!config.sk) {
            throw new Error('not passed config.sk');
        }

        this.modelsUrl = url.parse(config.modelsUrl);
        this.authCookies = config.authCookies;

        this.queryExtraParams = {
            idClient: config.idClient || String(Math.random().toFixed(16)).substr(2),
            sk: config.sk
        };
    }

    /**
     * Посылает запрос к ручке фронтбека
     *
     * @param {string} modelName - имя модели
     * @param {Object|Object[]} paramSets - список параметров запроса
     * @returns {Promise<any>}
     */
    async request(modelName, paramSets = {}) {
        if (!Array.isArray(paramSets)) {
            paramSets = [paramSets];
        }

        const options = {
            ...this.modelsUrl,
            method: 'POST',
            query: {
                _m: modelName
            },
            body: {
                ...this.queryExtraParams,
                ...this._toQueryParams(paramSets, modelName)
            },
            bodyEncoding: 'urlencoded',
            headers: cookiesHelper.writeCookies(this.authCookies),
            rejectUnauthorized: false,
            timeout: 2000
        };

        const response = await ask(options);
        let body;

        try {
            body = JSON.parse(response.data.toString());
        } catch (error) {
            throw new Error('unable to parse response body: ' + error.message);
        }

        return body;
    }

    /**
     * Преобразовывает массив наборов параметров в объект, вынося индекс набора параметров в имя свойства
     * Это соглашение по передаче параметров между носкриптом и сервером.
     *
     * @param {Object[]} paramSets - наборы параметров для запроса
     * @param {string} modelName - имя модели
     * @returns {Object}
     * @private
     */
    _toQueryParams(paramSets, modelName) {
        const result = {};

        for (let i = 0; i < paramSets.length; i++) {
            const paramsSet = paramSets[i];
            for (const paramName in paramsSet) {
                result[`${paramName}.${i}`] = paramsSet[paramName];
            }
            result[`_model.${i}`] = modelName;
        }

        return result;
    }
}

/**
 * Создает объект клиента, получает для него дополнительные параметры sk и version,
 * и возвращает клиент.
 *
 * @param {Object} authCookies - объект с куками для авторизации
 * @param {string} modelsUrl - URL для запроса клиентских носкриптовых моделей
 * @returns {Promise<DiskClient>}
 */
module.exports = async function getClient(authCookies, modelsUrl) {
    const client = new DiskClient({
        modelsUrl: modelsUrl,
        authCookies: authCookies,
        sk: 'invalid'
    });

    const result = await client.request('do-get-user-info');

    Object.assign(client.queryExtraParams, {
        sk: result.sk,
        version: result.version
    });

    return client;
};
