import {
    withDefaults,
    prepareData,
    prepareForCheck,
    parseData,
} from './DutySchedules.parsers';

describe('withDefaults', () => {
    it('Should add default values', () => {
        expect(withDefaults({ foo: 'bar' })).toEqual({
            foo: 'bar',
            role: { id: null },
            personsCount: 1,
            startTime: '00:00',
            duration: 5,
            dutyOnHolidays: true,
            dutyOnWeekends: true,
            considerOtherSchedules: true,
            showInStaff: true,
        });
    });

    it('Should not override existing values', () => {
        expect(withDefaults({ duration: -2 }).duration).toBe(-2);
    });
});

describe('prepareData', () => {
    it('Should add service id to the data', () => {
        expect(prepareData(42, {}).service).toBe(42);
    });

    it('Should convert fields to backend format', () => {
        expect(prepareData(0, { id: 1 }).id).toBe(1);
        expect(prepareData(0, { name: 'John' }).name).toBe('John');
        expect(prepareData(0, { slug: 'snail' }).slug).toBe('snail');
        expect(prepareData(0, { role: { id: 42, foo: 'bar' } }).role).toBe(42);
        expect(prepareData(0, { roleOnDuty: 11 }).role_on_duty).toBe(11);
        expect(prepareData(0, { personsCount: 2 }).persons_count).toBe(2);
        expect(prepareData(0, { startDate: new Date(2000, 0, 1) }).start_date).toBe('2000-01-01');
        expect(prepareData(0, { startTime: '12:34' }).start_time).toBe('12:34');
        expect(prepareData(0, { duration: 1 }).duration).toBe(24 * 60 * 60); // дни переводятся в секунды
        expect(prepareData(0, { dutyOnWeekends: true }).duty_on_weekends).toBe(true);
        expect(prepareData(0, { dutyOnHolidays: true }).duty_on_holidays).toBe(true);
        expect(prepareData(0, { considerOtherSchedules: false }).consider_other_schedules).toBe(false);
        expect(prepareData(0, { showInStaff: true }).show_in_staff).toBe(true);
        expect(prepareData(0, { needOrder: true, orders: [] }).algorithm).toBe('manual_order');
        expect(prepareData(0, { needOrder: true, orders: [{ login: 'john' }] }).orders).toEqual(['john']);
    });

    it('Should keep absent fields undefined', () => {
        expect(prepareData(0, {}).id).toBeUndefined(); // поле без преобразования
        expect(prepareData(0, {}).start_date).toBeUndefined(); // поле с преобразованием
    });

    it('Should convert boolean needOrder to algorithm enum', () => {
        expect(prepareData(0, { needOrder: 12 }).algorithm).toBeUndefined();
        expect(prepareData(0, { needOrder: false }).algorithm).toBe('no_order');
        expect(prepareData(0, { needOrder: true, orders: [] }).algorithm).toBe('manual_order');
    });

    it('Should ignore field `order` when needOrder !== true', () => {
        expect(prepareData(0, { orders: [{ login: 'john' }] }).orders).toBeUndefined();
        expect(prepareData(0, { needOrder: false, orders: [{ login: 'john' }] }).orders).toBeUndefined();
    });

    it('Should keep meaningful null values', () => {
        // поле Роль можно очистить
        expect(prepareData(0, { role: null }).role).toBeNull();
        expect(prepareData(0, { role: { id: null } }).role).toBeNull();
    });

    // В общем случае, подготовка данных не должна подменять значения, которые ей передали, её задача не в этом
    it('Should keep passed falsy values unchanged', () => {
        expect(prepareData(0, { startDate: null }).start_date).toBeNull();
        expect(prepareData(0, { duration: 0 }).duration).toBe(0);
        expect(prepareData(0, { name: '' }).name).toBe('');
        expect(prepareData(0, { name: undefined }).name).toBeUndefined();
    });
});

