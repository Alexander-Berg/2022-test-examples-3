import { StationPoint } from './api/types';

import { findNearestStation } from './nearest-station';

describe('findNearestStation', () => {
    it('should return nearest station', () => {
        const stations: StationPoint[] = [
            {
                id: 'station1',
                location: { lat: 35.55, lon: 36.6 },
                paymentRadius: 1000,
            },
            {
                id: 'station2',
                location: { lat: 20.1, lon: 20.2 },
                paymentRadius: 1000,
            },
            {
                id: 'station3',
                location: { lat: 40, lon: 50.1 },
                paymentRadius: 1000,
            }
        ];

        expect(findNearestStation({ lat: 35.56, lon: 36.6 }, stations)).toBe(stations[0]);
    });

    it('should not return without payment radius', () => {
        const stations: StationPoint[] = [
            {
                id: 'station1',
                location: { lat: 35.5, lon: 36.6 },
            },
            {
                id: 'station2',
                location: { lat: 20.1, lon: 20.2 },
            },
        ];

        expect(findNearestStation({ lat: 35.6, lon: 36.6 }, stations)).toBe(undefined);
    });

    it('should not return without station location', () => {
        const stations: StationPoint[] = [
            {
                id: 'station1',
            },
            {
                id: 'station2',
            },
        ];

        expect(findNearestStation({ lat: 35.6, lon: 36.6 }, stations)).toBe(undefined);
    });

    it('should not return outside of payment radius', () => {
        const stations: StationPoint[] = [
            {
                id: 'station1',
                location: { lat: 35.5, lon: 36.6 },
                paymentRadius: 100,
            },
            {
                id: 'station2',
                location: { lat: 20.1, lon: 20.2 },
                paymentRadius: 100,
            },
        ];

        expect(findNearestStation({ lat: 36.5, lon: 36.6 }, stations)).toBe(undefined);
    });

    it('should not return from empty list', function() {
        expect(findNearestStation({ lat: 37.6, lon: 37.6 }, [])).toBe(undefined);
    });
});
