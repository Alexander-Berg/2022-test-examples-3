import * as format from './DateFormat';
import i18n from '../../../shared/lib/i18n';
import * as en from '../../../langs/yamb/en.json';
import * as ru from '../../../langs/yamb/ru.json';

const OrgDate = global.Date;

function mockDate() {
    (global.Date as unknown) = new Proxy(Date, {
        construct(target, args: Parameters<DateConstructor>) {
            if (args.length === 0) {
                return new target('June 20, 2019 18:23:30');
            }

            return new target(...args);
        },
    });
}

function unmockDate() {
    global.Date = OrgDate;
}

describe('LL', () => {
    describe('lang en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('Дата в формате день, полный месяц, полный год', () => {
            expect(format.dateFull(new Date(1990, 10, 1, 0, 0, 0))).toBe('1 November 1990');
            expect(format.dateFull(new Date(2018, 3, 4, 10, 18, 0))).toBe('4 April 2018');
        });
    });

    describe('lang ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('Дата в формате день, полный месяц, полный год', () => {
            expect(format.dateFull(new Date(1990, 10, 1, 0, 0, 0))).toBe('1 ноября 1990 г.');
            expect(format.dateFull(new Date(2018, 3, 4, 10, 18, 0))).toBe('4 апреля 2018 г.');
        });
    });
});

describe('DMMMM', () => {
    describe('lang en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('Дата в формате день, полный месяц', () => {
            expect(format.dayAndMonth(new Date(1990, 10, 1, 0, 0, 0))).toBe('1 November');
            expect(format.dayAndMonth(new Date(2230, 0, 12, 1, 1, 1))).toBe('12 January');
        });
    });

    describe('lang ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('Дата в формате день, полный месяц', () => {
            expect(format.dayAndMonth(new Date(1990, 10, 1, 0, 0, 0))).toBe('1 ноября');
            expect(format.dayAndMonth(new Date(2230, 0, 12, 1, 1, 1))).toBe('12 января');
        });
    });
});

describe('dd', () => {
    describe('lang en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('Краткий формат дня недели', () => {
            expect(format.weekdayNameShort(new Date(1990, 10, 1, 0, 0, 0))).toBe('Thu');
            expect(format.weekdayNameShort(new Date(2018, 5, 20, 0, 0, 0))).toBe('Wed');
        });
    });

    describe('lang ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('Краткий формат дня недели', () => {
            expect(format.weekdayNameShort(new Date(1990, 10, 1, 0, 0, 0))).toBe('Чт');
            expect(format.weekdayNameShort(new Date(2018, 5, 20, 0, 0, 0))).toBe('Ср');
        });
    });
});

