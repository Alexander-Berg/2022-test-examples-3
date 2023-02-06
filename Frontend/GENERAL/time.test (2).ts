import {
    getLocalTimezone,
    getTimeArrayFromString,
    getTimeStringFromArray,
    getTimezoneOffsetMinutesFromString,
} from 'utils/time';

describe('utils/time/getTimezoneOffsetMinutesFromString', () => {
    it('Должна корректно получать из строчного представления времени смещение в минутах', () => {
        expect(getTimezoneOffsetMinutesFromString('12:34:56+0430')).toEqual(270);
    });

    it('Должна корректно получать из строчного представления времени смещение в минутах 2', () => {
        expect(getTimezoneOffsetMinutesFromString('01:02:00+1000')).toEqual(600);
    });

    it('Должна корректно получать из строчного представления времени отрицательное смещение в минутах', () => {
        expect(getTimezoneOffsetMinutesFromString('12:34:56-0215')).toEqual(-135);
    });

    it('Должна корректно получать из строчного представления времени нулевое смещение в минутах', () => {
        expect(getTimezoneOffsetMinutesFromString('12:34:56+0000')).toEqual(0);
    });

    it('Должна корректно получать из строчного представления времени нулевое смещение в минутах 2', () => {
        expect(getTimezoneOffsetMinutesFromString('12:34:56-0000')).toEqual(0);
    });
});

describe('utils/time/getLocalTimezone', () => {
    const OriginalGetTimezoneOffset = global.Date.prototype.getTimezoneOffset;

    afterEach(() => {
        global.Date.prototype.getTimezoneOffset = OriginalGetTimezoneOffset;
    });

    it('Должна возвращать текущую временную зону', () => {
        global.Date.prototype.getTimezoneOffset = () => -150;
        expect(getLocalTimezone()).toEqual('+0230');
    });

    it('Должна возвращать текущую временную зону 2', () => {
        global.Date.prototype.getTimezoneOffset = () => -780;
        expect(getLocalTimezone()).toEqual('+1300');
    });

    it('Должна возвращать текущую отрицательную временную зону', () => {
        global.Date.prototype.getTimezoneOffset = () => 405;
        expect(getLocalTimezone()).toEqual('-0645');
    });

    it('Должна возвращать текущую нулевую временную зону', () => {
        global.Date.prototype.getTimezoneOffset = () => 0;
        expect(getLocalTimezone()).toEqual('+0000');
    });
});

describe('utils/time/getTimeArrayFromString', () => {
    it('Должна парсить строчное представление времени в массив', () => {
        expect(getTimeArrayFromString('12:34')).toEqual([12, 34]);
    });

    it('Должна парсить строчное представление времени в массив 2', () => {
        expect(getTimeArrayFromString('00:01')).toEqual([0, 1]);
    });

    it('Должна парсить строчное представление времени в массив 3', () => {
        expect(getTimeArrayFromString('10:00')).toEqual([10, 0]);
    });

    it('Должна парсить строчное представление времени в массив 4', () => {
        expect(getTimeArrayFromString('00:00')).toEqual([0, 0]);
    });
});

describe('utils/time/getTimeStringFromArray', () => {
    it('Должна форматировать время, представленное в массиве', () => {
        expect(getTimeStringFromArray([12, 34])).toEqual('12:34');
    });

    it('Должна форматировать время, представленное в массиве 2', () => {
        expect(getTimeStringFromArray([0, 1])).toEqual('00:01');
    });

    it('Должна форматировать время, представленное в массиве 3', () => {
        expect(getTimeStringFromArray([10, 0])).toEqual('10:00');
    });

    it('Должна форматировать время, представленное в массиве 4', () => {
        expect(getTimeStringFromArray([0, 0])).toEqual('00:00');
    });
});
