const { parseTheContainerAt } = require('../parse-container-at');
const { createParser } = require('../skip-blocks');

const {
    blockWomHtml,
    inlineWomHtml,
} = createParser({});

const createLA0ContainerParser = container =>
    (value, fromIndex) =>
        parseTheContainerAt(container, value, fromIndex);

const parseBlockWomHtml = createLA0ContainerParser(blockWomHtml);
const parseInlineWomHtml = createLA0ContainerParser(inlineWomHtml);

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
describe('womHtml(block)', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['<', 0],
                {
                    type: 'womHtml',
                    inline: false,
                    openingInitialIndex: -1,
                    openingFollowingIndex: -1,
                    innerFirstIndex: -1,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    attributes: {},
                    children: [],
                },
            ],
            [
                ['<#\n#>', 0],
                {
                    type: 'womHtml',
                    inline: false,
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 5,
                    outerFirstIndex: 5,
                    attributes: {},
                    children: [
                        {
                            type: 'unknown',
                            inline: false,
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
                ['<##>', 0],
                {
                    type: 'womHtml',
                    inline: false,
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 4,
                    outerFirstIndex: 4,
                    attributes: {},
                    children: [],
                },
            ],
            [
                ['<##', 0],
                {
                    type: 'womHtml',
                    inline: false,
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    attributes: {},
                    children: [],
                },
            ],
            [
                ['<!#>', 0],
                {
                    type: 'womHtml',
                    inline: false,
                    openingInitialIndex: -1,
                    openingFollowingIndex: -1,
                    innerFirstIndex: -1,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    attributes: {},
                    children: [],
                },
            ],
            [
                ['x<#', 1],
                {
                    type: 'womHtml',
                    inline: false,
                    openingInitialIndex: -1,
                    openingFollowingIndex: -1,
                    innerFirstIndex: -1,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    attributes: {},
                    children: [],
                },
            ],
        ],
    );

    runSamples(parseBlockWomHtml, samples);
});

// 0 &&
describe('womHtml(inline)', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['<', 0],
                {
                    type: 'womHtml',
                    inline: true,
                    openingInitialIndex: -1,
                    openingFollowingIndex: -1,
                    innerFirstIndex: -1,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    attributes: {},
                    children: [],
                },
            ],
            [
                ['<##', 0],
                {
                    type: 'womHtml',
                    inline: true,
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    attributes: {},
                    children: [],
                },
            ],
            [
                ['<#\n#>', 0],
                {
                    type: 'womHtml',
                    inline: true,
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 5,
                    outerFirstIndex: 5,
                    attributes: {},
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
                ['<##>', 0],
                {
                    type: 'womHtml',
                    inline: true,
                    openingInitialIndex: 0,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 4,
                    outerFirstIndex: 4,
                    attributes: {},
                    children: [],
                },
            ],
            [
                ['<!#>', 0],
                {
                    type: 'womHtml',
                    inline: true,
                    openingInitialIndex: -1,
                    openingFollowingIndex: -1,
                    innerFirstIndex: -1,
                    closingInitialIndex: -1,
                    closingFollowingIndex: -1,
                    outerFirstIndex: -1,
                    attributes: {},
                    children: [],
                },
            ],
        ],
    );

    runSamples(parseInlineWomHtml, samples);
});
