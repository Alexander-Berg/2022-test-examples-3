const fs = require('fs');
const path = require('path');
const url = require('url');

const properties = require('properties');
// eslint-disable-next-line no-restricted-modules
const _ = require('lodash');

/**
 * @param {Object} props
 * @param {String[][]} propPaths
 */
function getHostByPropPaths(props, propPaths) {
    // for-of, т.к. нужен ранний выход
    for (const propPath of propPaths) { // eslint-disable-line no-restricted-syntax
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

        const recommendations = getHostByPropPaths(props, [
            'recommendations.big.books.data.url',
            'recommendations.books.data.url',
            'recommendations.categories.root.data.url',
            'recommendations.daily.clicks.data.url',
            'recommendations.elliptics.url',
            'recommendations.filters.data.url',
        ]);

        if (recommendations !== null) {
            module.exports.servant.recommendations = {host: recommendations};
        }

        const shopInfo = _.get(props, ['marketshopinfo.host'], null);
        if (shopInfo !== null) {
            module.exports.servant.shopInfo = {host: shopInfo};
        }

        const checkouter = getHostByPropPaths(props, ['market.checkouter.client.url']);
        if (checkouter !== null) {
            module.exports.servant.checkouter = {host: checkouter};
        }

        const marketUtils = _.get(props, ['market.checkout.referee.host'], null);
        if (marketUtils !== null) {
            module.exports.servant.notifications = {host: marketUtils};
            module.exports.servant.comparison = {host: marketUtils};
            module.exports.servant.comments = {host: marketUtils};
        }

        const aboPublic = getHostByPropPaths(props, ['market.abo.public.url']);
        if (aboPublic !== null) {
            module.exports.servant.aboPublic = {host: aboPublic};
        }

        const persGrade = getHostByPropPaths(props, ['pers.grade.url']);
        if (persGrade !== null) {
            module.exports.servant.grades = {host: persGrade};
            module.exports.servant.userStorage = {host: persGrade};
        }

        const persBasket = _.get(props, ['market.pers.basket.host'], null);
        if (persBasket !== null) {
            module.exports.servant.wishlist = {host: persBasket};
        }

        const tarantino = getHostByPropPaths(props, ['videoreviews.tarantino.url']);
        if (tarantino !== null) {
            module.exports.servant.tarantino = {host: tarantino};
        }

        const mbo = _.get(props, ['mbo.lite.host'], null);
        if (mbo !== null) {
            module.exports.servant.mbo = {host: mbo};
        }

        const clickDaemon = getHostByPropPaths(props, ['market.click.url']);
        if (clickDaemon !== null) {
            module.exports.servant.clickDaemon = {host: clickDaemon};
        }

        const report = getHostByPropPaths(props, ['market.search.url']);
        if (report !== null) {
            module.exports.servant.report = {host: report};
        }
    } catch (e) {
        // Продолжаем работать даже при невалидных датасорсах
        // Логгер ещё не проициализировался, так что консоль
        // eslint-disable-next-line no-console
        console.log(e);
    }
}

// Для демостендов ходим за статикой локально
module.exports.hosts = {static: null};
