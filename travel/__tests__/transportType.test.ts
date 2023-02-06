import {FilterTransportType, TransportType} from '../transportType';
import IStationGroup from '../../interfaces/state/stationsGroup/IStationGroup';
import IStateDirections from '../../interfaces/state/directions/IStateDirections';

// eslint-disable-next-line no-duplicate-imports
import {parseTransportType, getAvailableTransportTypes} from '../transportType';

describe('parseTransportType', () => {
    it("should return passed string if it's a known searchable transport type identifier", () => {
        expect(parseTransportType('all')).toBe(FilterTransportType.all);
        expect(parseTransportType('bus')).toBe(FilterTransportType.bus);
        expect(parseTransportType('train')).toBe(FilterTransportType.train);
        expect(parseTransportType('plane')).toBe(FilterTransportType.plane);
        expect(parseTransportType('suburban')).toBe(
            FilterTransportType.suburban,
        );
    });

    it('should return `all` for unknown or not searchable transport type identifiers', () => {
        expect(parseTransportType('water')).toBe(FilterTransportType.all);
    });
});

describe('getAvailableTransportTypes', () => {
    it('Возвращает упорядоченный список со всеми видами транспорта', () => {
        const stations = [
            {t_type: TransportType.train},
            {t_type: TransportType.bus},
            {t_type: TransportType.plane},
            {t_type: TransportType.water},
        ] as IStationGroup[];

        const directions = [{code: 'xxx'}] as IStateDirections;

        expect(getAvailableTransportTypes(stations, directions)).toEqual([
            TransportType.suburban,
            TransportType.bus,
            TransportType.train,
            TransportType.plane,
            TransportType.water,
        ]);
    });

    it('Возвращает упорядоченный список с типами - электрички, автобусы', () => {
        const stations = [{t_type: TransportType.bus}] as IStationGroup[];

        const directions = [{code: 'xxx'}] as IStateDirections;

        expect(getAvailableTransportTypes(stations, directions)).toEqual([
            TransportType.suburban,
            TransportType.bus,
        ]);
    });

    it('Возвращает упорядоченный список с типами - автобусы', () => {
        const stations = [{t_type: TransportType.bus}] as IStationGroup[];

        const directions = [];

        expect(getAvailableTransportTypes(stations, directions)).toEqual([
            TransportType.bus,
        ]);
    });
});
