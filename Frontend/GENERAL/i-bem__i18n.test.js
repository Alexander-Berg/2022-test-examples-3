/* eslint-disable quote-props */
const assert = require('assert');

describe('BEM.I18N', function() {
    const BEM = {};
    require('./i-bem__i18n.js')(BEM);

    let keysetCounter = 1;
    function getUniqueKeyset() {
        return `test-uniq-keyset-${keysetCounter++}`;
    }

    it('should create BEM.I18N function', function() {
        assert.strictEqual(typeof BEM.I18N, 'function');
    });

    it('should return localized string', function() {
        const KEYSET = getUniqueKeyset();

        BEM.I18N.decl(
            KEYSET,
            { 'Видео': 'Video en' },
            { lang: 'en' }
        );
        BEM.I18N.lang('en');
        assert.strictEqual(BEM.I18N(KEYSET, 'Видео'), 'Video en');

        BEM.I18N.decl(
            KEYSET,
            { 'Видео': 'Видео ru' },
            { lang: 'ru' }
        );
        BEM.I18N.lang('ru');
        assert.strictEqual(BEM.I18N(KEYSET, 'Видео'), 'Видео ru');
    });

    it('should return empty string when key is missing', function() {
        const KEYSET = getUniqueKeyset();

        BEM.I18N.decl(
            KEYSET,
            { 'Видео': 'Video kz' },
            { lang: 'kz' }
        );
        BEM.I18N.lang('kz');
        assert.strictEqual(BEM.I18N(KEYSET, 'Missing key'), '');
    });

    it('should use default language when key is not translated to target language', function() {
        const KEYSET = getUniqueKeyset();

        BEM.I18N.decl(
            KEYSET,
            { },
            { lang: 'kz' }
        );
        BEM.I18N.decl(
            KEYSET,
            { 'Видео': 'Видео ru' },
            { lang: 'ru' }
        );
        BEM.I18N.lang('kz');
        assert.strictEqual(BEM.I18N(KEYSET, 'Видео'), 'Видео ru');
    });

    it('should use default language when keyset in target language is missing', function() {
        const KEYSET = getUniqueKeyset();
        BEM.I18N.decl(
            KEYSET,
            { 'Видео': 'Видео ru' },
            { lang: 'ru' }
        );
        BEM.I18N.decl(
            KEYSET,
            { 'Видео': 'Video en' },
            { lang: 'en' }
        );

        // et fi id lt lv pl tr -> en
        ['et', 'fi', 'id', 'lt', 'lv', 'pl', 'tr'].forEach(lang => {
            BEM.I18N.lang(lang);
            assert.strictEqual(BEM.I18N(KEYSET, 'Видео'), 'Video en');
        });

        // any other -> ru
        BEM.I18N.lang('xx');
        assert.strictEqual(BEM.I18N(KEYSET, 'Видео'), 'Видео ru');
    });
});
