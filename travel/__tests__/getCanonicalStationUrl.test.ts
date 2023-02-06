import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';
import StationType from '../../../interfaces/state/station/StationType';
import getCanonicalStationUrl from '../getCanonicalStationUrl';
import StationSubtype from '../../../interfaces/state/station/StationSubtype';
import StationEventList from '../../../interfaces/state/station/StationEventList';
import IStateFlags from '../../../interfaces/state/flags/IStateFlags';

const commonParameters = {
    id: 1,
    tld: Tld.ru,
    language: Lang.ru,
    flags: {} as IStateFlags,
    type: StationType.railroad,
};
const commonBusParameters = {
    ...commonParameters,
    type: StationType.bus,
};
const commonWaterParameters = {
    ...commonParameters,
    type: StationType.water,
};

describe('getCanonicalStationUrl', () => {
    it('Должен вернуть корректную каноническую ссылку', () => {
        expect(
            getCanonicalStationUrl({
                ...commonParameters,
            }),
        ).toBe('/station/1/');

        // Если не указан mainSubtype, то игнорируем subtype
        expect(
            getCanonicalStationUrl({
                ...commonParameters,
                subtype: StationSubtype.suburban,
            }),
        ).toBe('/station/1/');

        // Если subtype это табло, то игнорируем subtype
        expect(
            getCanonicalStationUrl({
                ...commonParameters,
                subtype: StationSubtype.tablo,
                mainSubtype: StationSubtype.train,
            }),
        ).toBe('/station/1/');

        expect(
            getCanonicalStationUrl({
                ...commonParameters,
                subtype: StationSubtype.suburban,
                mainSubtype: StationSubtype.train,
            }),
        ).toBe('/station/1/suburban/');

        expect(
            getCanonicalStationUrl({
                ...commonParameters,
                subtype: StationSubtype.suburban,
                mainSubtype: StationSubtype.suburban,
            }),
        ).toBe('/station/1/');

        expect(
            getCanonicalStationUrl({
                ...commonParameters,
                subtype: StationSubtype.suburban,
                mainSubtype: StationSubtype.suburban,
                event: StationEventList.arrival,
            }),
        ).toBe('/station/1/?event=arrival');

        expect(
            getCanonicalStationUrl({
                ...commonParameters,
                subtype: StationSubtype.suburban,
                mainSubtype: StationSubtype.suburban,
                event: StationEventList.departure,
            }),
        ).toBe('/station/1/');
    });

    it('Для автобусной и водной станции каноникл только один: на сегодня', () => {
        expect(
            getCanonicalStationUrl({
                ...commonBusParameters,
            }),
        ).toBe('/station/1/');

        expect(
            getCanonicalStationUrl({
                ...commonBusParameters,
                subtype: StationSubtype.suburban,
                mainSubtype: StationSubtype.tablo,
            }),
        ).toBe('/station/1/');

        expect(
            getCanonicalStationUrl({
                ...commonBusParameters,
                event: StationEventList.arrival,
            }),
        ).toBe('/station/1/');

        expect(
            getCanonicalStationUrl({
                ...commonWaterParameters,
            }),
        ).toBe('/station/1/');

        expect(
            getCanonicalStationUrl({
                ...commonWaterParameters,
                subtype: StationSubtype.suburban,
                mainSubtype: StationSubtype.tablo,
            }),
        ).toBe('/station/1/');

        expect(
            getCanonicalStationUrl({
                ...commonWaterParameters,
                event: StationEventList.arrival,
            }),
        ).toBe('/station/1/');
    });
});
