import Slug from '../../../interfaces/Slug';
import DateRobot from '../../../interfaces/date/DateRobot';

import {getTravelTrainsSearchUrl} from '../getTravelTrainsSearchUrl';

const allDaysContext = {
    fromSlug: 'moscow' as Slug,
    toSlug: 'sochi' as Slug,
};

const dateContext = {
    ...allDaysContext,
    when: '2022-01-12' as DateRobot,
};

describe('getTravelTrainsSearchUrl', () => {
    describe('testing', () => {
        it('вернёт ссылку на дату', () => {
            expect(getTravelTrainsSearchUrl(dateContext, false)).toEqual(
                'https://travel-test.yandex.ru/trains/moscow--sochi/?when=2022-01-12',
            );
        });

        it('вернёт ссылку на все дни', () => {
            expect(getTravelTrainsSearchUrl(allDaysContext, false)).toEqual(
                'https://travel-test.yandex.ru/trains/moscow--sochi/',
            );
        });
    });

    describe('production', () => {
        it('вернёт ссылку на дату', () => {
            expect(getTravelTrainsSearchUrl(dateContext, true)).toEqual(
                'https://travel.yandex.ru/trains/moscow--sochi/?when=2022-01-12',
            );
        });

        it('вернёт ссылку на все дни', () => {
            expect(getTravelTrainsSearchUrl(allDaysContext, true)).toEqual(
                'https://travel.yandex.ru/trains/moscow--sochi/',
            );
        });
    });
});
