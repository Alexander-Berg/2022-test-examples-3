import {getNow} from 'utilities/dateUtils';
import DateMock from 'utilities/testUtils/DateMock';

describe('getNow()', () => {
    test('Должен вернуть количество миллисекунд прошедших с 1 января 1970 года', () => {
        DateMock.mock('2019-02-11T20:19:18Z');
        expect(getNow()).toBe(1549916358000);
        DateMock.restore();
    });
});
