import test from 'ava';
import { filter } from '../..';

function nop() {}

test.cb('should mark hidden files by relative path', t => {
    filter.hidden({ fullName: '.project', relative: 'file' }, function(err) {
        if (err) {
            t.end(err);
        }

        t.end();
    });
});

test('should remove dot files', t => {
    const node = { fullName: '.dot' };
    filter.hidden(node, nop);
    t.true(node.skip);
});

test('should remove files from dot directories', t => {
    const node = { fullName: '.dot/file' };
    filter.hidden(node, nop);
    t.true(node.skip);
});

test.cb('should call this.withContent', t => {
    const ctx = {
        withContent: function() {
            t.end();
        },
    };

    filter.empty.bind(ctx)({ fullName: '.dot' }, function() {});
});
