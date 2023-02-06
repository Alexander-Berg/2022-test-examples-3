import {
    selectIsLoading,
    selectError,
} from './common.selectors';

describe('common.selectors', () => {
    describe('selectIsLoading', () => {
        const store = {
            loadingReducer: {
                'abc-www/SingleLoading': true,
                'abc-www/SingleLoaded': false,
                'abc-www/Multiple': {
                    loading: true,
                    loaded: false,
                },
                falsyIds: {
                    '0': true,
                    '': true,
                },
                '': true,
            },
        };

        it('Should return true when given entity is loading', () => {
            expect(selectIsLoading(store, 'abc-www/SingleLoading')).toBe(true);
            expect(selectIsLoading(store, 'abc-www/Multiple', 'loading')).toBe(true);
        });

        it('Should return false when given entity is not loading', () => {
            expect(selectIsLoading(store, 'abc-www/SingleLoaded')).toBe(false);
            expect(selectIsLoading(store, 'abc-www/Multiple', 'loaded')).toBe(false);
        });

        it('Should return false for undefined entity', () => {
            expect(selectIsLoading(store, 'anything')).toBe(false);
            expect(selectIsLoading(store, 'anything', 'really')).toBe(false);
        });

        it('Should select correct values for falsy ids', () => {
            expect(selectIsLoading(store, '')).toBe(true);
            expect(selectIsLoading(store, 'falsyIds', 0)).toBe(true);
            expect(selectIsLoading(store, 'falsyIds', '')).toBe(true);
        });
    });

    describe('selectError', () => {
        const referenceError = new Error('trouble');
        const store = {
            errorReducer: {
                'abc-www/SingleFailure': referenceError,
                'abc-www/SingleSuccess': null,
                'abc-www/Multiple': {
                    failure: referenceError,
                    success: null,
                },
                falsyIds: {
                    '0': referenceError,
                    '': referenceError,
                },
                '': referenceError,
            },
        };

        it('Should select error when for a failed entity', () => {
            expect(selectError(store, 'abc-www/SingleFailure')).toBe(referenceError);
            expect(selectError(store, 'abc-www/Multiple', 'failure')).toBe(referenceError);
        });

        it('Should return null when there is no error', () => {
            expect(selectError(store, 'abc-www/SingleSuccess')).toBe(null);
            expect(selectError(store, 'abc-www/Multiple', 'success')).toBe(null);
        });

        it('Should return null for undefined entity', () => {
            expect(selectError(store, 'anything')).toBe(null);
            expect(selectError(store, 'anything', 'really')).toBe(null);
        });

        it('Should select correct values for falsy ids', () => {
            expect(selectError(store, '')).toBe(referenceError);
            expect(selectError(store, 'falsyIds', 0)).toBe(referenceError);
            expect(selectError(store, 'falsyIds', '')).toBe(referenceError);
        });
    });
});
