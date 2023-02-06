import { getDistance } from './tile-utils';

describe('getDistance', function() {
    it('should return 0 for identical locations', function() {
        expect(getDistance(
            { lat: 55.75222, lon: 37.61556 },
            { lat: 55.75222, lon: 37.61556 }
        )).toBe(0);
    });

    it('should calc distance', function() {
        expect(getDistance(
            { lat: 55.743325, lon: 37.567432 },
            { lat: 55.756993, lon: 37.661237 }
        )).toBe(6);
    });

    it('should calc distance between distance between poles', function() {
        expect(getDistance(
            { lat: 90, lon: 0 },
            { lat: -90, lon: 0 }
        )).toBe(20004);
    });

    it('should calc distance between distance along the equator', function() {
        expect(getDistance(
            { lat: 0, lon: -78.509746 },
            { lat: 0, lon: 109.325761 }
        )).toBe(19165);
    });

    it('should return -1 for incorrect locations', function() {
        expect(getDistance(
            // @ts-ignore
            { lat: 55.743325 },
            { lon: 37.661237 }
        )).toBe(-1);
    });
});
