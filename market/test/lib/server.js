const vow = require('vow');
const Form = require('formidable').IncomingForm;

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

    this.protocol = 'http:';
    this.port = port;
    this.host = (typeof host === 'string') ? host : 'localhost';
    this.servant = null;
}

TestServer.prototype.createServant = function (fn) {
    this.servant = fn(this.dispatcher.bind(this));
};

/**
 * @param {Function} callback
 */
TestServer.prototype.listen = function (callback) {
    return this.servant.listen(this.port, this.host, callback);
};

/**
 * @returns {formidable.Form}
 */
function buildForm() {
    const form = new Form({multiples: true});

    form.onPart = function (part) {
        if (part.filename === undefined && part.mime === 'application/octet-stream') {
            // kaero: dirty hack to parse buffers w/o filenames as files without original name
            part.filename = '';
        }
        this.handlePart(part);
    };

    return form;
}

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
            buildForm().parse(req, function (err, fields, files) {
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
            req.body = Buffer.alloc(0);

            req.on('data', function (d) {
                req.body = Buffer.concat([req.body, d], req.body.length + d.length);
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

function createTestServer(TestServerCls, nodeServerFn, startPort) {
    /**
     * Promisify all http tests, flawlessly create/close servers
     * @param {Function} testFn test function
     * @returns {Function}
     */
    return function httpxTest(testFn) {
        /**
         * @param {Function} mochaDone test done callback
         */
        return function (mochaDone) {
            return new vow.Promise(function (resolve) {
                const server = new TestServerCls(startPort++);
                server.createServant(nodeServerFn);
                server.listen(function () {
                    resolve(server);
                });
            })
                .then(function (server) {
                    const deferred = vow.defer();

                    testFn(function () {
                        server.close(mochaDone);
                    }, server);

                    return deferred.promise();
                })
                .done();
        };
    };
}

module.exports = createTestServer;
module.exports.TestServer = TestServer;
