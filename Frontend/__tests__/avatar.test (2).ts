import { getMessageAvatarById } from '../avatar';
import type { AppState } from '../../store';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import { messagesMockFactory } from '../../store/__tests__/mock/messages';

declare var global: NodeJS.Global & { FLAGS: Record<string, boolean> };

global.FLAGS = {
};

function getState(partialState: Partial<AppState> = {}): AppState {
    return partialState as AppState;
}

describe('MessageSelectors', () => {
    const usersMock = usersMockFactory();
    const messagesMock = messagesMockFactory();

    describe('#getMessageAvatar', () => {
        it('Should return avatar_id', () => {
            const [user] = usersMock.createAnonimous({
                avatar_id: 'user_avatar/center/skullbulb/1478764859',
            })();

            const [message] = messagesMock.createTextMessage({
                from: user,
            })();

            const state = getState({
                users: usersMock.createState(user),
                messages: messagesMock.createState([message]),
            });

            const avatar = getMessageAvatarById(state, message);

            if (!avatar) {
                throw new Error('Avatar not found');
            }

            expect(avatar.id).toEqual(user.avatar_id);
            expect(avatar.textColor).toBeDefined();
            expect(avatar.backgroundColor).toBeDefined();
        });

        it('Should return custom_avatar_id if exists', () => {
            const customAvatarId = 'user_avatar/center/skullbulb/2313123213';

            const [user] = usersMock.createAnonimous({
                avatar_id: 'photo1',
                display_name: 'sd',
            })();

            const [message] = messagesMock.createTextMessage({
                from: {
                    ...user,
                    custom_avatar_id: customAvatarId,
                },
            })();

            const state = getState({
                users: usersMock.createState(user),
                messages: messagesMock.createState([message]),
            });

            const avatar = getMessageAvatarById(state, message);

            if (!avatar) {
                throw new Error('Avatar not found');
            }

            expect(avatar.id).toEqual(customAvatarId);
            expect(avatar.textColor).toBeDefined();
            expect(avatar.backgroundColor).toBeDefined();
        });

        it('Should return different custom_avatar_id for same author', () => {
            // eslint-disable-next-line camelcase
            const customAvatarId = 'some-avatar-id';

            const [user1, user2] = usersMock.createAnonimous()(
                {},
                {
                    avatar_id: 'photo2',
                },
            );

            const [message1, message2] = messagesMock.createTextMessage()(
                {
                    from: {
                        ...user1,
                        custom_display_name: 'Some name',
                    },
                },
                {
                    from: {
                        ...user2,
                        custom_avatar_id: customAvatarId,
                    },
                },
            );

            const state = getState({
                users: usersMock.createState(user2),
                messages: messagesMock.createState([message1, message2]),
            });

            const avatar = getMessageAvatarById(state, message1);
            const avatar2 = getMessageAvatarById(state, message2);

            if (!avatar || !avatar2) {
                throw new Error('Avatar not found');
            }

            expect(avatar.id).toBeUndefined();
            expect(avatar.textColor).toBeDefined();
            expect(avatar.backgroundColor).toBeDefined();

            expect(avatar2.id).toEqual(customAvatarId);
            expect(avatar.textColor).toBeDefined();
            expect(avatar.backgroundColor).toBeDefined();
        });

        it('Should return undefined if avatar does not exist', () => {
            const [message] = messagesMock.createTextMessage()();

            const state = getState({
                users: usersMock.createState(),
                messages: messagesMock.createState([message]),
            });

            const avatar = getMessageAvatarById(state, message);

            if (!avatar) {
                throw new Error('Avatar not found');
            }

            expect(avatar.id).not.toBeDefined();
            expect(avatar.textColor).toBeDefined();
            expect(avatar.backgroundColor).toBeDefined();
        });
    });
});
