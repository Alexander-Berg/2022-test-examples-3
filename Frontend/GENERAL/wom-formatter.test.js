const { parseTheContainerAt } = require('../parse-container-at');
const { createParser } = require('../skip-blocks');

const {
    blockWomFormatter,
    inlineWomFormatter,
} = createParser({});

const createLA0ContainerParser = container =>
    (value, fromIndex) =>
        parseTheContainerAt(container, value, fromIndex);

const parseBlockWomFormatter = createLA0ContainerParser(blockWomFormatter);
const parseInlineWomFormatter = createLA0ContainerParser(inlineWomFormatter);

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
describe('womFormatter(block)', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['foo\n%%\n1%', 4],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 4,
                    openingFollowingIndex: 6,
                    innerFirstIndex: 6,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
            [
                ['%%%\ntest~%% test%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                            closingInitialIndex: 16,
                            closingFollowingIndex: 16,
                            outerFirstIndex: 16,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%\ntest~%% test%%%', 0],
                {

                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                            closingInitialIndex: 16,
                            closingFollowingIndex: 16,
                            outerFirstIndex: 16,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%\ntest~%% test%%', 0],
                {

                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 15,
                            closingFollowingIndex: 15,
                            outerFirstIndex: 15,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%%(x)\ntest%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
            [
                ['%%\ntest%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                ['%%(math)\n\\%%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: 'math',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
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
            ],
        ],
    );

    samples.push(
        ...[
            [
                ['%', 0],
                {
                    type: 'womFormatter',
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
                ['%%\n1\n%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                ['%%%\n', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                ['%%1%%', 0],
                {
                    type: 'womFormatter',
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
                ['%%\na\n%%%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 7,
                    closingFollowingIndex: 9,
                    outerFirstIndex: 9,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 7,
                            closingFollowingIndex: 7,
                            outerFirstIndex: 7,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%\na\n%%%x', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 6,
                    closingFollowingIndex: 8,
                    outerFirstIndex: 8,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 6,
                            closingFollowingIndex: 6,
                            outerFirstIndex: 6,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%\n1\n%', 0],
                {
                    type: 'womFormatter',
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
                ['%%%\n\n1\n%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 7,
                    closingFollowingIndex: 9,
                    outerFirstIndex: 9,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 7,
                            closingFollowingIndex: 7,
                            outerFirstIndex: 7,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['foo', 0],
                {
                    type: 'womFormatter',
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
                ['%%\nx\n%%\n<{\nx\n}>\nb', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
            [
                ['%%\n1\n%%\n\n%%\n2\n%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
            [
                ['%%\n1\n%%\n2\n%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
            [
                ['%%\n1\n%%\n%%\n1\n%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
            [
                ['%%\n1\n%%\n\n%%\n2\n%%\nx', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
            [
                ['%%%\n1\n%%%\n%%%\n1\n%%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                ['%%(x k="%%")\n1\n%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: 'x',
                        params: {
                            k: '%%',
                        },
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 12,
                    closingInitialIndex: 15,
                    closingFollowingIndex: 17,
                    outerFirstIndex: 17,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 12,
                            openingFollowingIndex: 12,
                            innerFirstIndex: 12,
                            closingInitialIndex: 15,
                            closingFollowingIndex: 15,
                            outerFirstIndex: 15,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%\nxx\n%%\nxx\n%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 6,
                    closingFollowingIndex: 8,
                    outerFirstIndex: 8,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 6,
                            closingFollowingIndex: 6,
                            outerFirstIndex: 6,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%\n""\n%%\n""\n%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 6,
                    closingFollowingIndex: 8,
                    outerFirstIndex: 8,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 6,
                            closingFollowingIndex: 6,
                            outerFirstIndex: 6,
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
                ['%%\n%%%\n1\n%%%\n%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
        ],
    );

    samples.push(
        ...[
            [
                ['%%\n<{\n%%\n}>\n', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 6,
                    closingFollowingIndex: 8,
                    outerFirstIndex: 8,
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 6,
                            closingFollowingIndex: 6,
                            outerFirstIndex: 6,
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
                ['%%%\n%%\n1\n%%\n%%%', 0],
                {
                    type: 'womFormatter',
                    inline: false,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
        ],
    );

    runSamples(parseBlockWomFormatter, samples);
});

// 0 &&
describe('womFormatter(inline)', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['foo %%(math)X~%%%', 4],
                {
                    type: 'womFormatter',
                    attributes: {
                        format: 'math',
                        params: {},
                    },
                    inline: true,
                    openingInitialIndex: 4,
                    openingFollowingIndex: 6,
                    innerFirstIndex: 12,
                    closingInitialIndex: 15,
                    closingFollowingIndex: 17,
                    outerFirstIndex: 17,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 12,
                            openingFollowingIndex: 12,
                            innerFirstIndex: 12,
                            closingInitialIndex: 15,
                            closingFollowingIndex: 15,
                            outerFirstIndex: 15,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%"nmb":%номер файла в списке%%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 30,
                    closingFollowingIndex: 32,
                    outerFirstIndex: 32,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 30,
                            closingFollowingIndex: 30,
                            outerFirstIndex: 30,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['foo%%1%', 3],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 3,
                    openingFollowingIndex: 5,
                    innerFirstIndex: 5,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    children: [],
                },
            ],
            [
                ['%', 0],
                {
                    type: 'womFormatter',
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
                ['%x', 0],
                {
                    type: 'womFormatter',
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
                ['x%%%x', 0],
                {
                    type: 'womFormatter',
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
                ['%%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                ['%%(x k="%%")1%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: 'x',
                        params: {
                            k: '%%',
                        },
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 12,
                    closingInitialIndex: 13,
                    closingFollowingIndex: 15,
                    outerFirstIndex: 15,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 12,
                            openingFollowingIndex: 12,
                            innerFirstIndex: 12,
                            closingInitialIndex: 13,
                            closingFollowingIndex: 13,
                            outerFirstIndex: 13,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%{[%%]}%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                ['%%(x k="%%"){[%%]}%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: 'x',
                        params: {
                            k: '%%',
                        },
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 12,
                    closingInitialIndex: 14,
                    closingFollowingIndex: 16,
                    outerFirstIndex: 16,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 12,
                            openingFollowingIndex: 12,
                            innerFirstIndex: 12,
                            closingInitialIndex: 14,
                            closingFollowingIndex: 14,
                            outerFirstIndex: 14,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%%(md)1~%% 2%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
            [
                ['%%%~%% 2%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 8,
                    closingFollowingIndex: 10,
                    outerFirstIndex: 10,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 8,
                            closingFollowingIndex: 8,
                            outerFirstIndex: 8,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%%(md)1%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 8,
                    closingFollowingIndex: 10,
                    outerFirstIndex: 10,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 2,
                            openingFollowingIndex: 2,
                            innerFirstIndex: 2,
                            closingInitialIndex: 8,
                            closingFollowingIndex: 8,
                            outerFirstIndex: 8,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['%%%1%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                ['%%1%%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
        ],
    );

    samples.push(
        ...[
            [
                ['%%(?foo bar?)%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
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

    samples.push(
        ...[
            [
                ['%%1%%%2%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
        ],
    );

    samples.push(
        ...[
            [
                ['%%%x', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                ['%%x', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
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
                ['%%(cs)\n' +
                'codecodecodecodecode\n' +
                'codecodecodecodecode\n' +
                'code code code code code\n' +
                '←%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: 'cs',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 6,
                    closingInitialIndex: 75,
                    closingFollowingIndex: 77,
                    outerFirstIndex: 77,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 6,
                            openingFollowingIndex: 6,
                            innerFirstIndex: 6,
                            closingInitialIndex: 75,
                            closingFollowingIndex: 75,
                            outerFirstIndex: 75,
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
                ['%%(math)1\\%%%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: 'math',
                        params: {},
                    },
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 8,
                    closingInitialIndex: 11,
                    closingFollowingIndex: 13,
                    outerFirstIndex: 13,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
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
            ],
        ],
    );

    samples.push(
        ...[
            [
                ['\\%%%1%%', 2],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
                    openingInitialIndex: 2,
                    openingFollowingIndex: 4,
                    innerFirstIndex: 4,
                    closingInitialIndex: 5,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [
                        {
                            type: 'unknown',
                            inline: true,
                            attributes: {},
                            openingInitialIndex: 4,
                            openingFollowingIndex: 4,
                            innerFirstIndex: 4,
                            closingInitialIndex: 5,
                            closingFollowingIndex: 5,
                            outerFirstIndex: 5,
                            children: [],
                        },
                    ],
                },
            ],
            [
                ['x%%%1%%', 2],
                {
                    type: 'womFormatter',
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

    // samples.length = 0;

    samples.push(
        ...[
            [
                ['%% ~%%код~%% %%', 0],
                {
                    type: 'womFormatter',
                    inline: true,
                    attributes: {
                        format: '',
                        params: {},
                    },
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

    runSamples(parseInlineWomFormatter, samples);
});
