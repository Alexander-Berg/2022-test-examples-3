'use strict';

const crypto = require('./../../utils/crypto');

/**
 * @description Куки Советника.
 */
class Cookie {
    /**
     * @constructor
     *
     * @param {String} key
     * @param {String|JSON} value
     * @param {Object} [options]
     */
    constructor(key, value, options) {
        if (!(this instanceof Cookie)) {
            return new Cookie(key, value, options);
        }

        this._key = key;
        this._value = value;

        this._options = {
            signed: false,
            path: '/',
            domain: 'sovetnik.market.yandex.ru',
            secure: false,
            httpOnly: true
        };

        if (options) {
            if (typeof options.path === 'string') {
                this._options.path = options.path;
            }
            if (typeof options.domain === 'string') {
                this._options.domain = options.domain;
            }
            if (typeof options.signed === 'boolean') {
                this._options.signed = options.signed;
            }
            if (typeof options.secure === 'boolean') {
                this._options.secure = options.secure;
            }
            if (typeof options.httpOnly === 'boolean') {
                this._options.httpOnly = options.httpOnly;
            }

            if (options.maxAge) {
                this._options.expires = new Date(Date.now() + (+options.maxAge));
            } else if (options.expires) {
                this._options.expires = options.expires;
            }
        }

        if (!this._options.expires) {
            this._options.expires = 0;
        }
    }

    /**
     * @description Возвращает ключ куки.
     *
     * @returns {String}
     */
    get key() {
        return this._key;
    }

    /**
     * @description Возвращает дешифрованное значение куки.
     *  Если значение зашифровано - дешифрует значение.
     *  Если значение представимо в JSON - распаршивает значение.
     *
     * @return {String|JSON}
     */
    get value() {
        let value = this._value;

        if (typeof value === 'string') {
            try {
                value = Cookie.decrypt(value);
            } catch (e) {
                //
            }

            try {
                value = JSON.parse(value);
            } catch (e) {
                //
            }
        }
        return value;
    }

    /**
     * @description Возвращает expires.
     *
     * @returns {Date|Number}
     */
    get expires() {
        return this._options.expires;
    }

    /**
     * @description Возвращает true, если куки сессионные, иначе - false.
     *
     * @returns {Boolean}
     */
    get isSession() {
        return this._options.expires === 0;
    }

    /**
     * @description Возвращает path.
     *
     * @returns {String}
     */
    get path() {
        return this._options.path;
    }

    /**
     * @description Возвращает domain.
     *
     * @return {String}
     */
    get domain() {
        return this._options.domain;
    }

    /**
     * @description Возвращает secure.
     *
     * @returns {Boolean}
     */
    get secure() {
        return this._options.secure;
    }

    /**
     * @description  Возвращает значение httpOnly.
     *
     * @returns {Boolean}
     */
    get httpOnly() {
        return this._options.httpOnly;
    }

    /**
     * @description Возвращает зашифрованное значение.
     *
     * @param {String} value TODO
     *
     * @returns {String}
     */
    static encrypt(value) {
        return crypto.encrypt(value);
    }

    /**
     * @description Возвращает дешифрованное значение.
     *
     * @param {String} value TODO
     *
     * @returns {String}
     */
    static decrypt(value) {
        return crypto.decrypt(value);
    }
}

module.exports = Cookie;
