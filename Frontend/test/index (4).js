/* global it */

var cat = require('..');
require('should');

it('should cat', function(done) {
    var node = {};
    cat.bind({
        cat: function(path, cb) {
            cb(null, '{}');
        },
    })(node, function(err) {
        if (err) { return done(err) }
        node.should.have.property('content', '{}');
        done();
    });
});
