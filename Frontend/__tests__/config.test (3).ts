jest.mock('../../services/History', () => {});

import {
    isVoiceMessagesEnabled,
    isFilesMessagesEnabled,
    isImageMessagesEnabled,
    isImportantMessagesEnabled,
    isHiddenInviteLinkNamespace,
} from '../config';
import { AppState } from '../../store';
import { BackendConfig } from '../../typings/config';
import { chatsMockFactory } from '../../store/__tests__/mock/chat';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import { configMockFactory } from '../../store/__tests__/mock/config';

const createPartialBackendConfig = (partial: Partial<BackendConfig>) => partial as BackendConfig;
const createPartialState = (partial: Partial<AppState>) => partial as AppState;

describe('ConfigSelector', () => {
    const user1Guid = 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxx1';
    const user2Guid = 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxx2';
    const robotGuid = 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxx3';
    const privateChatId = `${user1Guid}_${user2Guid}`;
    const robotChatId = `${user1Guid}_${robotGuid}`;
    const publicChat0 = '0/0/xxxxxxxxx';
    const publicChat1 = '0/1/xxxxxxxxx';
    const channel0 = '1/0/xxxxxxxxx';
    const channel1 = '1/1/xxxxxxxxx';

    const chatsMock = chatsMockFactory({ authId: user2Guid });
    const usersMock = usersMockFactory();
    const configMock = configMockFactory();

    const createState = (config: Partial<BackendConfig>): Partial<AppState> => {
        return {
            authId: user1Guid,
            chats: chatsMock.createState(
                {
                    chat_id: privateChatId,
                    private: true,
                    members: [user1Guid, user2Guid],
                    channel: false,
                },
                {
                    chat_id: robotChatId,
                    private: true,
                    members: [user1Guid, robotGuid],
                    channel: false,
                    is_robot: true,
                },
                {
                    chat_id: publicChat0,
                    channel: false,
                    namespace: 0,
                },
                {
                    chat_id: publicChat1,
                    channel: false,
                    namespace: 1,
                },
                {
                    chat_id: channel0,
                    channel: true,
                    namespace: 0,
                },
                {
                    chat_id: channel1,
                    channel: true,
                    namespace: 1,
                },
            ),
            config: configMock.createState(config),
            users: usersMock.createState(
                user1Guid,
                user2Guid,
                { guid: robotGuid, status: ['is_robot'], is_robot: true },
            ),
        };
    };

    describe('#isVoiceMessagesEnabled', () => {
        const state = createState({
            voice_messages: {
                max_duration_s: 600,
                restrictions: {
                    private: 'enabled',
                    enabled: {
                        channelsNS: [0],
                        groupsNS: [0],
                    },
                    default: 'disabled',
                },
            },
        });

        it('Should return true for private chat', () => {
            // @ts-ignore
            expect(isVoiceMessagesEnabled(state, privateChatId)).toEqual(true);
        });

        it('Should return false for private chat with robot', () => {
            // @ts-ignore
            expect(isVoiceMessagesEnabled(state, robotChatId)).toEqual(false);
        });

        it('Should return true for channel', () => {
            // @ts-ignore
            expect(isVoiceMessagesEnabled(state, channel0)).toEqual(true);
        });

        it('Should return false for channel', () => {
            // @ts-ignore
            expect(isVoiceMessagesEnabled(state, channel1)).toEqual(false);
        });

        it('Should return true for group chat', () => {
            // @ts-ignore
            expect(isVoiceMessagesEnabled(state, publicChat0)).toEqual(true);
        });

        it('Should return false for group chat', () => {
            // @ts-ignore
            expect(isVoiceMessagesEnabled(state, publicChat1)).toEqual(false);
        });
    });

    describe('#isFilesMessagesEnabled', () => {
        const state = createState({
            files_messages: {
                restrictions: {
                    private: 'enabled',
                    enabled: {
                        channelsNS: [0],
                        groupsNS: [0],
                    },
                    default: 'disabled',
                },
            },
        });

        it('Should return true for private chat', () => {
            // @ts-ignore
            expect(isFilesMessagesEnabled(state, privateChatId)).toEqual(true);
        });

        it('Should return false for private chat with robot', () => {
            // @ts-ignore
            expect(isFilesMessagesEnabled(state, robotChatId)).toEqual(false);
        });

        it('Should return true for channel', () => {
            // @ts-ignore
            expect(isFilesMessagesEnabled(state, channel0)).toEqual(true);
        });

        it('Should return false for channel', () => {
            // @ts-ignore
            expect(isFilesMessagesEnabled(state, channel1)).toEqual(false);
        });

        it('Should return true for group chat', () => {
            // @ts-ignore
            expect(isFilesMessagesEnabled(state, publicChat0)).toEqual(true);
        });

        it('Should return false for group chat', () => {
            // @ts-ignore
            expect(isFilesMessagesEnabled(state, publicChat1)).toEqual(false);
        });
    });

    describe('#isImageMessagesEnabled', () => {
        const state = createState({
            images_messages: {
                restrictions: {
                    private: 'enabled',
                    enabled: {
                        channelsNS: [0],
                        groupsNS: [0],
                    },
                    default: 'disabled',
                },
            },
        });

        it('Should return true for private chat', () => {
            // @ts-ignore
            expect(isImageMessagesEnabled(state, privateChatId)).toEqual(true);
        });

        it('Should return false for private chat with robot', () => {
            // @ts-ignore
            expect(isImageMessagesEnabled(state, robotChatId)).toEqual(false);
        });

        it('Should return true for channel', () => {
            // @ts-ignore
            expect(isImageMessagesEnabled(state, channel0)).toEqual(true);
        });

        it('Should return false for channel', () => {
            // @ts-ignore
            expect(isImageMessagesEnabled(state, channel1)).toEqual(false);
        });

        it('Should return true for group chat', () => {
            // @ts-ignore
            expect(isImageMessagesEnabled(state, publicChat0)).toEqual(true);
        });

        it('Should return false for group chat', () => {
            // @ts-ignore
            expect(isImageMessagesEnabled(state, publicChat1)).toEqual(false);
        });
    });

    describe('#isImportantMessagesEnabled', () => {
        const state = createState({
            important_messages: {
                restrictions: {
                    private: 'enabled',
                    channels: 'disabled',
                    disabled: {
                        groupsNS: [0],
                    },
                    groups: 'enabled',
                    robots: 'disabled',
                    default: 'enabled',
                },
            },
        });

        it('Should return true for private chat', () => {
            // @ts-ignore
            expect(isImportantMessagesEnabled(state, privateChatId)).toEqual(true);
        });

        it('Should return false for private chat with robot', () => {
            // @ts-ignore
            expect(isImportantMessagesEnabled(state, robotChatId)).toEqual(false);
        });

        it('Should return false for channel in 0 namespace', () => {
            // @ts-ignore
            expect(isImportantMessagesEnabled(state, channel0)).toEqual(false);
        });

        it('Should return false for channel in 1 namespace', () => {
            // @ts-ignore
            expect(isImportantMessagesEnabled(state, channel1)).toEqual(false);
        });

        it('Should return false for group chat in 0 namespace', () => {
            // @ts-ignore
            expect(isImportantMessagesEnabled(state, publicChat0)).toEqual(false);
        });

        it('Should return true for group chat in 1 namespace', () => {
            // @ts-ignore
            expect(isImportantMessagesEnabled(state, publicChat1)).toEqual(true);
        });
    });

    describe('#isHiddenInviteLinkNamespace', () => {
        const state = createPartialState({
            config: createPartialBackendConfig({
                hidden_invite_link_namespaces: [2],
            }),
        });

        it('Should return true for namespace 2', () => {
            expect(isHiddenInviteLinkNamespace(state, 2)).toEqual(true);
        });

        it('Should return false for namespace not 2', () => {
            expect(isHiddenInviteLinkNamespace(state, 1)).toEqual(false);
            expect(isHiddenInviteLinkNamespace(state, 0)).toEqual(false);
            expect(isHiddenInviteLinkNamespace(state, null)).toEqual(false);
        });

        it('Should return false from config without parameter, for all namespaces', () => {
            expect(
                isHiddenInviteLinkNamespace(createPartialState({ config: createPartialBackendConfig({}) }), 0),
            ).toEqual(false);
            expect(
                isHiddenInviteLinkNamespace(createPartialState({ config: createPartialBackendConfig({}) }), 1),
            ).toEqual(false);
        });
    });
});
