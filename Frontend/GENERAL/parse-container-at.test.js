const { isLineFeedChar, isWhitespaceChar } = require('./spec');

const {
    parseTheContainerAt,
} = require('./parse-container-at');

const inlineContainer = {
    type: 'braces',
    inline: true,
    possibleChildren: [],
    matchOpening(value, openingInitialIndex, stateNode) {
        if (value[openingInitialIndex] === '(') {
            stateNode.openingInitialIndex = openingInitialIndex;
            stateNode.openingFollowingIndex = openingInitialIndex + 1;
            stateNode.innerFirstIndex = openingInitialIndex + 1;
        }
    },
    matchClosing(value, closingInitialIndex, stateNode) {
        if (value[closingInitialIndex] === ')') {
            stateNode.closingInitialIndex = closingInitialIndex;
            stateNode.closingFollowingIndex = closingInitialIndex + 1;
            stateNode.outerFirstIndex = closingInitialIndex + 1;
        }
    },
    enterSpacing(value, spacingCurrentIndex, stateNode) {
        stateNode.lineFeedCount = 0;
    },
    checkSpacing(value, spacingCurrentIndex, stateNode) {
        if (isLineFeedChar(value, spacingCurrentIndex)) {
            stateNode.lineFeedCount += 1;

            if (stateNode.lineFeedCount > 1) {
                return false;
            }
        } else if (!isWhitespaceChar(value, spacingCurrentIndex)) {
            stateNode.lineFeedCount = 0;
        }

        return true;
    },
};

const blockContainer = {
    ...inlineContainer,
    inline: false,
    enterSpacing() {},
    checkSpacing() {
        return true;
    },
};

inlineContainer.possibleChildren.push(inlineContainer);

describe('parseTheContainerAt()', () => {
    it('Should parse container', () => {
        expect(parseTheContainerAt(inlineContainer, '()', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 0,
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: 1,
            closingFollowingIndex: 2,
            outerFirstIndex: 2,
            children: [],
        });

        expect(parseTheContainerAt(inlineContainer, '(())', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 0,
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: 3,
            closingFollowingIndex: 4,
            outerFirstIndex: 4,
            children: [
                {
                    type: 'braces',
                    inline: true,
                    attributes: {},
                    lineFeedCount: 0,
                    openingInitialIndex: 1,
                    openingFollowingIndex: 2,
                    innerFirstIndex: 2,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 3,
                    outerFirstIndex: 3,
                    children: [],
                },
            ],
        });

        expect(parseTheContainerAt(inlineContainer, '(x)', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 0,
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
        });

        expect(parseTheContainerAt(inlineContainer, '(\n)', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 1,
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
        });

        expect(parseTheContainerAt(inlineContainer, '(\nx\n)', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 1,
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: 4,
            closingFollowingIndex: 5,
            outerFirstIndex: 5,
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
        });

        expect(parseTheContainerAt(inlineContainer, '(\n\\\n)', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 1,
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: 4,
            closingFollowingIndex: 5,
            outerFirstIndex: 5,
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
        });

        expect(parseTheContainerAt(inlineContainer, '(\n\n)', 0)).toEqual({
            type: 'braces',
            inline: true,
            lineFeedCount: 2,
            attributes: {},
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: -1,
            closingFollowingIndex: -1,
            outerFirstIndex: -1,
            children: [],
        });

        expect(parseTheContainerAt(blockContainer, '(\n\n)', 0)).toEqual({
            type: 'braces',
            inline: false,
            attributes: {},
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: 3,
            closingFollowingIndex: 4,
            outerFirstIndex: 4,
            children: [
                {
                    type: 'unknown',
                    inline: false,
                    attributes: {},
                    openingInitialIndex: 1,
                    openingFollowingIndex: 1,
                    innerFirstIndex: 1,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 3,
                    outerFirstIndex: 3,
                    children: [],
                },
            ],
        });

        expect(parseTheContainerAt(inlineContainer, '(\n \n)', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 2,
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: -1,
            closingFollowingIndex: -1,
            outerFirstIndex: -1,
            children: [],
        });

        expect(parseTheContainerAt(inlineContainer, '(x(y)z)', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 0,
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: 6,
            closingFollowingIndex: 7,
            outerFirstIndex: 7,
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
                {
                    type: 'braces',
                    inline: true,
                    attributes: {},
                    lineFeedCount: 0,
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
        });

        expect(parseTheContainerAt(inlineContainer, '(\n(y)\n)', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 1,
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: 6,
            closingFollowingIndex: 7,
            outerFirstIndex: 7,
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
                {
                    type: 'braces',
                    inline: true,
                    attributes: {},
                    lineFeedCount: 0,
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
        });

        expect(parseTheContainerAt(inlineContainer, 'x()', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            openingInitialIndex: -1,
            openingFollowingIndex: -1,
            innerFirstIndex: -1,
            closingInitialIndex: -1,
            closingFollowingIndex: -1,
            outerFirstIndex: -1,
            children: [],
        });

        expect(parseTheContainerAt(inlineContainer, '(x', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: -1,
            closingFollowingIndex: -1,
            outerFirstIndex: -1,
            children: [],
        });

        expect(parseTheContainerAt(inlineContainer, '(()', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 0,
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: -1,
            closingFollowingIndex: -1,
            outerFirstIndex: -1,
            children: [],
        });

        expect(parseTheContainerAt(inlineContainer, '(()(x)', 0)).toEqual({
            type: 'braces',
            inline: true,
            attributes: {},
            lineFeedCount: 0,
            openingInitialIndex: 0,
            openingFollowingIndex: 1,
            innerFirstIndex: 1,
            closingInitialIndex: -1,
            closingFollowingIndex: -1,
            outerFirstIndex: -1,
            children: [],
        });
    });
});
