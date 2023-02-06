import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import getStationInformersUrl from '../getStationInformersUrl';

import StationSubtype from '../../../interfaces/state/station/StationSubtype';
import StationType from '../../../interfaces/state/station/StationType';

const id = 1;
const tld = Tld.ru;
const language = Lang.ru;

const baseUrl = 'https://domain.ru';

describe('getInfoUrl', () => {
    it('Должна вернуться ссылка, если переданы только обязательные параметры', () => {
        expect(
            getStationInformersUrl({
                id,
                tld,
                language,
            }),
        ).toBe('/informers/?station=1');
    });

    it('Должна вернуться ссылка с query параметром subtype, если он передан и содержится в списке корректных значений для параметра type', () => {
        expect(
            getStationInformersUrl({
                id,
                tld,
                language,
                subtype: StationSubtype.train,
            }),
        ).toBe('/informers/?station=1&type=train');
    });

    it('Должен вернуться ссылка без type, если subtype передан, но не содержится в списке корректных значений для параметра type', () => {
        expect(
            getStationInformersUrl({
                id,
                tld,
                language,
                subtype: StationSubtype.schedule,
            }),
        ).toBe('/informers/?station=1');
    });

    it('Должна вернуться ссылка без subtype, но с переданным originUrl', () => {
        expect(
            getStationInformersUrl({
                id,
                tld,
                language,
                originUrl: baseUrl,
            }),
        ).toBe('https://domain.ru/informers/?station=1');
    });

    it('Должна вернуться ссылка с переданными originUrl и subtype, если он содержится в в списке корректных значений для параметра type', () => {
        expect(
            getStationInformersUrl({
                id,
                tld,
                language,
                subtype: StationSubtype.suburban,
                originUrl: baseUrl,
            }),
        ).toBe('https://domain.ru/informers/?station=1&type=suburban');
    });

    it('Для аэропортов type в ссылке должен быть "tablo"', () => {
        expect(
            getStationInformersUrl({
                id,
                tld,
                language,
                type: StationType.plane,
                subtype: StationSubtype.plane,
            }),
        ).toBe('/informers/?station=1&type=tablo');
    });
});
