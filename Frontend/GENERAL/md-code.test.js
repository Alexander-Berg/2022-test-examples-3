const { parseTheContainerAt } = require('../parse-container-at');
const { createParser } = require('../skip-blocks');

const {
    blockMdCode,
    inlineMdCode,
    inlineWomBlock,
    inlineWomTable,
    blockWomTable,
} = createParser({});

const createLA0ContainerParser = container =>
    (value, fromIndex) =>
        parseTheContainerAt(container, value, fromIndex);

const parseBlockMdCode = createLA0ContainerParser(blockMdCode);
const parseInlineMdCode = createLA0ContainerParser(inlineMdCode);
const parseInlineWomBlock = createLA0ContainerParser(inlineWomBlock);
const parseInlineWomTable = createLA0ContainerParser(inlineWomTable);
const parseBlockWomTable = createLA0ContainerParser(blockWomTable);

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
describe('mdCode(block)', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['``', 0],
                {
                    type: 'code',
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
                ['xxx', 0],
                {
                    type: 'code',
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
                ['x```\n1\n```', 1],
                {
                    type: 'code',
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
                ['```1```', 0],
                {
                    type: 'code',
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
                ['``\n1\n```', 0],
                {
                    type: 'code',
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
                ['```\n1\nx```', 0],
                {
                    type: 'code',
                    inline: false,
                    attributes: { format: '' },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
            [
                ['```\n1\n```x', 0],
                {
                    type: 'code',
                    inline: false,
                    attributes: { format: '' },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
            [
                ['```1```', 0],
                {
                    type: 'code',
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
                ['```js\n1\n```', 0],
                {
                    type: 'code',
                    inline: false,
                    attributes: {
                        format: 'js',
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 5,
                    closingInitialIndex: 8,
                    closingFollowingIndex: 11,
                    outerFirstIndex: 11,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 5,
                            openingFollowingIndex: 5,
                            innerFirstIndex: 5,
                            closingInitialIndex: 8,
                            closingFollowingIndex: 8,
                            outerFirstIndex: 8,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['```js foo\n1\n```', 0],
                {
                    type: 'code',
                    inline: false,
                    attributes: {
                        format: 'js',
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 9,
                    closingInitialIndex: 12,
                    closingFollowingIndex: 15,
                    outerFirstIndex: 15,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 9,
                            openingFollowingIndex: 9,
                            innerFirstIndex: 9,
                            closingInitialIndex: 12,
                            closingFollowingIndex: 12,
                            outerFirstIndex: 12,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['```\n1\n```', 0],
                {
                    type: 'code',
                    inline: false,
                    attributes: { format: '' },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: 6,
                    closingFollowingIndex: 9,
                    outerFirstIndex: 9,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 3,
                            openingFollowingIndex: 3,
                            innerFirstIndex: 3,
                            closingInitialIndex: 6,
                            closingFollowingIndex: 6,
                            outerFirstIndex: 6,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['```\n\n1\n``', 0],
                {
                    type: 'code',
                    inline: false,
                    attributes: { format: '' },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
            [
                ['```\n1\n``', 0],
                {
                    type: 'code',
                    inline: false,
                    attributes: { format: '' },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
        ],
    );

    runSamples(parseBlockMdCode, samples);
});

// 0 &&
describe('mdCode(inline)', () => {
    const samples1 = [];

    // 0 &&
    samples1.push(
        ...[
            [
                ['x', 1],
                {
                    type: 'inlineCode',
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
                ['\\`1`', 1],
                {
                    type: 'inlineCode',
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
                ['`\n\n1\n\n`', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
            [
                ['`\nx\n1\nx\n`', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 8,
                    closingFollowingIndex: 9,
                    outerFirstIndex: 9,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 1,
                            openingFollowingIndex: 1,
                            innerFirstIndex: 1,
                            closingInitialIndex: 8,
                            closingFollowingIndex: 8,
                            outerFirstIndex: 8,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['`\n \n1\n \n`', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
            [
                ['`1`', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 3,
                    outerFirstIndex: 3,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 1,
                            openingFollowingIndex: 1,
                            innerFirstIndex: 1,
                            closingInitialIndex: 2,
                            closingFollowingIndex: 2,
                            outerFirstIndex: 2,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['`1``', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
            [
                ['``1`', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 1,
                    closingFollowingIndex: 2,
                    outerFirstIndex: 2,
                    children: [],
                },
            ],
            [
                ['``1`1', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 1,
                    closingFollowingIndex: 2,
                    outerFirstIndex: 2,
                    children: [],
                },
            ],
            [
                ['\\``1`', 2],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 2,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: 4,
                    closingFollowingIndex: 5,
                    outerFirstIndex: 5,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 3,
                            openingFollowingIndex: 3,
                            innerFirstIndex: 3,
                            closingInitialIndex: 4,
                            closingFollowingIndex: 4,
                            outerFirstIndex: 4,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['1`', 0],
                {
                    type: 'inlineCode',
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
                ['```1```', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: 4,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 3,
                            openingFollowingIndex: 3,
                            innerFirstIndex: 3,
                            closingInitialIndex: 4,
                            closingFollowingIndex: 4,
                            outerFirstIndex: 4,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['``1``', 0],
                {
                    type: 'inlineCode',
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
        ],
    );

    // 0 &&
    samples1.push(
        ...[
            [
                ['``````1', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 6,
                    outerFirstIndex: 6,
                    children: [],
                },
            ],
            [
                ['````1', 0],
                {
                    type: 'inlineCode',
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
                ['``1', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 1,
                    closingFollowingIndex: 2,
                    outerFirstIndex: 2,
                    children: [],
                },
            ],
            [
                ['1``````', 1],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 1,
                    openingFollowingIndex: 4,
                    innerFirstIndex: 4,
                    closingInitialIndex: 4,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [],
                },
            ],
            [
                ['1````', 1],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 1,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 5,
                    outerFirstIndex: 5,
                    children: [],
                },
            ],
            [
                ['1``', 1],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 1,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 3,
                    outerFirstIndex: 3,
                    children: [],
                },
            ],
            [
                ['1``````1', 1],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 1,
                    openingFollowingIndex: 4,
                    innerFirstIndex: 4,
                    closingInitialIndex: 4,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [],
                },
            ],
            [
                ['1````1', 1],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 1,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 5,
                    outerFirstIndex: 5,
                    children: [],
                },
            ],
            [
                ['1``1', 1],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 1,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 3,
                    outerFirstIndex: 3,
                    children: [],
                },
            ],
            [
                ['``', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 1,
                    closingFollowingIndex: 2,
                    outerFirstIndex: 2,
                    children: [],
                },
            ],
            [
                ['````', 0],
                {
                    type: 'inlineCode',
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
                ['``````', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 6,
                    outerFirstIndex: 6,
                    children: [],
                },
            ],
            [
                ['```', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
        ],
    );

    // 0 &&
    samples1.push(
        ...[
            [
                ['```1`123123', 0],
                {
                    type: 'inlineCode',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 3,
                    innerFirstIndex: 3,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
        ],
    );

    runSamples(parseInlineMdCode, samples1);

    const samples2 = [];

    // 0 &&
    samples2.push(
        ...[
            [
                ['{[```1`123123]}', 0],
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
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 13,
                            closingFollowingIndex: 13,
                            outerFirstIndex: 13,
                            children: [],
                        },
                    ],
                },
            ],
        ],
    );

    runSamples(parseInlineWomBlock, samples2);

    const samples3 = [];

    // 0 &&
    samples3.push(
        ...[
            [
                ['#|||``|||#', 0],
                {
                    type: 'womTable',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 8,
                    closingFollowingIndex: 10,
                    outerFirstIndex: 10,
                    children: [
                        {
                            type: 'womTableRow',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 4,
                            innerFirstIndex: 3,
                            closingInitialIndex: 6,
                            closingFollowingIndex: 8,
                            outerFirstIndex: 8,
                            children: [
                                {
                                    type: 'womTableCell',
                                    inline: false,
                                    attributes: {},
                                    openingInitialIndex: 3,
                                    openingFollowingIndex: 4,
                                    innerFirstIndex: 4,
                                    closingInitialIndex: 6,
                                    closingFollowingIndex: 7,
                                    outerFirstIndex: 6,
                                    children: [
                                        {
                                            type: 'inlineCode',
                                            inline: true,
                                            openingInitialIndex: 4,
                                            openingFollowingIndex: 5,
                                            innerFirstIndex: 5,
                                            closingInitialIndex: 5,
                                            closingFollowingIndex: 6,
                                            outerFirstIndex: 6,
                                            attributes: {},
                                            children: [],
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            ],
        ],
    );

    runSamples(parseInlineWomTable, samples3);

    const samples4 = [];

    samples4.push(
        ...[
            [
                ['#|\n||`foo `` | `` bar`||\n|#\n', 0],
                {
                    attributes: {},
                    children: [
                        {
                            attributes: {},
                            children: [],
                            closingFollowingIndex: 3,
                            closingInitialIndex: 3,
                            inline: false,
                            innerFirstIndex: 2,
                            openingFollowingIndex: 2,
                            openingInitialIndex: 2,
                            outerFirstIndex: 3,
                            type: 'unknown',
                        },
                        {
                            attributes: {},
                            children: [
                                {
                                    attributes: {},
                                    children: [
                                        {
                                            attributes: {},
                                            children: [
                                                {
                                                    attributes: {},
                                                    children: [],
                                                    closingFollowingIndex: 21,
                                                    closingInitialIndex: 21,
                                                    inline: true,
                                                    innerFirstIndex: 6,
                                                    openingFollowingIndex: 6,
                                                    openingInitialIndex: 6,
                                                    outerFirstIndex: 21,
                                                    type: 'unknown',
                                                },
                                            ],
                                            closingFollowingIndex: 22,
                                            closingInitialIndex: 21,
                                            inline: true,
                                            innerFirstIndex: 6,
                                            openingFollowingIndex: 6,
                                            openingInitialIndex: 5,
                                            outerFirstIndex: 22,
                                            type: 'inlineCode',
                                        },
                                    ],
                                    closingFollowingIndex: 23,
                                    closingInitialIndex: 22,
                                    inline: false,
                                    innerFirstIndex: 5,
                                    openingFollowingIndex: 5,
                                    openingInitialIndex: 4,
                                    outerFirstIndex: 22,
                                    type: 'womTableCell',
                                },
                            ],
                            closingFollowingIndex: 24,
                            closingInitialIndex: 22,
                            inline: false,
                            innerFirstIndex: 4,
                            openingFollowingIndex: 5,
                            openingInitialIndex: 3,
                            outerFirstIndex: 24,
                            type: 'womTableRow',
                        },
                        {
                            attributes: {},
                            children: [],
                            closingFollowingIndex: 25,
                            closingInitialIndex: 25,
                            inline: false,
                            innerFirstIndex: 24,
                            openingFollowingIndex: 24,
                            openingInitialIndex: 24,
                            outerFirstIndex: 25,
                            type: 'unknown',
                        },
                    ],
                    closingFollowingIndex: 27,
                    closingInitialIndex: 25,
                    inline: false,
                    innerFirstIndex: 2,
                    openingFollowingIndex: 2,
                    openingInitialIndex: 0,
                    outerFirstIndex: 27,
                    type: 'womTable',
                },
            ],
        ],
    );

    runSamples(parseBlockWomTable, samples4);
});
