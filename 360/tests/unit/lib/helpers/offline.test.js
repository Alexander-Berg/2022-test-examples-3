const OfflineMonitor = require('../../../../lib/helpers/offline').default;

describe('OfflineMonitor helper', () => {
    let ofm1;
    let pingUntilOnlineSpy;
    let onOnlineSpy;
    let fetchSpy;

    beforeEach(() => {
        ofm1 = new OfflineMonitor();
        pingUntilOnlineSpy = jest.spyOn(ofm1, 'pingUntilOnline');
        onOnlineSpy = jest.spyOn(ofm1, '_onOnline');
        fetchSpy = jest.spyOn(ofm1, '_fetch');
        fetchSpy.mockImplementation(() => Promise.resolve(200));
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should call _onOfflineCallbacks when offline event is dispatched', () => {
        const onOfflineCallback = jest.fn();

        ofm1.addOfflineEventListener(onOfflineCallback);

        window.dispatchEvent(new Event('offline'));
        window.dispatchEvent(new Event('online'));

        expect(onOfflineCallback).toHaveBeenCalled();
        expect(pingUntilOnlineSpy).toHaveBeenCalled();
    });

    it('should call _onOnlineCallbacks when connection is reestablished', async(done) => {
        const onOnlineCallback = jest.fn();
        ofm1.addOnlineEventListener(onOnlineCallback);

        window.dispatchEvent(new Event('offline'));
        window.dispatchEvent(new Event('online'));

        await expect(fetchSpy).toHaveBeenCalled();
        // из-за обёртки промиса проверки пытаются выполниться раньше, чем вызывается метод `_onOnline`
        setTimeout(() => {
            expect(onOnlineSpy).toHaveBeenCalled();
            expect(onOnlineCallback).toHaveBeenCalled();
            done();
        });
    });

    it('should continue calling _pingInternetConnection if _fetch rejects', (done) => {
        /**
         *
         */
        function callback() {
            expect(pingUntilOnlineSpy).toHaveBeenCalledTimes(2);
            done();
        }
        fetchSpy.mockImplementation(() => Promise.reject());

        window.dispatchEvent(new Event('offline'));
        window.dispatchEvent(new Event('online'));

        setTimeout(() => callback(), 2100);
    });

    it('multiple calls to constructor return the same instance', () => {
        const ofm2 = new OfflineMonitor();

        expect(ofm1).toBe(ofm2);
    });
});
