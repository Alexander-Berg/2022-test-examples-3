import test from 'ava';
import bunker from '../..';
import opts from '../opts';

test('should return bunker', t => {
    const b = bunker(opts);
    t.is(b.use(() => {}), b);
});

test('should call processor with right context', async t => {
    await bunker(opts)
        .use(function(node, next) {
            /* eslint-disable no-invalid-this */
            t.is(typeof this.cat, 'function');
            /* eslint-enable no-invalid-this */
            next();
        });
});

test('should call processors with same node', async t => {
    await bunker(opts)
        .use((node, cb) => {
            node.content = 1;
            cb();
        })
        .use((node, cb) => {
            t.is(node.content, 1);
            cb();
        });
});

test('should skip nodes', async() => {
    await bunker(opts)
        .use((node, cb) => {
            node.skip = true;
            cb();
        })
        .use(() => {
            throw new Error('Should not be called');
        });
});
