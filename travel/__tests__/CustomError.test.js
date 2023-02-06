jest.disableAutomock();

import CustomError from '../CustomError';

describe('CustomError', () => {
    it('Должен наследоваться от Error и иметь схожий интерфейс', () => {
        const err = new CustomError('name', 'message');

        expect(err instanceof CustomError).toBe(true);
        expect(err instanceof Error).toBe(true);
        expect(err.name).toBe('name');
        expect(err.message).toBe('message');
        expect(err.stack && typeof err.stack).toBe('string');
    });
});
