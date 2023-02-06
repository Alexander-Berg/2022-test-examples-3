/* eslint-disable camelcase */
const {sortExperimentKeysByDeps} = require.requireActual('../../experiments');

describe('experiments', () => {
    describe('sortExperimentKeysByDeps', () => {
        it('should sort experiment keys according to their dependencies', () => {
            const experiments_1 = {
                a: {
                    dependencies: {
                        c: [true],
                        b: [true],
                    },
                },
                b: {
                    dependencies: {
                        c: [true],
                    },
                },
                c: {},
            };

            const experiments_2 = {
                a: {
                    dependencies: {
                        c: [true],
                    },
                },
                b: {},
                c: {},
            };

            const experiments_3 = {
                a: {
                    dependencies: {
                        c: [true],
                    },
                },
                d: {
                    dependencies: {
                        c: [true],
                        a: [true],
                        b: [true],
                    },
                },
                b: {
                    dependencies: {
                        a: [true],
                    },
                },
                c: {},
            };

            const experiments_4 = {
                a: {},
                b: {},
            };

            const experiments_5 = {
                b: {},
                a: {},
            };

            const experiments_6 = {
                c: {
                    dependencies: {
                        b: [true],
                        a: [true],
                    },
                },
                a: {},
                b: {},
            };

            const experiments_7 = {
                c: {
                    dependencies: {
                        a: [true],
                        b: [true],
                    },
                },
                b: {},
                a: {},
            };

            expect(sortExperimentKeysByDeps(experiments_1)).toEqual([
                'c',
                'b',
                'a',
            ]);
            expect(sortExperimentKeysByDeps(experiments_2)).toEqual([
                'c',
                'a',
                'b',
            ]);
            expect(sortExperimentKeysByDeps(experiments_3)).toEqual([
                'c',
                'a',
                'b',
                'd',
            ]);
            expect(sortExperimentKeysByDeps(experiments_4)).toEqual(['a', 'b']);
            expect(sortExperimentKeysByDeps(experiments_5)).toEqual(['a', 'b']);
            expect(sortExperimentKeysByDeps(experiments_6)).toEqual([
                'a',
                'b',
                'c',
            ]);
            expect(sortExperimentKeysByDeps(experiments_7)).toEqual([
                'a',
                'b',
                'c',
            ]);
        });
    });
});
