import { IObject, IPair, toPairsDeep } from './toPairsDeep';

const structure: IObject = {
    '@jest/console': {
        version: '24.9.0',
        requires: {
            '@jest/source-map': '^24.9.0',
            chalk: '^2.0.1',
            slash: '^2.0.0',
        },
        dependencies: {
            slash: {
                version: '2.0.0',
            },
        },
    },
};

const pairs: IPair = [
    ['@jest/console',
        [
            ['dependencies',
                [['slash', [['version', '2.0.0']]]],
            ],
            ['requires', [['@jest/source-map', '^24.9.0'], ['chalk', '^2.0.1'], ['slash', '^2.0.0']]],
            ['version', '24.9.0'],
        ],
    ],
];

describe('toPairsDeep', () => {
    it('getReleaseServiceName', () => {
        expect(toPairsDeep(structure)).toEqual(pairs);
    });
});
