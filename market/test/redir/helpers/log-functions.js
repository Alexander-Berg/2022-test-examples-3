const os = require('os');
const url = require('url');

const getBrowser = require('./../utils/get-browser');

const badSymRE = /(\t|\n|\r|\0|\\|")/g;
const badSymbols = {
    '\n': '\\n',
    '\t': '\\t',
    '\r': '\\r',
    '\0': '\\0',
    '\\': '\\\\',
    '"': '\\"',
};

const IZNANKA_AFF_ID = 1651;
const hostname = os.hostname().replace(/([^\.])\..*/, '$1');

/**
 * Clear input string from bab symbols
 * @param {String} value
 */
function sanitizeValue(value) {
    if (typeof value === 'string') {
        value = replaceToken(value).replace(badSymRE, (sym) => {
            return badSymbols[sym];
        });
    } else if (typeof value === 'boolean') {
        value = value ? 1 : 0;
    } else if (Array.isArray(value)) {
        value = sanitizeValue(value.toString());
    }

    return value;
}

/**
 * Get IP from request
 * @param {Object} req
 * @return {String}
 */
function getIp(req) {
    return (
        req.headers['x-forwarded-for'] ||
        req.connection.remoteAddress ||
        req.socket.remoteAddress ||
        req.connection.socket.remoteAddress
    );
}

/**
 * Replace sensitive data, as user tokens
 * @param {String} value
 * @return {String}
 */
function replaceToken(value) {
    if (typeof value !== 'string') {
        return value;
    }

    const exceptions = ['access_token', 'bearer_token', 'auth_token', 'bearer', 'token'];

    const regex = new RegExp(
        exceptions.map((exception) => `((?<=${exception}=)(.*?)(?=&|"|\n|$))`).join('|'),
        'g',
    );

    const TOKEN_REPLACER = 'TOKEN_WAS_HERE';

    return value.replace(regex, TOKEN_REPLACER);
}

/**
 * Get certain fields from query and make log object
 * @param {Object} query
 * @return {Object}
 */
function getLogsFromQuery(query) {
    const fields = [
        'clid',
        'aff_id',
        'client_id',
        'url',
        'transaction_id',
        'model_id',
        'page_price',
        'price',
        'page_rating',
        'rating',
        'page_grade_count',
        'grade_count',
        'target',
        'discount',
        'click_type',
        'click_type_details',
        'offer_id',
        'shop_id',
        'offer_direct_domain',
        'offer_direct_url',
        'type_sovetnik',
        'type',
        'button',
        'from_button',
        'flight_price',
        'flight_intent',
        'flight_forward_changes',
        'flight_backward_changes',
        'v',
        'show_type',
        'bid_pp',
        'usaas_exp_boxes',
    ];

    let logs = fields.reduce((prev, field) => {
        if (query.hasOwnProperty(field)) {
            prev[field] = query[field];
        }
        return prev;
    }, {});

    try {
        const ab = query.ab ? JSON.parse(query.ab) : {};
        logs = Object.assign(logs, ab);
    } catch (ex) {
        //
    }

    return logs;
}

/**
 * Prepare log object from req
 * @param {Object} req
 * @return {Object}
 */
function prepareLogData(req) {
    const { ref, aff_id: affId } = req.query;
    const logs = {
        host: hostname,
        unixtime: Math.round(Date.now() / 1000),
    };

    logs.referrer = req.headers.referer || req.headers.referrer || req.headers.origin || ref;

    if (affId == IZNANKA_AFF_ID) {
        logs.referrer = ref || logs.referrer;
    }

    if (req.headers['user-agent']) {
        const browser = getBrowser(req.headers['user-agent']).name || 'other';
        logs.useragent = req.headers['user-agent'];
        logs.browser = browser;
    }

    const ip = getIp(req);
    if (ip) {
        logs.ip = ip;
    }

    if (logs.referrer) {
        logs.domain = url.parse(logs.referrer).hostname;
    }

    if (req.cookies.yandex_login) {
        logs.yandex_login = req.cookies.yandex_login;
    }

    if (req.cookies.yandexuid) {
        logs.yandexuid = req.cookies.yandexuid;
    }

    return logs;
}

/**
 * Make tskv log string from object
 * @param {Object} logs
 * @param {Boolean} sendToNR
 * @return {String}
 */
function makeLogString(logs) {
    return Object.keys(logs)
        .map((key) => [key, sanitizeValue(logs[key])])
        .filter(([, value]) => {
            // Slow filter, because we dont want to remove values like [0, false, '0']

            // Remove empty strings and arrays
            if ((typeof value === 'string' || Array.isArray(value)) && !value.length) {
                return false;
            }

            // Remove trash values
            if (value === null || value === undefined) {
                return false;
            }

            // Remove empty objects {}
            if (typeof value === 'object' && !Object.keys(value).length) {
                return false;
            }
            if (Number.isNaN(value)) {
                return false;
            }
            return true;
        })
        .map(([key, value]) => {
            return `${key}=${value}`;
        })
        .join('\t');
}

module.exports = {
    prepareLogData,
    sanitizeValue,
    getIp,
    replaceToken,
    getLogsFromQuery,
    makeLogString,
};
