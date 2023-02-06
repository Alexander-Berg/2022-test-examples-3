import { GPSSignalLevel } from 'shared/consts/GPSValueIcon';
import { getSignalLevel, SIGNAL_LEVEL_RED, SIGNAL_LEVEL_YELLOW } from 'shared/helpers/getSignalLevel/getSignalLevel';

describe('getSignalLevel', () => {
    it('should be none signal', () => {
        expect(getSignalLevel(null)).toEqual(GPSSignalLevel.NONE);
    });

    it('should be none signal for undefined', () => {
        expect(getSignalLevel(undefined)).toEqual(GPSSignalLevel.NONE);
    });

    it('should be none signal for zero', () => {
        expect(getSignalLevel(0)).toEqual(GPSSignalLevel.NONE);
    });

    it('should be bad signal', () => {
        expect(getSignalLevel(SIGNAL_LEVEL_RED - 1)).toEqual(GPSSignalLevel.RED);
    });

    it('should be normal signal', () => {
        expect(getSignalLevel(SIGNAL_LEVEL_RED)).toEqual(GPSSignalLevel.YELLOW);
    });

    it('should be also normal signal', () => {
        expect(getSignalLevel(SIGNAL_LEVEL_YELLOW - 1)).toEqual(GPSSignalLevel.YELLOW);
    });

    it('should be good signal', () => {
        expect(getSignalLevel(SIGNAL_LEVEL_YELLOW)).toEqual(GPSSignalLevel.GREEN);
    });
});
