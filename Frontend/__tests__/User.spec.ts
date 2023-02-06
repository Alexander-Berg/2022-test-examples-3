/* eslint-disable import/first */
jest.mock('../Api', () => ({
    default: {
        getUsers: jest.fn(),
        getUser: jest.fn(),
    },
}));

import RegistryApi from '../Api';
import { ensureUser } from '../User';
import { usersMockFactory } from '../../store/__tests__/mock/user';
import { stateMockFactory } from '../../store/__tests__/mock/state';
import { UserNotFoundError } from '../../errors';

function mockResolvedValue<V>(mockedFn: any, value: V) {
    (mockedFn as jest.Mock).mockResolvedValue(value);
}

describe('User service', () => {
    const usersMock = usersMockFactory();
    const stateMock = stateMockFactory();

    afterEach(() => {
        (RegistryApi.getUsers as jest.Mock).mockReset();
        (RegistryApi.getUser as jest.Mock).mockReset();
    });

    describe('ensureUser', () => {
        it('Should return user from store', async () => {
            const [user] = usersMock.createUnlimited()();

            const result = await ensureUser(stateMock.createState({
                users: usersMock.createState(user),
            }), { userId: user.guid });

            expect(result).toEqual(user);

            expect(RegistryApi.getUsers).not.toBeCalled();
        });

        it('Should return first user from API', async () => {
            const users = usersMock.createUnlimited()(2);

            mockResolvedValue(RegistryApi.getUser, users[0]);

            const result = await ensureUser(stateMock.createState({
                users: usersMock.createState(users[1]),
            }), { userId: users[0].guid });

            expect(result).toEqual(users[0]);

            expect(RegistryApi.getUser).toBeCalledTimes(1);
        });

        it('Should reject if user not found in server response', async () => {
            expect.assertions(2); // Должен сработать асерт в кэтче

            mockResolvedValue(RegistryApi.getUser, Promise.reject(new UserNotFoundError()));

            try {
                await ensureUser(stateMock.createState({
                    users: usersMock.createState(),
                }), { userId: usersMock.generateGuids(1)[0] });
            } catch (error) {
                expect(error.message).toMatch('User not found');
            }

            expect(RegistryApi.getUser).toBeCalledTimes(1);
        });
    });
});
