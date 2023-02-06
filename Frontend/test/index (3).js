/* global it */

var bunkerCache = require('..');
require('should');

it('should not update node with same version', function(done) {
    var cache = bunkerCache();
    var node = { fullName: '.dot', content: '2', version: 1 };
    cache(node, function() {
        node = { fullName: '.dot', content: '1', version: 1 };
        cache(node, function() {
            node.should.have.property('content', '2');
            done();
        });
    });
});

it('should update node with new version', function(done) {
    var cache = bunkerCache();
    var node = { fullName: '.dot', content: '2', version: 1 };
    cache(node, function() {
        node = { fullName: '.dot', content: '1', version: 2 };
        cache(node, function() {
            node.should.have.property('content', '1');
            done();
        });
    });
});

it('should update node with old version (if we published it)', function(done) {
    var cache = bunkerCache();
    var node = { fullName: '.dot', content: '2', version: 2 };
    cache(node, function() {
        node = { fullName: '.dot', content: '1', version: 1 };
        cache(node, function() {
            node.should.have.property('content', '1');
            done();
        });
    });
});
