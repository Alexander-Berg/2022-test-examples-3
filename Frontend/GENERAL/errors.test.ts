import { isURIError, isErrorWithResponse, ErrorWithResponse } from './errors';

describe('Lib. errors', () => {
    describe('isURIError', () => {
        test('должен возвращать true, если передали URIError', () => {
            expect(isURIError(new URIError())).toBe(true);
            expect(isURIError(new URIError('Error message'))).toBe(true);
        });

        test('должен возвращать false, если передали не URIError', () => {
            expect(isURIError(null)).toBe(false);
            expect(isURIError(undefined)).toBe(false);

            expect(isURIError({})).toBe(false);
            expect(isURIError(() => {})).toBe(false);
            expect(isURIError(new Error())).toBe(false);
        });
    });

    describe('isErrorWithResponse', () => {
        test('должен возвращать true, если передали RequestError/CancelError', async() => {
            const http400 = new ErrorWithResponse('HttpError', 'Bad request', 400);
            const http500 = new ErrorWithResponse('HttpError', 'Internal server error', 500);

            expect(isErrorWithResponse(http400)).toBe(true);
            expect(isErrorWithResponse(http500)).toBe(true);
        });

        test('должен возвращать false, если передали не RequestError/CancelError', () => {
            expect(isErrorWithResponse(null)).toBe(false);
            expect(isErrorWithResponse(undefined)).toBe(false);

            expect(isErrorWithResponse({})).toBe(false);
            expect(isErrorWithResponse(() => {})).toBe(false);
            expect(isErrorWithResponse(new Error())).toBe(false);
            expect(isErrorWithResponse({ response: { statusCode: 500 } })).toBe(false);
        });
    });
});
