import { deepEqual } from '../deepEqual';

describe('#deepEqual', () => {
    describe('prim', () => {
        it('should be equal', () => {
            expect(deepEqual(1, 1)).toBeTruthy();
            expect(deepEqual(0, 0)).toBeTruthy();
            expect(deepEqual(NaN, NaN)).toBeTruthy();
            expect(deepEqual('test', 'test')).toBeTruthy();
            expect(deepEqual('', '')).toBeTruthy();
            expect(deepEqual(null, null)).toBeTruthy();
            expect(deepEqual(undefined, undefined)).toBeTruthy();
            expect(deepEqual(true, true)).toBeTruthy();
            expect(deepEqual(false, false)).toBeTruthy();
        });

        it('should not be equal', () => {
            expect(deepEqual(1, 2)).toBeFalsy();
            expect(deepEqual(0, '0')).toBeFalsy();
            expect(deepEqual(NaN, 1)).toBeFalsy();
            expect(deepEqual(NaN, {})).toBeFalsy();
            expect(deepEqual('test', 'test1')).toBeFalsy();
            expect(deepEqual('', '3')).toBeFalsy();
            expect(deepEqual(true, false)).toBeFalsy();
        });
    });

    describe('objects', () => {
        it('should be equal if all props is same', () => {
            const A = {
                number: 1,
                string: 'test',
                undefined: undefined,
                null: null,
                NaN: NaN,
                true: true,
                false: false,
                array: [1, 2],
                object: {
                    test: 1,
                },
            };

            const B = {
                number: 1,
                string: 'test',
                undefined: undefined,
                null: null,
                NaN: NaN,
                true: true,
                false: false,
                array: [1, 2],
                object: {
                    test: 1,
                },
            };

            expect(deepEqual(A, B)).toBeTruthy();
        });

        it('should be equal deeeeeep', () => {
            const A = {
                a: {
                    b: {
                        c: {
                            d: {
                                e: 1,
                            },
                        },
                    },
                    b1: {
                        c1: {
                            d1: {
                                e: 2,
                            },
                        },
                    },
                },
            };

            const B = {
                a: {
                    b: {
                        c: {
                            d: {
                                e: 1,
                            },
                        },
                    },
                    b1: {
                        c1: {
                            d1: {
                                e: 2,
                            },
                        },
                    },
                },
            };

            expect(deepEqual(A, B)).toBeTruthy();
        });

        it('should not be equeal if values is not same', () => {
            const A = {
                a: 1,
                b: 2,
            };

            const B = {
                a: 1,
                b: 3,
            };

            expect(deepEqual(A, B)).toBeFalsy();
        });

        it('should not be equeal if keys is not same', () => {
            const A = {
                a: 1,
                b: 2,
            };

            const B = {
                a: 1,
                b: 2,
                c: 3,
            };

            const C = {
                a: 1,
            };

            expect(deepEqual(A, B)).toBeFalsy();
            expect(deepEqual(A, C)).toBeFalsy();
        });

        it('shoult not be deeply equal', () => {
            const A = {
                a: {
                    b: {
                        c: {
                            d: {
                                e: 1,
                            },
                        },
                    },
                    b1: {
                        c1: {
                            d1: {
                                e: 2,
                            },
                        },
                    },
                },
            };

            const B = {
                a: {
                    b: {
                        c: {
                            d: {
                                e: 1,
                            },
                        },
                    },
                    b1: {
                        c1: {
                            d2: {
                                e: 4,
                            },
                        },
                    },
                },
            };

            expect(deepEqual(A, B)).toBeFalsy();
        });
    });

    describe('arrays', () => {
        it('should be equal', () => {
            expect(deepEqual([1], [1])).toBeTruthy();
            expect(deepEqual([0], [0])).toBeTruthy();
            expect(deepEqual([NaN], [NaN])).toBeTruthy();
            expect(deepEqual(['test'], ['test'])).toBeTruthy();
            expect(deepEqual([''], [''])).toBeTruthy();
            expect(deepEqual([null], [null])).toBeTruthy();
            expect(deepEqual([undefined], [undefined])).toBeTruthy();
            expect(deepEqual([true], [true])).toBeTruthy();
            expect(deepEqual([false], [false])).toBeTruthy();

            expect(deepEqual([1, 2], [1, 2])).toBeTruthy();
            expect(deepEqual([], [])).toBeTruthy();
            expect(deepEqual([[1, 2], [1]], [[1, 2], [1]])).toBeTruthy();
        });

        it('should not be equal', () => {
            expect(deepEqual([1, 2], [1])).toBeFalsy();
            expect(deepEqual([1, 2], [1, 3])).toBeFalsy();
            expect(deepEqual([1, [2]], [1, 3])).toBeFalsy();
            expect(deepEqual([1, [2]], [1, [3]])).toBeFalsy();
            expect(deepEqual([1, { a: 1 }], [1, [3]])).toBeFalsy();
            expect(deepEqual([1, { a: 1 }], [1, { a: 2 }])).toBeFalsy();
        });
    });
});
