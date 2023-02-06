const properties = require('properties');
const fs = require('fs');
const path = require('path');
const url = require('url');
const _ = require('lodash');

const MEMCACHED_SERVERS = 'partner-front-cache.tst.vs.market.yandex.net:11239';

/**
 * @param {Object} props
 * @param {String[][]} propPaths
 */
const findServantInProps = props => (propPaths = []) => {
    /* eslint-disable no-restricted-syntax */
    for (const propPath of propPaths) {
        if (propPath in props) {
            const {host, port, path: parsedPath} = url.parse(props[propPath]);

            return {host, port, path: parsedPath};
        }
    }
    /* eslint-enable no-restricted-syntax */

    return null;
};

let rawProps;

try {
    const DATASOURCES_DIR = process.env.DATASOURCES_DIR || '/etc/yandex/market-datasources';
    rawProps = fs.readFileSync(path.join(DATASOURCES_DIR, 'datasources.properties')).toString();
} catch (error) {
    // Нет файла - нечего переопределять
    if (error.code !== 'ENOENT') {
        throw error;
    }
}

if (typeof rawProps !== 'undefined') {
    try {
        const props = properties.parse(rawProps, {});
        const parseProps = findServantInProps(props);

        const servant = _.omitBy(
            {
                bunker: parseProps(['mbo.articles.bunker.api-url']),
                checkouter: parseProps(['market.checkouter.client.url']),
                checkouterSecure: parseProps(['market.checkouter.client.https.url']),
                aboPublic: parseProps(['market.abo.public.url']),
                blackbox: parseProps(['black.box.url', 'market.blackbox.url']),
                priceCenter: parseProps(['vendors.analytics.url']),
            },
            _.isNull,
        );

        exports.servant = servant;
    } catch (error) {
        // Продолжаем работать даже при невалидных датасорсах
        // Логгер ещё не проициализировался, так что консоль
        // eslint-disable-next-line no-console
        console.log(error);
    }
}

// Для демостендов ходим за статикой локально
exports.hosts = exports.hostsTld = {static: null};

exports.cache = {
    worker: {
        capacity: 10000,
    },
    shared: {
        locations: MEMCACHED_SERVERS.split(';'),
        generation: '3',
        idle: 20000,
        retry: 300,
        reconnect: 1000,
        minTimeout: 100,
        maxTimeout: 200,
    },
};

// На демостендах запоминаем cocon_commit_id в куку для более удобной проверки тикетов
exports.isCoconCommitAutosavesToCookie = true;
