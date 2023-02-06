import { getWaitTime, MAX_TIMEOUT } from 'redux/store/actions/notifications';

describe('getWaitTime', () => {
    const cooldown = 20;

    const setCurrentDate = (ms) => {
        Date.now = jest.fn(() => ms);
    };

    it('должен вернуть время от текущей даты до окончания ожидания с момента показа предыдущей нотифайки', () => {
        setCurrentDate(20);
        expect(getWaitTime([{ hide_date: new Date(10) }], [{ send_date: new Date(10) }], cooldown)).toBe(10);
    });

    it('должен вернуть время от текущей даты до окончания ожидания с момента показа предыдущей нотифайки плюс времени до начала следующей', () => {
        setCurrentDate(20);
        expect(getWaitTime([{ hide_date: new Date(10) }], [{ send_date: new Date(40) }], cooldown)).toBe(20);
    });

    it('должен вернуть время, не превышающее максимальное для `setTimeout` значение', () => {
        setCurrentDate(20);
        expect(getWaitTime([{ hide_date: 0 }], [{ send_date: Number.MAX_SAFE_INTEGER }], cooldown)).toBe(MAX_TIMEOUT);
    });
});
