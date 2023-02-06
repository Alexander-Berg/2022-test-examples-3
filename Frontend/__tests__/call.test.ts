/* eslint-disable import/first */
jest.mock('../../services/History', () => {});

import { AppState } from '../../store';
import { callMockFactory } from '../../store/__tests__/mock/call';
import { chatsMockFactory } from '../../store/__tests__/mock/chat';
import { stateMockFactory } from '../../store/__tests__/mock/state';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import { canCallToChat, canMakeVideoCall } from '../call';

window.flags = {
    calls: '1',
};

describe('CallSelectors', () => {
    let state: AppState;
    let chatWithUser: APIv3.Chat;
    let chatWithRobot: APIv3.Chat;
    let groupChat: APIv3.Chat;

    const stateMock = stateMockFactory();
    const usersMock = usersMockFactory();
    const callMock = callMockFactory();

    const [user1, user2] = usersMock.createUnlimited()(2);
    const [robot] = usersMock.createUnlimited()({ status: ['is_robot'], is_robot: true });

    const chatsMock = chatsMockFactory({ authId: user1.guid });

    beforeEach(() => {
        chatWithUser = chatsMock.createPrivateChatWith(user2.guid, {
            metadata: {
                calls: {
                    video: 'default',
                    microphone: 'default',
                    may_call: true,
                    skip_feedback: true,
                },
            },
        });

        chatWithRobot = chatsMock.createPrivateChatWith(robot.guid, {
            metadata: {
                calls: {
                    video: 'default',
                    microphone: 'default',
                    may_call: true,
                    skip_feedback: true,
                },
            },
        });

        groupChat = chatsMock.createGroupChat({
            metadata: {
                calls: {
                    video: 'default',
                    microphone: 'default',
                    may_call: true,
                    skip_feedback: true,
                },
            },
        })(1)[0];

        state = stateMock.createState({
            userInfo: {},
            call: callMock.createState({ isAvailable: true }),
            authId: user1.guid,
            users: usersMock.createState(user1, user2, robot),
            chats: chatsMock.createState(chatWithRobot, chatWithUser, groupChat),
        });
    });

    describe('canCallToChat', () => {
        it('Returns true if chat with user & no metadata', () => {
            delete state.chats[chatWithUser.chat_id].metadata;

            expect(canCallToChat(state, chatWithUser.chat_id)).toBeTruthy();
        });

        it('Returns false if chat with bot & no metadata', () => {
            delete state.chats[chatWithRobot.chat_id].metadata;

            expect(canCallToChat(state, chatWithRobot.chat_id)).toBeFalsy();
        });

        it('Returns true if robot with metadata', () => {
            expect(canCallToChat(state, chatWithRobot.chat_id)).toBeTruthy();
        });

        it('Returns false if user chat & not may call to chat', () => {
            state.chats[chatWithUser.chat_id].metadata!.calls!.may_call = false;

            expect(canCallToChat(state, chatWithUser.chat_id)).toBeFalsy();
        });

        it('Returns false if group chat with meta', () => {
            expect(canCallToChat(state, groupChat.chat_id)).toBeTruthy();
        });

        it('Returns false if group chat without meta', () => {
            delete state.chats[groupChat.chat_id].metadata;

            expect(canCallToChat(state, groupChat.chat_id)).toBeFalsy();
        });

        it('Returns true if user can call to chat', () => {
            expect(canCallToChat(state, chatWithUser.chat_id)).toBeTruthy();
        });
    });

    describe('canMakeVideoCall', () => {
        it('Returns true if chat with user & no metadata', () => {
            delete state.chats[chatWithUser.chat_id].metadata;

            expect(canMakeVideoCall(state, chatWithUser.chat_id)).toBeTruthy();
        });

        it('Returns false if chat with bot & no metadata', () => {
            delete state.chats[chatWithRobot.chat_id].metadata;

            expect(canMakeVideoCall(state, chatWithRobot.chat_id)).toBeFalsy();
        });

        it('Returns false if user chat & video metadata is force_off', () => {
            state.chats[chatWithUser.chat_id].metadata!.calls!.video = 'force_off';

            expect(canMakeVideoCall(state, chatWithUser.chat_id)).toBeFalsy();
        });

        it('Returns false if group chat with meta', () => {
            expect(canMakeVideoCall(state, groupChat.chat_id)).toBeTruthy();
        });

        it('Returns false if group chat without meta', () => {
            delete state.chats[groupChat.chat_id].metadata;

            expect(canMakeVideoCall(state, groupChat.chat_id)).toBeFalsy();
        });

        it('Returns false if video metadata is force_off', () => {
            state.chats[chatWithUser.chat_id].metadata!.calls!.video = 'force_off';

            expect(canMakeVideoCall(state, chatWithUser.chat_id)).toBeFalsy();
        });

        it('Returns false if video metadata is not set', () => {
            state.chats[groupChat.chat_id].metadata = {
                calls: {
                    video: undefined,
                    microphone: 'default',
                    may_call: true,
                    skip_feedback: true,
                },
            };

            expect(canMakeVideoCall(state, groupChat.chat_id)).toBeFalsy();
        });

        it('Returns true if user can call to chat', () => {
            expect(canMakeVideoCall(state, chatWithUser.chat_id)).toBeTruthy();
        });
    });
});
