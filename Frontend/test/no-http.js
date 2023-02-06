let assert = require('chai').assert;
let resource = require('./resources/setup');

module.exports = {
    'not http method': function(done) {
        return resource('no-http', { please: true })
            .then(function(data) {
                assert.isTrue(data.success);

                done();
            })
            .done();
    },
};
