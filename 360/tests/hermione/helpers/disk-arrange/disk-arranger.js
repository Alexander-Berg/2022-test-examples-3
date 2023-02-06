const _ = require('lodash');
const path = require('path');

const ERR_RESOURCE_ALREADY_EXISTS = 13;
const ERR_RESOURCE_NOT_FOUND = 71;

module.exports = class DiskArranger {
    constructor(client) {
        this.client = client;
    }

    /**
     * Создать папки в диске.
     * Все несуществующие папки в переданных путях также будут созданы.
     * Если переданный путь указывает на существующий ресурс, метод пропускает его.
     * Пример использования:
     *
     * await client.makeDirs(['/path/to/dir1', '/path/to/dir2']);
     *
     * @param {string[]} paths - список путей к папкам, которые необходимо создать
     * @returns {Promise<void>}
     */
    async makeDirs(paths) {
        paths = paths
            .map(this._getListOfParentPaths.bind(this))
            .reduce((list, pathList) => list.concat(pathList), []);

        paths = _.uniq(paths).map((path) => ({ id: '/disk' + path }));

        //группировка путей по количеству уровней вложенности
        //необходима чтобы создать всю цепочку папок глубокой вложенности
        const groups = [];
        paths.forEach((path) => {
            const slashesCount = (path.id.match(/\//g) || []).length;

            if (!groups[slashesCount]) {
                groups[slashesCount] = [];
            }

            groups[slashesCount].push(path);
        });

        //создаем все папки одного уровня вложенности за один запрос
        for (let i = 0; i < groups.length; i++) {
            if (!groups[i]) {
                continue;
            }

            const result = await this.client.request('do-resource-create-folder', groups[i]);

            const errors = this._getErrors(result, [ERR_RESOURCE_ALREADY_EXISTS]);
            if (errors.length) {
                throw new Error(this._getErrorString(errors));
            }
        }
    }

    /**
     * Удаление ресурсов (в корзину).
     * Если путь указывает на несуществующий ресурс, метод пропускает его.
     * Пример использования:
     *
     * await client.remove('/path/to/dir', '/path/to/file.txt');
     *
     * @param {string[]} paths - список путей к ресурсам, которые необходимо удалить
     * @returns {Promise<*>}
     */
    async remove(paths) {
        paths = paths.map((path) => ({ id: '/disk' + path }));

        const result = await this.client.request('do-resource-delete', paths);

        const errors = this._getErrors(result, [ERR_RESOURCE_NOT_FOUND]);
        if (errors.length) {
            throw new Error(this._getErrorString(errors));
        }

        const oids = result.models.filter((model) => !model.data.error).map((model) => model.data.oid);

        return this._waitOperationDone(oids);
    }

    /**
     * Очистка корзины. Удаляет все ресурсы, находящиеся в корзине.
     * Пример использования:
     *
     * await client.clearTrash();
     *
     * @returns {Promise<*>}
     */
    async clearTrash() {
        const result = await this.client.request('do-clean-trash');

        const errors = this._getErrors(result);
        if (errors.length) {
            throw new Error(this._getErrorString(errors));
        }

        const oids = result.models.filter((model) => !model.data.error).map((model) => model.data.oid);

        return this._waitOperationDone(oids);
    }

    /**
     * Очистить диск. Удаляет все элементы, находящиеся в корне диска и очищает корзину.
     * Пример использования:
     *
     * await client.clear();
     *
     * @returns {Promise<*>}
     */
    async clear() {
        const resources = await this.list('/disk');
        await this.remove(resources.map((resource) => resource.path.replace(/^\/disk/, '')));
        return this.clearTrash();
    }

    /**
     * Получить список элементов в директории. Возвращается в виде массива объектов Resource.
     * Пример использования:
     *
     * await client.list('/');
     *
     * @typedef {Object} Resource
     * @property {string} id
     * @property {number} ctime
     * @property {number} mtime
     * @property {number} utime
     * @property {Object} meta
     * @property {string} name - имя ресурса
     * @property {string} path - путь к ресурсу на диске
     * @property {'dir'|'file'} type - тип ресурса
     *
     * @param {string} path
     * @returns {Promise<Array<Resource>>}
     */
    async list(path) {
        let resources = [];
        let chunk = [];
        let offset = 0;
        const amount = 300;

        do {
            const result = await this.client.request('resources', {
                idContext: path,
                order: 1,
                sort: 'name',
                offset: offset,
                amount: amount
            });

            const errors = this._getErrors(result);
            if (errors.length) {
                throw new Error(this._getErrorString(errors));
            }

            if (result.models[0]) {
                chunk = result.models[0].data.resources;
                resources = resources.concat(chunk);
            }

            offset += amount;
        } while (chunk.length === amount);

        return resources;
    }

    /**
     * Сохранить файл на диске.
     * Внимение: физически файл на диск не загружается. Файл уже должен быть известен кладуну, чтобы он его
     * смог дедуплицировать и добавить на диск.
     *
     * Использование:
     * 1. Необходимо вручную загрузить все файлы, с которыми будет работать этот метод (чтобы они могли быть
     * дедуплицированы и настоящая загрузка не потребовалась), в отдельный аккаунт:
     * (login: yndx-ufo-test-archive-00, password: gfhjkm13)
     * 2. Необходимо посчитать хеши и размеры этих файлов и сохранить их в JSON.
     * (воспользоваться утилитой helpers/run-generate-hashes)
     * 3. Вызвать метод, передав ему список объектов UploadFile ({path, md5, sha256, size});
     *
     * @typedef {Object} UploadFile
     * @property {string} path - путь по которому будет сохранен файл.
     * @property {string} md5 - md5 хеш файла
     * @property {string} sha256 - sha256 хеш файла
     * @property {number} size - размер файла в байтах
     *
     * @param {Array<UploadFile>} files - список файлов
     * @returns {Promise<void>}
     */
    async store(files) {
        const dirpaths = files
            .map((file) => path.dirname(file.path))
            .filter((filepath) => filepath !== '/' && filepath !== '.');

        await this.makeDirs(dirpaths);

        const options = files.map((file) => ({
            dst: '/disk' + file.path,
            force: 0,
            md5: file.md5,
            sha256: file.sha256,
            size: file.size
        }));

        const result = await this.client.request('do-resource-upload-url', options);

        const errors = this._getErrors(result);
        if (errors.length) {
            throw new Error(this._getErrorString(errors));
        }

        const notHardlinked = result.models.filter((model) => model.data.status !== 'hardlinked');
        if (notHardlinked.length) {
            throw new Error('stored file is not hardlinked');
        }
    }

    /**
     * Получает список путей к родительским директориям.
     * '/path/to/dir' -> ['/path', '/path/to', '/path/to/dir']
     *
     * @param {string} path
     * @returns {Array}
     * @private
     */
    _getListOfParentPaths(path) {
        const parents = [];
        let current = '';

        path = this._trimSlashes(path);
        path.split('/').forEach((segment) => {
            parents.push(current = `${current}/${segment}`);
        });

        return parents;
    }

    /**
     * Удаляет ведущие и замыкающие слеши
     *
     * @param {string} path
     * @returns {string}
     * @private
     */
    _trimSlashes(path) {
        return path.trim().replace(/^\/+|\/+$/g, '');
    }

    /**
     * Возвращает объект результата асинхронной операции
     *
     * @param {Array<number>} oids - список идентификаторов операций
     * @returns {Promise<*>}
     * @private
     */
    async _getOperationStatus(oids) {
        const result = await this.client.request('do-status-operation', oids.map((oid) => ({ oid: oid })));
        const errors = result.models.filter((model) => _.get(model, 'data.error')).map((model) => model.data.error);

        if (errors.length) {
            throw new Error(this._getErrorString(errors));
        }

        return result;
    }

    /**
     * Возвращает промис, ожидающий удачного завершения списка переданных асинхронных операций.
     *
     * @param {Array<number>} oids - список идентификаторов операций
     * @param {number} timeout - таймаут ожидания
     * @param {number} interval - интервал проверки завершенности
     * @returns {Promise<*>}
     * @private
     */
    async _waitOperationDone(oids, timeout = 10000, interval = 750) {
        const startTime = Date.now();

        if (!oids.length) {
            return Promise.resolve();
        }

        return new Promise((resolve, reject) => {
            const makeCheck = async() => {
                const result = await this._getOperationStatus(oids);

                const statuses = result.models.map((model) => model.data.status);
                const isDone = statuses.every((status) => status === 'DONE');
                const isFailed = statuses.every(
                    (status) => status === 'FAILED' || status === 'ABORTED' || status === 'REJECTED'
                );

                if (isFailed) {
                    reject('disk arranger: async disk operation failed');
                } else if (isDone) {
                    resolve();
                } else if (startTime + timeout < Date.now()) {
                    reject('disk arranger: async disk operation timeout');
                } else {
                    setTimeout(makeCheck, interval);
                }
            };

            setTimeout(makeCheck, interval);
        });
    }

    /**
     * Извлекает список объектов ошибок из результата выполнения запроса
     *
     * @param {Object} response - ответ от сервера
     * @param {Array<number>} ignoreCodes - коды ошибок, которые нужно игнорировать
     * @returns {*}
     * @private
     */
    _getErrors(response, ignoreCodes = []) {
        if (!response.models) {
            throw new Error('response does not have models property');
        }

        return response.models
            .filter(
                (model) => _.get(model, 'data.error') && !ignoreCodes.includes(_.get(model, 'data.error.body.code'))
            )
            .map((model) => model.data.error);
    }

    /**
     * Формирует строковое описание ошибок по переданному списку объектов ошибок.
     * Если ошибок не обнаружено, возвращает false.
     *
     * @param {Object[]} errors
     * @param {number} errors[].id
     * @param {string} errors[].message
     * @param {Object} [errors[].body]
     * @param {number} [errors[].body.code]
     * @param {string} [errors[].body.title]
     *
     * @returns {string|boolean}
     * @private
     */
    _getErrorString(errors) {
        const errorMsgs = [];

        errors.forEach((error) => {
            let msg = `${error.id} ${error.message}`;
            msg += error.body ? ` code ${error.body.code}: ${error.body.title}` : '';
            errorMsgs.push(msg);
        });

        return errorMsgs.length ? errorMsgs.join('\n') : false;
    }
};
