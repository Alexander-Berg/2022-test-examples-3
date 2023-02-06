const path = require('path');
const os = require('os');
const fs = require('fs');
const https = require('https');
const util = require('util');
const url = require('url');
const assert = require('chai').assert;
const { calculateHash, removeRecursive } = require('../helpers/files-helper');
const popups = require('../page-objects/client-popups');
const common = require('../page-objects/client-common').common;
const consts = require('../config').consts;

const mkdtemp = util.promisify(fs.mkdtemp);

const actions = {
    desktop: {},
    touch: {},
    common: {
        /**
         * Получает URL для скачивания файла, путем выбора переданного файла по имени
         * и нажатия кнопки download
         * Будет получать ссылку на файл из скрытого iframe.
         *
         * @param {string} resourceName
         * @returns {Promise<string>}
         */
        yaGetDownloadUrl(resourceName) {
            return this.yaGetDownloadUrlUsing(resourceName);
        },
        /**
         * Выполняет клик на кнопке для скачивания и возвращает ссылку на скачиваемый файл
         * извлекая ее из невидимого iframe
         *
         * @param {boolean} [clickOnDownload]
         * @returns {Promise<string>}
         * @private
         */
        async retrieveUrlFromIframe(clickOnDownload = true) {
            const waitTimeout = 2000;
            const waitCheckInterval = 100;

            if (clickOnDownload) {
                const isMobile = await this.yaIsMobile();
                await this.click(isMobile ? popups.touch.actionBar.downloadButton() :
                    popups.desktop.actionBar.downloadButton());
            }

            let url;

            await this.waitUntil(async() => {
                const iframe = await this.$(common.downloadIframe());

                if (!(await iframe.isExisting())) {
                    return false;
                }

                url = await iframe.getProperty('src');

                return true;
            }, {
                timeout: waitTimeout,
                timeoutMsg: 'IFrame для скачивания не создался',
                interval: waitCheckInterval
            });

            return url;
        },
        /**
         * Получает URL для скачивания файла, путем выбора переданного файла по имени и нажатия кнопки download
         *
         * @param {string} resourceName имя ресурса
         * @returns {Promise<string>}
         */
        async yaGetDownloadUrlUsing(resourceName) {
            await this.yaSelectResource(resourceName);
            await this.yaWaitActionBarDisplayed();
            return this.retrieveUrlFromIframe();
        },
        /**
         * Проверяет что хеш файла, находящегося по переданному пути равен переданному хешу
         *
         * @param {string} filePath
         * @param {string} expectedHash
         * @returns {Promise<void>}
         */
        async yaAssertFileHashEquals(filePath, expectedHash) {
            const calculatedHash = await calculateHash(filePath, consts.TEST_FILES_HASH_ALGORITHM);
            assert.equal(calculatedHash, expectedHash, 'хеш файлов различается (файлы не совпадают)');
        },

        /**
         * Вызывает скачивание файла по переданному URL
         *
         * @param {string} fileUrl - URL по которому будет скачиваться файл
         * @param {string} fileName - имя, которым будет назван скачиваемый файл
         * @param {Object} assertHeaders - хеш из имен заголовков и их ожидаемых значений,
         *                                 которые должен содержать ответ от сервера
         * @returns {Promise<Object>}
         */
        yaDownloadFileFromUrl(fileUrl, fileName, assertHeaders = {}) {
            return new Promise(async(resolve, reject) => {
                const tempDirectoryPath = await mkdtemp(path.join(os.tmpdir(), 'download-'));
                const tempFilePath = path.join(tempDirectoryPath, fileName);

                const fileStream = fs.createWriteStream(tempFilePath, { encoding: 'binary' });
                const request = https.get({
                    ...(url.parse(fileUrl)),
                    rejectUnauthorized: false
                }, (response) => {
                    Object.keys(assertHeaders).forEach((key) => {
                        key = key.toLowerCase();
                        try {
                            assert.equal(
                                response.headers[key], assertHeaders[key], `получен неверный заголовок ${key}`
                            );
                        } catch (error) {
                            reject(error);
                        }
                    });

                    response.pipe(fileStream);
                    response.on('end', () => {
                        resolve({
                            path: tempFilePath,
                            cleanUpFn: cleanUp
                        });
                    });
                });

                /**
                 * @param {Error} error
                 */
                function onError(error) {
                    fileStream.end();
                    request.abort();

                    cleanUp()
                        .then(() => reject(new Error(`yaDownloadFileFromUrl: ${error.message}`)))
                        .catch(
                            (error) => reject(new Error(`yaDownloadFileFromUrl ошибка при очистке: ${error.message}`))
                        );
                }

                /**
                 * @returns {Promise<void>}
                 */
                function cleanUp() {
                    return removeRecursive(tempDirectoryPath);
                }

                fileStream.on('error', onError);
                request.on('error', onError);
            });
        }
    }
};

module.exports = exports = actions;
