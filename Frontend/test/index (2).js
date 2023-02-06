/* global it */

var should = require('should');
var avatar = require('..');

it('should skip nodes with no mime type', function(done) {
    var node = {};
    avatar(node, function(err, ret) {
        if (err) { return done(err) }
        should.not.exist(ret);
        node.should.eql({});
        done();
    });
});

it('should skip nodes with invalid mime type', function(done) {
    var node = { mime: { type: 'application', subtype: 'tjson' } };
    avatar(node, function(err, ret) {
        if (err) { return done(err) }
        should.not.exist(ret);
        node.should.eql({ mime: { type: 'application', subtype: 'tjson' } });
        done();
    });
});

it('should skip nodes with not avatar-href in mime', function(done) {
    var node = { mime: { type: 'image', subtype: 'svg', suffix: 'xml', parameters: {} } };
    avatar(node, function(err, ret) {
        if (err) { return done(err) }
        should.not.exist(ret);
        node.should.eql({ mime: { type: 'image', subtype: 'svg', suffix: 'xml', parameters: {} } });
        done();
    });
});

it('should parse out url from avatar-href', function(done) {
    var node = { mime: { type: 'image', subtype: 'svg', suffix: 'xml', parameters: { 'avatar-href': 'http://wat.ru/' } } };
    avatar(node, function(err, ret) {
        if (err) { return done(err) }
        should.not.exist(ret);
        node.should.have.property('content', '//wat.ru/');
        done();
    });
});

it('should parse out url from avatar-href with protocol', function(done) {
    var node = { mime: { type: 'image', subtype: 'svg', suffix: 'xml', parameters: { 'avatar-href': 'http://wat.ru/' } } };
    avatar.withProtocol(node, function(err, ret) {
        if (err) { return done(err) }
        should.not.exist(ret);
        node.should.have.property('content', 'http://wat.ru/');
        done();
    });
});
