import { Role } from '../../constants/relation';
import { chatsMockFactory } from '../../store/__tests__/mock/chat';
import { stateMockFactory } from '../../store/__tests__/mock/state';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import { isAdminInChat } from '../relations';

describe('selectors/relations', () => {
    describe('#isAdminInChat', () => {
        const stateFactory = stateMockFactory();
        const usersFactory = usersMockFactory();
        const [author, ...users] = usersFactory.createUnlimited()(5);
        const chatsFactory = chatsMockFactory({
            authId: author.guid,
        });

        it('Пользователь приложения является админом если у него роль ADMIN вне зависимости от прав', () => {
            const [chat] = chatsFactory.createGroupChat()({
                relations: chatsFactory.createRelation([], 12, Role.ADMIN),
            });

            const state = stateFactory.createState({
                authId: author.guid,
                chats: chatsFactory.createState(chat),
            });

            expect(isAdminInChat(state, chat.chat_id, author.guid)).toBeTruthy();
        });

        it('Пользователь приложения является админом если у него неизвестная роль -1 и он есть в списке админов', () => {
            const [chat1, chat2] = chatsFactory.createGroupChat()(
                {
                    relations: chatsFactory.createRelation([], 12, -1),
                    roles: {
                        admin: [author.guid],
                    },
                },
                {
                    relations: chatsFactory.createRelation([], 12, -1),
                },
            );

            const state = stateFactory.createState({
                authId: author.guid,
                chats: chatsFactory.createState(chat1, chat2),
            });

            expect(isAdminInChat(state, chat1.chat_id, author.guid)).toBeTruthy();
            expect(isAdminInChat(state, chat2.chat_id, author.guid)).toBeFalsy();
        });

        it('Участник чата является админом если находится в списке админов', () => {
            const [chat1, chat2] = chatsFactory.createGroupChat()(
                {
                    relations: chatsFactory.createRelation([], 12, Role.ADMIN),
                    roles: {
                        admin: [users[0].guid],
                    },
                },
                {
                    roles: {
                        admin: [users[0].guid],
                    },
                },
            );

            const state = stateFactory.createState({
                authId: author.guid,
                chats: chatsFactory.createState(chat1, chat2),
            });

            expect(isAdminInChat(state, chat1.chat_id, users[0].guid)).toBeTruthy();
            expect(isAdminInChat(state, chat1.chat_id, users[0].guid)).toBeTruthy();
            expect(isAdminInChat(state, chat2.chat_id, users[1].guid)).toBeFalsy();
        });
    });
});
