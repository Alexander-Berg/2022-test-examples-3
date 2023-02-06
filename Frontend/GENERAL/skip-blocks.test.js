const { parseTheContainerAt } = require('./parse-container-at');
const { createParser } = require('./skip-blocks');

const {
    inlineWomBlockquote,
    blockWomMarkdown,
    blockWomTable,
} = createParser({});

const createLA0ContainerParser = container =>
    (value, fromIndex) =>
        parseTheContainerAt(container, value, fromIndex);

const parseInlineWomBlockquote = createLA0ContainerParser(inlineWomBlockquote);
const parseBlockWomMarkdown = createLA0ContainerParser(blockWomMarkdown);
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

describe('Infinite loop', () => {
    it('Should parse', () => {
        const node = parseBlockWomMarkdown('%%(md)\n<[\n<[]>\n%%', 0);

        rmExtraProps(node);

        expect(node).toEqual({
            type: 'womMarkdown',
            inline: false,
            attributes: {
                format: 'md',
                params: {},
            },
            openingInitialIndex: 0,
            openingFollowingIndex: 2,
            innerFirstIndex: 6,
            closingInitialIndex: 15,
            closingFollowingIndex: 17,
            outerFirstIndex: 17,
            children: [
                {
                    type: 'unknown',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 6,
                    openingFollowingIndex: 6,
                    innerFirstIndex: 6,
                    closingInitialIndex: 10,
                    closingFollowingIndex: 10,
                    outerFirstIndex: 10,
                    children: [],
                },
                {
                    type: 'womBlockquote',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 10,
                    openingFollowingIndex: 12,
                    innerFirstIndex: 12,
                    closingInitialIndex: 12,
                    closingFollowingIndex: 14,
                    outerFirstIndex: 14,
                    children: [],
                },
                {
                    type: 'unknown',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 14,
                    openingFollowingIndex: 14,
                    innerFirstIndex: 14,
                    closingInitialIndex: 15,
                    closingFollowingIndex: 15,
                    outerFirstIndex: 15,
                    children: [],
                },
            ],
        });
    });
});

// 0 &&
describe('Velocity issue (не должно зависать)', () => {
    it('Should not parse', () => {
        const node = parseInlineWomBlockquote('<[<[<[<[]><[<[]>', 0);

        rmExtraProps(node);

        expect(node).toEqual({
            type: 'womBlockquote',
            inline: true,
            attributes: {},
            openingInitialIndex: 0,
            openingFollowingIndex: 2,
            innerFirstIndex: 2,
            closingInitialIndex: -1,
            closingFollowingIndex: -1,
            outerFirstIndex: -1,
            children: [],
        });
    });

    it('Should parse', () => {
        const node = parseInlineWomBlockquote('<[x%%x]>', 0);

        rmExtraProps(node);

        expect(node).toEqual({
            type: 'womBlockquote',
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
                    type: 'unknown',
                    inline: true,
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
        });
    });

    const samples = [
        (() => {
            const times = 100;
            const str = `${'<[x'.repeat(times)}]>`;

            return [
                [str, 0],
                {
                    type: 'womBlockquote',
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
            ];
        })(),
        (() => {
            const times = 50;
            const str = `${'<[x{[x'.repeat(times)}]>`;

            return [
                [str, 0],
                {
                    type: 'womBlockquote',
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
            ];
        })(),
        (() => {
            const str = `${'<[x'.repeat(100)}`;

            return [
                [str, 0],
                {
                    type: 'womBlockquote',
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
            ];
        })(),
        (() => {
            const str = `${'<[x{[x'.repeat(100)}`;

            return [
                [str, 0],
                {
                    type: 'womBlockquote',
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
            ];
        })(),
    ];

    samples.push(
        (() => {
            const str = `<[{[]>]}${'{[x]}'.repeat(50)}`;

            return [
                [str, 0],
                {
                    type: 'womBlockquote',
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
            ];
        })(),
    );

    runSamples(parseInlineWomBlockquote, samples);
});

describe('Extra slow', () => {
    it('Should parse in normal time', () => {
        expect(parseBlockWomTable(
            '#|\n' +
            '|| #| x || || x | x || |#\n' +
            ('test-'.repeat(20) + '\n').repeat(100) +
            '#| || x || |#\n',
            0,
        )).toEqual({
            type: 'womTable',
            inline: false,
            attributes: {},
            openingInitialIndex: 0,
            openingFollowingIndex: 2,
            innerFirstIndex: 2,
            closingInitialIndex: 26,
            closingFollowingIndex: 28,
            outerFirstIndex: 28,
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
                {
                    type: 'womTableRow',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 11,
                    openingFollowingIndex: 13,
                    innerFirstIndex: 12,
                    closingInitialIndex: 14,
                    closingFollowingIndex: 16,
                    outerFirstIndex: 16,
                    children: [
                        {
                            type: 'womTableCell',
                            inline: false,
                            attributes: {},
                            openingInitialIndex: 12,
                            openingFollowingIndex: 13,
                            innerFirstIndex: 13,
                            closingInitialIndex: 14,
                            closingFollowingIndex: 15,
                            outerFirstIndex: 14,
                            children: [
                                {
                                    type: 'unknown',
                                    inline: false,
                                    attributes: {},
                                    openingInitialIndex: 13,
                                    openingFollowingIndex: 13,
                                    innerFirstIndex: 13,
                                    closingInitialIndex: 14,
                                    closingFollowingIndex: 14,
                                    outerFirstIndex: 14,
                                    children: [],
                                },
                            ],
                        },
                    ],
                },
                {
                    type: 'unknown',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 16,
                    openingFollowingIndex: 16,
                    innerFirstIndex: 16,
                    closingInitialIndex: 26,
                    closingFollowingIndex: 26,
                    outerFirstIndex: 26,
                    children: [],
                },
            ],
        });
    });
});
