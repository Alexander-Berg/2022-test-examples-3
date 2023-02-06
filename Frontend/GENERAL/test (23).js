import test from 'ava';
import blackbox from '.';

test('should return function', t => {
    t.is(typeof blackbox, 'function');
});

test('should throw without api in options', t => {
    t.throws(blackbox().sessionid, 'options.api is required');
});

test('should parse JSON response from blackbox', async t => {
    const res = await blackbox({ api: 'pass-test.yandex.ru' }).sessionid();
    t.is(res.body.exception.value, 'INVALID_PARAMS');
});
