import { getRegStatus, isAnonymousUser, isLimitedUser } from '../user';
import { stateMockFactory } from '../../store/__tests__/mock/state';
import { usersMockFactory } from '../../store/__tests__/mock/user';

describe('User Selectors', () => {
    const stateMock = stateMockFactory();
    const usersMock = usersMockFactory();

    describe('#getRegStatus', () => {
        it('Should return reg status', () => {
            const [user] = usersMock.createAnonimous()();

            const state = stateMock.createState({
                authId: user.guid,
                users: usersMock.createState(user),
            });

            expect(getRegStatus(state)).toEqual('L');
        });

        it('Should return reg status wrong user', () => {
            const [user] = usersMock.createAnonimous()();

            const state = stateMock.createState({
                authId: user.guid,
                users: usersMock.createState(),
            });

            expect(getRegStatus(state)).toEqual(undefined);
        });
    });

    describe('#isAnonimusUser', () => {
        it('Should return true if regStatus is L', () => {
            const [user] = usersMock.createAnonimous()();

            const state = stateMock.createState({
                authId: user.guid,
                users: usersMock.createState(user),
            });

            expect(isAnonymousUser(state)).toEqual(true);
        });

        it('Should return false if regStatus is Lu', () => {
            const [user] = usersMock.createLimited()();

            const state = stateMock.createState({
                authId: user.guid,
                users: usersMock.createState(user),
            });

            expect(isAnonymousUser(state)).toEqual(false);
        });

        it('Should return false if regStatus is U', () => {
            const [user] = usersMock.createUnlimited()();

            const state = stateMock.createState({
                authId: user.guid,
                users: usersMock.createState(user),
            });

            expect(isAnonymousUser(state)).toEqual(false);
        });

        it('Should return false if wrong user', () => {
            const [user] = usersMock.createAnonimous()();

            const state = stateMock.createState({
                authId: user.guid,
                users: usersMock.createState(),
            });

            expect(isAnonymousUser(state)).toEqual(false);
        });
    });

    describe('#isLimitedUser', () => {
        it('Should return false if regStatus is L', () => {
            const [user] = usersMock.createAnonimous()();

            const state = stateMock.createState({
                authId: user.guid,
                users: usersMock.createState(user),
            });

            expect(isLimitedUser(state)).toEqual(false);
        });

        it('Should return true if regStatus is Lu', () => {
            const [user] = usersMock.createLimited()();

            const state = stateMock.createState({
                authId: user.guid,
                users: usersMock.createState(user),
            });

            expect(isLimitedUser(state)).toEqual(true);
        });

        it('Should return false if regStatus is U', () => {
            const [user] = usersMock.createUnlimited()();

            const state = stateMock.createState({
                authId: user.guid,
                users: usersMock.createState(user),
            });

            expect(isLimitedUser(state)).toEqual(false);
        });

        it('Should return false if wrong user', () => {
            const [user] = usersMock.createLimited()();

            const state = stateMock.createState({
                authId: user.guid,
                users: usersMock.createState(),
            });

            expect(isLimitedUser(state)).toEqual(false);
        });
    });
});
