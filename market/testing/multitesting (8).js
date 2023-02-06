const fs = require('fs');
const path = require('path');
const url = require('url');

const properties = require('properties');

/**
 * @param {Object} props
 * @param {String[][]} propPaths
 */
function getHostByPropPaths(props, propPaths) {
    for (const propPath of propPaths) {
        const resultUrl = props[propPath];

        if (resultUrl !== undefined) {
            return url.parse(resultUrl).hostname;
        }
    }

    return null;
}

let rawProps;

try {
    const DATASOURCES_DIR = process.env.DATASOURCES_DIR || '/etc/yandex/market-datasources';
    rawProps = fs.readFileSync(path.join(DATASOURCES_DIR, 'datasources.properties')).toString();
} catch (e) {
    // Нет файла - нечего переопределять
    if (e.code !== 'ENOENT') {
        throw e;
    }
}

if (rawProps !== undefined) {
    try {
        const props = properties.parse(rawProps, {});

        module.exports.servant = {};

        // Не нашёл в etcd ничего окромя репорта из бэкендов вендоров.
        // Понадобится — сначала заведём там.
        const report = getHostByPropPaths(props, ['market.search.url']);
        if (report !== null) {
            module.exports.servant.report = {host: report};
        }
    } catch (e) {
        // Продолжаем работать, тут всё ок
        // eslint-disable-next-line no-console
        console.log(e);
    }
}

// Для демостендов ходим за статикой локально
process.env.STATIC_SELF = true;