describe('dddd', () => {
    describe('lang en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('День недели', () => {
            expect(format.weekdayName(new Date(1990, 10, 1, 0, 0, 0))).toBe('Thursday');
            expect(format.weekdayName(new Date(2018, 5, 20, 0, 0, 0))).toBe('Wednesday');
            expect(format.weekdayName(new Date(2018, 5, 19, 0, 0, 0))).toBe('Tuesday');
            expect(format.weekdayName(new Date(2018, 5, 18, 0, 0, 0))).toBe('Monday');
            expect(format.weekdayName(new Date(2018, 5, 17, 0, 0, 0))).toBe('Sunday');
            expect(format.weekdayName(new Date(2018, 5, 16, 0, 0, 0))).toBe('Saturday');
            expect(format.weekdayName(new Date(2018, 5, 15, 0, 0, 0))).toBe('Friday');
        });

        it('День недели без склонения', () => {
            expect(format.weekdayName(new Date(1990, 10, 1, 0, 0, 0), false)).toBe('Thursday');
            expect(format.weekdayName(new Date(2018, 5, 20, 0, 0, 0), false)).toBe('Wednesday');
            expect(format.weekdayName(new Date(2018, 5, 19, 0, 0, 0), false)).toBe('Tuesday');
            expect(format.weekdayName(new Date(2018, 5, 18, 0, 0, 0), false)).toBe('Monday');
            expect(format.weekdayName(new Date(2018, 5, 17, 0, 0, 0), false)).toBe('Sunday');
            expect(format.weekdayName(new Date(2018, 5, 16, 0, 0, 0), false)).toBe('Saturday');
            expect(format.weekdayName(new Date(2018, 5, 15, 0, 0, 0), false)).toBe('Friday');
        });

        it('День недели не зависит от склонения', () => {
            expect(format.weekdayName(new Date(1990, 10, 1, 0, 0, 0), true)).toBe('Thursday');
            expect(format.weekdayName(new Date(2018, 5, 20, 0, 0, 0), true)).toBe('Wednesday');
            expect(format.weekdayName(new Date(2018, 5, 19, 0, 0, 0), true)).toBe('Tuesday');
            expect(format.weekdayName(new Date(2018, 5, 18, 0, 0, 0), true)).toBe('Monday');
            expect(format.weekdayName(new Date(2018, 5, 17, 0, 0, 0), true)).toBe('Sunday');
            expect(format.weekdayName(new Date(2018, 5, 16, 0, 0, 0), true)).toBe('Saturday');
            expect(format.weekdayName(new Date(2018, 5, 15, 0, 0, 0), true)).toBe('Friday');
        });
    });

    describe('lang ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('День недели по дефолту (без склонения)', () => {
            expect(format.weekdayName(new Date(1990, 10, 1, 0, 0, 0))).toBe('четверг');
            expect(format.weekdayName(new Date(2018, 5, 20, 0, 0, 0))).toBe('среда');
            expect(format.weekdayName(new Date(2018, 5, 19, 0, 0, 0))).toBe('вторник');
            expect(format.weekdayName(new Date(2018, 5, 18, 0, 0, 0))).toBe('понедельник');
            expect(format.weekdayName(new Date(2018, 5, 17, 0, 0, 0))).toBe('воскресенье');
            expect(format.weekdayName(new Date(2018, 5, 16, 0, 0, 0))).toBe('суббота');
            expect(format.weekdayName(new Date(2018, 5, 15, 0, 0, 0))).toBe('пятница');
        });

        it('День недели без склонения', () => {
            expect(format.weekdayName(new Date(1990, 10, 1, 0, 0, 0), false)).toBe('четверг');
            expect(format.weekdayName(new Date(2018, 5, 20, 0, 0, 0), false)).toBe('среда');
            expect(format.weekdayName(new Date(2018, 5, 19, 0, 0, 0), false)).toBe('вторник');
            expect(format.weekdayName(new Date(2018, 5, 18, 0, 0, 0), false)).toBe('понедельник');
            expect(format.weekdayName(new Date(2018, 5, 17, 0, 0, 0), false)).toBe('воскресенье');
            expect(format.weekdayName(new Date(2018, 5, 16, 0, 0, 0), false)).toBe('суббота');
            expect(format.weekdayName(new Date(2018, 5, 15, 0, 0, 0), false)).toBe('пятница');
        });

        it('День недели в винительном падеже', () => {
            expect(format.weekdayName(new Date(1990, 10, 1, 0, 0, 0), true)).toBe('четверг');
            expect(format.weekdayName(new Date(2018, 5, 20, 0, 0, 0), true)).toBe('среду');
            expect(format.weekdayName(new Date(2018, 5, 19, 0, 0, 0), true)).toBe('вторник');
            expect(format.weekdayName(new Date(2018, 5, 18, 0, 0, 0), true)).toBe('понедельник');
            expect(format.weekdayName(new Date(2018, 5, 17, 0, 0, 0), true)).toBe('воскресенье');
            expect(format.weekdayName(new Date(2018, 5, 16, 0, 0, 0), true)).toBe('субботу');
            expect(format.weekdayName(new Date(2018, 5, 15, 0, 0, 0), true)).toBe('пятницу');
        });
    });
});

