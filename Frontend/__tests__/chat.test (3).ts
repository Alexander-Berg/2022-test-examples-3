import {
    isRobotChat,
    appendRecommendedChats,
    getPinnedChats,
    GetChatListOptions,
    getDialogChats,
    DialogChats,
    getRegularChats,
    getChatList,
    getChatMetadata,
} from '../chat';
import { ChatListItem, ChatListItemType } from '../../typings/chat';
import {
    EMPTY_DIALOG,
    EMPTY_DIALOG_COMPOSE,
    ALICE_DIALOG_ID,
} from './chat.dataset';
import { AppState } from '../../store';
import { chatsMockFactory } from '../../store/__tests__/mock/chat';
import { UserRights } from '../../constants/Chat';
import { BucketsState } from '../../store/buckets';
import { AssistantDialogsState } from '../../reducers/dialogs';
import { DialogsComposesState } from '../../reducers/dialogsComposeses';
import { messagesMockFactory } from '../../store/__tests__/mock/messages';
import { Role } from '../../constants/relation';
import { generateGuids } from '../../store/__tests__/mock/common';
import { composeMockFactory } from '../../store/__tests__/mock/compose';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import { stateMockFactory } from '../../store/__tests__/mock/state';

function getPinnedChatsBuckets(pinnedChatIds: string[]): BucketsState {
    return {
        maxVersion: 0,
        pinned_chats: {
            data: {
                pinned_chats: pinnedChatIds,
            },
            version: 1,
        },
    };
}

function createDialogsState(chatIds: string[]): AssistantDialogsState {
    return chatIds.reduce((acc, curr) => {
        acc[curr] = EMPTY_DIALOG;
        return acc;
    }, {});
}

function createDialogsComposesState(chatIds: string[]): DialogsComposesState {
    return chatIds.reduce((acc, curr) => {
        acc[curr] = EMPTY_DIALOG_COMPOSE;
        return acc;
    }, {});
}

function createChatItemsFromIds(
    chatIds: string[],
    type: ChatListItemType = ChatListItemType.CHAT,
    messages?: AppState['messages'],
    chatOptions?: GetChatListOptions,
): ChatListItem[] {
    return chatIds.map(((id) => {
        const time = (() => {
            if (chatOptions?.selectedChatId === id) {
                return Infinity;
            }

            return messages ? Math.floor(messages.chats[id].lastTimestamp / 1000) : 0;
        })();

        return {
            type: type,
            time,
            id,
            isSeparator: false,
        };
    })).sort((a, b) => b.time - a.time);
}

