import test from 'ava';
import { json } from '../..';

test.cb('should skip nodes with no mime type', t => {
    const node = { fullName: 'path' };

    json(node, function(err) {
        if (err) {
            return t.end(err);
        }

        t.deepEqual(node, { fullName: 'path' });
        t.end();
    });
});

test.cb('should skip nodes with invalid mime type', t => {
    const node = { fullName: 'path', mime: { type: 'application', subtype: 'tjson' } };

    json(node, function(err) {
        if (err) {
            return t.end(err);
        }

        t.deepEqual(node, { fullName: 'path', mime: { type: 'application', subtype: 'tjson' } });
        t.end();
    });
});

test.cb('should format error message', t => {
    const node = { fullName: 'path', mime: { type: 'application', subtype: 'json' } };

    json.bind({
        cat: function() {
            return new Promise(resolve => {
                resolve('wat');
            });
        },
    })(node, function(err) {
        t.regex(err.message, /Failed to parse JSON in path: Unexpected token w/);
        t.end();
    });
});

test.cb('should format error message', t => {
    const node = { fullName: 'path', mime: { type: 'application', subtype: 'json' } };

    json.bind({
        cat: function(path) {
            return new Promise(resolve => {
                t.is(path, 'path');
                resolve('{}');
            });
        },
    })(node, function(err) {
        if (err) {
            return t.end(err);
        }

        t.deepEqual(node.content, {});
        t.end();
    });
});
