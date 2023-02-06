const { parseTheContainerAt } = require('../parse-container-at');
const { createParser } = require('../skip-blocks');

const {
    womImage,
} = createParser({});

const createLA0ContainerParser = container =>
    (value, fromIndex) =>
        parseTheContainerAt(container, value, fromIndex);

const parseWomImage = createLA0ContainerParser(womImage);

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
describe('womImage', () => {
    const samples = [];

    samples.push(
        ...[
            [
                ['0', 0],
                {
                    type: 'womImage',
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
                ['0x0', 0],
                {
                    type: 'womImage',
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
                ['0x0:foo', 0],
                {
                    type: 'womImage',
                    inline: true,
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 0,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    attributes: {
                        url: 'foo',
                        width: 0,
                        height: 0,
                    },
                    children: [],
                },
            ],
            [
                ['x0x0:foo', 1],
                {
                    type: 'womImage',
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
                ['\\0x0:foo', 1],
                {
                    type: 'womImage',
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
                ['0x0: foo', 0],
                {
                    type: 'womImage',
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
                ['50000000000x50000000000:foo', 0],
                {
                    type: 'womImage',
                    inline: true,
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 0,
                    closingFollowingIndex: 27,
                    outerFirstIndex: 27,
                    attributes: {
                        url: 'foo',
                        width: 50000000000,
                        height: 50000000000,
                    },
                    children: [],
                },
            ],
        ],
    );

    runSamples(parseWomImage, samples);
});
