import { mutate } from '../../store/__tests__/mock/utils';
import { chatsMockFactory } from '../../store/__tests__/mock/chat';
import { orgsMockFactory } from '../../store/__tests__/mock/organization';
import { stateMockFactory } from '../../store/__tests__/mock/state';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import { getChatOrganizationsIds, getRegStatusWithinOrg } from '../organization';

describe('selectors/organization', () => {
    const usersMock = usersMockFactory();
    const orgsMock = orgsMockFactory();

    beforeEach(() => {
        window.flags.enableWorkplace = '1';
    });

    afterEach(() => {
        delete window.flags.enableWorkplace;
    });

    const orgs = orgsMock.createFrom()({
        organization_id: 1,
        registration_status: 'U',
    });

    const [currentUser] = usersMock.createLimited()({
        organizations: orgsMock.createUserOrgs(...orgs),
    });

    const chatsMock = chatsMockFactory({
        authId: currentUser.guid,
    });

    const stateMock = stateMockFactory();

    describe('#getChatOrganizationsIds', () => {
        it('should be [0] if there is not organization_ids in chat', () => {
            const [chat] = chatsMock.createGroupChat()();

            const state = stateMock.createState({
                chats: chatsMock.createState(chat),
                users: usersMock.createState(currentUser),
                authId: currentUser.guid,
            });

            expect(getChatOrganizationsIds(state, chat.chat_id)).toStrictEqual([0]);
        });

        it('should be [1] if there is not organization_ids in chat', () => {
            const [chat] = chatsMock.createGroupChat()({
                organization_ids: orgsMock.createChatOrgIds(...orgs, {
                    organization_id: 2,
                }),
            });

            const state = stateMock.createState({
                chats: chatsMock.createState(chat),
                users: usersMock.createState(currentUser),
                authId: currentUser.guid,
            });

            expect(getChatOrganizationsIds(state, chat.chat_id)).toStrictEqual([1]);
        });

        it('should be [1, 2] if there is not organization_ids in chat', () => {
            const orgs2 = orgsMock.createFrom()(...orgs, {
                organization_id: 2,
                registration_status: 'Lu',
            });
            const [chat] = chatsMock.createGroupChat()({
                organization_ids: orgsMock.createChatOrgIds(...orgs, {
                    organization_id: 2,
                }),
            });

            const state = stateMock.createState({
                chats: chatsMock.createState(chat),
                users: usersMock.createState(mutate(currentUser, {
                    organizations: orgsMock.createUserOrgs(...orgs2),
                })),
                authId: currentUser.guid,
            });

            expect(getChatOrganizationsIds(state, chat.chat_id)).toStrictEqual([1, 2]);
        });
    });

    describe('#getRegStatusWithinOrg', () => {
        it('should be U if org reg status is U and user is Lu', () => {
            const [chat] = chatsMock.createGroupChat()({
                organization_ids: orgsMock.createChatOrgIds(...orgs),
            });

            const state = stateMock.createState({
                chats: chatsMock.createState(chat),
                users: usersMock.createState(currentUser),
                authId: currentUser.guid,
            });

            expect(getRegStatusWithinOrg(state, { chatId: chat.chat_id })).toBe('U');
        });

        it('should be L if org reg status is U and user is L', () => {
            const [chat] = chatsMock.createGroupChat()({
                organization_ids: orgsMock.createChatOrgIds(...orgs),
            });

            const state = stateMock.createState({
                chats: chatsMock.createState(chat),
                users: usersMock.createState(mutate(currentUser, {
                    registration_status: 'L',
                })),
                authId: currentUser.guid,
            });

            expect(getRegStatusWithinOrg(state, { chatId: chat.chat_id })).toBe('L');
        });

        it('should be U if org reg status is Lu and user is U', () => {
            const luOrgs = orgsMock.createFrom()({
                organization_id: 2,
                registration_status: 'Lu',
            });

            const [chat] = chatsMock.createGroupChat()({
                organization_ids: orgsMock.createChatOrgIds(...luOrgs),
            });

            const state = stateMock.createState({
                chats: chatsMock.createState(chat),
                users: usersMock.createState(mutate(currentUser, {
                    registration_status: 'U',
                    organizations: orgsMock.createUserOrgs(...luOrgs),
                })),
                authId: currentUser.guid,
            });

            expect(getRegStatusWithinOrg(state, { chatId: chat.chat_id })).toBe('U');
        });

        it('should be Lu if one of org has reg status Lu and user is U', () => {
            const luOrgs = orgsMock.createFrom()(...orgs, {
                organization_id: 2,
                registration_status: 'Lu',
            });

            const [chat] = chatsMock.createGroupChat()({
                organization_ids: orgsMock.createChatOrgIds(...luOrgs),
            });

            const state = stateMock.createState({
                chats: chatsMock.createState(chat),
                users: usersMock.createState(mutate(currentUser, {
                    registration_status: 'Lu',
                    organizations: orgsMock.createUserOrgs(...luOrgs),
                })),
                authId: currentUser.guid,
            });

            expect(getRegStatusWithinOrg(state, { chatId: chat.chat_id })).toBe('Lu');
        });
    });
});
