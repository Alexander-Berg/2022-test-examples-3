import {momentTimezone as moment} from '../../../../reexports';

import {getPeriodText, getCustomText, maskToString} from '../stringifyMask';

jest.mock('../../../i18n/mask-patterns', () =>
    jest.fn((name, params) => {
        switch (name) {
            case 'everyday':
                return 'ежедневно';
            case 'period-format':
                return 'DD.MM';
            case 'period-full-format':
                return 'DD.MM.YYYY';
            case 'dates':
                return `${params.dates} и в др. дни`;
            case 'period-from-to':
                return `ежедневно с ${params.start} по ${params.end}`;
            case 'before-date':
                return `до ${params.date}`;
        }
    }),
);

const lang = 'ru';
const today = moment('2016-07-15', 'YYYY-MM-DD');
const timezone = 'Asia/Yekaterinburg';

moment.locale(lang);

describe('stringifyMask', () => {
    describe('getPeriodText', () => {
        it('should return period string for dates in same year', () => {
            const mask = {
                2016: {
                    4: [
                        ...Array.from({length: 14}, () => 1),
                        ...Array.from({length: 17}, () => 0),
                    ],
                },
            };

            expect(getPeriodText(mask, timezone)).toEqual(
                'ежедневно с 01.04 по 14.04',
            );
        });

        it('should return period string for dates in different years', () => {
            const mask = {
                2016: {
                    12: [
                        ...Array.from({length: 14}, () => 0),
                        ...Array.from({length: 17}, () => 1),
                    ],
                },
                2017: {
                    1: [
                        ...Array.from({length: 14}, () => 1),
                        ...Array.from({length: 17}, () => 0),
                    ],
                },
            };

            expect(getPeriodText(mask, timezone)).toEqual(
                'ежедневно с 15.12.2016 по 14.01.2017',
            );
        });
    });

    describe('getCustomText', () => {
        it('should return empty string for empty mask', () => {
            expect(getCustomText({}, today, timezone)).toEqual('');
        });

        it('should return single date', () => {
            const mask = {
                2016: {
                    7: [...Array.from({length: 30}, () => 0), 1],
                },
            };

            expect(getCustomText(mask, today, timezone)).toEqual('31 июля');
        });

        it('should return several dates', () => {
            const mask = {
                2016: {
                    7: [...Array.from({length: 28}, () => 0), 1, 1, 1],
                },
            };

            expect(getCustomText(mask, today, timezone)).toEqual(
                '29, 30, 31 июля',
            );
        });

        it('should return only five dates', () => {
            const mask = {
                2016: {
                    7: [
                        ...Array.from({length: 26}, () => 0),
                        ...Array.from({length: 5}, () => 1),
                    ],
                },
            };

            expect(getCustomText(mask, today, timezone)).toEqual(
                '27, 28, 29, 30, 31 июля',
            );
        });

        it('should return days from current day', () => {
            const mask = {
                2016: {
                    7: Array.from({length: 31}, () => 1),
                },
            };

            expect(getCustomText(mask, today, timezone)).toEqual(
                '15, 16, 17, 18, 19 июля и в др. дни',
            );
        });

        it('should return last date', () => {
            const mask = {
                2016: {
                    6: Array.from({length: 30}, () => 1),
                },
            };

            expect(getCustomText(mask, today, timezone)).toEqual('до 30 июня');
        });

        it('should return only five dates an dots', () => {
            const mask = {
                2016: {
                    7: [
                        ...Array.from({length: 26}, () => 0),
                        ...Array.from({length: 5}, () => 1),
                    ],
                    8: Array.from({length: 31}, () => 1),
                },
            };

            expect(getCustomText(mask, today, timezone)).toEqual(
                '27, 28, 29, 30, 31 июля и в др. дни',
            );
        });

        it('should return dates from different months', () => {
            const singleYearMask = {
                2016: {
                    8: [...Array.from({length: 30}, () => 0), 1],
                    9: [...Array.from({length: 29}, () => 0), 1],
                    10: [...Array.from({length: 30}, () => 0), 1],
                    11: [...Array.from({length: 29}, () => 0), 1],
                    12: [...Array.from({length: 30}, () => 0), 1],
                },
            };
            const differentYearsMask = {
                2016: {
                    12: [...Array.from({length: 30}, () => 0), 1],
                },
                2017: {
                    1: [...Array.from({length: 30}, () => 0), 1],
                },
            };

            expect(getCustomText(singleYearMask, today, timezone)).toEqual(
                '31 августа, 30 сентября, 31 октября, 30 ноября, 31 декабря',
            );
            expect(getCustomText(differentYearsMask, today, timezone)).toEqual(
                '31 декабря, 31 января',
            );
        });
    });

    describe('maskToString', () => {
        it('should return string for everyday mask', () => {
            const mask = {
                2016: {
                    7: Array.from({length: 31}, () => 1),
                    8: Array.from({length: 31}, () => 1),
                    9: Array.from({length: 30}, () => 1),
                    10: Array.from({length: 31}, () => 1),
                },
            };

            expect(maskToString(mask, today, timezone)).toEqual('ежедневно');
        });

        it('should return string for period mask', () => {
            const mask = {
                2016: {
                    4: [
                        ...Array.from({length: 14}, () => 1),
                        ...Array.from({length: 17}, () => 0),
                    ],
                },
            };

            expect(maskToString(mask, today, timezone)).toEqual(
                'ежедневно с 01.04 по 14.04',
            );
        });

        it('should return string for custom mask', () => {
            const mask = {
                2016: {
                    7: [...Array.from({length: 28}, () => 0), 1, 1, 1],
                },
            };

            expect(maskToString(mask, today, timezone)).toEqual(
                '29, 30, 31 июля',
            );
        });
    });
});
