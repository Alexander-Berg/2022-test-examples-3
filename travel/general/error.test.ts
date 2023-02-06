import {unknownToErrorOrUndefined, unknownErrToString} from './error';

describe('Error utilities', () => {
    it('unknownToErrorOrUndefined', () => {
        const message = 'test';
        const result1 = unknownToErrorOrUndefined(Error(message));
        const result2 = unknownToErrorOrUndefined(message);
        const result3 = unknownToErrorOrUndefined(undefined);

        expect(result1 instanceof Error && result1.message).toBe(message);

        expect(result2 instanceof Error && result2.message).toBe(message);

        expect(result3).toBeUndefined();
    });

    it('unknownErrToString', () => {
        const message = 'test';
        const result1 = unknownErrToString(Error(message));
        const result2 = unknownErrToString(message);
        const result3 = unknownErrToString(undefined);

        expect(result1).toBe(message);

        expect(result2).toBe(message);

        expect(result3).toBe('');
    });
});
