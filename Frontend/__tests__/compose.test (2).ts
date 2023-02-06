import { mockDateNowOnEachTest, getDateNowResult } from '@yandex-int/messenger.utils/lib/mocks';
import { update, composeReducer, allUpdate } from '../compose';

describe('Compose reducer', () => {
    function generateMessagesKeys(timestamps: number[]): APIv3.MessageKey[] {
        return timestamps.map((timestamp) => ({
            chatId: 'chatId',
            timestamp,
        }));
    }

    mockDateNowOnEachTest();

    describe('#composeUpdate', () => {
        it('Should update state with  mode 1', () => {
            const initialState = {};
            const newState = composeReducer(initialState, update({
                chatId: '123',
                suggestions: undefined,
                messagesKeys: generateMessagesKeys([0, 1, 2]),
            }));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                '123': {
                    draftDate: getDateNowResult(0),
                    mode: 1,
                    suggestions: undefined,
                    messagesKeys: generateMessagesKeys([0, 1, 2]),
                },
            });
        });

        it('Время показа драфт должно остаться старым', (done) => {
            const initialState = {};
            let newState = composeReducer(initialState, update({
                chatId: '123',
                suggestions: undefined,
                text: 'test',
            }));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                '123': {
                    draftDate: getDateNowResult(0),
                    mode: 1,
                    suggestions: undefined,
                    text: 'test',
                },
            });

            setTimeout(() => {
                newState = composeReducer(newState, update({
                    chatId: '123',
                    suggestions: undefined,
                    text: 'test 2',
                }));

                expect(newState).not.toBe(initialState);

                expect(newState).toEqual({
                    '123': {
                        draftDate: getDateNowResult(1),
                        mode: 1,
                        suggestions: undefined,
                        text: 'test 2',
                    },
                });

                done();
            }, 2000);
        });

        it('Should update state with specified mode', () => {
            const initialState = {};
            const newState = composeReducer(initialState, update({
                chatId: '123',
                mode: 2,
            }));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                '123': {
                    mode: 2,
                },
            });
        });
    });

    describe('#composeAllUpdate', () => {
        it('Should update state all compose', () => {
            const initialState = {};
            const newState = composeReducer(initialState, allUpdate([
                {
                    mode: 1,
                    chatId: '123',
                    suggestions: undefined,
                    messagesKeys: generateMessagesKeys([0, 1, 2]),
                },
                {
                    mode: 2,
                    chatId: '321',
                    suggestions: undefined,
                    messagesKeys: generateMessagesKeys([2, 1, 0]),
                },
            ]));

            expect(newState).not.toBe(initialState);

            expect(newState).toEqual({
                '123': {
                    draftDate: getDateNowResult(0),
                    mode: 1,
                    suggestions: undefined,
                    messagesKeys: generateMessagesKeys([0, 1, 2]),
                },
                '321': {
                    draftDate: getDateNowResult(1),
                    mode: 2,
                    suggestions: undefined,
                    messagesKeys: generateMessagesKeys([2, 1, 0]),
                },
            });
        });
    });
});
