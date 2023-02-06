const {
    skipLineFollowingSpacing,
    skipLineRest,
    getNodeInnerText,
    trimPrecedingBlankLine,
    trimEqualSpacingAround,
    createStickyMatcher,
    createNativeStickyMatcher,
    createPolyfillStickyMatcher,
    parseWomActionHeader,
    parseContainerParams,
    trimPrecedingSpacing,
    trimFollowingSpacing,
} = require('./skip-blocks-utils');

describe('skipLineFollowingSpacing()', () => {
    it('Should return line tail following index', () => {
        expect(skipLineFollowingSpacing('x', 1)).toEqual(1);
        expect(skipLineFollowingSpacing('xx', 1)).toEqual(-1);
        expect(skipLineFollowingSpacing('x x\n', 1)).toEqual(-1);
        expect(skipLineFollowingSpacing('x', 0)).toEqual(-1);
        expect(skipLineFollowingSpacing('x  ', 0)).toEqual(-1);
        expect(skipLineFollowingSpacing('x  ', 1)).toEqual(3);
        expect(skipLineFollowingSpacing('x  \n', 1)).toEqual(3);
        expect(skipLineFollowingSpacing('x  \n  ', 1)).toEqual(3);
    });
});

describe('skipLineRest()', () => {
    it('Should return line rest following index', () => {
        expect(skipLineRest('xx', 0)).toEqual(2);
        expect(skipLineRest('xx\n', 0)).toEqual(2);
        expect(skipLineRest('xx\nx', 0)).toEqual(2);
        expect(skipLineRest('xx\n\nx', 0)).toEqual(2);
        expect(skipLineRest('', 0)).toEqual(0);
        expect(skipLineRest('x', 1)).toEqual(1);
    });
});

describe('getNodeInnerText()', () => {
    it('Should return node source', () => {
        expect(getNodeInnerText('a', {
            children: [],
        })).toEqual('');

        expect(getNodeInnerText('xyz', {
            children: [
                {
                    openingInitialIndex: 1,
                    outerFirstIndex: 2,
                },
            ],
        })).toEqual('y');

        expect(getNodeInnerText('xyz', {
            children: [
                {
                    openingInitialIndex: 1,
                    outerFirstIndex: 1,
                },
                {
                    openingInitialIndex: 2,
                    outerFirstIndex: 2,
                },
            ],
        })).toEqual('y');
    });
});

describe('trimPrecedingBlankLine()', () => {
    it('Should not fail on empty nodes', () => {
        const node = {
            children: [],
        };

        trimPrecedingBlankLine('', node);
    });

    it('Should do nothing on known nodes', () => {
        const node = {
            children: [
                {
                    type: 'asd',
                    openingInitialIndex: 0,
                    outerFirstIndex: 2,
                },
            ],
        };

        trimPrecedingBlankLine(' \n', node);

        expect(node.children.length).toEqual(1);
    });

    it('Should remove empty unknown children', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    openingInitialIndex: 0,
                    outerFirstIndex: 0,
                },
                {
                    type: 'unknown',
                    openingInitialIndex: 0,
                    outerFirstIndex: 2,
                },
                {
                    type: 'unknown',
                    openingInitialIndex: 2,
                    outerFirstIndex: 2,
                },
            ],
        };

        trimPrecedingBlankLine(' \n', node);

        expect(node.children.length).toEqual(0);
    });

    it('Should remove empty unknown even if not trimmed', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    openingInitialIndex: 0,
                    outerFirstIndex: 0,
                },
            ],
        };

        trimPrecedingBlankLine('x', node);

        expect(node.children.length).toEqual(0);
    });

    it('Should trim space before', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 3,
                    outerFirstIndex: 3,
                },
            ],
        };

        trimPrecedingBlankLine(' \nx', node);

        expect(node.children.length).toEqual(1);
        expect(node.children[0].openingInitialIndex).toEqual(2);
        expect(node.children[0].openingFollowingIndex).toEqual(2);
        expect(node.children[0].innerFirstIndex).toEqual(2);
    });

    it('Should trim space to content start', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 3,
                    outerFirstIndex: 3,
                },
            ],
        };

        trimPrecedingBlankLine('  x', node);

        expect(node.children.length).toEqual(1);
        expect(node.children[0].openingInitialIndex).toEqual(2);
        expect(node.children[0].openingFollowingIndex).toEqual(2);
        expect(node.children[0].innerFirstIndex).toEqual(2);
    });

    it('Should stop after first line feed', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 3,
                    outerFirstIndex: 3,
                },
            ],
        };

        trimPrecedingBlankLine(' \n \n', node);

        expect(node.children.length).toEqual(1);
        expect(node.children[0].openingInitialIndex).toEqual(2);
        expect(node.children[0].openingFollowingIndex).toEqual(2);
        expect(node.children[0].innerFirstIndex).toEqual(2);
    });
});

