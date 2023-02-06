import test from 'ava';
import nestedProp from 'nested-prop';
import bunker from '../..';
import opts from '../opts';

test('should throw without project option', t => {
    t.throws(bunker);
});

test('should get nodes', async t => {
    const obj = await bunker(opts);

    t.true('text-node' in obj);
});

test('should get nodes with content', async t => {
    const obj = await bunker(opts)
        .parse((node, cb) => {
            if (node.mime && node.mime.type === 'image') {
                node.content = 'path-to-image';
            } else {
                node.content = { name: node.fullName };
            }

            cb();
        });

    t.is(nestedProp.get(obj, ['image', 'name']), undefined);
    t.is(nestedProp.get(obj, ['image', 'express-bunker.png']), 'path-to-image');
});

test('should get dirs content include directories', async t => {
    const obj = await bunker(Object.assign({}, opts, { directories: true }))
        .parse((node, cb) => {
            if (node.mime && node.mime.type === 'image') {
                node.content = 'path/to/image';
            } else {
                node.content = { name: node.fullName };
            }

            cb();
        });

    t.is(nestedProp.get(obj, ['image', 'name']), '/.bunker-test/api/image');
    t.is(nestedProp.get(obj, ['image', 'express-bunker.png']), 'path/to/image');
});

test('should get dirs content only where parent is object', async t => {
    const obj = await bunker(Object.assign({}, opts, { directories: true }))
        .parse((node, cb) => {
            // Imitate text node
            if (node.fullName === '/.bunker-test/api/foldir') {
                node.content = 'ipsum lorem dalor';
            }

            cb();
        });

    t.is(typeof obj.foldir, 'object');
    t.true('file' in obj.foldir);
});

test('should skip dirs with childs', async t => {
    const obj = await bunker(opts);

    t.true('foldir' in obj);
    t.true('file' in obj.foldir);
});

test('should not skip dirs with option', async t => {
    let found = false;
    await bunker(Object.assign({}, opts, { directories: true }))
        .use((node, next) => {
            found = node.fullName === '/.bunker-test/api/foldir' || found;
            next();
        });

    t.truthy(found);
});

test('bunker object should be frozen deep', async t => {
    const obj = await bunker(opts);
    t.throws(() => {
        obj.foldir.file = 'somefile';
    }, /read only property/);
});

// TODO: Restore this test
test.cb.skip('should work with all existing parsers', t => {
    const parsers = [
        bunker.filter.hidden,
        bunker.filter.empty,
        bunker.avatar,
        bunker.tjson,
        bunker.json,
        bunker.cat,
    ];

    const b = bunker(opts);

    parsers.forEach(b.parse, b);

    b.then(obj => {
        t.deepEqual(obj, {
            deleted: { 'this-still-a-node': 'I\'m alive!' },
            foldir: {
                file: 'Got ya',
            },
            image: {
                'express-bunker.png': '//avatars.mds.yandex.net/get-bunker/135516/381e565d207672cd800f8f0809da212043d91163/orig',
                'screenshot.png': '//avatars.mds.yandex.net/get-bunker/120922/286fa62f1b238cf8d2ea9795029ae2052c762c18/orig',
            },
            'single-node-inside': { 'this-is-the-one': 'Choosen one.' },
            'text-node': 'Hello plain!',
        });

        t.end();
    });
});
