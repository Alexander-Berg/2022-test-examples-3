import { throwError, appReducer, initialState } from '../app';
import { ApiErrorType } from '../../utils/ApiError';
import { TypedError } from '../../utils/errors';

describe.skip('App action creators', () => {
    it('creates "throwError" action', () => {
        const error = new TypedError(
            'Error message',
            ApiErrorType.TRANSPORT,
        );

        const label = 'Error label';

        const expectedAction = {
            type: 'app/throwError',
            error,
            label,
        };

        expect(throwError(error, label)).toEqual(expectedAction);
    });
});

describe('App reducer', () => {
    describe('throwError', () => {
        it('returns correct state', () => {
            const error = new TypedError(
                'Error message',
                ApiErrorType.TRANSPORT,
            );

            const label = 'Error label';

            const newState = appReducer(initialState, throwError(error, label));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                ...initialState,
                error: {
                    error,
                    label,
                },
            });
        });
    });
});
