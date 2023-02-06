import StationSubtype from '../../../interfaces/state/station/StationSubtype';
import StationType from '../../../interfaces/state/station/StationType';

import getPossibleSubtypes from '../getPossibleSubtypes';

describe('getPossibleSubtypes', () => {
    it('Если type не передан, то вернет все возможные подтипы', () => {
        expect(getPossibleSubtypes()).toStrictEqual(
            Object.values(StationSubtype),
        );
    });

    it('Проверка подтипов для типов станций', () => {
        expect(getPossibleSubtypes(StationType.railroad)).toStrictEqual([
            StationSubtype.train,
            StationSubtype.suburban,
            StationSubtype.tablo,
        ]);
        expect(getPossibleSubtypes(StationType.plane)).toStrictEqual([
            StationSubtype.plane,
        ]);
        expect(getPossibleSubtypes(StationType.bus)).toStrictEqual([
            StationSubtype.schedule,
        ]);
        expect(getPossibleSubtypes(StationType.water)).toStrictEqual([
            StationSubtype.schedule,
        ]);
    });
});
