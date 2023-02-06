import { requestJson, JSON_REQUEST } from './request-json';

describe('requestJson', () => {
    it('Should create JSON_REQUEST action', () => {
        const params = { foo: 'bar' };

        expect(requestJson(params, 'ASDASD', { customMeta: 42 }))
            .toEqual({
                type: JSON_REQUEST,
                payload: params,
                meta: {
                    type: 'ASDASD',
                    customMeta: 42
                }
            });
    });
});
