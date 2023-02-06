const { isLineFeedChar, isWhitespaceChar } = require('./spec');
const {
    parseAnyContainerAt,
    findLineEndBetween,
    findMatchBetween,
} = require('./parse-container-utils');

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

describe('parseAnyContainerAt()', () => {
    it('Should parse any container', () => {
        expect(parseAnyContainerAt([inlineContainer], '(\n\n)', 0)).toEqual(null);

        expect(parseAnyContainerAt([inlineContainer, blockContainer], '(\n\n)', 0)).toEqual({
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
    });
});

describe('findMatchBetween()', () => {
    it('Should not allow match ahead nextIndex', () => {
        expect(findMatchBetween([], 'asd', 0, 2, () => {
            return 42;
        })).toEqual(-1);
    });

    it('Should find free line feed', () => {
        expect(findLineEndBetween([blockContainer], 'asd\n', 0, 4)).toEqual(3);

        expect(findLineEndBetween([blockContainer], '\n', 0, 1)).toEqual(0);

        expect(findLineEndBetween([blockContainer], 'asd\n', 0, 3)).toEqual(-1);

        expect(findLineEndBetween([blockContainer], 'asd', 0, 3)).toEqual(3);

        expect(findLineEndBetween([blockContainer], 'asd\n', 0, 2)).toEqual(-1);

        expect(findLineEndBetween([blockContainer], 'asd', 0, 2)).toEqual(-1);

        expect(findLineEndBetween([blockContainer], 'asd\ntest', 0, 8)).toEqual(3);

        expect(findLineEndBetween([blockContainer], 'asd\\\nte\nst', 0, 10)).toEqual(7);

        expect(findLineEndBetween([blockContainer], 'asd(\n)\ntest', 0, 11)).toEqual(6);

        expect(findLineEndBetween([blockContainer], 'asd(\n)\ntest', 0, 7)).toEqual(6);

        expect(findLineEndBetween([blockContainer], 'asd(\n)\ntest', 0, 5)).toEqual(4);

        expect(findLineEndBetween([blockContainer], 'asd(\n)(\n)\ntest', 0, 14)).toEqual(9);

        expect(findLineEndBetween([blockContainer], 'asd(x)(\n)\ntest', 0, 8)).toEqual(7);

        expect(findLineEndBetween([blockContainer], 'asd(\n)(\n)\ntest', 0, 8)).toEqual(7);

        expect(findLineEndBetween([blockContainer], 'asd(\n)(\n)\ntest', 0, 9)).toEqual(9);

        expect(findLineEndBetween([blockContainer], 'asd(x)(\n)\ntest', 0, 9)).toEqual(9);

        expect(findLineEndBetween([blockContainer], 'asd(\n)\ntest', 0, 6)).toEqual(6);

        expect(findLineEndBetween([blockContainer], '<{test', 0, 6)).toEqual(6);

        expect(findLineEndBetween([blockContainer], '<{t()t', 0, 6)).toEqual(6);
    });
});
