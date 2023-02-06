let sinon = require('sinon');
let extend = require('../../../nodules-libs').util.extend;

module.exports = function createMockParams(addition) {
    return extend(true, {
        req: {
            headers: {},
        },
        res: {
            statusCode: 200,
            headers: {},
            setHeader: function(header, value) {
                this.headers[header.toLowerCase()] = value;
            },
            getHeader: function(header) {
                return this.headers[header.toLowerCase()];
            },
            end: sinon.spy(),
            _hasConnectPatch: true,
        },
    }, addition);
};
