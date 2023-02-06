import moment from 'moment';

import DateMock from 'utilities/testUtils/DateMock';
import {getDateLocale, setDateLocale} from 'utilities/dateUtils';
import locales from 'utilities/dateUtils/locales';
import getHumanDateWithSpecialValues from 'projects/trains/lib/date/getHumanDateWithSpecialValues';

const DEFAULT_LOCALE = getDateLocale();

describe('getHumanDateWithSpecialValues', () => {
    beforeEach(() => {
        setDateLocale(locales.RU);
        DateMock.mock('2019-02-11');
    });

    afterEach(() => {
        setDateLocale(DEFAULT_LOCALE);
        DateMock.restore();
    });

    it('Сегодня', () => {
        expect(getHumanDateWithSpecialValues(moment('2019-02-11'))).toBe(
            'Сегодня',
        );
    });

    it('Завтра', () => {
        expect(getHumanDateWithSpecialValues(moment('2019-02-12'))).toBe(
            'Завтра',
        );
    });

    it('Послезавтра', () => {
        expect(getHumanDateWithSpecialValues(moment('2019-02-13'))).toBe(
            'Послезавтра',
        );
    });

    it('Вернет не специальное значение', () => {
        expect(getHumanDateWithSpecialValues(moment('2019-03-12'))).toBe(
            '12 марта',
        );
    });
});
