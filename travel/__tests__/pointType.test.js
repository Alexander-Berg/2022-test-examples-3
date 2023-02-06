import PointType from '../../../interfaces/PointType';

import {
    getPointType,
    isCity,
    isStation,
    getStationPointById,
    getCityPointById,
} from '../pointType';

describe('pointType functions', () => {
    describe('getPointType', () => {
        it('should return undefined if point was not passed', () => {
            const result = getPointType();

            expect(result).toBe(undefined);
        });

        it('should return undefined if point has no `key` property', () => {
            const result = getPointType({});

            expect(result).toBe(undefined);
        });

        it('should return undefined if point `key` property type is not correct', () => {
            const result = getPointType({key: 123});

            expect(result).toBe(undefined);
        });

        it('should return STATION type', () => {
            const result = getPointType({key: 's3456'});

            expect(result).toBe(PointType.station);
        });

        it('should return CITY type', () => {
            const result = getPointType({key: 'c8'});

            expect(result).toBe(PointType.city);
        });
    });

    describe('isStation', () => {
        it('should return false if point was not passed', () => {
            const result = isStation();

            expect(result).toBe(false);
        });

        it('should return false if point has no `key` property', () => {
            const result = isStation({});

            expect(result).toBe(false);
        });

        it('should return true', () => {
            const result = isStation({key: 's2345'});

            expect(result).toBe(true);
        });

        it('should return false', () => {
            const result = isStation({key: 'c8'});

            expect(result).toBe(false);
        });
    });

    describe('isCity', () => {
        it('should return false if point was not passed', () => {
            const result = isCity();

            expect(result).toBe(false);
        });

        it('should return false if point has no `key` property', () => {
            const result = isCity({});

            expect(result).toBe(false);
        });

        it('should return false', () => {
            const result = isCity({key: 's2345'});

            expect(result).toBe(false);
        });

        it('should return true', () => {
            const result = isCity({key: 'c8'});

            expect(result).toBe(true);
        });
    });

    describe('getStationPointById', () => {
        it('', () => {
            expect(getStationPointById('14')).toBe('s14');
            expect(getStationPointById('')).toBe('s');
            expect(getStationPointById(undefined)).toBe('sundefined');
            expect(getStationPointById(null)).toBe('snull');
        });
    });

    describe('getCityPointById', () => {
        it('', () => {
            expect(getCityPointById('14')).toBe('c14');
            expect(getCityPointById('')).toBe('c');
            expect(getCityPointById(undefined)).toBe('cundefined');
            expect(getCityPointById(null)).toBe('cnull');
        });
    });
});
