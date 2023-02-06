jest.disableAutomock();

import Error404 from '../Error404';

describe('Error404', () => {
    it('Должен наследоваться от Error и иметь схожий интерфейс', () => {
        const err = new Error404();

        expect(err instanceof Error404).toBe(true);
        expect(err instanceof Error).toBe(true);
        expect(err.name).toBe('Error404');
        expect(err.message).toBe('Not found');
        expect(err.stack && typeof err.stack).toBe('string');
    });
});
