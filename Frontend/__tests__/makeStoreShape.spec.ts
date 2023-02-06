import { makeStoreShape } from '../makeStoreShape';

describe('store/makeStoreShape', () => {
    it('возвращает унифицированный redux state', () => {
        expect(makeStoreShape({ AnyWidget: {} }, { anyCollection: {} })).toEqual({
            'beru.ru': {
                widgets: {
                    AnyWidget: {},
                },
                collections: {
                    anyCollection: {},
                },
            },
        });
    });
});
