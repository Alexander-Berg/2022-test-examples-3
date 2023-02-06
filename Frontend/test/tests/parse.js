import test from 'ava';
import bunker from '../..';
import opts from '../opts';

test('should return bunker', t => {
    const b = bunker(opts);
    t.is(b.parse(() => {}), b);
});

test('should have cat method in context', async t => {
    await bunker(opts)
        .parse(function(node, next) {
            /* eslint-disable no-invalid-this */
            t.is(typeof this.cat, 'function');
            /* eslint-enable no-invalid-this */
            next();
        });
});

test('should skip other parsers, when content is parsed', async t => {
    let called = false;

    await bunker(opts)
        .parse((node, cb) => {
            node.content = 1;
            cb();
        })
        .parse((node, cb) => {
            called = true;
            cb();
        });

    t.is(called, false);
});
