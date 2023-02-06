const DEFAULT_HEADER = {
    xscript_request_id: 1334,
};

const DEFAULT_DATA = {
    user: {
        clientIp: '127.0.0.1',
    },
};

function Request(mock) {
    if (!mock) {
        mock = {};
    }
    this.cookies = mock.cookies || {};
    this.headers = mock.headers || DEFAULT_HEADER;
    this.params = mock.params || {};
    this.data = mock.data || DEFAULT_DATA;
    this.url = mock.url || {
        hostname: 'market.yandex.ru',
    };
    this.url.host = this.url.hostname;
}

Request.prototype.getParam = function (key) {
    return this.params[key];
};

Request.prototype.getHeader = function (key) {
    return this.headers[key];
};

Request.prototype.getCookie = function (key) {
    return this.cookies[key];
};

Request.prototype.getData = function (key) {
    return this.data[key];
};

module.exports = {
    DEFAULT: new Request(),
    WITH_KIEV_LR: new Request({
        params: {
            lr: 143,
        },
    }),
    UA_HOSTNAME_ONLY: new Request({
        url: {
            hostname: 'market.yandex.ua',
        },
    }),
    NON_KUBR: new Request({
        url: {
            hostname: 'market.yandex.so',
        },
    }),
    UNEXISTING_LR: new Request({
        params: {
            lr: 13346661334,
        },
    }),
    RU_LR_WITH_UA_TLD: new Request({
        url: {
            hostname: 'market.yandex.ua',
        },
        params: {
            lr: 213,
        },
    }),
    WITH_IP: new Request({
        data: {
            user: {
                clientIp: '176.36.14.15',
            },
        },
    }),
};
