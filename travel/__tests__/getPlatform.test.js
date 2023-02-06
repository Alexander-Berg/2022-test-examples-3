import Platform from '../../../common/interfaces/Platform';

import getPlatform from '../getPlatform';

const req = {
    query: {
        m: '',
    },
    uatraits: {
        isMobile: false,
    },
    headers: {
        'x-is-mobile': false,
    },
};

describe('getPlatform', () => {
    it('есть параметр m (mobile) - возвращаем MOBILE', () => {
        expect(
            getPlatform({
                ...req,
                query: {
                    m: 'value',
                },
            }),
        ).toBe(Platform.mobile);
    });

    it('есть кука isMobile = true - возвращаем MOBILE', () => {
        expect(
            getPlatform({
                ...req,
                uatraits: {
                    isMobile: true,
                },
            }),
        ).toBe(Platform.mobile);
    });

    it('есть хедер x-is-mobile - возвращаем MOBILE', () => {
        expect(
            getPlatform({
                ...req,
                headers: {
                    'x-is-mobile': true,
                },
            }),
        ).toBe(Platform.mobile);
    });

    it('куки/хедеры/параметры не установлены - возвращаем DESKTOP', () => {
        expect(getPlatform(req)).toBe(Platform.desktop);
    });
});
