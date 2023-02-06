const url = require('url');
const querystring = require('querystring');
const abtFlags = require('../abt-flags');
const PO = require('../page-objects');

function convertAbtFlagsToString(abtFlags) {
    return Object.keys(abtFlags)
        .map(currentValue => `${currentValue}:${abtFlags[currentValue]}`)
        .join(';');
}

function injectQueryToUrl(urlObject, query) {
    urlObject.query = Object.assign({}, querystring.parse(urlObject.query), query);
    delete urlObject.search;
}

function getSkeleton(path = '') {
    /**
     * Синхронизировать с src/index.tsx
     */
    const [urlPart] = path.match(/details|search|month|region/) || [];
    let skeleton;

    switch (urlPart) {
        case 'search':
            skeleton = PO.search.Skeleton;
            break;
        case 'region':
            skeleton = PO.region.Skeleton;
            break;
        case 'month':
            skeleton = PO.month.Skeleton;
            break;
        case 'details':
            skeleton = PO.details.Skeleton;
            break;
        default:
            skeleton = PO.index.Skeleton;
    }

    return skeleton;
}

/**
 * Открывает страницу, ждём пока будет загружен переданный селектор
 * @param  {String} path
 * @param  {String} waitForVisibleSelector
 * @returns {*}
 */
module.exports.ywOpenPage = function ywOpenPage(path, { query = {}, lang = {} }) {
    const servicePath = ('/pogoda-dataprod/' + path).replace(/\/{2,}/g, '/');
    const urlObject = url.parse(servicePath);
    const { showmethehamster } = query;

    if (lang.yaConf) {
        query.yaConf = lang.yaConf;
    }

    const flags = Object.assign(
        {
            is_autotest: 1,
            wproxy: 'dataprod'
        },
        abtFlags,
        showmethehamster
    );

    const flagsAsString = convertAbtFlagsToString(flags);

    if (flagsAsString) {
        query.showmethehamster = flagsAsString;
    }

    injectQueryToUrl(urlObject, query);

    let urlString = url.format(urlObject);

    let chain = this
        .url(urlString)
        .ywSetYSFont()
        .ywWaitForVisible(lang.lang ? `html[lang=${lang.lang}]` : 'html[lang=ru]', 10000);

    if (!(showmethehamster && showmethehamster.show_skeleton)) {
        chain = chain
            /**
             * тут ждем пока прогрузятся данные
             *
             * можно прокачать, добавив проверку на показ попапа ошибки
             * если есть попап, то ждать не надо, нужно бросить фейл
             */
            .ywWaitForVisible(getSkeleton(path), 10000, undefined, true);
    }

    return chain;
};

/* для ручных тестов, не для CI */
module.exports.ywOpenMeteumPage = function ywOpenMeteumPage(path, query = {}) {
    const servicePath = ('/' + path).replace(/\/{2,}/g, '/');
    const urlObject = url.parse(servicePath);
    const { showmethehamster } = query;

    const flags = Object.assign(
        { is_autotest: 1 },
        showmethehamster
    );

    const flagsAsString = convertAbtFlagsToString(flags);

    if (flagsAsString) {
        query.showmethehamster = flagsAsString;
    }

    injectQueryToUrl(urlObject, query);

    let urlString = url.format(urlObject);

    let chain = this
        .url(urlString)
        .ywSetYSFont()
        .ywWaitForVisible(`html[lang=${query.lang}]`, 10000)
        .ywWaitForVisible(getSkeleton(path), 10000, undefined, true);

    return chain;
};
