import { bucketsReducer, update } from '../buckets';

const createBucket = (a: string, v: number) => {
    return {
        bucket_name: a,
        bucket_value: a,
        version: v,
    };
};

const createSBL = (a: string, v: number) => [createBucket(a, v)];

const createInitialState = () => ({ maxVersion: 0 });

describe('Buckets reducer', () => {
    describe('#Set buckets', () => {
        it('should set buckets', () => {
            const initialState = createInitialState();

            const action = update(createSBL('test', 1));

            const newState = bucketsReducer(initialState, action);

            expect(newState).not.toBe(initialState);
            expect(typeof newState).toEqual('object');
            expect(newState).toMatchObject({
                test: {
                    data: 'test',
                    version: 1,
                },
            });
        });

        it('should remain the same', () => {
            const initialState = createInitialState();

            const action = update(createSBL('test', 1));

            const state1 = bucketsReducer(initialState, action);

            expect(state1).not.toBe(initialState);
            expect(typeof state1).toEqual('object');
            expect(state1).toMatchObject({
                test: {
                    data: 'test',
                    version: 1,
                },
            });

            const state2 = bucketsReducer(state1, action);

            expect(state2).not.toBe(initialState);
            expect(typeof state2).toEqual('object');
            expect(state2).toMatchObject(state1);
        });

        it('should add bucket', () => {
            const initialState = createInitialState();

            const action1 = update(createSBL('test', 1));

            const action2 = update(createSBL('test2', 1));

            const state1 = bucketsReducer(initialState, action1);

            expect(state1).not.toBe(initialState);
            expect(typeof state1).toEqual('object');
            expect(state1).toMatchObject({
                test: {
                    data: 'test',
                    version: 1,
                },
            });

            const state2 = bucketsReducer(state1, action2);

            expect(state2).not.toBe(initialState);
            expect(state2).not.toBe(state1);
            expect(typeof state2).toEqual('object');
            expect(state2).toMatchObject({
                test: {
                    data: 'test',
                    version: 1,
                },
                test2: {
                    data: 'test2',
                    version: 1,
                },
            });
        });

        it('should update bucket by version', () => {
            const initialState = createInitialState();

            const action1 = update(createSBL('test', 1));

            const action2 = update(createSBL('test', 2));

            const state1 = bucketsReducer(initialState, action1);

            expect(state1).not.toBe(initialState);
            expect(typeof state1).toEqual('object');
            expect(state1).toMatchObject({
                test: {
                    data: 'test',
                    version: 1,
                },
            });

            const state2 = bucketsReducer(state1, action2);

            expect(state2).not.toBe(initialState);
            expect(state2).not.toBe(state1);
            expect(typeof state2).toEqual('object');
            expect(state2).toMatchObject({
                test: {
                    data: 'test',
                    version: 2,
                },
            });
        });
    });

    describe('#Clear state', () => {
        it('should clear state', () => {
            const initialState = createInitialState();

            const action = update(createSBL('test', 1));

            const state1 = bucketsReducer(initialState, action);

            expect(state1).not.toBe(initialState);
            expect(typeof state1).toEqual('object');
            expect(state1).toMatchObject({
                test: {
                    data: 'test',
                    version: 1,
                },
            });
        });
    });
});
