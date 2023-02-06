const libPath = '../../../lib_cjs/public/index.js';

describe('test lib', () => {
    it('should import #createNativeApi', () => {
        expect(require(libPath).createNativeApi).toBeDefined();
        expect(typeof require(libPath).createNativeApi).toBe('function');
    });

    it('should import #createRegistryApi', () => {
        expect(require(libPath).createRegistryApi).toBeDefined();
        expect(typeof require(libPath).createRegistryApi).toBe('function');
    });

    it('should import #PostMessageTransport', () => {
        expect(require(libPath).PostMessageTransport).toBeDefined();
    });

    it('should import #MessengerPushSubscriber', () => {
        expect(require(libPath).MessengerPushSubscriber).toBeDefined();
    });

    it('should import #REGISTRY_API_SCOPE', () => {
        expect(require(libPath).REGISTRY_API_SCOPE).toBeDefined();
    });

    it('should import #NATIVE_TRANSPORT_SCOPE', () => {
        expect(require(libPath).NATIVE_TRANSPORT_SCOPE).toBeDefined();
    });
});