describe('HHmm', () => {
    describe('lang en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('Время ЧЧ:ММ', () => {
            expect(format.time(new Date(1990, 10, 1, 0, 0, 0))).toBe('00:00');
            expect(format.time(new Date(1990, 10, 1, 10, 59, 59))).toBe('10:59');
        });
    });

    describe('lang ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('Время ЧЧ:ММ', () => {
            expect(format.time(new Date(1990, 10, 1, 0, 0, 0))).toBe('00:00');
            expect(format.time(new Date(1990, 10, 1, 10, 59, 59))).toBe('10:59');
        });
    });
});

describe('DMMYY', () => {
    describe('lang en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('День/Месяц/Год', () => {
            expect(format.dateShort(new Date(1990, 10, 1, 0, 0, 0))).toBe('1/11/90');
            expect(format.dateShort(new Date(2018, 5, 20, 0, 0, 0))).toBe('20/06/18');
        });
    });

    describe('lang ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('День.Месяц.Год', () => {
            expect(format.dateShort(new Date(1990, 10, 1, 0, 0, 0))).toBe('1.11.90');
            expect(format.dateShort(new Date(2018, 5, 20, 0, 0, 0))).toBe('20.06.18');
        });
    });
});

describe('Smart date', () => {
    describe('lang en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
            mockDate();
        });

        afterAll(() => {
            unmockDate();
        });

        it('дата должна быть в формате "dd month yyyy, hh:mm"', () => {
            expect(format.smartDate(new Date(1990, 10, 1, 0, 0, 0))).toBe('1 Nov 1990, 00:00');
            expect(format.smartDate(new Date(2017, 2, 20, 10, 11, 0))).toBe('20 Mar 2017, 10:11');
        });

        it('дата должна быть в формате "dd month, hh:mm", текущий год не показывается', () => {
            expect(format.smartDate(new Date(2019, 5, 22, 10, 11, 0))).toBe('22 June, 10:11');
        });

        it('дата должна быть в формате "Today, hh:mm"', () => {
            expect(format.smartDate(new Date())).toBe('Today, 18:23');
        });

        it('дата должна быть в формате "Yesterday, hh:mm"', () => {
            expect(format.smartDate(new Date('June 19, 2019 18:23:30'))).toBe('Yesterday, 18:23');
        });
    });

    describe('lang ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
            mockDate();
        });

        afterAll(() => {
            unmockDate();
        });

        it('дата должна быть в формате "dd month yyyy, hh:mm"', () => {
            expect(format.smartDate(new Date(1990, 10, 1, 0, 0, 0))).toBe('1 ноя 1990, 00:00');
            expect(format.smartDate(new Date(2017, 5, 20, 10, 11, 0))).toBe('20 июня 2017, 10:11');
        });

        it('дата должна быть в формате "dd month, hh:mm", текущий год не показывается', () => {
            expect(format.smartDate(new Date(2019, 5, 22, 10, 11, 0))).toBe('22 июня, 10:11');
        });

        it('дата должна быть в формате "Сегодня, hh:mm"', () => {
            expect(format.smartDate(new Date())).toBe('Сегодня, 18:23');
        });

        it('дата должна быть в формате "Вчера, hh:mm"', () => {
            expect(format.smartDate(new Date('June 19, 2019 18:23:30'))).toBe('Вчера, 18:23');
        });
    });
});

describe('L', () => {
    describe('lang en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('дата должна быть в формате dd/mm/yyyy', () => {
            expect(format.dateMiddle(new Date(1990, 10, 1, 0, 0, 0))).toBe('01/11/1990');
            expect(format.dateMiddle(new Date(2018, 5, 20, 0, 0, 0))).toBe('20/06/2018');
        });
    });

    describe('lang ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('дата должна быть в формате dd.mm.yyyy', () => {
            expect(format.dateMiddle(new Date(1990, 10, 1, 0, 0, 0))).toBe('01.11.1990');
            expect(format.dateMiddle(new Date(2018, 5, 20, 0, 0, 0))).toBe('20.06.2018');
        });
    });
});

