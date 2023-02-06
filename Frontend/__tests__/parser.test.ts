import { LINK_REGEXP_STR, assertDefinedAndReturn } from '@mssngr/util';
import {
    buildTree,
    parseTokens,
    buildUnionRegexp,
    isLiteral,
    getTagFromMatch,
    findFirstClosingToken,
    TAGS,
} from '../parser';
import { Tag, TagType, Token } from '../types';
import { BOLD_REGEXP_STR, CODE_REGEXP_STR, CURSIVE_REGEXP_STR } from '../regExps';

describe('Parser', () => {
    const regexp = buildUnionRegexp(TAGS);

    const textTag: Tag = {
        type: TagType.TEXT,
        groups: 0,
    };

    const bold: Tag = {
        regexp: BOLD_REGEXP_STR,
        type: TagType.BOLD,
        groups: 1,
        closable: true,
    };

    const cursive: Tag = {
        regexp: CURSIVE_REGEXP_STR,
        type: TagType.CURSIVE,
        groups: 1,
        closable: true,
    };

    const link: Tag = {
        regexp: LINK_REGEXP_STR,
        type: TagType.LINK,
        groups: 3,
        getTokenStart: expect.any(Function),
        modify: expect.any(Function),
    };

    const code: Tag = {
        regexp: CODE_REGEXP_STR,
        type: TagType.CODE,
        groups: 2,
    };

    describe('#isLiteral', () => {
        it('returns true when char=b', () => {
            expect(isLiteral('d')).toBeTruthy();
        });

        it('returns true when char=B', () => {
            expect(isLiteral('D')).toBeTruthy();
        });

        it('returns true when char=б', () => {
            expect(isLiteral('д')).toBeTruthy();
        });

        it('returns true when char=Б', () => {
            expect(isLiteral('Д')).toBeTruthy();
        });

        it('returns true when char=ё', () => {
            expect(isLiteral('ё')).toBeTruthy();
        });

        it('returns true when char=Ё', () => {
            expect(isLiteral('Ё')).toBeTruthy();
        });

        it('returns true when char=0', () => {
            expect(isLiteral('0')).toBeTruthy();
        });

        it('returns false when char=/', () => {
            expect(isLiteral('/')).toBeFalsy();
        });

        it('returns false when char is empty', () => {
            expect(isLiteral()).toBeFalsy();
            expect(isLiteral('')).toBeFalsy();
        });
    });

    describe('#buildUnionRegexp', () => {
        it('Should build valid union reg exp', () => {
            const tags = [
                {
                    regexp: '`',
                },
                {
                    regexp: '\\*\\*',
                },
                {
                    regexp: 'xx',
                },
            ] as Tag[] as any;
            expect(buildUnionRegexp(tags)).toEqual(/(`)|(\*\*)|(xx)/gi);
        });
    });

    describe('#getTagFromMatch', () => {
        it('Should return valid tag from regexp match', () => {
            const match = regexp.exec('1235 **123 ya.ru');

            expect(getTagFromMatch(assertDefinedAndReturn(match), TAGS)).toEqual(bold);
        });

        it('Should return undefined if no tags were specified', () => {
            const match = regexp.exec('1235 **123 ya.ru');

            expect(getTagFromMatch(assertDefinedAndReturn(match), [])).toBeUndefined();
        });
    });

    describe('#parseTokens', () => {
        it('Should properly parse all tokens from text', () => {
            let text = '**bold** yandex.ru ```code```';
            let result: Token[] = [
                {
                    start: 0,
                    end: 2,
                    tag: bold,
                    match: ['**'],
                },
                {
                    start: 2,
                    end: 6,
                    tag: textTag,
                    match: ['bold'],
                },
                {
                    start: 6,
                    end: 8,
                    tag: bold,
                    match: ['**'],
                },
                {
                    start: 8,
                    end: 9,
                    match: [' '],
                    tag: textTag,
                },
                {
                    start: 9,
                    end: 18,
                    match: [' yandex.ru', ' ', 'yandex.ru'],
                    tag: link,
                },
                {
                    start: 18,
                    end: 19,
                    match: [' '],
                    tag: textTag,
                },
                {
                    start: 19,
                    end: 29,
                    match: ['```code```', 'code'],
                    tag: code,
                },
            ];

            expect(parseTokens(text)).toEqual(result);

            text = '**yandex.ru';
            result = [
                {
                    start: 0,
                    end: 2,
                    tag: bold,
                    match: ['**'],
                },
                {
                    start: 2,
                    end: 11,
                    match: ['yandex.ru', '', 'yandex.ru'],
                    tag: link,
                },
            ];

            expect(parseTokens(text)).toStrictEqual(result);
        });

        it('Should return full text if no tokens were found', () => {
            const text = 'hello';

            expect(parseTokens(text)).toStrictEqual([
                {
                    start: 0,
                    end: 4,
                    match: ['hello'],
                    tag: textTag,
                },
            ]);
        });

        it('Should return text after and before token', () => {
            const text = '123 ** 123';
            const result = [
                {
                    start: 0,
                    end: 4,
                    match: ['123 '],
                    tag: textTag,
                },
                {
                    start: 4,
                    end: 6,
                    match: ['**'],
                    tag: bold,
                },
                {
                    start: 6,
                    end: 9,
                    match: [' 123'],
                    tag: textTag,
                },
            ];

            expect(parseTokens(text)).toStrictEqual(result);
        });
    });

    describe('#findFirstClosingToken', () => {
        it('Should find closing token if it exists', () => {
            const closingToken = {
                start: 8,
                end: 10,
                match: ['**'],
                children: [],
                tag: bold,
            };

            const tokens = [
                {
                    match: ['12'],
                    start: 0,
                    end: 2,
                    children: [],
                    tag: textTag,
                },
                {
                    match: ['hello '],
                    start: 2,
                    end: 8,
                    children: [],
                    tag: textTag,
                },
                closingToken,
            ] as Token[] as any[];

            expect(findFirstClosingToken(bold, tokens)).toEqual([2, closingToken]);
        });

        it('Should not return closing token if there\'s literal after it', () => {
            const closingToken = {
                start: 7,
                end: 9,
                match: ['**'],
                children: [],
                tag: bold,
            };

            const tokens = [
                {
                    match: ['12'],
                    start: 0,
                    end: 2,
                    children: [],
                    tag: textTag,
                },
                {
                    match: ['hello'],
                    start: 2,
                    end: 7,
                    children: [],
                    tag: textTag,
                },
                closingToken,
                {
                    match: ['1'],
                    start: 9,
                    end: 10,
                    tag: textTag,
                },
            ] as Token[] as any[];

            expect(findFirstClosingToken(bold, tokens)).toEqual([]);
        });
    });

    describe('#buildTree', () => {
        it('Should build valid tree from tokens', () => {
            const text = '**__bold__** ```code```';
            const firstInnerText = {
                match: ['bold'],
                children: [],
                tag: textTag,
            };

            const cursiveToken = {
                match: ['__'],
                children: [firstInnerText],
                tag: cursive,
            };

            const boldToken = {
                match: ['**'],
                children: [cursiveToken],
                tag: bold,
            };

            const spaceToken = {
                match: [' '],
                children: [],
                tag: textTag,
            };

            const codeToken = {
                children: [],
                match: [
                    '```code```',
                    'code',
                ],
                tag: code,
            };

            const root = {
                children: [
                    boldToken,
                    spaceToken,
                    codeToken,
                ],
            };

            expect(buildTree(parseTokens(text))).toEqual(root);
        });
    });
});
