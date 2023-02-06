const { MS_IN_HOUR, prefixes } = require('./helpers/consts');
const { defaults } = require('lodash');
const { NAVIGATION } = require('../hermione/config').consts;
const KEEP_HOURS = Number(process.env.KEEP_HOURS) * MS_IN_HOUR || undefined;
const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('./config/login'));

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 * @typedef { import('./helpers/types').ArtifactConfig } ArtifactConfig
 * @typedef { import('./helpers/types').FilterOptions } FilterOptions
 * @typedef { import('./helpers/types').CleanerHandler } CleanerHandler
 * @typedef { import('./helpers/types').CleanerConfig } CleanerConfig
 */

/**
 * Enum for cleaner types
 *
 * @readonly
 * @enum {string}
 */
const consts = {
    FILES: 'listing',
    TRASH: 'trash',
    FAST_TRASH: 'fast_trash',
    RESTORE_TRASH: 'restore_trash',
    ALBUMS: 'albums',
    PHOTOSLICE_RENAME: 'photoslice_rename',
    MOVE_PHOTOS: 'move_photos',
    OVERDRAFT_HARD: 'overdraft_hard',
    OVERDRAFT_LITE: 'overdraft_lite',
};

const overdraftHandler = require('./handlers/overdraft');

/**
 * Cleaner-handler mapping
 *
 * @constant
 * @type {Map<string, CleanerHandler>}
 */
const cleanersMap = new Map([
    [consts.FILES, require('./handlers/listing')],
    [consts.TRASH, require('./handlers/trash')],
    [consts.FAST_TRASH, require('./handlers/fast_trash')],
    [consts.RESTORE_TRASH, require('./handlers/restore_trash')],
    [consts.ALBUMS, require('./handlers/albums')],
    [consts.PHOTOSLICE_RENAME, require('./handlers/photoslice_rename')],
    [consts.MOVE_PHOTOS, require('./handlers/move_photos')],
    [consts.OVERDRAFT_HARD, overdraftHandler.hard],
    [consts.OVERDRAFT_LITE, overdraftHandler.lite],
]);

/**
 * Artifact-filter mapping
 *
 * @constant
 * @type {Map<string, Object>}
 */
const defaultArtifactFilterMap = new Map([
    [consts.FILES, { byName: prefixes.TMP }]
]);

/**
 * Constructs filter
 *
 * @param {ArtifactConfig} artifact
 * @param {CleanerConfig} config
 * @returns {FilterOptions}
 */
function constructFilters(artifact, config) {
    const defaultFilter = {
        // если не передать переменную окружения `KEEP_HOURS`, то
        // очистятся все временные ресурсы старше часа
        beforeDate: KEEP_HOURS ? new Date(Date.now() - KEEP_HOURS) : new Date(Date.now() - MS_IN_HOUR)
    };

    const defaultArtifactFilter = defaultArtifactFilterMap.get(artifact.name) || {};

    return defaults(artifact.filter, config.filter, defaultArtifactFilter, defaultFilter);
}

/**
 * Функция для очистки артефакта
 *
 * @param {Browser} bro
 * @param {ArtifactConfig} artifact
 * @param {FilterOptions} filter
 */
async function artifactCleaner(bro, artifact, filter) {
    const cleanerFn = cleanersMap.get(artifact.name);

    if (cleanerFn) {
        if (artifact.beforeClean && typeof artifact.beforeClean === 'function') {
            await artifact.beforeClean(bro);
        }

        if (artifact.url) {
            await bro.url(artifact.url);
        } else {
            await bro.url(NAVIGATION.disk.url);
        }

        await cleanerFn(bro, { filter });

        if (artifact.afterClean && typeof artifact.afterClean === 'function') {
            await artifact.afterClean(bro);
        }
    }
}

/**
 * Функция очистки пользователя
 *
 * @param {Browser} bro
 * @param {CleanerConfig} config
 */
async function userCleaner(bro, config) {
    /**
     * @param {string | ArtifactConfig} artifact
     * @returns {ArtifactConfig}
     */
    const getArtifact = (artifact) => typeof artifact === 'string' ? { name: artifact } : artifact;

    if (config.beforeClean && typeof config.beforeClean === 'function') {
        await config.beforeClean(bro);
    }

    for (const art of config.artifacts) {
        const artifact = getArtifact(art);
        const filter = constructFilters(artifact, config);

        await artifactCleaner(bro, artifact, filter);
    }

    if (config.afterClean && typeof config.afterClean === 'function') {
        await config.afterClean(bro);
    }
}

/**
 * @param {string} category - категория задачи
 * @param {CleanerConfig[]} configs - список конфигураций
 */
function taskRunner(category, configs) {
    describe(category, () => {
        configs.forEach(({ users, ...config }) => {
            users.forEach((user) => {
                it(`${config.testId} - ${user}`, async function() {
                    this.browser.executionContext.timeout(1000000); // При большом количестве файлов - таймаутит
                    const bro = this.browser;
                    await bro.yaClientLoginFastUser(getUser(user));
                    await userCleaner(bro, { user, ...config });
                });
            });
        });
    });
}

module.exports = { consts, taskRunner };
