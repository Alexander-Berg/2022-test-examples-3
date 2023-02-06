const { pick } = require('lodash');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

/**
 * Получает список ресурсов
 *
 * @param {Browser} bro Browser
 * @returns {Promise<any[]>} resources
 */
async function getResources(bro) {
    return (await bro.executeAsync((amount, done) => {
        window.rawFetchModel('resources', {
            idContext: ns.page.current.params.idContext,
            offset: 0,
            amount,
            sort: 'mtime',
            order: '1'
        }).then(
            ({ resources }) => done(resources),
            () => done()
        );
    }, 500));
}

/**
 * Удаляет ресурсы
 *
 * @param {Browser} bro Browser
 * @param {any[]} resources
 * @returns {string[]} Массив с именами удаленных ресурсов
 */
async function deleteResources(bro, resources) {
    const result = await bro.executeAsync((resources, done) => {
        const wait = () => new Promise((resolve) => setTimeout(() => resolve(), 2000));

        const checkOperation = (name) => ([{ data: { oid } }]) => {
            const checkState = (pollCount = 10) => {
                if (!pollCount) {
                    return null;
                }

                return wait().then(() => {
                    return window.rawFetchModel('do-status-operation', { oid });
                }).then(([{ data: { state } }]) => {
                    if (state === 'COMPLETED') {
                        return name;
                    } else {
                        return checkState(pollCount - 1);
                    }
                }).catch(() => null);
            };

            return checkState();
        };

        const deleteResource = ({ id, name }) =>
            window.rawFetchModel('do-resource-delete', { id })
                .then(checkOperation(name)).catch(() => null);

        Promise.all(resources.map(deleteResource))
            .then((names) => done(names.filter(Boolean)))
            .catch(() => done([]));
    }, resources.map((resource) => pick(resource, ['id', 'name'])));

    return result;
}

/**
 * Получает список альбомов
 *
 * @param {Browser} bro Browser
 * @returns {Promise<any[]>} albums
 */
async function getAlbums(bro) {
    return (await bro.executeAsync((done) => {
        window.rawFetchModel('albums', { }).then(
            (data) => done(data),
            () => done()
        );
    }));
}

/**
 * Удаляет альбомы
 *
 * @param {Browser} bro Browser
 * @param {any[]} albums
 * @returns {string[]} Массив с именами удаленных альбомов
 */
async function deleteAlbums(bro, albums) {
    const results = (await Promise.all(albums.map(({ title, id }) =>
        bro.executeAsync((id, title, done) => {
            window.rawFetchModel('do-remove-album', { id }).then(
                () => done(title),
                () => done()
            );
        }, id, title)
    )));

    if (results.some((name) => !name)) {
        throw new Error('Не все альбомы были удалены');
    }

    return results;
}

module.exports = {
    getResources,
    deleteResources,
    getAlbums,
    deleteAlbums
};
