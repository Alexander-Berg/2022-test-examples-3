import { matchers } from 'jest-json-schema';

import { getUsersMainUmbrellas } from './get-users-main-umbrellas';

import { userMainUmbrellaRepository, UserMainUmbrellaRepository } from '../../../repositories/UserMainUmbrellaRepository';
import UserMainUmbrellaSchema from '../../../services/goals/schemas/v2/IUserMainUmbrella.json';

jest.mock('../../../repositories/UserMainUmbrellaRepository', () => {
    const mock: {userMainUmbrellaRepository: Partial<UserMainUmbrellaRepository>;} = {
        userMainUmbrellaRepository: {
            getAll: jest.fn(() => Promise.resolve([{ login: 'zzhanka', umbrella: 123 }])),
        },
    };

    return mock;
});

expect.extend(matchers);

describe('Get users main umbrellas', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should return data from repository', async() => {
        const result = await getUsersMainUmbrellas(2);

        expect(userMainUmbrellaRepository.getAll).toBeCalledTimes(1);
        expect(userMainUmbrellaRepository.getAll).toBeCalledWith(100, 200);
        result.forEach(item => expect(item).toMatchSchema(UserMainUmbrellaSchema));
    });
});