describe('responseTime', () => {
    describe('lang ru', () => {
        beforeAll(() => {
            i18n.locale('ru', ru);
        });

        it('Среднее время ответа в секундах', () => {
            expect(format.responseTime(0)).toBe('время ответа ≈0 сек');
            expect(format.responseTime(1000)).toBe('время ответа ≈1 сек');
            expect(format.responseTime(3000)).toBe('время ответа ≈3 сек');
            expect(format.responseTime(20000)).toBe('время ответа ≈20 сек');
            expect(format.responseTime(59000)).toBe('время ответа ≈59 сек');
        });

        it('Среднее время ответа в минутах', () => {
            expect(format.responseTime(60000)).toBe('время ответа ≈1 мин');
            expect(format.responseTime(90001)).toBe('время ответа ≈2 мин');
            expect(format.responseTime(508000)).toBe('время ответа ≈8 мин');
            expect(format.responseTime(511000)).toBe('время ответа ≈9 мин');
        });

        it('Среднее время ответа в часах', () => {
            expect(format.responseTime(18253000)).toBe('время ответа ≈5 ч');
        });

        it('Среднее время ответа в днях', () => {
            expect(format.responseTime(432000000)).toBe('время ответа ≈5 дн');
        });

        it('Среднее время ответа более недели', () => {
            expect(format.responseTime(85246876000)).toBe('');
            expect(format.responseTime(99999999999999000)).toBe('');
        });
    });

    describe('lang en', () => {
        beforeAll(() => {
            i18n.locale('en', en);
        });

        it('Среднее время ответа в секундах', () => {
            expect(format.responseTime(0)).toBe('average response time ≈0 sec');
            expect(format.responseTime(1000)).toBe('average response time ≈1 sec');
            expect(format.responseTime(3000)).toBe('average response time ≈3 sec');
            expect(format.responseTime(20000)).toBe('average response time ≈20 sec');
            expect(format.responseTime(59000)).toBe('average response time ≈59 sec');
        });

        it('Среднее время ответа в минутах', () => {
            expect(format.responseTime(60000)).toBe('average response time ≈1 min');
            expect(format.responseTime(90001)).toBe('average response time ≈2 min');
            expect(format.responseTime(508000)).toBe('average response time ≈8 min');
            expect(format.responseTime(511000)).toBe('average response time ≈9 min');
        });

        it('Среднее время ответа в часах', () => {
            expect(format.responseTime(18253000)).toBe('average response time ≈5 hr');
        });

        it('Среднее время ответа в днях', () => {
            expect(format.responseTime(432000000)).toBe('average response time ≈5 d');
        });

        it('Среднее время ответа более недели', () => {
            expect(format.responseTime(85246876000)).toBe('');
            expect(format.responseTime(99999999999999000)).toBe('');
        });
    });
});

describe('#weekdayFromat17To06', () => {
    it('Конвертирует формат нумерации дней недели общепринятый формат', () => {
        expect(format.weekdayFromat17To06(1)).toBe(1);
        expect(format.weekdayFromat17To06(2)).toBe(2);
        expect(format.weekdayFromat17To06(3)).toBe(3);
        expect(format.weekdayFromat17To06(4)).toBe(4);
        expect(format.weekdayFromat17To06(5)).toBe(5);
        expect(format.weekdayFromat17To06(6)).toBe(6);
        expect(format.weekdayFromat17To06(7)).toBe(0);
    });
});

describe('#nextWeekday', () => {
    it('Возвращает номер следющего дня недели', () => {
        expect(format.nextWeekday(0)).toBe(1);
        expect(format.nextWeekday(1)).toBe(2);
        expect(format.nextWeekday(2)).toBe(3);
        expect(format.nextWeekday(3)).toBe(4);
        expect(format.nextWeekday(4)).toBe(5);
        expect(format.nextWeekday(5)).toBe(6);
        expect(format.nextWeekday(6)).toBe(0);
    });
});

