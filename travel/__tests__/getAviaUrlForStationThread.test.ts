import getAviaUrlForStationThread from '../getAviaUrlForStationThread';

const aviaLink = 'test';

describe('getAviaUrlForStationThread', () => {
    it('Должна вернуться ссылка', () => {
        expect(getAviaUrlForStationThread(aviaLink, false)).toBe(aviaLink);
    });

    it('Для рейсов-пополнений не возвращаем ссылки', () => {
        expect(getAviaUrlForStationThread(aviaLink, true)).toBe(undefined);
    });

    it('Если ссылка пустая, то вернет undefined', () => {
        expect(getAviaUrlForStationThread('', false)).toBe(undefined);
    });
});
