import {InterruptError, PollingInterruptError} from '../executionErrors';

describe('Ошибки прерывания операции', () => {
    it('InterruptError', () => {
        const error = new InterruptError('stop');

        expect(error instanceof InterruptError).toBe(true);
        expect(error instanceof Error).toBe(true);
        expect(error.message).toBe('stop');
    });

    it('PollingInterruptError', () => {
        const error = new PollingInterruptError('stop polling');

        expect(error instanceof PollingInterruptError).toBe(true);
        expect(error instanceof InterruptError).toBe(true);
        expect(error instanceof Error).toBe(true);
        expect(error.message).toBe('stop polling');
    });
});
