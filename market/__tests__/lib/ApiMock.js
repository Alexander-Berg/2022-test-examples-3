'use strict';

const qs = require('qs');
const nmock = require('nmock');

/**
 * @description TODO
 */
class ApiMock {
    /**
     *
     * @param {string|RegExp} host The full lowercased host portion
     *  of the URL, including port information.
     *
     * @param {string|RegExp} [pathname = '/'] The path section of the URL,
     *  that comes after the host and before the query, including
     *  the initial slash if present.
     *
     * @param {Object<string, string|RegExp>} query TODO
     *
     * @param {{ comment: string, status: number, body: Object }} result TODO
     *
     * @param {Object} [options = { allowUnmocked: true }] TODO
     *
     * @example
     *  const mock = new ApiMock(
     *      'host.com:8080',
     *      '/p/a/t/h',
     *      {
     *          query: 'string'
     *      },
     *      {
     *          text: 'response'
     *      }
     *  );
     */
    constructor(host, pathname = '/', query, result, options = { allowUnmocked: true }) {
        this._host = host;
        this._pathname = pathname;
        this._query = query;
        this._result = result;
        this._options = options;
    }

    /**
     * @description Returns the full lowercased host portion
     *  of the URL, including port information.
     *
     * @public
     *
     * @returns {string|RegExp}
     */
    get host() {
        return this._host;
    }

    /**
     * @description Returns the path section of the URL,
     *  that comes after the host and before the query,
     *  including the initial slash if present.
     *
     * @public
     *
     * @returns {string|RegExp}
     */
    get pathname() {
        return this._pathname;
    }

    /**
     * @description TODO
     *
     * @public
     *
     * @returns {Object<string, string|RegExp>}
     */
    get query() {
        return this._query;
    }

    /**
     * @description TODO
     *
     * @public
     *
     * @returns {Object|string}
     */
    get result() {
        return this._result;
    }

    /**
     * @description TODO
     *
     * @public
     *
     * @returns {Object}
     */
    get options() {
        return this._options;
    }

    /**
     * @description TODO
     *
     * @example
     *  const object = {
     *    a: {
     *      b: 'lol',
     *      c: 'lolol'
     *    }
     *  };
     *
     *  const attrs = {
     *    a: {
     *      c: /olo/
     *    }
     *  };
     *
     *  const result = Client._isMatch(object, attrs); // true
     *
     * @private
     *
     * @param {Object} object TODO
     * @param {Object} [attrs = {}] TODO
     *
     * @return {boolean}
     */
    static _isMatch(object, attrs = {}) {
        if (object === null) {
            return false;
        }

        const obj = Object(object);

        return Object.keys(attrs).every((key) => {
            if (typeof obj[key] === 'object' && typeof attrs[key] === 'object') {
                return ApiMock._isMatch(obj[key], attrs[key]);
            } else {
                if (attrs[key] instanceof RegExp) {
                    return attrs[key].test(obj[key]);
                } else {
                    return attrs[key] == obj[key];
                }
            }
        });
    }

    /**
     * @description Transforms strings of query params into JSON.
     *
     * @public
     * @static
     *
     * @param {Object|string} query
     *
     * @return {Object|JSON}
     */
    static queryToObject(query) {
        let q = query || {};
        if (typeof query === 'string') {
            q = qs.parse(query);
        }

        return q;
    }

    /**
     * @description TODO
     *
     * @public
     *
     * @param {Client} client
     *
     * @returns {undefined}
     */
    activate(client) {
        this._scope = nmock(this.host, this.options)
            .persist()
            .get(this.pathname)
            .query((actual) => {
                return this.query
                    ? ApiMock._isMatch(actual, ApiMock.queryToObject(this.query))
                    : true;
            });

        this._scope = this._scope
            .reply(
                this.result.status || 200,
                this.result.body
            );

        this._scope.on('replied', (req, interceptor, options) => {
            client.emit('mocked-request', Object.assign({
                mockInfo: {
                    isMocked: true,
                    mock: this.result.comment
                },
                response: {
                    status: this.result.status || 200,
                    body: this.result.body
                }
            }, options));
        });
    }

    /**
     * @description Restore the HTTP interceptor to
     *  the normal unmocked behaviour.
     *
     * @public
     *
     * @returns {undefined}
     */
    deactivate() {
        // TODO: this._scope.restore();
        nmock.cleanAll();
    }
}

module.exports = ApiMock;
