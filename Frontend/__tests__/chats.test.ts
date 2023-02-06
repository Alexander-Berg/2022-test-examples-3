/**
 * @todo: reducers/chats тянет selectors/messages, которые тянут MessengerApi и так далее
 * в итоге затягиваются все редьюсеры. Нужно разобраться с зависимостями @see https://st.yandex-team.ru/MSSNGRFRONT-3613
 */
/* eslint-disable import/first */
import { getRelationFromApi } from '../../lib/compat';

jest.mock('redux');

import {
    chatsReducer,
    updateChat,
    updateChatMembers,
    updateItems,
    updateChatInfo,
    updateChatRoles,
    updateMemberCount,
} from '../chats';
import { chatsMockFactory } from './mock/chat';
import { mutate } from './mock/utils';
import { generateGuids } from './mock/common';
import { removeChat, updateHistory } from '../sharedActions';
import { buildChat } from '../../helpers/chat';

describe('reducers chats', () => {
    const chatsMock = chatsMockFactory();

    describe('#updateItems', () => {
        it('Функция должна правильно накладывать дифф', () => {
            expect(updateItems(['0', '1', '2', '3'], {
                remove: ['2', '0'],
                add: ['4'],
            })).toStrictEqual(['1', '3', '4']);
        });
    });

    describe('#removeChat', () => {
        it('Чат должен быть удален', () => {
            const chats = chatsMock.createGroupChat()({ chat_id: 'chat1' });
            const state = chatsMock.createState(...chats);

            expect(chatsReducer(state, removeChat(chats[0].chat_id))[chats[0].chat_id])
                .toBeUndefined();
        });
    });

    describe('#updateMemberCount', () => {
        it('Кол-во участников чата должно измениться', () => {
            const chatId = 'chat1';
            const chats = chatsMock.createGroupChat()({ chat_id: chatId, member_count: 12 });
            const state = chatsMock.createState(...chats);

            expect(chatsReducer(state, updateMemberCount(chatId, 32))[chatId].member_count)
                .toBe(32);
        });

        it('Состояние должно остаться прежним, если чата chatId нет в сторе', () => {
            const chats = chatsMock.createGroupChat()({ chat_id: 'chat1', member_count: 12 });
            const state = chatsMock.createState(...chats);

            expect(chatsReducer(state, updateMemberCount('chat2', 12)))
                .toBe(state);
        });

        it('Состояние должно остаться прежним, если кол-во участников не изменилось', () => {
            const chatId = 'chat1';
            const chats = chatsMock.createGroupChat()({ chat_id: chatId, member_count: 12 });
            const state = chatsMock.createState(...chats);

            expect(chatsReducer(state, updateMemberCount(chatId, 12)))
                .toBe(state);
        });
    });

    describe('#updateChatMembers', () => {
        it('Участники должны быть обновлены если chat.version === diff.previos_version', () => {
            const members = generateGuids(7);
            const diff = { remove: members.slice(0, 3), add: generateGuids(5) };
            const membersResult = updateItems(members, diff);

            const [chat] = chatsMock.createGroupChat()({
                members,
                roles: {
                    admin: members.slice(0, 4),
                },
            });
            const state = chatsMock.createState(chat);

            expect(chatsReducer(state, updateChatMembers({
                previous_version: 1,
                version: 2,
                chat_id: chat.chat_id,
                members: diff,
                members_count: undefined,
            }))[chat.chat_id]).toMatchObject(mutate(chat, {
                version: 2,
                members: membersResult,
                member_count: membersResult.length,
                roles: {
                    admin: [members[3]],
                },
            }));
        });

        it('Участники не должны быть обновлены если chat.version !== diff.previos_version', () => {
            const [chat] = chatsMock.createGroupChat()();
            const state = chatsMock.createState(chat);

            expect(chatsReducer(state, updateChatMembers({
                previous_version: 2,
                version: 2,
                chat_id: chat.chat_id,
                members: { add: generateGuids(3), remove: [] },
                members_count: undefined,
            }))).toBe(state);
        });

        it('Участники не должны быть обновлены если chat.lazy === true', () => {
            const [chat] = chatsMock.createGroupChat()({ lazy: true });
            const state = chatsMock.createState(chat);

            expect(chatsReducer(state, updateChatMembers({
                previous_version: 1,
                version: 2,
                chat_id: chat.chat_id,
                members: { add: generateGuids(3), remove: [] },
                members_count: undefined,
            }))).toBe(state);
        });
    });

    describe('#updateChat', () => {
        it('Роли не должны обновиться если передан тот же самый объект', () => {
            const [chat1] = chatsMock.createGroupChat()({
                version: 1,
                relations: {
                    rights: 12,
                    version: 2,
                    role: 2,
                },
            });

            const state = chatsMock.createState(chat1);

            expect(chatsReducer(state, updateChat({ ...chat1 }))).toBe(state);
        });

        it('Значение флага lazy должно быть взято из нового объекта чата', () => {
            const [chat1] = chatsMock.createGroupChat()({ lazy: true });
            const state = chatsMock.createState(chat1);

            const nextState = chatsReducer(state, updateChat(mutate(chat1, { name: 'test', version: 2 })));

            expect(chat1).not.toBe(nextState[chat1.chat_id]);
            expect(nextState[chat1.chat_id]).toMatchObject(mutate(chat1, {
                lazy: true,
                name: 'test',
                version: 2,
            }));

            const nextState1 = chatsReducer(state, updateChat(mutate(chat1, { lazy: false, version: 2 })));

            expect(chat1).not.toBe(nextState1[chat1.chat_id]);
            expect(nextState1[chat1.chat_id]).toMatchObject(mutate(chat1, {
                lazy: false,
                version: 2,
            }));
        });

        it('Отстствующее значение member_count берётся из members.length', () => {
            const [chat1] = chatsMock.createGroupChat()({ member_count: 1, members: ['1'] });
            const state = chatsMock.createState(chat1);

            const nextState = chatsReducer(state, updateChat(buildChat({
                ...chat1,
                members: ['1', '2', '3'],
                member_count: undefined,
                version: 2,
            })));

            expect(chat1).not.toBe(nextState[chat1.chat_id]);
            expect(nextState[chat1.chat_id]).toMatchObject(mutate(chat1, {
                members: ['1', '2', '3'],
                member_count: 3,
                version: 2,
            }));
        });

        it('Для канала значение member_count должно быть взято из старого объекта чата, если отсутствует в новом', () => {
            const [chat1] = chatsMock.createGroupChat()({ chat_id: '1/0/', member_count: 1 });
            const state = chatsMock.createState(chat1);

            const nextState = chatsReducer(
                state,
                updateChat(mutate(chat1, { member_count: undefined, version: 2 })),
            );

            expect(chat1).not.toBe(nextState[chat1.chat_id]);
            expect(nextState[chat1.chat_id]).toMatchObject(mutate(chat1, {
                member_count: 1,
                version: 2,
            }));

            const nextState1 = chatsReducer(state, updateChat(mutate(chat1, { member_count: 2, version: 3 })));

            expect(chat1).not.toBe(nextState1[chat1.chat_id]);
            expect(nextState1[chat1.chat_id]).toMatchObject(mutate(chat1, {
                member_count: 2,
                version: 3,
            }));
        });

        it('Данные должны обновиться если текущая версия меньше чем новая', () => {
            const [chat1, chat2] = chatsMock.createGroupChat()({}, { lazy: true, members: [] });
            const state = chatsMock.createState(chat1, chat2);

            const nextState = chatsReducer(state, updateChat(mutate(chat1, {
                name: 'test',
                description: 'descr',
                version: 2,
            })));

            expect(chat1).not.toBe(nextState[chat1.chat_id]);
            expect(nextState[chat1.chat_id]).toMatchObject(mutate(chat1, {
                name: 'test',
                description: 'descr',
                version: 2,
            }));
        });

        it('Данные не должны обновиться если текущая версия >= новой', () => {
            const [chat1] = chatsMock.createGroupChat()({ version: 2 });
            const state = chatsMock.createState(chat1);

            expect(chatsReducer(state, updateChat(mutate(chat1, {
                description: 'descr',
                version: 1,
            })))).toStrictEqual(state);

            expect(chatsReducer(state, updateChat(mutate(chat1, {
                description: 'descr',
                version: 2,
            })))).toStrictEqual(state);
        });

        it('Данные должны обновиться если версии равны и у старого объекта стоит флаг lazy', () => {
            const [chat1] = chatsMock.createGroupChat()({ version: 2, lazy: true });
            const state = chatsMock.createState(chat1);

            expect(chatsReducer(state, updateChat(mutate(chat1, {
                description: 'descr',
                version: 1,
            })))).toStrictEqual(state);

            const nextState = chatsReducer(state, updateChat(mutate(chat1, {
                description: 'descr',
                version: 2,
                lazy: false,
            })));

            expect(nextState[chat1.chat_id]).not.toBe(chat1);
            expect(nextState[chat1.chat_id]).toMatchObject(mutate(chat1, {
                description: 'descr',
                version: 2,
                lazy: false,
            }));
        });
    });

    describe('#updateChatRoles', () => {
        it('Роли должны обновиться, но роль пользователя не должна обноовиться', () => {
            const admin = generateGuids(3);
            const [chat1] = chatsMock.createGroupChat()({
                version: 1,
                roles: {
                    admin,
                },
                relations: {
                    rights: 12,
                    version: 2,
                    role: 2,
                },
            });
            const state = chatsMock.createState(chat1);

            expect(chatsReducer(state, updateChatRoles({
                chat_id: chat1.chat_id,
                previous_version: 1,
                roles: { admin: { add: ['added'], remove: [admin[0]] } },
                version: 2,
            }, getRelationFromApi()))[chat1.chat_id]).toMatchObject(mutate(chat1, {
                roles: {
                    admin: [admin[1], admin[2], 'added'],
                },
                version: 2,
            }));
        });

        it('Роли не должны обновиться', () => {
            const admin = generateGuids(3);
            const [chat1] = chatsMock.createGroupChat()({
                version: 1,
                roles: {
                    admin,
                },
                relations: {
                    rights: 12,
                    version: 2,
                    role: 2,
                },
            });
            const state = chatsMock.createState(chat1);

            expect(chatsReducer(state, updateChatRoles({
                chat_id: chat1.chat_id,
                previous_version: 2,
                roles: { admin: { add: ['added'], remove: [admin[0]] } },
                version: 3,
            }, getRelationFromApi()))).toBe(state);
        });

        it('Роль пользователя должна обновиться', () => {
            const admin = generateGuids(3);
            const [chat1] = chatsMock.createGroupChat()({
                version: 1,
                roles: {
                    admin,
                },
                relations: {
                    rights: 12,
                    version: 2,
                    role: 1,
                },
            });
            const state = chatsMock.createState(chat1);

            expect(chatsReducer(state, updateChatRoles({
                chat_id: chat1.chat_id,
                previous_version: 2,
                roles: { admin: { add: ['added'], remove: [admin[0]] } },
                version: 3,
            }, getRelationFromApi({
                rights: ['write', 'read', 'change_rights'],
                version: 10,
                role: 'admin',
            })))[chat1.chat_id]).toMatchObject(mutate(chat1, {
                relations: {
                    rights: 8204,
                    role: 2,
                    version: 10,
                },
            }));
        });
    });

    describe('#updateChatInfo', () => {
        it('Иноформация о чатах должна обновиться', () => {
            const [chat1] = chatsMock.createGroupChat()({ version: 2 });
            const state = chatsMock.createState(chat1);

            expect(chatsReducer(state, updateChatInfo({
                chat_id: chat1.chat_id,
                description: 'test',
                name: 'name2',
                version: 3,
            }))[chat1.chat_id]).toMatchObject(mutate(chat1, {
                chat_id: chat1.chat_id,
                avatar_id: '',
                description: 'test',
                name: 'name2',
                version: 3,
            }));
        });

        it('Иноформация о чатах не должна обновиться если версия не больше текущей', () => {
            const [chat1] = chatsMock.createGroupChat()({ version: 2 });
            const state = chatsMock.createState(chat1);

            expect(chatsReducer(state, updateChatInfo({
                chat_id: chat1.chat_id,
                description: 'test',
                name: 'name2',
                version: 1,
            }))).toBe(state);
        });

        it('Инвайт ссылка должна обновиться', () => {
            const [chat1] = chatsMock.createGroupChat()({ version: 2, invite_hash: 'test_hash' });
            const state = chatsMock.createState(chat1);

            expect(chatsReducer(state, updateChatInfo({
                chat_id: chat1.chat_id,
                description: 'test',
                name: 'name2',
                version: 3,
            }))[chat1.chat_id]).toMatchObject(mutate(chat1, {
                chat_id: chat1.chat_id,
                avatar_id: '',
                description: 'test',
                name: 'name2',
                invite_hash: undefined,
                version: 3,
            }));

            expect(chatsReducer(state, updateChatInfo({
                chat_id: chat1.chat_id,
                description: 'test',
                name: 'name2',
                invite_hash: 'new_hash',
                version: 4,
            }))[chat1.chat_id]).toMatchObject(mutate(chat1, {
                chat_id: chat1.chat_id,
                avatar_id: '',
                description: 'test',
                invite_hash: 'new_hash',
                name: 'name2',
                version: 4,
            }));
        });

        it('Аватарка должна меняться корректно', () => {
            const [chat1] = chatsMock.createGroupChat()({ version: 2 });
            const state = chatsMock.createState(chat1);

            expect(chatsReducer(state, updateChatInfo({
                chat_id: chat1.chat_id,
                description: 'test',
                avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                name: 'name2',
                version: 3,
            }))[chat1.chat_id]).toMatchObject(mutate(chat1, {
                chat_id: chat1.chat_id,
                avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                description: 'test',
                name: 'name2',
                version: 3,
            }));
        });

        describe('Обновление organizations_ids', () => {
            it('organization_ids должен обновиться если его не было в чате', () => {
                const [chat1] = chatsMock.createGroupChat()({ version: 2 });
                const state = chatsMock.createState(chat1);

                expect(chatsReducer(state, updateChatInfo({
                    chat_id: chat1.chat_id,
                    description: 'test',
                    avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                    name: 'name2',
                    version: 3,
                    organization_ids: { '1': null, '2': null },
                }))[chat1.chat_id]).toMatchObject(mutate(chat1, {
                    chat_id: chat1.chat_id,
                    avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                    description: 'test',
                    name: 'name2',
                    version: 3,
                    organization_ids: { '1': null, '2': null },
                }));
            });

            it('organization_ids должен обновиться на новый', () => {
                const [chat1] = chatsMock.createGroupChat()({
                    version: 2,
                    organization_ids: { '1': null, '2': null },
                });
                const state = chatsMock.createState(chat1);

                expect(chatsReducer(state, updateChatInfo({
                    chat_id: chat1.chat_id,
                    description: 'test',
                    avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                    name: 'name2',
                    version: 3,
                    organization_ids: { '1': null, '4': null },
                }))[chat1.chat_id]).toMatchObject(mutate(chat1, {
                    chat_id: chat1.chat_id,
                    avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                    description: 'test',
                    name: 'name2',
                    version: 3,
                    organization_ids: { '1': null, '4': null },
                }));
            });

            it('Объект организаций не должен обнвоиться если пришел тот же самый список организаций', () => {
                const [chat1] = chatsMock.createGroupChat()({
                    version: 2,
                    organization_ids: { '1': null, '2': null },
                });
                const state = chatsMock.createState(chat1);

                const nextState = chatsReducer(state, updateChatInfo({
                    chat_id: chat1.chat_id,
                    description: 'test',
                    avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                    name: 'name2',
                    version: 3,
                    organization_ids: { '1': null, '2': null },
                }));

                expect(nextState[chat1.chat_id].organization_ids).toBe(chat1.organization_ids);

                expect(nextState[chat1.chat_id]).toMatchObject(mutate(chat1, {
                    chat_id: chat1.chat_id,
                    avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                    description: 'test',
                    name: 'name2',
                    version: 3,
                    organization_ids: { '1': null, '2': null },
                }));
            });

            it('Объект организаций быть undefined если поле не пришло вообще', () => {
                const [chat1] = chatsMock.createGroupChat()({
                    version: 2,
                    organization_ids: { '1': null, '2': null },
                });
                const state = chatsMock.createState(chat1);

                expect(chatsReducer(state, updateChatInfo({
                    chat_id: chat1.chat_id,
                    description: 'test',
                    avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                    name: 'name2',
                    version: 3,
                }))[chat1.chat_id]).toMatchObject(mutate(chat1, {
                    chat_id: chat1.chat_id,
                    avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                    description: 'test',
                    name: 'name2',
                    version: 3,
                    organization_ids: undefined,
                }));
            });
            it('Объект организаций быть undefined если пришел undefined', () => {
                const [chat1] = chatsMock.createGroupChat()({
                    version: 2,
                    organization_ids: { '1': null, '2': null },
                });
                const state = chatsMock.createState(chat1);

                expect(chatsReducer(state, updateChatInfo({
                    chat_id: chat1.chat_id,
                    description: 'test',
                    avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                    name: 'name2',
                    version: 3,
                    organization_ids: undefined,
                }))[chat1.chat_id]).toMatchObject(mutate(chat1, {
                    chat_id: chat1.chat_id,
                    avatar_id: 'chat_avatar/1/0/d57dc28a-c728-4906-a276-7ba628fa12dd/67d9bf78-1656-4b50-a5c2-840c3bf9fe4e',
                    description: 'test',
                    name: 'name2',
                    version: 3,
                    organization_ids: undefined,
                }));
            });
        });
    });

    describe('#receiveRawData', () => {
        it('Обновление без патча', () => {
            const [chat1, chat2, chat3] = chatsMock.createGroupChat()(3);

            expect(chatsReducer({},
                updateHistory({
                    authId: chatsMock.authId,
                    histories: [],
                    users: [],
                    chats: [chat1, chat2, chat3],
                }))).toMatchObject(chatsMock.createState(chat1, chat2, chat3));

            expect(chatsReducer(
                chatsMock.createState(chat1, chat2),
                updateHistory({
                    authId: chatsMock.authId,
                    histories: [],
                    users: [],
                    chats: [mutate(chat2, { version: 2 })],
                }),
            )).toMatchObject(chatsMock.createState(chat1, mutate(chat2, { version: 2 })));

            expect(chatsReducer(
                chatsMock.createState(chat1, chat2),
                updateHistory({
                    authId: chatsMock.authId,
                    histories: [],
                    users: [],
                    chats: [chat3],
                }),
            )).toMatchObject(chatsMock.createState(chat1, chat2, chat3));

            const state = chatsMock.createState(chat1, chat2);

            expect(chatsReducer(
                state,
                updateHistory({
                    authId: chatsMock.authId,
                    histories: [],
                    users: [],
                    chats: [chat2],
                }),
            )).toStrictEqual(state);
        });

        it('Обновление с патчем', () => {
            const [chat1, chat2, chat3] = chatsMock.createGroupChat()(3);
            const state = chatsMock.createState(chat1, chat2);

            expect(chatsReducer(
                state,
                updateHistory({
                    authId: chatsMock.authId,
                    histories: [],
                    users: [],
                    chats: [mutate(chat2, { version: 2 }), chat3],
                    patch: true,
                }),
            )).toMatchObject(chatsMock.createState(mutate(chat2, { version: 2 }), chat3));

            expect(chatsReducer(
                state,
                updateHistory({
                    authId: chatsMock.authId,
                    histories: [],
                    users: [],
                    chats: [],
                    patch: true,
                }),
            )).toMatchObject({});
        });

        it('Кол-во участников должно обновиться, вне зависимости от остальных полей', () => {
            const [chat] = chatsMock.createChannel()({ version: 1 });
            const state = chatsMock.createState(chat);

            expect(chatsReducer(
                state,
                updateHistory({
                    authId: chatsMock.authId,
                    histories: [],
                    users: [],
                    chats: [mutate(chat, { member_count: 795, lazy: true })],
                }),
            )).toMatchObject(chatsMock.createState(mutate(chat, { member_count: 795 })));
        });
    });
});
