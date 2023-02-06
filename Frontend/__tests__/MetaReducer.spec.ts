import { metaReducer, getInitialState } from '../reducer';

describe('Meta reducer', () => {
    it('Должен вернуть initialState', () => {
        const initialState = getInitialState();
        expect(metaReducer(undefined,
            {
                type: 'test',
                payload: {},
            }))
            .toEqual(initialState);
    });

    it('Должен обновить cartMeta', () => {
        const initialState = getInitialState();
        expect(metaReducer(undefined,
            {
                type: 'test',
                payload: { data: { meta: { cartMeta: 'test' } } },
            }))
            .toEqual({ ...initialState, cartMeta: 'test' });

        expect(metaReducer({ ...initialState, cartMeta: 'cartMeta type 1' },
            {
                type: 'test',
                payload: { data: { meta: { cartMeta: 'cartMeta type 2' } } },
            }))
            .toEqual({ ...initialState, cartMeta: 'cartMeta type 2' });
    });
});
