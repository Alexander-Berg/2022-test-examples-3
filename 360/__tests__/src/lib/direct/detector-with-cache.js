import Cookies from 'js-cookie';
import detectWithCache from 'lib/direct/detector-with-cache';
import rawDetect from 'lib/direct/raw-detector';

jest.mock('lib/direct/raw-detector');
jest.mock('js-cookie');

const flushPromises = (data) => new Promise((resolve) => setImmediate(() => resolve(data)));

describe('src/lib/direct/detector-with-cache', () => {
    describe('without cookie cache', () => {
        beforeEach(() => {
            Cookies.get.mockReturnValue('');
        });

        it('should detect adblock and set cookie', () => {
            rawDetect.mockResolvedValue({ blocked: true });

            return detectWithCache().then((result) => {
                expect(result).toEqual({ blocked: true });
                expect(Cookies.set).toHaveBeenCalledWith('dv_iale', '1', { expires: 14 });
            });
        });

        it('shouldn\'t detect adblock and set cookie', () => {
            rawDetect.mockResolvedValue({ blocked: false });

            return detectWithCache().then(flushPromises).then((result) => {
                expect(result).toEqual({ blocked: false });
                expect(Cookies.set).toHaveBeenCalledWith('dv_iale', '0', { expires: 14 });
            });
        });
    });

    describe('with cookie cache', () => {
        it('should return true from cache and update cookie with 0', () => {
            Cookies.get.mockReturnValue('1');
            rawDetect.mockResolvedValue({ blocked: false });

            return detectWithCache().then((result) => {
                expect(result).toEqual({ blocked: true });
                expect(Cookies.set).toHaveBeenCalledWith('dv_iale', '0', { expires: 14 });
            });
        });

        it('should return false from cache and update cookie with 1', () => {
            Cookies.get.mockReturnValue('0');
            rawDetect.mockResolvedValue({ blocked: true });

            return detectWithCache().then((result) => {
                expect(result).toEqual({ blocked: false });
                expect(Cookies.set).toHaveBeenCalledWith('dv_iale', '1', { expires: 14 });
            });
        });
    });
});
