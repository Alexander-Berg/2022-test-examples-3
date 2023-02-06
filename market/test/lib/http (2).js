const http = require('http');

const Form = require('formidable').IncomingForm;

const form = new Form();
let PORT = process.env.ASKER_TEST_PORT || 10080;

/**
 * @constructor
 * @class TestServer
 * @param {Number} port
 * @param {String} [host=localhost]
 * @param {Array} testDispatchers array of functions used as dispatchers for request
 */
function TestServer(port, host, testDispatchers) {
    if (Array.isArray(host) && typeof testDispatchers === 'undefined') {
        this.tests = host;
    } else {
        this.tests = testDispatchers ? [].concat(testDispatchers) : [];
    }

    this.port = port;
    this.host = (typeof host === 'string') ? host : 'localhost';

    this.servant = http.createServer(this.dispatcher.bind(this));
}

/**
 * @param {Function} callback
 */
TestServer.prototype.listen = function (callback) {
    return this.servant.listen(this.port, this.host, callback);
};

/**
 * @param {http.IncomingMessage} req
 * @param {http.ServerResponse} res
 */
TestServer.prototype.dispatcher = function (req, res) {
    const d = this.tests.shift();

    if (!d || (typeof d !== 'function')) {
        res.statusCode = 500;
        res.end(this.buildResponse(false, (!d) ? 'test dispatcher not found' : 'test dispatcher is not a function'));
    } else {
        // Use formidable parser when getting typed content (urlencoded, json, multipart)
        // eslint-disable-next-line no-lonely-if
        if (/(urlencoded|json|multipart)/.test(req.headers['content-type'])) {
            form.parse(req, function (err, fields, files) {
                if (err) {
                    res.statusCode = 500;
                    res.end(`formidable error: ${err}`);
                } else {
                    req.body = fields;
                    req.files = files;
                    d(req, res);
                }
            });
        } else {
            req.body = '';

            req.on('data', function (d) {
                req.body += d;
            });

            req.on('end', function () {
                d(req, res);
            });
        }
    }
};

/**
 * add test dispatcher to tests pool
 * @param {Function} testDispatcher
 */
TestServer.prototype.addTest = function (testDispatcher) {
    this.tests.push(testDispatcher);
};

/**
 * build JSON response
 * @param {Boolean} isDone
 * @param {String} message
 * @returns {String} stringified object { done : isDone, message : message }
 */
TestServer.prototype.buildResponse = function (isDone, message) {
    return JSON.stringify({
        done: isDone,
        message,
    });
};

/**
 * proxy to http.Server.close method
 * @param {Function} callback
 */
TestServer.prototype.close = function (callback) {
    this.servant.on('close', callback);
    return this.servant.close();
};

/**
 * Promisify all http tests, flawlessly create/close servers
 * @param {Function} testFn test function
 * @returns {Function}
 */
function httpTest(testFn) {
    /**
     * @param {Function} mochaDone test done callback
     */
    return function (mochaDone) {
        const server = new TestServer(PORT++);

        server.listen(function () {
            testFn(function () {
                server.close(mochaDone);
            }, server);
        });
    };
}

module.exports = httpTest;