describe('trimEqualSpacingAround()', () => {
    it('Should not fail on empty nodes', () => {
        const node = {
            children: [],
        };

        trimEqualSpacingAround('', node);
    });

    it('Should remove empty nodes after trim', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 2,
                    closingFollowingIndex: 2,
                    outerFirstIndex: 2,
                    children: [],
                },
            ],
        };

        trimEqualSpacingAround('  ', node);

        expect(node.children.length).toEqual(0);
    });

    it('Should remove empty nodes after trim 2', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 1,
                    closingFollowingIndex: 1,
                    outerFirstIndex: 1,
                    children: [],
                },
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
        };

        trimEqualSpacingAround('  ', node);

        expect(node.children.length).toEqual(0);
    });

    it('Should no nothing if both corner nodes are known 1', () => {
        const node = {
            children: [
                {
                    type: 'asd',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 1,
                    closingFollowingIndex: 1,
                    outerFirstIndex: 1,
                    children: [],
                },
                {
                    type: 'asd',
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
        };

        trimEqualSpacingAround('  ', node);

        expect(node.children.length).toEqual(2);
    });

    it('Should no nothing if both corner nodes are known 2', () => {
        const node = {
            children: [
                {
                    type: 'asd',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 1,
                    closingFollowingIndex: 1,
                    outerFirstIndex: 1,
                    children: [],
                },
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
        };

        trimEqualSpacingAround('  ', node);

        expect(node.children.length).toEqual(2);
    });

    it('Should no nothing if both corner nodes are known 3', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 1,
                    closingFollowingIndex: 1,
                    outerFirstIndex: 1,
                    children: [],
                },
                {
                    type: 'asd',
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
        };

        trimEqualSpacingAround('  ', node);

        expect(node.children.length).toEqual(2);
    });

    it('Should not remove empty nodes after trim', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 3,
                    closingFollowingIndex: 3,
                    outerFirstIndex: 3,
                    children: [],
                },
            ],
        };

        trimEqualSpacingAround(' 1 ', node);

        expect(node.children.length).toEqual(1);

        expect(node.children[0].openingInitialIndex).toEqual(1);
        expect(node.children[0].openingFollowingIndex).toEqual(1);
        expect(node.children[0].innerFirstIndex).toEqual(1);

        expect(node.children[0].closingInitialIndex).toEqual(2);
        expect(node.children[0].closingFollowingIndex).toEqual(2);
        expect(node.children[0].outerFirstIndex).toEqual(2);
    });
});

describe('createStickyMatcher()', () => {
    it('Should work equally', () => {
        const samples = [
            [
                ['s'],
                ['asd', 1],
                ['s'],
            ],
            [
                ['d'],
                ['asd', 0],
                null,
            ],
            [
                ['x'],
                ['asd', 0],
                null,
            ],
        ];

        function call(fn, ...args) {
            const res = fn(...args);

            if (Array.isArray(res)) {
                return [...res];
            }

            return res;
        }

        for (const [args1, args2, expected] of samples) {
            expect(call(createStickyMatcher(...args1), ...args2))
                .toEqual(expected);
            expect(call(createNativeStickyMatcher(...args1), ...args2))
                .toEqual(expected);
            expect(call(createPolyfillStickyMatcher(...args1), ...args2))
                .toEqual(expected);
        }
    });
});

