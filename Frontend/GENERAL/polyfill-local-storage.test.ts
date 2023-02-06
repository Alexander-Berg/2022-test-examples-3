import './polyfill-local-storage';
import { logError } from './lib/rum';

jest.mock('./lib/rum');

const lsSetItem = localStorage.mockedSetItem as jest.MockedFunction<typeof localStorage.setItem>;
const lsRemoveItem = localStorage.removeItem as jest.MockedFunction<typeof localStorage.removeItem>;
const rumLogError = logError as jest.MockedFunction<typeof logError>;

describe('polyfill-local-storage', () => {
    beforeEach(() => {
        localStorage.clear();
        rumLogError.mockClear();
        lsRemoveItem.mockClear();
    });

    it('should set value', () => {
        localStorage.setItem('persist:weather.main', 'foobar');
        localStorage.setItem('persist:weather.something-non-critical', 'foobar');
        expect(localStorage.getItem('persist:weather.main')).toBe('foobar');
        expect(localStorage.getItem('persist:weather.something-non-critical')).toBe('foobar');
        expect(rumLogError.mock.calls).toHaveLength(0);
        expect(lsRemoveItem.mock.calls).toHaveLength(0);
    });

    it('should clear data and set critical item', () => {
        localStorage.setItem('persist:weather.garbage0', 'foobar');
        localStorage.setItem('persist:weather.garbage1', 'foobar');
        expect(localStorage.getItem('persist:weather.garbage0')).toBe('foobar');
        expect(localStorage.getItem('persist:weather.garbage1')).toBe('foobar');
        expect(rumLogError.mock.calls).toHaveLength(0);
        expect(lsRemoveItem.mock.calls).toHaveLength(0);

        lsSetItem.mockImplementationOnce(() => {
            throw new Error('quota');
        });

        localStorage.setItem('persist:weather.main', 'foobar');
        expect(localStorage.getItem('persist:weather.main')).toBe('foobar');
        expect(localStorage.getItem('persist:weather.garbage0')).toBeNull();
        expect(localStorage.getItem('persist:weather.garbage1')).toBeNull();
        expect(rumLogError.mock.calls).toHaveLength(0);
        expect(lsRemoveItem.mock.calls).toHaveLength(2);
    });

    it('should log cleanup error', () => {
        localStorage.setItem('persist:weather.garbage0', 'foobar');

        lsSetItem.mockImplementationOnce(() => {
            throw new Error('quota');
        });

        lsRemoveItem.mockImplementationOnce(() => {
            throw new Error('cannot cleanup');
        });

        localStorage.setItem('persist:weather.search', 'foobar');
        expect(localStorage.getItem('persist:weather.search')).toBe('foobar');
        expect(rumLogError).toHaveBeenLastCalledWith(expect.objectContaining({
            additional: expect.objectContaining({
                msg: 'Cannot make cleanup',
                key: 'persist:weather.search'
            })
        }), expect.objectContaining({}));
    });

    it('should log retry error', () => {
        localStorage.setItem('persist:weather.garbage0', 'foobar');

        lsSetItem
            .mockImplementationOnce(() => {
                throw new Error('quota');
            })
            .mockImplementationOnce(() => {
                throw new Error('quota');
            });

        lsRemoveItem.mockImplementationOnce(() => {
            throw new Error('cannot cleanup');
        });

        localStorage.setItem('persist:weather.search', 'foobar');
        expect(localStorage.getItem('persist:weather.search')).toBe(null);
        expect(rumLogError.mock.calls).toHaveLength(2);
        expect(rumLogError).toHaveBeenLastCalledWith(expect.objectContaining({
            additional: expect.objectContaining({
                msg: 'quota',
                key: 'persist:weather.search'
            })
        }), expect.objectContaining({}));
    });

    it('should error on uncritical keys with no cleanup', () => {
        localStorage.setItem('persist:weather.garbage0', 'foobar');

        lsSetItem
            .mockImplementationOnce(() => {
                throw new Error('quota');
            })
            .mockImplementationOnce(() => {
                throw new Error('quota');
            });

        lsRemoveItem.mockImplementationOnce(() => {
            throw new Error('cannot cleanup');
        });

        localStorage.setItem('persist:weather.something-non-critical', 'foobar');
        expect(localStorage.getItem('persist:weather.something-non-critical')).toBe(null);
        expect(lsRemoveItem.mock.calls).toHaveLength(0);
        expect(rumLogError.mock.calls).toHaveLength(1);
        expect(rumLogError).toHaveBeenLastCalledWith(expect.objectContaining({
            additional: expect.objectContaining({
                msg: 'quota',
                key: 'persist:weather.something-non-critical'
            })
        }), expect.objectContaining({}));
    });
});