describe('#prevWeekday', () => {
    it('Возвращает номер предыдущего дня недели', () => {
        expect(format.prevWeekday(0)).toBe(6);
        expect(format.prevWeekday(1)).toBe(0);
        expect(format.prevWeekday(2)).toBe(1);
        expect(format.prevWeekday(3)).toBe(2);
        expect(format.prevWeekday(4)).toBe(3);
        expect(format.prevWeekday(5)).toBe(4);
        expect(format.prevWeekday(6)).toBe(5);
    });
});

describe('#isToday', () => {
    it('Возвращает истину, если день-месяц-год двух дат совпадают', () => {
        expect(format.isToday(new Date('2019.01.01 00:00'), new Date('2019.01.01 23:59'))).toBe(true);
        expect(format.isToday(new Date('2000.02.29 01:13'), new Date('2000.02.29 12:00'))).toBe(true);
    });

    it('Возвращает ложь, если день-месяц-год двух дат не совпадают', () => {
        expect(format.isToday(new Date('2019.01.01 00:00'), new Date('2019.01.01 24:00'))).toBe(false);
        expect(format.isToday(new Date('2000.02.29 01:13'), new Date('2004.02.29 12:00'))).toBe(false);
    });

    it('Возвращает ответ относительно текущей даты, если параметр date2 не указан', () => {
        const today = new Date();
        today.setHours(12, 0);

        expect(format.isToday(new Date('1970.01.01 00:00'))).toBe(false);
        expect(format.isToday(today)).toBe(true);
    });
});

describe('#isYesterday', () => {
    it('Возвращает истину, если первая дата находится в предыдущем дне относительно второй', () => {
        expect(format.isYesterday(new Date('2019.01.01 00:00'), new Date('2019.01.02 23:59'))).toBe(true);
        expect(format.isYesterday(new Date('1997.11.05 12:15'), new Date('1997.11.06 04:20'))).toBe(true);
        expect(format.isYesterday(new Date('1997.11.05 12:15'), new Date('1997.11.06 14:20'))).toBe(true);
        expect(format.isYesterday(new Date('2000.02.28 23:59'), new Date('2000.02.29 00:00'))).toBe(true);
    });

    it('Возвращает ложь, если первая дата не находится в предыдущем дне относительно второй', () => {
        expect(format.isYesterday(new Date('2019.01.01 00:00'), new Date('2019.01.01 08:00'))).toBe(false);
        expect(format.isYesterday(new Date('2000.02.29 01:13'), new Date('2004.02.29 12:00'))).toBe(false);
    });

    it('Возвращает ответ относительно текущей даты, если параметр date2 не указан', () => {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);

        expect(format.isYesterday(new Date('1970.01.01 00:00'))).toBe(false);
        expect(format.isYesterday(yesterday)).toBe(true);
    });
});

describe('#isTomorrow', () => {
    it('Возвращает истину, если первая дата находится в предыдущем дне относительно второй', () => {
        expect(format.isTomorrow(new Date('2019.01.02 23:59'), new Date('2019.01.01 00:00'))).toBe(true);
        expect(format.isTomorrow(new Date('1997.11.06 04:20'), new Date('1997.11.05 12:15'))).toBe(true);
        expect(format.isTomorrow(new Date('1997.11.06 14:20'), new Date('1997.11.05 12:15'))).toBe(true);
        expect(format.isTomorrow(new Date('2000.02.29 00:00'), new Date('2000.02.28 23:59'))).toBe(true);
    });

    it('Возвращает ложь, если первая дата не находится в предыдущем дне относительно второй', () => {
        expect(format.isTomorrow(new Date('2019.01.01 08:00'), new Date('2019.01.01 00:00'))).toBe(false);
        expect(format.isTomorrow(new Date('2004.02.29 12:00'), new Date('2000.02.29 01:13'))).toBe(false);
    });

    it('Возвращает ответ относительно текущей даты, если параметр date2 не указан', () => {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);

        expect(format.isTomorrow(new Date('1970.01.01 00:00'))).toBe(false);
        expect(format.isTomorrow(tomorrow)).toBe(true);
    });
});
