const assert = require('assert');

const langdetect = process._linkedBinding('langdetect');

const { LangDetector } = langdetect;
const data = process.env.DATA_PATH;

let detector = new LangDetector(data);


(() => {
    const expected = { host: 'maps.yandex.ua', changed: true, 'content-region': 24896 };
    const result = detector.findDomain('24896,20529,20524,187,166,10001,10000', 'ua,by,kz', 'maps.yandex.ru', '');
    const result2 = detector.findDomain('24896,20529,20524,187,166,10001,10000', 'ua,by,kz', 'http://maps.yandex.ru/');
    assert.deepStrictEqual(result, expected);
    assert.deepStrictEqual(result, result2);
}) ();


(() => {
    const parents_ids = '11503,103674,983,111,10001,10000';
    const domains = 'by,com,com.tr,kz,ua';
    const host = 'harita.yandex.com.tr';
    const expected = { 'host': host, 'changed': false, 'content-region': 11503 };

    const result = detector.findDomain(parents_ids, domains, host)

    assert.deepStrictEqual(result, expected);
}) ();


(() => {
    const expected = { host: 'maps.yandex.com.ge', changed: true, 'content-region': 10277 };
    const result = detector.findDomain('10277,169,183,10001,10000', 'ua,by,kz,com.ge', 'maps.yandex.ru', '');
    assert.deepStrictEqual(result, expected);
}) ();


(() => {
    const expected = { host: 'maps.yandex.com.ge', changed: true, 'content-region': 10277 };
    const result = detector.findDomain('10277,169,183,10001,10000', 'ru,by,kz,com.ge', 'maps.yandex.ua', '');
    assert.deepStrictEqual(result, expected);
}) ();


(() => {
    const expected = { host: 'maps.yandex.com.ge', changed: true, 'content-region': 10283 };
    const result = detector.findDomain('10283,122060,169,183,10001,10000', 'com.tr,com.ge', 'maps.yandex.com');
    assert.deepStrictEqual(result, expected);
}) ();


(() => {
    const expected = { host: 'maps.yandex.com.ge', changed: false, 'content-region': 10283 };
    const result = detector.findDomain('10283,122060,169,183,10001,10000', 'ua,ru,by,kz,com', 'maps.yandex.com.ge');
    assert.deepStrictEqual(result, expected);
}) ();
