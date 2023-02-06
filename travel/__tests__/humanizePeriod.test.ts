import {IHumanizePeriod} from 'types/common/date/IHumanizePeriod';

import humanizePeriod from 'utilities/dateUtils/humanizePeriod';
import {getDateLocale, setDateLocale} from 'utilities/dateUtils';
import locales from 'utilities/dateUtils/locales';
import DateMock from 'utilities/testUtils/DateMock';
import {CHAR_NBSP} from 'utilities/strings/charCodes';

describe('humanizePeriod', () => {
    const DEFAULT_LOCALE = getDateLocale();
    const MOCK_DATE = '2019-01-01';

    beforeAll(() => {
        setDateLocale(locales.RU);
        DateMock.mock(MOCK_DATE);
    });

    afterAll(() => {
        setDateLocale(DEFAULT_LOCALE);
        DateMock.restore();
    });

    const cases: [string, string, string, IHumanizePeriod][] = [
        ['2019-01-01', '2019-01-01', `1${CHAR_NBSP}января`, {short: true}],
        [
            '2019-01-21',
            '2019-01-26',
            `21${CHAR_NBSP}– 26${CHAR_NBSP}янв`,
            {short: true},
        ],
        [
            '2019-04-01',
            '2019-05-01',
            `1${CHAR_NBSP}апр${CHAR_NBSP}– 1${CHAR_NBSP}мая`,
            {short: true},
        ],
        [
            '2019-12-25',
            '2020-01-05',
            `25${CHAR_NBSP}дек${CHAR_NBSP}– 5${CHAR_NBSP}янв`,
            {short: true},
        ],
        [
            '2019-12-25',
            '2020-01-05',
            `25${CHAR_NBSP}дек${CHAR_NBSP}– 5${CHAR_NBSP}янв'20`,
            {short: true, checkAnotherEndYear: true},
        ],
        ['2019-01-01', '2019-01-01', `1${CHAR_NBSP}января`, {short: false}],

        [
            '2019-01-21',
            '2019-01-26',
            `21${CHAR_NBSP}– 26${CHAR_NBSP}января`,
            {short: false},
        ],
        [
            '2019-04-01',
            '2019-05-01',
            `1${CHAR_NBSP}апреля${CHAR_NBSP}– 1${CHAR_NBSP}мая`,
            {short: false},
        ],
        [
            '2019-12-25',
            '2020-01-05',
            `25${CHAR_NBSP}декабря${CHAR_NBSP}– 5${CHAR_NBSP}января`,
            {short: false},
        ],
        [
            '2019-12-25',
            '2020-01-05',
            `25${CHAR_NBSP}декабря${CHAR_NBSP}– 5${CHAR_NBSP}января${CHAR_NBSP}2020`,
            {short: false, checkAnotherEndYear: true},
        ],
        [
            '2020-01-01',
            '2020-01-01',
            `1${CHAR_NBSP}января${CHAR_NBSP}2020`,
            {short: true},
        ],
        [
            '2020-01-21',
            '2020-01-26',
            `21${CHAR_NBSP}– 26${CHAR_NBSP}янв'20`,
            {short: true},
        ],
        [
            '2020-04-01',
            '2020-05-01',
            `1${CHAR_NBSP}апр'20${CHAR_NBSP}– 1${CHAR_NBSP}мая'20`,
            {short: true},
        ],
        [
            '2020-04-01',
            '2021-05-01',
            `1${CHAR_NBSP}апр'20${CHAR_NBSP}– 1${CHAR_NBSP}мая'21`,
            {short: true},
        ],
        [
            '2020-01-01',
            '2020-01-01',
            `1${CHAR_NBSP}января${CHAR_NBSP}2020`,
            {short: false},
        ],
        [
            '2020-01-21',
            '2020-01-26',
            `21${CHAR_NBSP}– 26${CHAR_NBSP}января${CHAR_NBSP}2020`,
            {short: false},
        ],
        [
            '2020-04-01',
            '2020-05-01',
            `1${CHAR_NBSP}апреля${CHAR_NBSP}2020${CHAR_NBSP}– 1${CHAR_NBSP}мая${CHAR_NBSP}2020`,
            {short: false},
        ],
        [
            '2020-04-01',
            '2021-05-01',
            `1${CHAR_NBSP}апреля${CHAR_NBSP}2020${CHAR_NBSP}– 1${CHAR_NBSP}мая${CHAR_NBSP}2021`,
            {short: false},
        ],
    ];

    it.each(cases)(
        'start: %p, end: %p, short: %p, expected: %p',
        (start, end, expected, options) => {
            expect(humanizePeriod(start, end, options)).toBe(expected);
        },
    );
});
