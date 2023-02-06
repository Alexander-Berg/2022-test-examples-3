import * as UsersActions from '../users';
import * as SharedActions from '../sharedActions';
import { usersMockFactory } from './mock/user';
import { messagesMockFactory } from './mock/messages';
import { historyMockFactory } from './mock/history';
import { mutate } from './mock/utils';

describe('Users reducer', () => {
    const usersMock = usersMockFactory();
    const messagesMock = messagesMockFactory();
    const historyMock = historyMockFactory();

    describe('#Add user', () => {
        it('should add user', () => {
            const [user] = usersMock.createUnlimited()();

            const newState = UsersActions.usersReducer(
                undefined,
                UsersActions.addUser(user),
            );

            expect(newState).toMatchObject({
                [user.guid]: user,
            });
        });

        it('should not update user if version was not changed', () => {
            const [user] = usersMock.createUnlimited()();

            const state = usersMock.createState(user);

            expect(UsersActions.usersReducer(
                state,
                UsersActions.addUser(mutate(user, { display_name: 'Test' })),
            )).toBe(state);
        });

        it('should not update user if version was changed', () => {
            const [user] = usersMock.createUnlimited()();

            const state = usersMock.createState(user);

            const userWithNewVersion = mutate(user, { version: 2 });

            const state2 = UsersActions.usersReducer(
                state,
                UsersActions.addUser(userWithNewVersion),
            );

            expect(state2).not.toBe(state);
            expect(state2).toMatchObject({
                [user.guid]: userWithNewVersion,
            });
        });

        it('should update user if contact_name added', () => {
            const [user] = usersMock.createUnlimited()();

            const state = usersMock.createState(user);

            const userWithContactName = mutate(user, { contact_name: 'Test', version: 1, });

            const state2 = UsersActions.usersReducer(
                state,
                SharedActions.setContacts([userWithContactName]),
            );

            expect(state2).not.toBe(state);
            expect(state2).toMatchObject({
                [user.guid]: {
                    ...userWithContactName,
                    contact_name: userWithContactName.contact_name,
                    contact_version: userWithContactName.version,
                    version: user.version,
                },
            });
        });

        it('should add new user beside existing', () => {
            const [user1, user2] = usersMock.createUnlimited()(2);

            const state = usersMock.createState(user1);

            const state2 = UsersActions.usersReducer(
                state,
                UsersActions.addUser(user2),
            );

            expect(state2).not.toBe(state);
            expect(state2).toMatchObject({
                [user1.guid]: user1,
                [user2.guid]: user2,
            });
        });
    });

    describe('#Update users on contacts update', () => {
        it('should update users', () => {
            const [user1, user2] = usersMock.createUnlimited()(2);

            const state = UsersActions.usersReducer(
                undefined,
                SharedActions.setRecommendedContacts([user1, user2], 0),
            );

            expect(state).toMatchObject({
                [user1.guid]: user1,
                [user2.guid]: user2,
            });
        });
    });

    describe('#Set users on Receive Raw Data', () => {
        it('should set contacts', () => {
            const [
                contact1,
                contact2,
            ] = usersMock.createContact()(2);

            const state = UsersActions.usersReducer(
                undefined,
                SharedActions.setContacts([contact1, contact2]),
            );

            expect(state).toMatchObject({
                [contact1.guid]: {
                    ...contact1,
                    contact_version: contact1.version,
                    version: 0,
                },
                [contact2.guid]: {
                    ...contact2,
                    contact_version: contact2.version,
                    version: 0,
                },
            });
        });

        it('should set users', () => {
            const [
                user1,
                user2,
                user3,
            ] = usersMock.createUnlimited()(3);

            const [
                from1,
            ] = usersMock.createFrom()();

            const state = UsersActions.usersReducer(
                undefined,
                UsersActions.update({
                    user: user1,
                    users: [user2, user3],
                    histories: historyMock.createChatHistory()({
                        messages: messagesMock.createTextMessage()({ from: from1 }),
                    }),
                }),
            );

            expect(state).toMatchObject({
                [user1.guid]: user1,
                [user2.guid]: user2,
                [from1.guid]: from1,
            });
        });
    });

    describe('#Set users on Replace Messages', () => {
        it('should set users', () => {
            const [user] = usersMock.createUnlimited()();

            const state = UsersActions.usersReducer(
                undefined,
                SharedActions.updateMessages(messagesMock.createTextMessage()({ from: user }), user.guid),
            );

            expect(state).toMatchObject({
                [user.guid]: user,
            });
        });
    });
});
