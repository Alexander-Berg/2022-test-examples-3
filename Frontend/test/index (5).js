/* global describe, it */

var filter = require('..');
require('should');

function nop() {}

describe('filter.hidden', function() {
    it('should mark hidden files by relative path', function(done) {
        filter.hidden({ fullName: '.project', relative: 'file' }, function(err) {
            (err === null).should.be.false;
            done();
        });
    });

    it('should remove dot files', function() {
        var node = { fullName: '.dot' };
        filter.hidden(node, nop);
        node.skip.should.be.true;
    });

    it('should remove files from dot directories', function() {
        var node = { fullName: '.dot/file' };
        filter.hidden(node, nop);
        node.skip.should.be.true;
    });
});

describe('filter.empty', function() {
    it('should call this.withContent', function(done) {
        var ctx = { withContent: function() { done() } };
        filter.empty.bind(ctx)({ fullName: '.dot' }, function() {});
    });
});
