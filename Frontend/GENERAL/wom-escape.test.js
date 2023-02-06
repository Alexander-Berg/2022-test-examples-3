const { parseTheContainerAt } = require('../parse-container-at');
const { createParser } = require('../skip-blocks');

const {
    inlineWomEscape,
    stringWomEscape,
} = createParser({});

const createLA0ContainerParser = container =>
    (value, fromIndex) =>
        parseTheContainerAt(container, value, fromIndex);

const parseInlineWomEscape = createLA0ContainerParser(inlineWomEscape);
const parseStringWomEscape = createLA0ContainerParser(stringWomEscape);

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
describe('womEscape(string)', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['~', 0],
                {
                    type: 'womEscape',
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
                ['~|', 0],
                {
                    type: 'womEscape',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 2,
                    outerFirstIndex: 2,
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
                ['~xxx', 0],
                {
                    type: 'womEscape',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 4,
                    closingFollowingIndex: 4,
                    outerFirstIndex: 4,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 1,
                            openingFollowingIndex: 1,
                            innerFirstIndex: 1,
                            closingInitialIndex: 4,
                            closingFollowingIndex: 4,
                            outerFirstIndex: 4,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['~| ', 0],
                {
                    type: 'womEscape',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 2,
                    outerFirstIndex: 2,
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
                ['~~ ', 0],
                {
                    type: 'womEscape',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 2,
                    outerFirstIndex: 2,
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
                ['~ test', 0],
                {
                    type: 'womEscape',
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
                ['Тильда внутри ~ текста', 14],
                {
                    type: 'womEscape',
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
        ],
    );

    runSamples(parseStringWomEscape, samples);
});

// 0 &&
describe('womEscape(inline)', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['""1"', 0],
                {
                    type: 'womEscape',
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
            [
                ['"', 0],
                {
                    type: 'womEscape',
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
                ['"""', 0],
                {
                    type: 'womEscape',
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
            [
                ['"1""', 0],
                {
                    type: 'womEscape',
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
                ['""1""', 0],
                {
                    type: 'womEscape',
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
                ['""1"" ""2""', 0],
                {
                    type: 'womEscape',
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
                ['"" 1 "" "" 2 ""', 0],
                {
                    type: 'womEscape',
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
            [
                ['"""""', 0],
                {
                    type: 'womEscape',
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
                ['"" %% x %% ""', 0],
                {
                    type: 'womEscape',
                    inline: true,
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
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 11,
                            closingFollowingIndex: 11,
                            outerFirstIndex: 11,
                            children: [],
                        },
                    ],
                },
            ],
        ],
    );

    runSamples(parseInlineWomEscape, samples);
});
