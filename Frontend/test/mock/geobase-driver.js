'use strict';

var DEFAULT_LANG = 'ru';

function argsToArray(args) {
    return Array.prototype.slice.call(args);
}

function GeobaseMockDriver(responseData) {
    this._data = responseData;
}

GeobaseMockDriver.prototype.init = function(done) {
    if (typeof done === 'function') {
        done();
    }
};

GeobaseMockDriver.prototype.idIsIn = function(id, pid, lang, done) {
    if (typeof lang === 'function' && typeof done === 'undefined') {
        done = lang;
        lang = DEFAULT_LANG;
    }
    done(null, this.getResponse('idIsIn', [id, pid, lang]));
};

GeobaseMockDriver.prototype.tzinfo = function(id, done) {
    done(null, this.getResponse('tzinfo', [id]));
};

GeobaseMockDriver.prototype.regionByIp = function(ip, lang, done) {
    if (typeof lang === 'function' && typeof done === 'undefined') {
        done = lang;
        lang = DEFAULT_LANG;
    }
    // Временный хак для тестов
    if (ip === '127.0.0.1') {
        done(null);
    }
    done(null, this.getResponse('regionByIp', [ip, lang]));
};

GeobaseMockDriver.prototype.region = function(yandex_gid, x_forwarded_for, x_real_ip, done) {
    // Временный хак для тестов
    if (x_real_ip === '127.0.0.1') {
        done(null);
    }
    done(null, this.getResponse('region', [yandex_gid, x_forwarded_for, x_real_ip]));
};

GeobaseMockDriver.prototype.linguistics = function(id, lang, done) {
    if (typeof lang === 'function') {
        done = lang;
        lang = DEFAULT_LANG;
    }

    done(null, this.getResponse('linguistics', [id, lang]));
};

GeobaseMockDriver.prototype.getCountryInfoByRegionId = function(id, lang, done) {
    if (typeof lang === 'function' && typeof done === 'undefined') {
        done = lang;
        lang = DEFAULT_LANG;
    }
    done(null, this.getResponse('getCountryInfoByRegionId', [id, lang]));
};

GeobaseMockDriver.prototype.regionById = function(id, lang, done) {
    if (typeof lang === 'function' && typeof done === 'undefined') {
        done = lang;
        lang = DEFAULT_LANG;
    }
        done(null, this.getResponse('regionById', [id, lang]));
};

GeobaseMockDriver.prototype.getResponse = function(method, args) {
    args = argsToArray(args).filter(function(arg) {
        return typeof arg !== 'function';
    }).join('_');

    if (typeof this._data[method] === 'undefined') {
        // console.info('MockDriver: response not found: ' + method + '#' + args);
    }
    if (typeof this._data[method][args] === 'undefined') {
        // console.info('MockDriver: response not found: ' + method + '#' + args);
    }

    return this._data[method][args];
};

module.exports = GeobaseMockDriver;
