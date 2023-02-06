const { parseTheContainerAt } = require('../parse-container-at');
const { createParser } = require('../skip-blocks');

const { womMonospace } = createParser({});

const createLA0ContainerParser = container =>
    (value, fromIndex) =>
        parseTheContainerAt(container, value, fromIndex);

const parseWomMonospace = createLA0ContainerParser(womMonospace);

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
describe('womMonospace(inline)', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['#', 0],
                {
                    type: 'womMonospace',
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
                ['#asd##', 0],
                {
                    type: 'womMonospace',
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
                ['## asd##', 0],
                {
                    type: 'womMonospace',
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
                ['##asd ##', 0],
                {
                    type: 'womMonospace',
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
                ['##asd##', 0],
                {
                    type: 'womMonospace',
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

    runSamples(parseWomMonospace, samples);
});
