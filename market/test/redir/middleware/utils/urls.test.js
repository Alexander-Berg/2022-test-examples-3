const urls = require('./urls');

const { FIREFOX, CHROME, YANDEX, OPERA, LANDING } = urls;

describe('urls module returns the right links', () => {
    test('Firefox', () => {
        expect(FIREFOX).toMatchInlineSnapshot(
            `"https://addons.mozilla.org/ru/firefox/addon/sovetnik/"`,
        );
    });

    test('Chrome', () => {
        expect(CHROME).toMatchInlineSnapshot(
            `"https://chrome.google.com/webstore/detail/ppiaojpbclpegkkkmikabinlpbahhbha"`,
        );
    });

    test('Yandex', () => {
        expect(YANDEX).toMatchInlineSnapshot(
            `"https://addons.opera.com/ru/extensions/details/metabarsovetnik/"`,
        );
    });

    test('Opera', () => {
        expect(OPERA).toMatchInlineSnapshot(
            `"https://addons.opera.com/ru/extensions/details/metabarsovetnik/"`,
        );
    });

    test('Landing', () => {
        expect(LANDING).toMatchInlineSnapshot(`"https://sovetnik.yandex.ru"`);
    });
});
