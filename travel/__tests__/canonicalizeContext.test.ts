import {
    IEmptyContextPoint,
    IFilledContextPoint,
    IRaspParseContextApiResponse,
} from 'server/api/RaspApi/types/IRaspParseContextApiResponse';
import {ITrainsCanonicalResponse} from 'server/api/TrainsSearchApi/types/ITrainsCanonical';

import {canonicalizeContext} from 'projects/trains/lib/context/canonicalizeContext';

const EMPTY_POINT: IEmptyContextPoint = {
    timezone: null,
    country: null,
    slug: '',
    key: '',
    title: '',
};

const FILLED_CONTEXT = {
    from: {
        country: {
            code: 'RU',
            title: 'Россия',
        },
        key: 's9613054',
        popularTitle: '',
        preposition: 'в',
        region: {title: 'Краснодарский край'},
        settlement: {
            slug: 'adler-krasnodar-krai',
            title: 'Адлер',
        },
        slug: 'adler-station',
        timezone: 'Europe/Moscow',
        title: 'Адлер',
        titleAccusative: 'Адлер',
        titleGenitive: 'Адлера',
    } as IFilledContextPoint,
    to: {
        country: {
            code: 'RU',
            title: 'Россия',
        },
        key: 'c35',
        popularTitle: 'Краснодар',
        preposition: 'в',
        region: {title: 'Краснодарский край'},
        settlement: {slug: 'krasnodar', title: 'Краснодар'},
        slug: 'krasnodar',
        timezone: 'Europe/Moscow',
        title: 'Краснодар',
        titleAccusative: 'Краснодар',
        titleGenitive: 'Краснодара',
    } as IFilledContextPoint,
} as IRaspParseContextApiResponse;

const NOT_FILLED_CONTEXT = {
    from: EMPTY_POINT,
    to: EMPTY_POINT,
} as IRaspParseContextApiResponse;

const CANONICAL_INFO: ITrainsCanonicalResponse = {
    fromSlug: 'adler-krasnodar-krai',
    fromTitle: 'Адлер',
    fromPopularTitle: 'Адок',
    toSlug: 'krasnodar',
    toTitle: 'Краснодар',
    toPopularTitle: 'Краснодар',
};

describe('canonicalizeContext', () => {
    it('Если нет информации о канонических слагах - вернём оригинальный контекст', () => {
        expect(canonicalizeContext(FILLED_CONTEXT, null)).toBe(FILLED_CONTEXT);
    });

    it('Если слаги совпадают - вернём оригинальный контекст', () => {
        expect(
            canonicalizeContext(FILLED_CONTEXT, {
                ...CANONICAL_INFO,
                fromSlug: 'adler-station',
            }),
        ).toBe(FILLED_CONTEXT);
    });

    it('Если по пунтку отправления/прибытия нет информации - возвращаем оригинальный контекст', () => {
        expect(canonicalizeContext(NOT_FILLED_CONTEXT, CANONICAL_INFO)).toBe(
            NOT_FILLED_CONTEXT,
        );
    });

    it('Если слаги отличаются - вернём обновлённый контекст', () => {
        expect(canonicalizeContext(FILLED_CONTEXT, CANONICAL_INFO)).toEqual({
            ...FILLED_CONTEXT,
            from: {
                ...FILLED_CONTEXT.from,
                slug: 'adler-krasnodar-krai',
                title: 'Адлер',
                popularTitle: 'Адок',
            },
        });
    });
});
