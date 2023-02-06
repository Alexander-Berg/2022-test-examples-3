const moment = require('../../../../reexports').momentTimezone;

jest.dontMock('../../search/isAllDaysSearch');

const params = jest.fn();

jest.setMock('../../yaMetrika', {params});

const reachGoalTomorrowSegments = require.requireActual(
    '../reachGoalTomorrowSegments',
).default;

const now = 1518434359943; // 2018-02-12 16:19:19
const timezone = 'Asia/Novosibirsk';
const today = moment(now);

describe('reachGoalTomorrowSegments', () => {
    describe('Отправка параметров не происходит, если ...', () => {
        const context = {
            when: {
                date: today.format('YYYY-MM-DD'),
            },
            time: {
                now,
            },
            from: {
                timezone,
            },
        };
        const segments = [{}];

        it('поиск на все дни', () => {
            reachGoalTomorrowSegments(segments, {
                ...context,
                when: {
                    special: 'all-days',
                },
            });
            expect(params).not.toHaveBeenCalled();
        });

        it('нет результатов поиска', () => {
            reachGoalTomorrowSegments([], context);
            expect(params).not.toHaveBeenCalled();
        });
    });

    [
        {
            key: 'onlyTomorrowSegments',
            date: today,
        },
        {
            key: 'onlyNextDateSegments',
            date: today.clone().add(1, 'week'),
        },
    ].forEach(({key, date}) => {
        const dateSegment = {
            departure: `${date.format('YYYY-MM-DD')}T10:33:00+00:00`,
            stationFrom: {timezone},
        };
        const nextDateSegment = {
            // С учетом таймзоны - это следующие сутки
            departure: `${date.format('YYYY-MM-DD')}T18:05:00+00:00`,
            stationFrom: {timezone},
        };
        const context = {
            when: {
                date: date.format('YYYY-MM-DD'),
            },
            time: {
                now,
            },
            from: {
                timezone,
            },
        };

        describe(`Отправляем параметр ${key} со значением true, если...`, () => {
            it('есть только сегменты с датой отправления на завтра', () => {
                reachGoalTomorrowSegments([nextDateSegment], context);
                expect(params).toHaveBeenCalledWith({[key]: true});
            });

            it('есть сегменты с датой отправления на завтра и все сегменты на сегодня ушли', () => {
                reachGoalTomorrowSegments(
                    [
                        {
                            ...dateSegment,
                            isGone: true,
                        },
                        nextDateSegment,
                    ],
                    context,
                );
                expect(params).toHaveBeenCalledWith({[key]: true});
            });
        });

        describe(`Отправляем параметр ${key} со значением false, если...`, () => {
            it('есть только сегменты с датой отправления на сегодня', () => {
                reachGoalTomorrowSegments([dateSegment], context);
                expect(params).toHaveBeenCalledWith({[key]: false});
            });

            it('есть только сегменты с датой отправления на сегодня, но все ушли', () => {
                reachGoalTomorrowSegments(
                    [
                        {
                            ...dateSegment,
                            isGone: true,
                        },
                    ],
                    context,
                );
                expect(params).toHaveBeenCalledWith({[key]: false});
            });

            it('есть сегменты на сегодня и на завтра, но на сегодня еще не все ушли', () => {
                reachGoalTomorrowSegments(
                    [dateSegment, nextDateSegment],
                    context,
                );
                expect(params).toHaveBeenCalledWith({[key]: false});
            });
        });
    });
});
