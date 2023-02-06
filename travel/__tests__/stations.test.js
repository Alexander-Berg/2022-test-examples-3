jest.disableAutomock();

import {stationIsCity} from '../stations';

describe('stationIsCity', () => {
    it('Вернёт false если хотябы один из идентификаторов не определен', () => {
        expect(stationIsCity({})).toBe(false);
        expect(
            stationIsCity({
                id: 7,
            }),
        ).toBe(false);
        expect(
            stationIsCity({
                settlementId: 7,
            }),
        ).toBe(false);
    });

    it('Вернёт false если идентификаторы города и станции отличаются', () => {
        expect(
            stationIsCity({
                id: 8,
                settlementId: 7,
            }),
        ).toBe(false);
    });

    it('Вернёт true если идентификаторы города и станции совпадают', () => {
        expect(
            stationIsCity({
                id: 213,
                settlementId: 213,
            }),
        ).toBe(true);
    });
});
