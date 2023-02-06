import {TRAIN_TYPE} from '../../transportType';

import {getSubtitle} from '../searchTitle';

const context = {
    transportType: 'all',
    time: {
        now: 1541584692842,
        timezone: 'Asia/Yekaterinburg',
    },
};

describe('searchTitle', () => {
    it('getSubtitle. Для поиска на дату должна вернуть дату', () => {
        expect(
            getSubtitle({
                ...context,
                when: {
                    text: '8 ноября',
                    hint: '8 ноября',
                    date: '2018-11-08',
                    formatted: '8 ноября',
                    nextDate: '2018-11-09',
                },
            }),
        ).toBe('8 november, thursday'); // тут у нас нет настройки локали поэтому на английском
    });

    it(
        'getSubtitle. Для поиска на сегодня должна вернуть дату,' +
            'потому что не используются слагм в урл',
        () => {
            expect(
                getSubtitle({
                    ...context,
                    transportType: TRAIN_TYPE,
                    when: {
                        text: 'сегодня',
                        date: '2018-11-07',
                        special: 'today',
                    },
                }),
            ).toBe('7 november, wednesday'); // тут у нас нет настройки локали поэтому на английском
        },
    );

    it('getSubtitle. Для поиска на все дни вернет пустую строку', () => {
        expect(
            getSubtitle({
                ...context,
                when: {
                    text: 'на все дни',
                    hint: 'на все дни',
                    special: 'all-days',
                    formatted: 'на все дни',
                },
            }),
        ).toBe('');
    });
});
