import { messagesMockFactory } from '../../store/__tests__/mock/messages';

/* eslint-disable import/first */
jest.mock('@mssngr/util');
jest.mock('react-loadable', () => {
    return {
        default: {
            Map: () => () => null,
        },
    };
});
jest.mock('../../services/Api');
jest.mock('../../store/compose', () => {
    return {
        composeReducer: () => ({}),
        update: jest.fn(),
    };
});

jest.mock('../../services/Rum');

// Приходится мокать этот модуль потому что в противном случае тест падает
// когда в одной из зависимостей вызывается конструктор метрики packages/util/lib/metrika.ts:157
// eslint-disable-next-line no-unused-vars
import '@mssngr/util';

import {
    replyMessage,
    cancelEditor,
    submitEditor,
    editMessage,
    disableUrlPreview,
    updateEditor,
} from '../Compose';
import * as ComposeActionsForSpy from '../Compose';
import { ComposeModes } from '../../constants/Chat';
import { MessageType } from '../../constants/message';
import { AppState } from '../../store';
import { normalizeMessageKey } from '../../helpers/messages';

describe('Compose', () => {
    const dispatch = jest.fn(() => {});
    const messagesMock = messagesMockFactory();

    function getStateFactory(state: any) {
        return jest.fn(() => state as AppState);
    }

    beforeEach(() => {
        jest.spyOn(ComposeActionsForSpy, 'updateEditor');
    });

    afterEach(() => {
        jest.spyOn(ComposeActionsForSpy, 'updateEditor').mockReset();
    });

    describe('editMessage() thunk', () => {
        const messageText = 'http://ya.ru';
        const [message] = messagesMock.createTextMessage()({
            type: MessageType.PLAIN,
            messageId: 'y',
            chatId: 'x',
            version: 1,
            data: {
                text: {
                    message_text: messageText,
                    tokens: { children: [] },
                    links: [],
                    emojies: [],
                },
            },
            timestamp: 999,
        });

        const state = { messages: messagesMock.createState([message]) };

        it('dispatches composeUpdate action without preview', () => {
            editMessage(message)(dispatch, getStateFactory(state));

            const expectedData = {
                important: undefined,
                chatId: message.chatId,
                mode: ComposeModes.EDIT,
                messagesKeys: [normalizeMessageKey(message)],
                mentions: [],
                text: messageText,
                preview: 'http://ya.ru',
                urlPreviewDisabled: undefined,
            };

            expect(updateEditor).toBeCalledWith(expectedData);
        });

        it('dispatch action with dispatching disableUrlPreview', () => {
            const _message = {
                ...message,
                urlPreviewDisabled: true,
            };

            const _state = { messages: messagesMock.createState([_message]) };

            editMessage(message)(dispatch, getStateFactory(_state));

            const expectedData = {
                important: undefined,
                chatId: message.chatId,
                mode: ComposeModes.EDIT,
                messagesKeys: [normalizeMessageKey(message)],
                mentions: [],
                text: messageText,
                preview: 'http://ya.ru',
                urlPreviewDisabled: true,
            };

            expect(updateEditor).toBeCalledWith(expectedData);
        });
    });

    describe('replyMessage() thunk', () => {
        it('dispatches composeUpdate action with correct data', () => {
            const [message] = messagesMock.createTextMessage()({
                version: 1,
            });

            const state = { messages: messagesMock.createState([message]) };

            replyMessage(message)(dispatch, getStateFactory(state));

            const expectedData = {
                mode: ComposeModes.FORWARD,
                messagesKeys: [message],
                chatId: message.chatId,
            };

            expect(updateEditor).toBeCalledWith(expectedData);
        });

        it('doesn\'t dispatch composeUpdate when message has no version', () => {
            const [message] = messagesMock.createTextMessage()({
                version: 0,
            });

            const state = { messages: messagesMock.createState([message]) };

            replyMessage(message)(dispatch, getStateFactory(state));

            expect(updateEditor).toBeCalledTimes(0);
        });
    });

    describe('disableUrlPreview() thunk', () => {
        it('dispatches composeUpdate action with existed url', async () => {
            const chatId = 'X';
            const previewUrl = 'http://ya.ru';
            const state = { compose: { [chatId]: { preview: previewUrl } } };

            disableUrlPreview(chatId)(dispatch, getStateFactory(state));

            const expectedData = {
                chatId,
                preview: previewUrl,
                urlPreviewDisabled: true,
            };

            expect(updateEditor).toBeCalledWith(expectedData);
        });
    });

    describe('cancelEditor() thunk', () => {
        const chatId = 'x';

        it('dispatches composeUpdate action with correct data (on reply / forward)', () => {
            const state = {
                compose: {
                    [chatId]: {
                        mode: ComposeModes.FORWARD,
                        mentions: 'mentions',
                        text: 'text',
                        preview: { disabled: true, url: 'http://ya.ru' },
                    },
                },
            };

            cancelEditor(chatId)(dispatch, getStateFactory(state));

            const expectedData = {
                chatId,
                mode: ComposeModes.NEW,
                messagesKeys: undefined,
                mentions: state.compose[chatId].mentions,
                text: state.compose[chatId].text,
                preview: { disabled: true, url: 'http://ya.ru' },
            };

            expect(updateEditor).toBeCalledWith(expectedData);
        });

        it('dispatches composeUpdate action with correct data (on edit)', () => {
            const state = {
                compose: {
                    [chatId]: {
                        mode: ComposeModes.EDIT,
                        mentions: 'mentions',
                        text: 'text',
                        preview: { disabled: true, url: 'http://ya.ru' },
                    },
                },
            };

            cancelEditor(chatId)(dispatch, getStateFactory(state));

            const expectedData = {
                chatId,
                mode: ComposeModes.NEW,
                messagesKeys: undefined,
                mentions: undefined,
                text: '',
                preview: undefined,
            };

            expect(updateEditor).toBeCalledWith(expectedData);
        });

        it('doesn\'t dispatch composeUpdate when there is no compose for chatId', () => {
            const state = { compose: {} };

            cancelEditor(chatId)(dispatch, getStateFactory(state));

            expect(updateEditor).toBeCalledTimes(0);
        });
    });

    describe('submitEditor() thunk', () => {
        describe('on reply / forward', () => {
            const chatId = 'x';

            it('dispatches clearEditor action with correct data', () => {
                const state = {
                    compose: {
                        [chatId]: {
                            mode: ComposeModes.FORWARD,
                        },
                    },
                    popup: {},
                };

                submitEditor(chatId)(dispatch, getStateFactory(state));

                expect(updateEditor).toBeCalledWith({
                    chatId,
                    text: '',
                    mode: ComposeModes.NEW,
                    messagesKeys: undefined,
                    mentions: undefined,
                    preview: undefined,
                    urlPreviewDisabled: false,
                    voice: undefined,
                    uploadMode: false,
                    clearToken: Object.create(null),
                });
            });
        });
    });
});
