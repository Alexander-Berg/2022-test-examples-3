import {insertingDaysFixture} from '../__fixtures__/fixtures';
import {insertMissedDays} from '../utils';
import {ServiceTimeDayOfWeekPeriodEntity} from '../types';

declare const test: jest.It;

const DEFAULT_START_END_TIMES = {
    endTime: '',
    startTime: '',
};

describe('Working hours utils', () => {
    /**
     * Чтобы не писать огромные фиксутры, эмилируем entity
     * объектами только с необходимыми полями
     */
    test.each(insertingDaysFixture)('insert missed day', (days, expectedDays) => {
        const transformedDays = insertMissedDays(
            days.map(item => ({
                ...item,
                ...DEFAULT_START_END_TIMES,
            })) as ServiceTimeDayOfWeekPeriodEntity[]
        ).map(item => {
            if (typeof item.dayOfWeek === 'string') {
                return {
                    dayOfWeek: {
                        code: item.dayOfWeek,
                    },
                    ...DEFAULT_START_END_TIMES,
                };
            }

            return {
                ...item,
                ...DEFAULT_START_END_TIMES,
            };
        });

        const transformedResul = expectedDays.map(item => {
            if (typeof item.dayOfWeek === 'string') {
                return {
                    dayOfWeek: {
                        code: item,
                    },
                    ...DEFAULT_START_END_TIMES,
                };
            }

            return {
                ...item,
                ...DEFAULT_START_END_TIMES,
            };
        });

        expect(transformedDays).toEqual(transformedResul);
    });
});
