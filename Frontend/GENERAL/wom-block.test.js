const { parseTheContainerAt } = require('../parse-container-at');
const { createParser } = require('../skip-blocks');

const {
    blockWomBlock,
    inlineWomBlock,
} = createParser({});

const createLA0ContainerParser = container =>
    (value, fromIndex) =>
        parseTheContainerAt(container, value, fromIndex);

const parseBlockWomBlock = createLA0ContainerParser(blockWomBlock);
const parseInlineWomBlock = createLA0ContainerParser(inlineWomBlock);

function traverse(node, fn) {
    fn(node);

    for (const childNode of node.children) {
        traverse(childNode, fn);
    }
}

function rmExtraProps(root) {
    traverse(root, node => {
        delete node.lineFeedCount;
    });
}

function runSamples(fn, samples) {
    for (const [args, expected] of samples) {
        it(JSON.stringify([...args]), () => {
            const node = fn(...args);

            rmExtraProps(node);

            expect(node).toEqual(expected);
        });
    }
}

// 0 &&
describe('womBlock(block)', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['{', 0],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: -1,
                    openingFollowingIndex: -1,
                    innerFirstIndex: -1,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
            [
                ['{[\n\n\n]}\n', 0],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 5,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 5,
                            closingFollowingIndex: 5,
                            outerFirstIndex: 5,
                            children: [],
                        },
                    ],
                },
            ],
        ],
    );

    samples.push(
        ...[
            [
                ['{[\na{[b]}c\n]}', 0],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 11,
                    closingFollowingIndex: 13,
                    outerFirstIndex: 13,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            innerFirstIndex: 2,
                            openingFollowingIndex: 2,
                            closingInitialIndex: 4,
                            outerFirstIndex: 4,
                            closingFollowingIndex: 4,
                            children: [],
                        },
                        {
                            type: 'womBlock',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 4,
                            openingFollowingIndex: 6,
                            innerFirstIndex: 6,
                            closingInitialIndex: 7,
                            closingFollowingIndex: 9,
                            outerFirstIndex: 9,
                            children: [
                                {
                                    type: 'unknown',
                                    inline: true,
                                    attributes: {},
                                    openingInitialIndex: 6,
                                    innerFirstIndex: 6,
                                    openingFollowingIndex: 6,
                                    closingInitialIndex: 7,
                                    outerFirstIndex: 7,
                                    closingFollowingIndex: 7,
                                    children: [],
                                },
                            ],
                        },
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 9,
                            innerFirstIndex: 9,
                            openingFollowingIndex: 9,
                            closingInitialIndex: 11,
                            outerFirstIndex: 11,
                            closingFollowingIndex: 11,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[\na{[b\n]}c\n]}', 0],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 12,
                    closingFollowingIndex: 14,
                    outerFirstIndex: 14,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            innerFirstIndex: 2,
                            openingFollowingIndex: 2,
                            closingInitialIndex: 4,
                            outerFirstIndex: 4,
                            closingFollowingIndex: 4,
                            children: [],
                        },
                        {
                            type: 'womBlock',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 4,
                            openingFollowingIndex: 6,
                            innerFirstIndex: 6,
                            closingInitialIndex: 8,
                            closingFollowingIndex: 10,
                            outerFirstIndex: 10,
                            children: [
                                {
                                    type: 'unknown',
                                    inline: true,
                                    attributes: {},
                                    openingInitialIndex: 6,
                                    innerFirstIndex: 6,
                                    openingFollowingIndex: 6,
                                    closingInitialIndex: 8,
                                    outerFirstIndex: 8,
                                    closingFollowingIndex: 8,
                                    children: [],
                                },
                            ],
                        },
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 10,
                            innerFirstIndex: 10,
                            openingFollowingIndex: 10,
                            closingInitialIndex: 12,
                            outerFirstIndex: 12,
                            closingFollowingIndex: 12,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[\nb]}\n]}', 0],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 4,
                    closingFollowingIndex: 6,
                    outerFirstIndex: 6,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            innerFirstIndex: 2,
                            openingFollowingIndex: 2,
                            closingInitialIndex: 4,
                            outerFirstIndex: 4,
                            closingFollowingIndex: 4,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[\nx\n{[\ny\n]}\nz\n]}', 0],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 15,
                    closingFollowingIndex: 17,
                    outerFirstIndex: 17,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            innerFirstIndex: 2,
                            openingFollowingIndex: 2,
                            closingInitialIndex: 5,
                            outerFirstIndex: 5,
                            closingFollowingIndex: 5,
                            children: [],
                        },
                        {
                            type: 'womBlock',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 5,
                            openingFollowingIndex: 7,
                            innerFirstIndex: 7,
                            closingInitialIndex: 10,
                            closingFollowingIndex: 12,
                            outerFirstIndex: 12,
                            children: [
                                {
                                    type: 'unknown',
                                    inline: false,
                                    attributes: {},
                                    openingInitialIndex: 7,
                                    innerFirstIndex: 7,
                                    openingFollowingIndex: 7,
                                    closingInitialIndex: 10,
                                    outerFirstIndex: 10,
                                    closingFollowingIndex: 10,
                                    children: [],
                                },
                            ],
                        },
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 12,
                            innerFirstIndex: 12,
                            openingFollowingIndex: 12,
                            closingInitialIndex: 15,
                            outerFirstIndex: 15,
                            closingFollowingIndex: 15,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[\nx\n{[y]}\nz\n]}', 0],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 13,
                    closingFollowingIndex: 15,
                    outerFirstIndex: 15,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            innerFirstIndex: 2,
                            openingFollowingIndex: 2,
                            closingInitialIndex: 5,
                            outerFirstIndex: 5,
                            closingFollowingIndex: 5,
                            children: [],
                        },
                        {
                            type: 'womBlock',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 5,
                            innerFirstIndex: 7,
                            openingFollowingIndex: 7,
                            closingInitialIndex: 8,
                            outerFirstIndex: 10,
                            closingFollowingIndex: 10,
                            children: [
                                {
                                    type: 'unknown',
                                    inline: true,
                                    attributes: {},
                                    openingInitialIndex: 7,
                                    innerFirstIndex: 7,
                                    openingFollowingIndex: 7,
                                    closingInitialIndex: 8,
                                    outerFirstIndex: 8,
                                    closingFollowingIndex: 8,
                                    children: [],
                                },
                            ],
                        },
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 10,
                            innerFirstIndex: 10,
                            openingFollowingIndex: 10,
                            closingInitialIndex: 13,
                            outerFirstIndex: 13,
                            closingFollowingIndex: 13,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[\nx\n\n{[\ny\n]}\nz\n]}', 0],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 16,
                    closingFollowingIndex: 18,
                    outerFirstIndex: 18,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 6,
                            outerFirstIndex: 6,
                            closingFollowingIndex: 6,
                            children: [],
                        },
                        {
                            type: 'womBlock',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 6,
                            openingFollowingIndex: 8,
                            innerFirstIndex: 8,
                            closingInitialIndex: 11,
                            closingFollowingIndex: 13,
                            outerFirstIndex: 13,
                            children: [
                                {
                                    type: 'unknown',
                                    inline: false,
                                    attributes: {},
                                    openingInitialIndex: 8,
                                    openingFollowingIndex: 8,
                                    innerFirstIndex: 8,
                                    closingInitialIndex: 11,
                                    closingFollowingIndex: 11,
                                    outerFirstIndex: 11,
                                    children: [],
                                },
                            ],
                        },
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 13,
                            innerFirstIndex: 13,
                            openingFollowingIndex: 13,
                            closingInitialIndex: 16,
                            outerFirstIndex: 16,
                            closingFollowingIndex: 16,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[a\n{[\n\nb\n\n]}\nc]}', 4],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 4,
                    openingFollowingIndex: 6,
                    innerFirstIndex: 6,
                    closingInitialIndex: 11,
                    closingFollowingIndex: 13,
                    outerFirstIndex: 13,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 6,
                            innerFirstIndex: 6,
                            openingFollowingIndex: 6,
                            closingInitialIndex: 11,
                            outerFirstIndex: 11,
                            closingFollowingIndex: 11,
                            children: [],
                        },
                    ],
                },
            ],
        ],
    );

    samples.push(
        ...[
            [
                ['{[', 0],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
        ],
    );

    samples.push(
        ...[
            [
                ['{[\n\\]}', 0],
                {
                    type: 'womBlock',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
        ],
    );

    runSamples(parseBlockWomBlock, samples);
});

// 0 &&
describe('womBlock(inline)', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['{', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: -1,
                    openingFollowingIndex: -1,
                    innerFirstIndex: -1,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
            [
                ['{[b]}', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 5,
                    outerFirstIndex: 5,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 3,
                            closingFollowingIndex: 3,
                            outerFirstIndex: 3,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[b]}\n', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 5,
                    outerFirstIndex: 5,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 3,
                            closingFollowingIndex: 3,
                            outerFirstIndex: 3,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[b\n]}\n', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 4,
                    closingFollowingIndex: 6,
                    outerFirstIndex: 6,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 4,
                            closingFollowingIndex: 4,
                            outerFirstIndex: 4,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[a\n{[\nb\n]}\nc]}', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 13,
                    closingFollowingIndex: 15,
                    outerFirstIndex: 15,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            innerFirstIndex: 2,
                            openingFollowingIndex: 2,
                            closingInitialIndex: 4,
                            outerFirstIndex: 4,
                            closingFollowingIndex: 4,
                            children: [],
                        },
                        {
                            type: 'womBlock',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 4,
                            innerFirstIndex: 6,
                            openingFollowingIndex: 6,
                            closingInitialIndex: 9,
                            outerFirstIndex: 11,
                            closingFollowingIndex: 11,
                            children: [
                                {
                                    type: 'unknown',
                                    inline: false,
                                    attributes: {},
                                    openingInitialIndex: 6,
                                    innerFirstIndex: 6,
                                    openingFollowingIndex: 6,
                                    closingInitialIndex: 9,
                                    outerFirstIndex: 9,
                                    closingFollowingIndex: 9,
                                    children: [],
                                },
                            ],
                        },
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 11,
                            innerFirstIndex: 11,
                            openingFollowingIndex: 11,
                            closingInitialIndex: 13,
                            outerFirstIndex: 13,
                            closingFollowingIndex: 13,
                            children: [],
                        },
                    ],
                },
            ],
        ],
    );

    samples.push(
        ...[
            [
                ['{[x\n]}', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 4,
                    closingFollowingIndex: 6,
                    outerFirstIndex: 6,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 4,
                            closingFollowingIndex: 4,
                            outerFirstIndex: 4,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[x\n\n]}', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 5,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 5,
                            closingFollowingIndex: 5,
                            outerFirstIndex: 5,
                            children: [],
                        },
                    ],
                },
            ],
        ],
    );

    samples.push(
        ...[
            [
                ['{[]}', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 4,
                    outerFirstIndex: 4,
                    children: [],
                },
            ],
            [
                ['{[{[]}]}', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 6,
                    closingFollowingIndex: 8,
                    outerFirstIndex: 8,
                    children: [
                        {
                            type: 'womBlock',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 4,
                            innerFirstIndex: 4,
                            closingInitialIndex: 4,
                            closingFollowingIndex: 6,
                            outerFirstIndex: 6,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['{[1{[2]}3]}', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 9,
                    closingFollowingIndex: 11,
                    outerFirstIndex: 11,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 3,
                            closingFollowingIndex: 3,
                            outerFirstIndex: 3,
                            children: [],
                        },
                        {
                            type: 'womBlock',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 3,
                            openingFollowingIndex: 5,
                            innerFirstIndex: 5,
                            closingInitialIndex: 6,
                            closingFollowingIndex: 8,
                            outerFirstIndex: 8,
                            children: [
                                {
                                    type: 'unknown',
                                    inline: true,
                                    attributes: {},
                                    openingInitialIndex: 5,
                                    openingFollowingIndex: 5,
                                    innerFirstIndex: 5,
                                    closingInitialIndex: 6,
                                    closingFollowingIndex: 6,
                                    outerFirstIndex: 6,
                                    children: [],
                                },
                            ],
                        },
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 8,
                            openingFollowingIndex: 8,
                            innerFirstIndex: 8,
                            closingInitialIndex: 9,
                            closingFollowingIndex: 9,
                            outerFirstIndex: 9,
                            children: [],
                        },
                    ],
                },
            ],
        ],
    );

    samples.push(
        ...[
            [
                ['{[{[]}{[{[]}', 0],
                {
                    type: 'womBlock',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
        ],
    );

    runSamples(parseInlineWomBlock, samples);
});
