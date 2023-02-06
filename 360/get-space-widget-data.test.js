'use strict';

const model = require('./get-space-widget-data');

const service = jest.fn();
const core = { service: () => service };

describe('models/disk/get-space-widget-data', () => {
    it('должен вернуть результат из ручек', async () => {
        const response = Symbol();

        service.mockResolvedValueOnce(response);

        const result = await model({}, core);

        expect(service.mock.calls).toMatchSnapshot();

        expect(result).toBe(response);
    });

    it('должен вернуть ошибку', async () => {
        service.mockRejectedValueOnce(new Error());

        await expect(() => model({}, core)).rejects.toBeInstanceOf(Error);
    });
});
