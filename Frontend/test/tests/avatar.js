import test from 'ava';
import { avatar } from '../..';

test.cb('should skip nodes with no mime type', t => {
    const node = {};

    avatar(node, function(err, ret) {
        if (err) {
            return t.end(err);
        }

        t.falsy(ret);
        t.deepEqual(node, {});
        t.end();
    });
});

test.cb('should skip nodes with invalid mime type', t => {
    const node = { mime: { type: 'application', subtype: 'tjson' } };

    avatar(node, function(err, ret) {
        if (err) {
            return t.end(err);
        }

        t.falsy(ret);
        t.deepEqual(node, { mime: { type: 'application', subtype: 'tjson' } });
        t.end();
    });
});

test.cb('should skip nodes with not avatar-href in mime', t => {
    const node = { mime: { type: 'image', subtype: 'svg', suffix: 'xml', parameters: {} } };

    avatar(node, function(err, ret) {
        if (err) {
            return t.end(err);
        }

        t.falsy(ret);
        t.deepEqual(node, { mime: { type: 'image', subtype: 'svg', suffix: 'xml', parameters: {} } });
        t.end();
    });
});

test.cb('should parse out url from avatar-href', t => {
    const node = { mime: { type: 'image', subtype: 'svg', suffix: 'xml', parameters: { 'avatar-href': 'http://wat.ru/' } } };

    avatar(node, function(err, ret) {
        if (err) {
            return t.end(err);
        }

        t.falsy(ret);
        t.is(node.content, '//wat.ru/');
        t.end();
    });
});

test.cb('should parse out url from avatar-href with protocol', t => {
    const node = { mime: { type: 'image', subtype: 'svg', suffix: 'xml', parameters: { 'avatar-href': 'http://wat.ru/' } } };

    avatar.withProtocol(node, function(err, ret) {
        if (err) {
            return t.end(err);
        }

        t.falsy(ret);
        t.is(node.content, 'http://wat.ru/');
        t.end();
    });
});
