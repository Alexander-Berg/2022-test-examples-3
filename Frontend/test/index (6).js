/* global it */

var json = require('..');
require('should');

it('should skip nodes with no mime type', function(done) {
    var node = { fullName: 'path' };
    json(node, function(err) {
        if (err) { return done(err) }
        node.should.eql({ fullName: 'path' });
        done();
    });
});

it('should skip nodes with invalid mime type', function(done) {
    var node = { fullName: 'path', mime: { type: 'application', subtype: 'tjson' } };
    json(node, function(err) {
        if (err) { return done(err) }
        node.should.eql({ fullName: 'path', mime: { type: 'application', subtype: 'tjson' } });
        done();
    });
});

it('should format error message', function(done) {
    var node = { fullName: 'path', mime: { type: 'application', subtype: 'json' } };
    var msg = parseInt(process.version.substring(1)) < 6 ?
        'Failed to parse JSON in path: Unexpected token w' :
        'Failed to parse JSON in path: Unexpected token w in JSON at position 0';
    json.bind({ cat: function(path, cb) { cb(null, 'wat') } })(node, function(err) {
        err.message.should.match(msg);
        done();
    });
});

it('should cat and parse JSON', function(done) {
    var node = { fullName: 'path', mime: { type: 'application', subtype: 'json' } };
    json.bind({
        cat: function(path, cb) {
            path.should.eql('path');
            cb(null, '{}');
        },
    })(node, function(err) {
        if (err) { return done(err) }
        node.should.have.property('content', {});
        done();
    });
});
