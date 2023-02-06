const assert = require('assert');

const langdetect = process._linkedBinding('langdetect');

const { LangDetector } = langdetect;
const data = process.env.DATA_PATH;

let detector = new LangDetector(data);


assert.strictEqual(typeof langdetect, 'object');
assert.strictEqual(typeof LangDetector, 'function');
assert.strictEqual(typeof detector, 'object');


assert.deepStrictEqual(detector.find({
    domain: 'http://mail.yandex.ru/neo2',
    filter: 'tt,ru,uk',
    geo: '24896,20529,20524,187,166,10001,10000'
}), {
    id: 'ru',
    name: 'Ru'
});


assert.deepStrictEqual(detector.findWithoutDomain({
    language: 'En-en, Ru-ru',
    cookie: '5',
    default: 'ru'
}), {
    id: 'be',
    name: 'By'
});


assert.deepStrictEqual(detector.findWithoutDomain({
    language: 'En-en, Ru-ru',
    cookie: 5,
    default: 'ru'
}), {
    id: 'be',
    name: 'By'
});


assert.deepStrictEqual(detector.findDomain('24896,20529,20524,187,166,10001,10000', 'ua,by,kz', 'http://mail.yandex.ru/neo2'), {
    host: 'mail.yandex.ua',
    changed: true,
    'content-region': 24896
});


assert.deepStrictEqual(detector.list({
    domain: 'http://mail.yandex.ru/neo2',
    filter: 'tt,ru,uk',
    geo: '24896,20529,20524,187,166,10001,10000'
}), [
    { id: 'ru', name: 'Ru' },
    { id: 'uk', name: 'Ua' }
]);


assert.strictEqual(detector.cookie2language(1), 'ru');
assert.strictEqual(detector.cookie2language(2), 'uk');
assert.strictEqual(detector.cookie2language(3), 'en');
assert.strictEqual(detector.cookie2language(4), 'kk');
assert.strictEqual(detector.cookie2language(5), 'be');


assert.strictEqual(detector.language2cookie('ru'), 1);
assert.strictEqual(detector.language2cookie('uk'), 2);
assert.strictEqual(detector.language2cookie('en'), 3);
assert.strictEqual(detector.language2cookie('kk'), 4);
assert.strictEqual(detector.language2cookie('be'), 5);
