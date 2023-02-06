import { onMessageConstructor } from './useMessageSubscription';

interface CheckParams {
    allowedHosts: string[];
    origin: string;
    data: string;
}

describe('onMessageConstructor', () => {
    const check = ({ allowedHosts, origin, data }: CheckParams) => {
        const spy = jest.fn();

        const onMessage = onMessageConstructor(spy, allowedHosts).bind(window);

        const event = new MessageEvent('message', { origin, data });

        onMessage(event);

        return spy;
    };

    const testData = '{"type":"test"}';

    it('should construct handler that accepts valid origin 1', () => {
        const spy = check({
            allowedHosts: ['example.com'],
            origin: 'https://example.com',
            data: testData,
        });

        expect(spy).toBeCalled();
    });

    it('should construct handler that accepts valid origin 2', () => {
        const spy = check({
            allowedHosts: ['*.example.com'],
            origin: 'https://example.com',
            data: testData,
        });

        expect(spy).toBeCalled();
    });

    it('should construct handler that accepts valid origin 3', () => {
        const spy = check({
            allowedHosts: ['https://*.example.com'],
            origin: 'https://example.com',
            data: testData,
        });

        expect(spy).toBeCalled();
    });

    it('should construct handler that accepts valid origin 4', () => {
        const spy = check({
            allowedHosts: ['*.example.com:*'],
            origin: 'https://example.com',
            data: testData,
        });

        expect(spy).toBeCalled();
    });

    it('should construct handler that accepts valid origin 5', () => {
        const spy = check({
            allowedHosts: ['https://*.example.com'],
            origin: 'https://subdomain.example.com',
            data: testData,
        });

        expect(spy).toBeCalled();
    });

    it('should construct handler that accepts valid origin 6', () => {
        const spy = check({
            allowedHosts: ['*.example.com'],
            origin: 'https://subdomain.example.com',
            data: testData,
        });

        expect(spy).toBeCalled();
    });

    it('should construct handler that accepts valid origin 7', () => {
        const spy = check({
            allowedHosts: ['*.example.com:*'],
            origin: 'https://subdomain.example.com',
            data: testData,
        });

        expect(spy).toBeCalled();
    });

    it('should construct handler that not accepts invalid origin 1', () => {
        const spy = check({
            allowedHosts: ['http://example.com'],
            origin: 'https://example.com',
            data: testData,
        });

        expect(spy).toBeCalledTimes(0);
    });

    it('should construct handler that not accepts invalid origin 2', () => {
        const spy = check({
            allowedHosts: ['https://example.com'],
            origin: 'https://subdomain.example.com',
            data: testData,
        });

        expect(spy).toBeCalledTimes(0);
    });

    it('should construct handler that not accepts invalid origin 3', () => {
        const spy = check({
            allowedHosts: ['https://example.com'],
            origin: 'https://evil-host-example.com',
            data: testData,
        });

        expect(spy).toBeCalledTimes(0);
    });

    it('should construct handler that not accepts invalid origin 4', () => {
        const spy = check({
            allowedHosts: ['https://example.com'],
            origin: 'https://example.com.evil.domain',
            data: testData,
        });

        expect(spy).toBeCalledTimes(0);
    });

    it('should construct handler that not accepts invalid origin 5', () => {
        const spy = check({
            allowedHosts: ['https://*.example.com'],
            origin: 'https://example.com.evil.domain',
            data: testData,
        });

        expect(spy).toBeCalledTimes(0);
    });

    it('should construct handler that not accepts invalid origin 6', () => {
        const spy = check({
            allowedHosts: ['https://*.example.com'],
            origin: 'https://evil-host-example.com',
            data: testData,
        });

        expect(spy).toBeCalledTimes(0);
    });

    it('should construct handler that not accepts invalid origin 7', () => {
        const spy = check({
            allowedHosts: ['https://example.com:*'],
            origin: 'https://subdomain.example.com',
            data: testData,
        });

        expect(spy).toBeCalledTimes(0);
    });
});