describe('params', () => {
    describe('parseWomActionHeader()', () => {
        const samples = [
            [
                [
                    '{{' +
                    'tasks ' +
                    'url="https://st.test.yandex-team.ru' +
                    '/staff/filter' +
                    '?author=user3370&status=4f6b29673004ccc0zx27df1e"50&type=4f6b271330049b12e72e3ef3"' +
                    '}}',
                    0,
                ],
                {
                    format: 'tasks',
                    offset: 140,
                    params: ' url="https://st.test.yandex-team.ru/staff/filter?author=user3370&status=4f6b29673004ccc0zx27df1e"50&type=4f6b271330049b12e72e3ef3"',
                },
            ],
            [
                ['{{tree page="/users/spacy}}{{tree page=/users/vas"}}', 0],
                {
                    format: 'tree',
                    offset: 27,
                    params: ' page="/users/spacy',
                },
            ],
            [
                ['{{test}}{{x}}', 0],
                {
                    format: 'test',
                    offset: 8,
                    params: '',
                },
            ],
            [
                ['{{ test}}{{x}}', 0],
                {
                    format: 'test',
                    offset: 9,
                    params: '',
                },
            ],
            [
                ['{{test }}{{x}}', 0],
                {
                    format: 'test',
                    offset: 9,
                    params: ' ',
                },
            ],
            [
                ['{{ test }}{{x}}', 0],
                {
                    format: 'test',
                    offset: 10,
                    params: ' ',
                },
            ],
            [
                ['{{TEST}}{{x}}', 0],
                {
                    format: 'test',
                    offset: 8,
                    params: '',
                },
            ],
            [
                ['{{x a b = 11 c=\'22\' d="33" }}{{x}}', 0],
                {
                    format: 'x',
                    offset: 29,
                    params: ' a b = 11 c=\'22\' d="33" ',
                },
            ],
            [
                ['{{test x="" y = \'\' a= b=}}{{x}}', 0],
                {
                    format: 'test',
                    offset: 26,
                    params: ' x="" y = \'\' a= b=',
                },
            ],
            [
                ['{{foo bar="https://foo"bar"}}{{x}}', 0],
                {
                    format: 'foo',
                    offset: 29,
                    params: ' bar="https://foo"bar"',
                },
            ],
            [
                ['{{test в г=д ж=\'з\' и="й"}}', 0],
                {
                    format: 'test',
                    offset: 26,
                    params: ' в г=д ж=\'з\' и="й"',
                },
            ],
            [
                ['{{test =}}', 0],
                {
                    format: 'test',
                    params: ' =',
                    offset: 10,
                },
            ],
        ];

        for (const [args, expected] of samples) {
            it(JSON.stringify(args), () => {
                expect(parseWomActionHeader(...args)).toEqual(expected);
            });
        }
    });

    describe('parseContainerParams', () => {
        const samples = [
            [
                ['foo=bar bar=baz'],
                {
                    foo: 'bar',
                    bar: 'baz',
                },
            ],
            [
                ['foo="bar" bar=\'baz\''],
                {
                    foo: 'bar',
                    bar: 'baz',
                },
            ],
            [
                ['foo="bar" bar=\'baz\' baz=zot'],
                {
                    foo: 'bar',
                    bar: 'baz',
                    baz: 'zot',
                },
            ],
            [
                ['foo="bar'],
                {
                    foo: '"bar',
                },
            ],
            [
                [' a b=1 c=\'2\' d="3"'],
                {
                    a: null,
                    b: '1',
                    c: '2',
                    d: '3',
                },
            ],
            [
                ['='],
                {},
            ],
            [
                [' x="" y=\'\' a= b='],
                {
                    x: null,
                    y: null,
                    a: null,
                    b: null,
                },
            ],
            [
                [' x="a"b y=\'a\'b'],
                {
                    x: '"a"b',
                    y: '\'a\'b',
                },
            ],
            [
                [' x="a"b" y=\'a\'b\''],
                {
                    x: '"a"b"',
                    y: '\'a\'b\'',
                },
            ],
            [
                [' в г=д ж=\'з\' и="й"'],
                {
                    // eslint-disable-next-line ascii/valid-name
                    в: null,
                    // eslint-disable-next-line ascii/valid-name
                    г: 'д',
                    // eslint-disable-next-line ascii/valid-name
                    ж: 'з',
                    // eslint-disable-next-line ascii/valid-name
                    и: 'й',
                },
            ],
        ];

        for (const [args, expected] of samples) {
            it(JSON.stringify(args), () => {
                expect(parseContainerParams(...args)).toEqual(expected);
            });
        }
    });
});

describe('trimPrecedingSpacing()', () => {
    it('Should trim any preceding spacing', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 7,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [],
                },
            ],
        };

        trimPrecedingSpacing(' \t\n1\n\t ', node);

        expect(node.children[0].openingInitialIndex).toEqual(3);
        expect(node.children[0].openingFollowingIndex).toEqual(3);
        expect(node.children[0].innerFirstIndex).toEqual(3);
        expect(node.children[0].closingInitialIndex).toEqual(7);
        expect(node.children[0].closingFollowingIndex).toEqual(7);
        expect(node.children[0].outerFirstIndex).toEqual(7);
    });

    it('Should remove empty nodes', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 0,
                    closingFollowingIndex: 0,
                    outerFirstIndex: 0,
                    children: [],
                },
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 7,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [],
                },
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 7,
                    openingFollowingIndex: 7,
                    innerFirstIndex: 7,
                    closingInitialIndex: 7,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [],
                },
            ],
        };

        trimPrecedingSpacing(' \t\n \n\t ', node);

        expect(node.children.length).toEqual(0);
    });
});

describe('trimFollowingSpacing()', () => {
    it('Should trim any preceding spacing', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 7,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [],
                },
            ],
        };

        trimFollowingSpacing(' \t\n1\n\t ', node);

        expect(node.children[0].openingInitialIndex).toEqual(0);
        expect(node.children[0].openingFollowingIndex).toEqual(0);
        expect(node.children[0].innerFirstIndex).toEqual(0);
        expect(node.children[0].closingInitialIndex).toEqual(4);
        expect(node.children[0].closingFollowingIndex).toEqual(4);
        expect(node.children[0].outerFirstIndex).toEqual(4);
    });

    it('Should remove empty nodes', () => {
        const node = {
            children: [
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 0,
                    closingFollowingIndex: 0,
                    outerFirstIndex: 0,
                    children: [],
                },
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 0,
                    openingFollowingIndex: 0,
                    innerFirstIndex: 0,
                    closingInitialIndex: 7,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [],
                },
                {
                    type: 'unknown',
                    inline: true,
                    attributes: {},
                    openingInitialIndex: 7,
                    openingFollowingIndex: 7,
                    innerFirstIndex: 7,
                    closingInitialIndex: 7,
                    closingFollowingIndex: 7,
                    outerFirstIndex: 7,
                    children: [],
                },
            ],
        };

        trimFollowingSpacing(' \t\n \n\t ', node);

        expect(node.children.length).toEqual(0);
    });
});
