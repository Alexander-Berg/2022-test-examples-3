import test from 'ava';
import bunkerMiddleware from '.';

const opts = {
    api: 'http://bunker-api-dot.yandex.net/v1',
    project: '.bunker-test/api',
    retries: 0,
};

test.cb('should work', t => {
    const req = {};

    bunkerMiddleware(opts)(req, {}, function() {
        t.truthy(req.bunker);
        t.is(req.bunker.foldir.file, 'Got ya');
        t.is(req.bunker.image['express-bunker.png'], '//avatars.mds.yandex.net/get-bunker/135516/381e565d207672cd800f8f0809da212043d91163/orig');
        t.end();
    });
});
