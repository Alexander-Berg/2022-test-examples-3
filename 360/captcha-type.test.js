'use strict';

jest.mock('@yandex-int/duffman');

const model = require('./captcha-type');

describe('captcha-type model', () => {
    let core;

    beforeEach(() => {
        core = {
            config: {
                locale: 'foo',
                pddDomain: 'domain',
                IS_CORP: false
            }
        };
    });

    it.each([
        // локаль | тип капчи
        [ 'ru', 'txt_v1' ],
        [ 'uk', 'txt_v1' ],
        [ 'be', 'txt_v1' ],
        [ 'kk', 'txt_v1' ],
        [ 'other', 'txt_v1_en' ]
    ])('тип капчи для локали %s', async (locale, expected) => {
        core.config.locale = locale;

        const type = await model(null, core);

        expect(type).toBe(expected);
    });
});
