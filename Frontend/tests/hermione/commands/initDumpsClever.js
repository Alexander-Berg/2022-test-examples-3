'use strict';

/**
 * Добавляет путь для сохранения моков
 * @this {Object}
 * @param {String} [slug] Короткое, уникальное в рамках файла, имя
 * @param {Object} [cookieOptions] Параметры устанавливаемой куки
 * @param {String} [cookieOptions.name] Имя куки
 * @param {String} [cookieOptions.domain] Домен, в котором она доступна
 * @param {String} [cookieOptions.path] Путь, в котором она доступна
 * @param {String} [folderName] Название папки, в которой лежат дампы
 */
module.exports = async function initDumpsClever(slug = '', cookieOptions = {
    name: 'dumps_test_path',
    domain: '.yandex-team.ru',
    path: '/',
}, folderName) {
    const test = this.executionContext;

    if (!test || !test.file) {
        throw new Error('Command "initDumps" should be run in test context (in "it" function body).');
    }

    const options = await this.getCommandsOptions();

    if (slug) {
        test.ctx.slug = slug;
    }

    const fullPath = options.createDataPath(folderName, test);
    const testPath = (
        fullPath.indexOf(options.rootDir) === 0 ?
            `.${fullPath.slice(options.rootDir.length)}` :
            fullPath
    );

    return this.setCookie({
        ...cookieOptions,
        value: testPath,
    });
};
