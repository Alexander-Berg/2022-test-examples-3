import {replaceMaskValue, getActivePlainMask, EMPTY_DAY} from '../mask';

const NOT_EMPTY_DAY_STRING = 1;
const FORMAT = 'YYYY-MM-DD';
const timezone = 'Asia/Yekaterinburg';

describe('mask', () => {
    describe('replace mask value', () => {
        it('should return original mask', () => {
            const mask = {
                2016: {
                    2: Array.from({length: 31}, () => EMPTY_DAY),
                },
            };
            const replaceValue = 'W';
            const result = replaceMaskValue(mask, replaceValue);

            expect(result).toEqual(mask);
        });

        it('should return modified mask', () => {
            const mask = {
                2016: {
                    4: [
                        ...Array.from({length: 15}, () => EMPTY_DAY),
                        ...Array.from({length: 15}, () => NOT_EMPTY_DAY_STRING),
                    ],
                },
            };
            const replaceValue = 'W';
            const result = replaceMaskValue(mask, replaceValue);

            expect(result).toEqual({
                2016: {
                    4: [
                        ...Array.from({length: 15}, () => EMPTY_DAY),
                        ...Array.from({length: 15}, () => replaceValue),
                    ],
                },
            });
        });
    });

    describe('getActivePlainMask', () => {
        it('should return empty array for empty mask', () => {
            const mask = {
                2016: {
                    2: Array.from({length: 31}, () => EMPTY_DAY),
                },
            };
            const result = getActivePlainMask({mask, timezone});

            expect(result).toEqual([]);
        });

        it('should return array length = 1', () => {
            const emptyDays = Array.from({length: 30}, () => EMPTY_DAY);
            const mask = {
                2016: {
                    1: [...emptyDays, NOT_EMPTY_DAY_STRING],
                },
            };
            const result = getActivePlainMask({mask, timezone});

            expect(result.length).toEqual(1);
        });

        it('should return array for full month', () => {
            const mask = {
                2016: {
                    1: Array.from({length: 31}, () => NOT_EMPTY_DAY_STRING),
                },
            };
            const result = getActivePlainMask({mask, timezone});

            expect(result.length).toEqual(31);
        });

        it('should return array with', () => {
            const mask = {
                2016: {
                    1: [
                        ...Array.from({length: 30}, () => EMPTY_DAY),
                        NOT_EMPTY_DAY_STRING,
                    ],
                },
                2017: {
                    1: [
                        ...Array.from({length: 30}, () => EMPTY_DAY),
                        NOT_EMPTY_DAY_STRING,
                    ],
                },
            };

            const result = getActivePlainMask({mask, timezone});
            const start = result[0];
            const end = result[result.length - 1];

            expect(result.length).toEqual(2);
            expect(start.format(FORMAT)).toEqual('2016-01-31');
            expect(end.format(FORMAT)).toEqual('2017-01-31');
        });
    });
});