describe('prepareForCheck', () => {
    it('Should add service id to the data', () => {
        expect(prepareForCheck(42, {}).service).toBe(42); // new calendar
        expect(prepareForCheck(42, { id: 11 }).service).toBe(42); // existing calendar
    });

    it('Should not add service id for deleted calendars', () => {
        expect(prepareForCheck(42, { id: 1, status: 'deleted' }).service).toBeUndefined();
    });

    it('Should add defaults to new calendars', () => {
        // keep in sync with withDefaults(), only in backend format
        const defaults = {
            role: null,
            persons_count: 1,
            start_time: '00:00',
            duration: 432000,
            duty_on_holidays: true,
            duty_on_weekends: true,
            consider_other_schedules: true,
            show_in_staff: true,
        };

        expect(prepareForCheck(0, {
            slug: 'dev',
        })).toEqual({
            ...defaults,
            slug: 'dev',
            service: 0,
        });
    });

    it('Should not add defaults to existing calendars', () => {
        expect(prepareForCheck(0, {
            id: 11,
            slug: 'dev',
        })).toEqual({
            id: 11,
            slug: 'dev',
            service: 0,
        });
    });

    it('Should only send state for deleted calendars', () => {
        expect(prepareForCheck(0, {
            id: 42,
            status: 'deleted',
            foo: 'bar',
        })).toEqual({
            id: 42,
            status: 'deleted',
        });
    });

    it('Should include field `orders` for existing calendars, unless needOrder is explicitly false', () => {
        expect(prepareForCheck(42, {
            id: 1, // есть id, значит существующий календарь
            orders: [{ login: 'vasya' }],
        }).orders).toEqual(['vasya']);
        expect(prepareForCheck(42, {
            id: 1,
            needOrder: true,
            orders: [{ login: 'vasya' }],
        }).orders).toEqual(['vasya']);

        // если явно false, то orders не нужны в диффе
        expect(prepareForCheck(42, {
            id: 1,
            needOrder: false,
            orders: [{ login: 'vasya' }],
        }).orders).toBeUndefined();
    });
});

describe('parseData', () => {
    it('Should convert fields to frontend format', () => {
        expect(parseData({ id: 1 }).id).toBe(1);
        expect(parseData({ service: { foo: 'bar' } }).service).toEqual({ foo: 'bar' });
        expect(parseData({ name: 'John' }).name).toBe('John');
        expect(parseData({ slug: 'snail' }).slug).toBe('snail');
        expect(parseData({ role: { id: 42, foo: 'bar' } }).role).toEqual({ id: 42, foo: 'bar' });
        expect(parseData({ role_on_duty: 11 }).roleOnDuty).toBe(11);
        expect(parseData({ persons_count: 2 }).personsCount).toBe(2);
        expect(parseData({ start_date: '2000-01-01' }).startDate).toMatchDate(new Date(Date.UTC(2000, 0, 1)));
        expect(parseData({ start_time: '01:02:03' }).startTime).toBe('01:02');
        expect(parseData({ duration: '42 00:00:00' }).duration).toBe(42); // приходит стрингой с часами
        expect(parseData({ duty_on_holidays: true }).dutyOnHolidays).toBe(true);
        expect(parseData({ duty_on_weekends: true }).dutyOnWeekends).toBe(true);
        expect(parseData({ consider_other_schedules: false }).considerOtherSchedules).toBe(false);
        expect(parseData({ show_in_staff: true }).showInStaff).toBe(true);
        expect(parseData({ algorithm: 'manual_order' }).needOrder).toBe(true);
    });

    it('Should convert algorithm enum boolean needOrder', () => {
        expect(parseData({ algorithm: 'manual_order' }).needOrder).toBe(true);
        expect(parseData({ algorithm: 'no_order' }).needOrder).toBe(false);
        expect(parseData({ algorithm: 'foobar' }).needOrder).toBe(false);
        expect(parseData({}).needOrder).toBe(false);
    });
});
