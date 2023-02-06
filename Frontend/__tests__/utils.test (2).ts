import {
    matchAll,
    calcModule,
    makeEditable,
    stringify,
    updateMentionsOffsets,
    deleteMentionByCursorPos,
    getMentionMatchInfo,
    isValidText,
    cutInvalidCharacters,
    findMention,
    handlePaste,
} from '../utils';

const mapper = (index = 1) => (match: RegExpExecArray, lastIndex: number) => ({
    text: match[index],
    startOffset: lastIndex - match[index].length,
    endOffset: lastIndex,
});

describe('MessageEditorLight utils', () => {
    describe('#matchAll', () => {
        it('simple testing', () => {
            expect(matchAll('awesome test case', /([a-z]+)/ig, mapper())).toEqual([
                {
                    text: 'awesome',
                    startOffset: 0,
                    endOffset: 7,
                },
                {
                    text: 'test',
                    startOffset: 8,
                    endOffset: 12,
                },
                {
                    text: 'case',
                    startOffset: 13,
                    endOffset: 17,
                },
            ]);
        });

        it('testing with large amount of groups', () => {
            expect(matchAll('очень крутые test cases', /(^| )([a-z]+)/ig, mapper(2))).toEqual([
                {
                    text: 'test',
                    startOffset: 13,
                    endOffset: 17,
                },
                {
                    text: 'cases',
                    startOffset: 18,
                    endOffset: 23,
                },
            ]);
        });
    });

    describe('#calcModule', () => {
        it('testing positive numbers', () => {
            expect(calcModule(21, 3)).toEqual(0);
            expect(calcModule(42, 5)).toEqual(2);
        });

        it('testing negative value', () => {
            expect(calcModule(-21, 3)).toEqual(-0);
            expect(calcModule(-42, 5)).toEqual(3);
        });

        it('testing negative and zero module', () => {
            expect(calcModule(-21, -3)).toEqual(-1);
            expect(calcModule(-42, 0)).toEqual(-1);
        });
    });

    describe('#makeEditable', () => {
        it('testing guids less than names', () => {
            const inputMentions = {
                '1abs1': {
                    guid: '1abs1',
                    display_name: 'Антон Дмитриев',
                    version: 1,
                },
                '2abs2': {
                    guid: '2abs2',
                    display_name: 'Даниил Тучин',
                    version: 1,
                },
            };

            const inputText = '@1abs1 идет гулять, @2abs2 идет в столовку';

            const { text, mentions } = makeEditable(inputText, inputMentions);

            expect(text).toEqual('@Антон Дмитриев идет гулять, @Даниил Тучин идет в столовку');
            expect(mentions).toEqual([
                {
                    endOffset: 15,
                    startOffset: 0,
                    user: {
                        display_name: 'Антон Дмитриев',
                        guid: '1abs1',
                        version: 1,
                    },
                },
                {
                    endOffset: 42,
                    startOffset: 29,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abs2',
                        version: 1,
                    },
                },
            ]);
        });

        it('testing guids larger than names', () => {
            const inputMentions = {
                '1abawdascxsfcaxzdsadxsdcxszcawdaz123': {
                    guid: '1abawdascxsfcaxzdsadxsdcxszcawdaz123',
                    display_name: 'Антон Дмитриев',
                    version: 1,
                },
                '2abawdascxsfcaxzdsadxsdcxszcawdaz123': {
                    guid: '2abawdascxsfcaxzdsadxsdcxszcawdaz123',
                    display_name: 'Даниил Тучин',
                    version: 1,
                },
            };

            const inputText =
                '@1abawdascxsfcaxzdsadxsdcxszcawdaz123 идет гулять, @2abawdascxsfcaxzdsadxsdcxszcawdaz123 идет в столовку';

            const { text, mentions } = makeEditable(inputText, inputMentions);

            expect(text).toEqual('@Антон Дмитриев идет гулять, @Даниил Тучин идет в столовку');
            expect(mentions).toEqual([
                {
                    endOffset: 15,
                    startOffset: 0,
                    user: {
                        display_name: 'Антон Дмитриев',
                        guid: '1abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
                {
                    endOffset: 42,
                    startOffset: 29,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
            ]);
        });
    });

    describe('#stringify', () => {
        it('testing guids less than names', () => {
            const inputMentions = [
                {
                    endOffset: 15,
                    startOffset: 0,
                    user: {
                        display_name: 'Антон Дмитриев',
                        guid: '1abs1',
                        version: 1,
                    },
                },
                {
                    endOffset: 42,
                    startOffset: 29,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abs2',
                        version: 1,
                    },
                },
            ];

            const inputText = '@Антон Дмитриев идет гулять, @Даниил Тучин идет в столовку';

            const { text, mentions } = stringify(inputText, inputMentions);

            expect(text).toEqual('@1abs1 идет гулять, @2abs2 идет в столовку');
            expect(mentions).toEqual(['2abs2', '1abs1']);
        });

        it('testing guids larger than names', () => {
            const inputMentions = [
                {
                    endOffset: 15,
                    startOffset: 0,
                    user: {
                        display_name: 'Антон Дмитриев',
                        guid: '1abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
                {
                    endOffset: 42,
                    startOffset: 29,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
            ];

            const inputText = '@Антон Дмитриев идет гулять, @Даниил Тучин идет в столовку';

            const { text, mentions } = stringify(inputText, inputMentions);

            expect(text).toEqual(
                '@1abawdascxsfcaxzdsadxsdcxszcawdaz123 идет гулять, @2abawdascxsfcaxzdsadxsdcxszcawdaz123 идет в столовку',
            );
            expect(mentions).toEqual(['2abawdascxsfcaxzdsadxsdcxszcawdaz123', '1abawdascxsfcaxzdsadxsdcxszcawdaz123']);
        });
    });

    describe('#updateMentionsOffsets', () => {
        it('testing positive diff', () => {
            const inputMentions = [
                {
                    endOffset: 15,
                    startOffset: 0,
                    user: {
                        display_name: 'Антон Дмитриев',
                        guid: '1abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
                {
                    endOffset: 42,
                    startOffset: 29,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
                {
                    endOffset: 57,
                    startOffset: 30,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
            ];

            const mentions = updateMentionsOffsets(inputMentions, 20, 29);
            expect(mentions).toEqual([
                {
                    endOffset: 15,
                    startOffset: 0,
                    user: {
                        display_name: 'Антон Дмитриев',
                        guid: '1abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
                {
                    endOffset: 62,
                    startOffset: 49,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
                {
                    endOffset: 77,
                    startOffset: 50,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
            ]);
        });
    });

    describe('#deleteMentionByRange', () => {
        it('testing positive numbers', () => {
            const inputMentions = [
                {
                    endOffset: 15,
                    startOffset: 0,
                    user: {
                        display_name: 'Антон Дмитриев',
                        guid: '1abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
                {
                    endOffset: 42,
                    startOffset: 29,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
                {
                    endOffset: 57,
                    startOffset: 30,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
            ];

            const mentions = deleteMentionByCursorPos(inputMentions, -30, 0);
            expect(mentions).toEqual([
                {
                    endOffset: 57,
                    startOffset: 30,
                    user: {
                        display_name: 'Даниил Тучин',
                        guid: '2abawdascxsfcaxzdsadxsdcxszcawdaz123',
                        version: 1,
                    },
                },
            ]);
        });
    });

    describe('#getMentionMatchInfo', () => {
        it('returns guid and prefix', () => {
            expect(getMentionMatchInfo(' @wow')).toEqual({
                guid: 'wow',
                prefix: ' ',
            });
        });
    });

    describe('#cutInvalidCharacters', () => {
        it('returns input text without dataType', () => {
            const inputText = '+7 (926)sss 9_6%#$@!|/?\.,4  61rrr5-+0';
            expect(cutInvalidCharacters(inputText)).toEqual(inputText);
        });

        it('returns phone valid symbols with dataType = "phone"', () => {
            expect(cutInvalidCharacters('+7 (926)sss 964  61rrr5-+0', 'phone')).toEqual('+7 (926) 964  615-+0');
        });

        it('returns date valid symbols with dataType = "date"', () => {
            expect(cutInvalidCharacters('+7 (926)sss 964  61rrr5-+0', 'date')).toEqual('79269646150');
        });
    });

    describe('#isValidText', () => {
        it('returns true without dataType', () => {
            const inputText = '+7 (926)sss 9_6%#$@!|/?\.,4  61rrr5-+0';
            expect(isValidText(inputText)).toEqual(true);
        });

        it('returns true for valid phone and dataType = "phone"', () => {
            expect(isValidText('+7 (926)964 61-50', 'phone')).toEqual(true);
        });

        it('returns false for invalid phone and dataType = "phone": length more then 13 symbols', () => {
            expect(isValidText('+7 (926)964 61-50 00 0', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": length less then 11', () => {
            expect(isValidText('7 (926)964 61-5', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains А-Я', () => {
            expect(isValidText('тел 8 (926)964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol @', () => {
            expect(isValidText('8@(926)964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol /', () => {
            expect(isValidText('8/(926)964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol |', () => {
            expect(isValidText('8|(926)964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol "', () => {
            expect(isValidText('8"(926)964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol _', () => {
            expect(isValidText('8"(926)964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol *', () => {
            expect(isValidText('8*(926)964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol $', () => {
            expect(isValidText('8$(926)964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol #', () => {
            expect(isValidText('8$(926)964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol [', () => {
            expect(isValidText('8 [926)964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol ]', () => {
            expect(isValidText('8 926]964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol .', () => {
            expect(isValidText('8 926.964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol ,', () => {
            expect(isValidText('8 926,964 61-50', 'phone')).toEqual(false);
        });

        it('returns false for invalid phone and dataType = "phone": contains symbol ;', () => {
            expect(isValidText('8 926 964 61-50;', 'phone')).toEqual(false);
        });

        it('returns true for valid date and dataType = "date"', () => {
            expect(isValidText('22.02.1993', 'date')).toEqual(true);
        });

        it('returns false for invalid date and dataType = "date": contains symbol /', () => {
            expect(isValidText('22/02/1993', 'date')).toEqual(false);
        });

        it('returns false for invalid date and dataType = "date": contains А-Я', () => {
            expect(isValidText('22 февраля 1993', 'date')).toEqual(false);
        });

        it('returns false for invalid date and dataType = "date": contains space instead .', () => {
            expect(isValidText('22 02 1993', 'date')).toEqual(false);
        });

        it('returns false for invalid date and dataType = "date": invalid range year', () => {
            expect(isValidText('22.02.1899', 'date')).toEqual(false);
        });

        it('returns false for invalid date and dataType = "date": invalid range month', () => {
            expect(isValidText('22.00.1999', 'date')).toEqual(false);
        });

        it('returns false for invalid date and dataType = "date": invalid range day', () => {
            expect(isValidText('32.01.1999', 'date')).toEqual(false);
        });
    });

    describe('#findMention', () => {
        it('finds emoji mention', () => {
            expect(findMention('hello :)', 8)).toEqual({
                endOffset: 8,
                startOffset: 7,
                text: ')',
                type: 'shortname',
            });
        });

        it('finds sticker mention', () => {
            expect(findMention('😀', 0)).toEqual({
                startOffset: 0,
                endOffset: 2,
                text: '😀',
                type: 'stiker',
            });
        });
    });

    describe('#handlePaste', () => {
        it('should ignore non files paste', () => {
            const callback = jest.fn();
            const preventDefault = jest.fn();

            handlePaste({
                clipboardData: {
                    items: {
                        length: 1,
                    },
                    files: {
                        length: 0,
                    },
                },
                preventDefault,
            } as unknown as React.ClipboardEvent, callback);

            expect(callback).not.toHaveBeenCalled();
            expect(preventDefault).not.toHaveBeenCalled();
        });

        it('should handle files', () => {
            const callback = jest.fn();
            const preventDefault = jest.fn();
            const file = new Blob([new ArrayBuffer(100)]);

            handlePaste({
                clipboardData: {
                    items: {
                        length: 1,
                    },
                    files: [
                        file,
                    ],
                    types: [
                        'Files',
                    ],
                },
                preventDefault,
            } as unknown as React.ClipboardEvent, callback);

            expect(preventDefault).toHaveBeenCalled();
            expect(callback).toBeCalledWith([file]);
        });

        it('should ignore word text image', () => {
            const callback = jest.fn();
            const preventDefault = jest.fn();
            const file = new Blob([new ArrayBuffer(100)]);

            handlePaste({
                clipboardData: {
                    items: {
                        length: 4,
                    },
                    types: [
                        'text/plain',
                        'text/html',
                        'text/rtf',
                        'Files',
                    ],
                    files: [
                        file,
                    ],
                },
                preventDefault,
            } as unknown as React.ClipboardEvent, callback);

            expect(preventDefault).not.toHaveBeenCalled();
            expect(callback).not.toHaveBeenCalled();
        });
    });
});
