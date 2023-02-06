const {assert} = require('chai');

const resource = require('./resources/setup');

module.exports = {
    'cache: get with overriden instance\'s getCache method': function (done) {
        return resource('cache-override', {cache: true}, {cache: {get: true}})
            .then(function (data) {
                assert.strictEqual(data, 2);
                done();
            })
            .done();
    },
};