describe('ChatSelectors', () => {
    let state: AppState;
    let chatOptions: GetChatListOptions;

    const [authId, user2Guid, user3Guid, user4Guid, bussinessGuid] = generateGuids(5);
    const stateMock = stateMockFactory();
    const messagesMock = messagesMockFactory();
    const composeMock = composeMockFactory();
    const chatsMock = chatsMockFactory({ authId });
    const usersMock = usersMockFactory();
    const [GROUP_CHAT_ID] = chatsMock.generateGroupsIds(1, 0);
    const [BUSSINESS_CHAT_ID] = chatsMock.generateBussinessIds(1, 0);
    const [PRIVATE_ROBOT_CHAT_ID, PRIVATE_CHAT_ID] = chatsMock.generatePrivateIdWith(user2Guid, user3Guid);
    const [currentUser] = usersMock.createUnlimited()(authId);

    beforeEach(() => {
        const users = usersMock.createFrom()(
            { guid: authId },
            { guid: user2Guid, status: ['is_robot'], is_robot: true },
            { guid: user3Guid },
            { guid: user4Guid, status: ['is_robot'], is_robot: true },
            { guid: bussinessGuid },
        );

        const groupChat = chatsMock.createGroupChat()(GROUP_CHAT_ID, {
            chat_id: BUSSINESS_CHAT_ID,
            members: [authId, bussinessGuid],
            partner_guid: bussinessGuid,
        });

        const privateRoboChat = chatsMock.createPrivateChatWith(user2Guid, { is_robot: true });
        const privateChat = chatsMock.createPrivateChatWith(user3Guid);

        state = {
            users: usersMock.createState(...users),
            chats: chatsMock.createState(...groupChat, privateRoboChat, privateChat),
            authId,
            userInfo: {},
            conversations: {},
        } as any as AppState;
    });

    describe('#isRobotChat', () => {
        it('Should not be robot chat', () => {
            expect(isRobotChat(state, PRIVATE_CHAT_ID)).toBe(false);
        });

        it('Should be robot chat', () => {
            expect(isRobotChat(state, PRIVATE_ROBOT_CHAT_ID)).toBe(true);
        });

        it('Robot chat should be private', () => {
            expect(isRobotChat(state, GROUP_CHAT_ID)).toBe(false);
        });
    });

    describe('#appendRecommendedChats', () => {
        it('Should add only joinable recommended chats', () => {
            const ids = chatsMock.generateChannelsIds(3, 2);
            const recomendedIds = chatsMock.generateChannelsIds(3, 2);
            const chats = chatsMock.createChannel({
                relations: chatsMock.createRelation([UserRights.LEAVE], 0, Role.MEMBER),
            })(...ids, recomendedIds[0]);

            const recommendedChats = chatsMock.createChannel({
                relations: chatsMock.createRelation([UserRights.JOIN], 0, Role.GONE),
            })(...recomendedIds);

            const appState = stateMock.createState({
                recommendedChats: chatsMock.createRecommendedState(...recommendedChats),
                chats: chatsMock.createState(...chats),
            });

            const result = [];

            appendRecommendedChats(appState, result);

            expect(result).toHaveLength(3);
            expect(result[0]).toMatchObject({
                type: ChatListItemType.RECOMMENDED_CHAT_HEADER,
            });

            recomendedIds.slice(1).forEach((id, index) => expect(result[index + 1]).toMatchObject({
                type: ChatListItemType.RECOMMENDED_CHAT,
                id,
            }));
        });

        it('Should be empty if all chats were joined', () => {
            const ids = chatsMock.generateChannelsIds(3, 2);
            const chats = chatsMock.createChannel({
                relations: chatsMock.createRelation([UserRights.LEAVE]),
            })(...ids);
            const appState = stateMock.createState({
                recommendedChats: chatsMock.createRecommendedState(...chats),
                chats: chatsMock.createState(...chats),
                conversations: {},
            });

            const result = [];

            appendRecommendedChats(appState, result);

            expect(result).toHaveLength(0);
        });

        it('Should be empty if status error', () => {
            const ids = chatsMock.generateChannelsIds(3, 2);
            const chats = chatsMock.createChannel({
                relations: chatsMock.createRelation([UserRights.JOIN]),
            })(...ids);

            const appState = {
                recommendedChats: {
                    status: 'error',
                    chats: {},
                },
                chats: chatsMock.createState(...chats),
                conversations: {},
            } as AppState;

            const result = [];

            appendRecommendedChats(appState, result);

            expect(result).toHaveLength(0);
        });

        it('Should have header if status progress', () => {
            const appState = {
                recommendedChats: {
                    status: 'progress',
                    chats: {},
                },
                chats: chatsMock.createState(4),
            } as AppState;

            const result = [];

            appendRecommendedChats(appState, result);

            expect(result).toHaveLength(1);

            expect(result[0]).toMatchObject({
                type: ChatListItemType.RECOMMENDED_CHAT_HEADER,
            });
        });

        it('Should have not header if status idle', () => {
            const appState = {
                recommendedChats: {
                    status: 'idle',
                    chats: {},
                },
                chats: chatsMock.createState(4),
            } as AppState;

            const result = [];

            appendRecommendedChats(appState, result);

            expect(result).toHaveLength(0);
        });
    });

    describe('#getDialogChats', () => {
        let appState: AppState;
        let emptyDialogChats: DialogChats;

        const dialogIds = ['1', '2'];
        const ids = [...dialogIds, ALICE_DIALOG_ID];

        const dialogs = ids.reduce((acc, curr) => {
            acc[curr] = EMPTY_DIALOG;
            return acc;
        }, {});
        const dialogsComposes = ids.reduce((acc, curr) => {
            acc[curr] = EMPTY_DIALOG_COMPOSE;
            return acc;
        }, {});

        const createDialogItem = (id: string): ChatListItem => ({
            type: ChatListItemType.DIALOG,
            time: dialogs[id].access_time / 1000,
            id,
            isSeparator: false,
        });

        const dialogItems: { [key: string]: ChatListItem } = {};
        ids.forEach((id) => {
            dialogItems[id] = createDialogItem(id);
        });

        beforeEach(() => {
            appState = {
                authId,
                dialogs,
                dialogsComposes,
            } as Partial<AppState> as AppState;

            chatOptions = {
                selectedChatId: '',
            };

            emptyDialogChats = { pinnedChats: [], chats: [] };
        });

        it('Should return empty dialog chats if dialogs state is empty', () => {
            appState.dialogs = {};

            expect(getDialogChats(appState, chatOptions)).toEqual(emptyDialogChats);
        });

        it('Should return empty dialog chats if dialogs composes state is empty', () => {
            appState.dialogsComposes = {};

            expect(getDialogChats(appState, chatOptions)).toEqual(emptyDialogChats);
        });

        it('Should return dialog chats without chat excluded by predicate', () => {
            chatOptions.predicate = (type: ChatListItemType, id: string): boolean => {
                return type === ChatListItemType.DIALOG && id !== ALICE_DIALOG_ID;
            };

            const expected: DialogChats = {
                pinnedChats: [],
                chats: [dialogItems[dialogIds[0]], dialogItems[dialogIds[1]]],
            };

            expect(getDialogChats(appState, chatOptions)).toEqual(expected);
        });

        it('Should return alice in pinned chats if such option is specified', () => {
            chatOptions.isAlicePinned = true;

            const expected: DialogChats = {
                chats: [dialogItems[dialogIds[0]], dialogItems[dialogIds[1]]],
                pinnedChats: [dialogItems[ALICE_DIALOG_ID]],
            };

            expect(getDialogChats(appState, chatOptions)).toEqual(expected);
        });

        it('Should return all dialog chats', () => {
            const expected: DialogChats = {
                chats: [
                    dialogItems[dialogIds[0]],
                    dialogItems[dialogIds[1]],
                    dialogItems[ALICE_DIALOG_ID],
                ],
                pinnedChats: [],
            };

            expect(getDialogChats(appState, chatOptions)).toEqual(expected);
        });
    });

    describe('#getRegularChats', () => {
        let appState: AppState;

        const channelIds = chatsMock.generateChannelsIds(2, 1);
        const regularIds = chatsMock.generatePrivateId(3);
        const pinnedIds = chatsMock.generatePrivateId(3);

        let regularPrivateChats;
        let pinnedPrivateChats;
        let channels;

        beforeEach(() => {
            regularPrivateChats = chatsMock.createPrivateChat({
                relations: chatsMock.createRelation([UserRights.LEAVE], 1, Role.MEMBER),
            })(...regularIds);
            pinnedPrivateChats = chatsMock.createPrivateChat({
                relations: chatsMock.createRelation([UserRights.LEAVE], 1, Role.MEMBER),
            })(...pinnedIds);
            channels = chatsMock.createChannel({
                relations: chatsMock.createRelation([UserRights.JOIN]),
            })(...channelIds);

            appState = stateMock.createState({
                authId,
                buckets: {
                    ...getPinnedChatsBuckets(pinnedIds),
                },
                recommendedChats: {
                    chats: {},
                    status: 'idle',
                },
                compose: {},
                users: { [authId]: currentUser },
            });

            chatOptions = {
                selectedChatId: '',
            };
        });

        it('Should return empty array if chats state is empty', () => {
            appState.chats = {};

            expect(getRegularChats(appState, chatOptions)).toEqual([]);
        });

        it('Should return chat items without empty chats', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats);

            // Делаем первый чат пустым
            const messages = messagesMock.createTextMessage()(...regularIds.slice(1).map((chatId) => ({ chatId })));
            appState.messages = messagesMock.createState(messages);

            const expected: ChatListItem[] = createChatItemsFromIds(
                regularIds.slice(1),
                undefined,
                appState.messages,
            );

            expect(getChatList(appState, chatOptions)).toEqual(expected);
        });

        it('Should return chat items with selected empty chats with time = Infinity', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats);

            // Делаем первый чат пустым и выбираем его
            const messages = messagesMock.createTextMessage()(...regularIds.slice(1).map((chatId) => ({ chatId })));

            appState.messages = messagesMock.createState(messages);

            chatOptions.selectedChatId = regularIds[0];

            const expected: ChatListItem[] = createChatItemsFromIds(
                regularIds,
                undefined,
                appState.messages,
                chatOptions,
            );

            expect(getChatList(appState, chatOptions)).toEqual(expected);
        });

        it('Should return chat items with unjoined selected chat', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats.concat(channels));

            // Делаем первый чат пустым и выбираем его
            const messages = messagesMock.createTextMessage()(...regularIds.slice(1).map((chatId) => ({ chatId })));

            appState.messages = messagesMock.createState(messages);
            chatOptions.selectedChatId = regularIds[0];

            const expected: ChatListItem[] = createChatItemsFromIds(
                regularIds,
                undefined,
                appState.messages,
                chatOptions,
            );

            expect(getChatList(appState, chatOptions)).toEqual(expected);
        });

        it('Should return chat items without pinned chats', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats.concat(pinnedPrivateChats));
            const messages = messagesMock.createTextMessage()(
                ...regularIds.concat(pinnedIds).map((chatId) => ({ chatId })),
            );

            appState.messages = messagesMock.createState(messages);

            const expected: ChatListItem[] = createChatItemsFromIds(regularIds);

            expected[0].time = Math.floor(messages[0].timestamp / 1000);
            expected[1].time = Math.floor(messages[1].timestamp / 1000);
            expected[2].time = Math.floor(messages[2].timestamp / 1000);

            expect(getRegularChats(appState, chatOptions)).toEqual(expected);
        });

        it('Should return chat items without chat excluded by predicate', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats);

            const messages = messagesMock.createTextMessage()(
                ...regularIds.map((chatId) => ({ chatId })),
            );

            appState.messages = messagesMock.createState(messages);

            chatOptions.predicate = (type: ChatListItemType, id: string): boolean => {
                return type === ChatListItemType.CHAT && id !== regularIds[0];
            };

            const expected: ChatListItem[] = createChatItemsFromIds(regularIds.slice(1));
            expected[0].time = Math.floor(messages[1].timestamp / 1000);
            expected[1].time = Math.floor(messages[2].timestamp / 1000);

            expect(getRegularChats(appState, chatOptions)).toEqual(expected);
        });

        it('Should return regular chat items', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats);

            const messages = messagesMock.createTextMessage()(
                ...regularIds.map((chatId) => ({ chatId })),
            );

            appState.messages = messagesMock.createState(messages);

            const expected: ChatListItem[] = createChatItemsFromIds(regularIds);

            expected[0].time = Math.floor(messages[0].timestamp / 1000);
            expected[1].time = Math.floor(messages[1].timestamp / 1000);
            expected[2].time = Math.floor(messages[2].timestamp / 1000);

            expect(getRegularChats(appState, chatOptions)).toEqual(expected);
        });

        it('chats with drafts should be sorted by draftDate', () => {
            const draftDate = Date.now();

            appState.chats = chatsMock.createState(...regularPrivateChats);

            const messages = messagesMock.createTextMessage()(
                ...regularIds.map((chatId) => ({ chatId })),
            );

            appState.messages = messagesMock.createState(messages);
            appState.compose = composeMock.createState({
                chatId: regularIds[0],
                draftCommited: true,
                draftDate,
            });

            const expected = [
                {
                    type: ChatListItemType.CHAT,
                    id: regularIds[0],
                    time: draftDate,
                    isSeparator: false,
                },
                ...createChatItemsFromIds(
                    regularIds.slice(1),
                    undefined,
                    appState.messages,
                ),
            ];

            expect(getChatList(appState, chatOptions)).toEqual(expected);
        });
    });

    describe('#getPinnedChats', () => {
        let appState: AppState;

        const regularIds = chatsMock.generateChannelsIds(2, 1);
        const pinnedIds = chatsMock.generateChannelsIds(3, 1);
        const ids = [...regularIds, ...pinnedIds];

        const stateFactory = chatsMockFactory({ authId });

        beforeEach(() => {
            const relations = stateFactory.createRelation([
                UserRights.READ,
                UserRights.WRITE,
                UserRights.LEAVE,
            ]);
            const ownPrivateChats = stateFactory.createPrivateChat({
                relations,
            })(...ids);
            const channels = stateFactory.createChannel({
                relations,
            })(pinnedIds[2]);
            const currentChatsState = stateFactory.createState(...ownPrivateChats.concat(channels));

            appState = {
                authId,
                chats: currentChatsState,
                promoChats: {
                    chats: {},
                },
                users: {
                    [authId]: {
                        guid: authId,
                        display_name: 'test',
                        registration_status: 'u',
                    },
                },
                conversations: {},
            } as unknown as AppState;

            chatOptions = {
                selectedChatId: '1',
            };
        });

        it('Should return empty array if pinned chats bucket is empty', () => {
            appState.buckets = getPinnedChatsBuckets([]);

            expect(getPinnedChats(appState, chatOptions)).toEqual([]);
        });

        it('Should return chat list without chat excluded by predicate', () => {
            appState.buckets = getPinnedChatsBuckets(pinnedIds);
            chatOptions.predicate = (type: ChatListItemType, id: string): boolean => {
                return type === ChatListItemType.CHAT && id !== pinnedIds[0];
            };

            const expected: ChatListItem[] = createChatItemsFromIds([pinnedIds[1], pinnedIds[2]]);

            expect(getPinnedChats(appState, chatOptions)).toEqual(expected);
        });

        it('Should return all channels and own private chats', () => {
            appState.buckets = getPinnedChatsBuckets(pinnedIds);

            const expected: ChatListItem[] = createChatItemsFromIds(
                ids.filter((id) => pinnedIds.indexOf(id) !== -1),
            );

            expect(getPinnedChats(appState, chatOptions)).toEqual(expected);
        });

        it('Should not return unjoined channel', () => {
            appState.buckets = getPinnedChatsBuckets(pinnedIds);
            appState.chats[pinnedIds[2]].relations = stateFactory.createRelation([
                UserRights.READ,
                UserRights.JOIN,
            ], 0, 3);

            const expected: ChatListItem[] = createChatItemsFromIds(
                ids.filter((id) => pinnedIds.indexOf(id) !== -1 && id !== pinnedIds[2]),
            );

            expect(getPinnedChats(appState, chatOptions)).toEqual(expected);
        });
    });

    describe('#getChatList', () => {
        let appState: AppState;

        const separator = {
            type: ChatListItemType.SEPARATOR,
            id: '',
            time: 0,
            isSeparator: true,
        };
        const recommendedChatHeader = {
            type: ChatListItemType.RECOMMENDED_CHAT_HEADER,
            id: 'recommended_chats_header',
            time: 0,
            isSeparator: true,
        };

        const regularIds = chatsMock.generatePrivateId(3);
        const pinnedIds = chatsMock.generatePrivateId(2);
        const recommendedIds = chatsMock.generateChannelsIds(2, 0);

        const regularPrivateChats = chatsMock.createPrivateChat()(...regularIds);
        const pinnedChats = chatsMock.createPrivateChat()(...pinnedIds);
        //  Заджоиненные промо чаты
        const recommendedChats = chatsMock.createChannel({
            relations: chatsMock.createRelation([UserRights.JOIN], 0, Role.GONE),
        })(...recommendedIds);

        beforeEach(() => {
            appState = stateMock.createState({
                users: { [authId]: currentUser },
                authId,
                buckets: {
                    ...getPinnedChatsBuckets(pinnedIds),
                },
                recommendedChats: chatsMock.createRecommendedState(...recommendedChats),
                compose: {},
                dialogs: createDialogsState([ALICE_DIALOG_ID]),
                dialogsComposes: createDialogsComposesState([ALICE_DIALOG_ID]),
            });

            chatOptions = {
                selectedChatId: '',
            };
        });

        it('Should return empty list if chats state is empty', () => {
            appState.chats = {};
            expect(getChatList(appState, chatOptions)).toEqual([]);
        });

        it('Should sort regular chats by time', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats);
            const messages = messagesMock.createTextMessage()(
                ...regularIds.map((chatId) => ({ chatId })),
            );

            appState.messages = messagesMock.createState(messages);

            const expected: ChatListItem[] = createChatItemsFromIds(
                regularIds,
                undefined,
                appState.messages,
            );

            expected.forEach((item) => {
                item.time = Math.floor(appState.messages.chats[item.id].lastTimestamp / 1000);
            });

            expect(getChatList(appState, chatOptions)).toEqual(expected);
        });

        it('Should return chat items with first alice dialog if option specified alice is pinned', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats);

            const messages = messagesMock.createTextMessage()(
                ...regularIds.map((chatId) => ({ chatId })),
            );

            appState.messages = messagesMock.createState(messages);
            chatOptions.includeAssistant = true;
            chatOptions.isAlicePinned = true;

            const expected: ChatListItem[] = [
                {
                    type: ChatListItemType.DIALOG,
                    time: appState.dialogs[ALICE_DIALOG_ID].access_time / 1000,
                    id: ALICE_DIALOG_ID,
                    isSeparator: false,
                },
                ...createChatItemsFromIds(
                    regularIds,
                    undefined,
                    appState.messages,
                ),
            ];

            expect(getChatList(appState, chatOptions)).toEqual(expected);
        });

        it('Should return chat items with pinned chats at top', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats, ...pinnedChats);

            const messages = messagesMock.createTextMessage()(
                ...regularIds.concat(pinnedIds).map((chatId) => ({ chatId })),
            );

            appState.messages = messagesMock.createState(messages);

            const expected = [
                ...createChatItemsFromIds(
                    pinnedIds,
                ),
                ...createChatItemsFromIds(
                    regularIds,
                    undefined,
                    appState.messages,
                ),
            ];

            expect(getChatList(appState, chatOptions)).toEqual(expected);
        });

        it('Should return chat items with pinned chats without sorting', () => {
            appState.chats = chatsMock.createState(...pinnedChats);

            const messages = messagesMock.createTextMessage()(
                ...pinnedIds.map((chatId) => ({ chatId })),
            );

            appState.messages = messagesMock.createState(messages);

            const expected = createChatItemsFromIds(pinnedIds);
            expect(getChatList(appState, chatOptions)).toEqual(expected);
        });

        it('Should return separator between regular and pinned chats with option specified', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats, ...pinnedChats);

            const messages = messagesMock.createTextMessage()(
                ...regularIds.concat(pinnedIds).map((chatId) => ({ chatId })),
            );

            appState.messages = messagesMock.createState(messages);

            chatOptions.includeSeparator = true;

            const expected: ChatListItem[] = [
                ...createChatItemsFromIds(pinnedIds),
                separator,
                ...createChatItemsFromIds(
                    regularIds,
                    undefined,
                    appState.messages,
                ),
            ];

            expect(getChatList(appState, chatOptions)).toEqual(expected);
        });

        it('Should append recommended chats with option specified', () => {
            appState.chats = chatsMock.createState(...regularPrivateChats);

            const messages = messagesMock.createTextMessage()(
                ...regularIds.map((chatId) => ({ chatId })),
            );

            appState.messages = messagesMock.createState(messages);
            appState.recommendedChats = chatsMock.createRecommendedState(...recommendedChats);

            chatOptions.includeRecommendedChats = true;

            const expected: ChatListItem[] = [
                ...createChatItemsFromIds(
                    regularIds,
                    undefined,
                    appState.messages,
                ),
                recommendedChatHeader,
                ...createChatItemsFromIds(recommendedIds, ChatListItemType.RECOMMENDED_CHAT),
            ];

            expect(getChatList(appState, chatOptions)).toEqual(expected);
        });
    });

    describe('#getChatMetadata', () => {
        it('returns metadata from chat when metadata is defined in chat and user', () => {
            state.chats[PRIVATE_ROBOT_CHAT_ID].metadata = {
                chatbar: {
                    title: { text: 'from chat', i18n_key: 'test' },
                },
            };

            state.users[user2Guid].metadata = {
                chatbar: {
                    title: { text: 'from user', i18n_key: 'test' },
                },
            };

            expect(getChatMetadata(state, PRIVATE_ROBOT_CHAT_ID, 'chatbar'))
                .toEqual(state.chats[PRIVATE_ROBOT_CHAT_ID].metadata!.chatbar);
        });

        it('returns metadata from chat when metadata is defined in chat', () => {
            state.chats[PRIVATE_ROBOT_CHAT_ID].metadata = {
                chatbar: {
                    title: { text: 'from chat', i18n_key: 'test' },
                },
            };

            expect(getChatMetadata(state, PRIVATE_ROBOT_CHAT_ID, 'chatbar'))
                .toEqual(state.chats[PRIVATE_ROBOT_CHAT_ID].metadata!.chatbar);
        });

        it('returns metadata from user when metadata is defined in user', () => {
            const meta = {
                chatbar: {
                    title: { text: 'from user', i18n_key: 'test' },
                },
            };
            state.users[user2Guid].metadata = meta;

            expect(getChatMetadata(state, PRIVATE_ROBOT_CHAT_ID, 'chatbar'))
                .toEqual(meta.chatbar);
        });

        it('returns undefined from user when metadata is not defined in chat or user', () => {
            expect(getChatMetadata(state, PRIVATE_ROBOT_CHAT_ID, 'chatbar')).toBeUndefined();
        });
    });
});
