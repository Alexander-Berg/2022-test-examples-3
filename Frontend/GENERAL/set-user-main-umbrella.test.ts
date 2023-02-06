import { matchers } from 'jest-json-schema';

import { setUserMainUmbrella } from './set-user-main-umbrella';

import { Injector } from '../../../interfaces/Injector';
import { userMainUmbrellaRepository, UserMainUmbrellaRepository } from '../../../repositories/UserMainUmbrellaRepository';
import { IUserMainUmbrella } from '../../../services/goals/interfaces/v2/IUserMainUmbrella';
import UserMainUmbrellaSchema from '../../../services/goals/schemas/v2/IUserMainUmbrella.json';

jest.mock('../../../repositories/UserMainUmbrellaRepository', () => {
    const mock: {userMainUmbrellaRepository: Partial<UserMainUmbrellaRepository>;} = {
        userMainUmbrellaRepository: {
            upsert: jest.fn(() => Promise.resolve({ login: 'zzhanka', umbrella: 123 })),
        },
    };

    return mock;
});

expect.extend(matchers);

describe('Set user main umbrella', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should save data to repository and return it', async() => {
        const injector = {
            trackerAPI: {
                searchIssues: jest.fn((goalIds: number[], fields: string) => Promise.resolve([{}])),
                updateIssue: jest.fn((goalId: number, obj: Object) => Promise.resolve({})),
            },
        } as unknown as Injector;
        const payload: IUserMainUmbrella = { login: 'zzhanka', umbrella: 123 };
        const result = await setUserMainUmbrella(payload, injector);

        expect(injector.trackerAPI.searchIssues).toBeCalledTimes(1);

        expect(userMainUmbrellaRepository.upsert).toBeCalledTimes(1);
        expect(userMainUmbrellaRepository.upsert).toBeCalledWith(payload);
        expect(result).toMatchSchema(UserMainUmbrellaSchema);
    });

    it('should throw error if umbrella doesn\'t exists', async() => {
        const injector = {
            trackerAPI: {
                searchIssues: jest.fn(() => Promise.resolve([])),
            },
        } as unknown as Injector;
        const payload: IUserMainUmbrella = { login: 'zzhanka', umbrella: 123 };

        await expect(setUserMainUmbrella(payload, injector)).rejects.toThrowError();

        expect(injector.trackerAPI.searchIssues).toBeCalledTimes(1);
        expect(injector.trackerAPI.searchIssues)
            .toBeCalledWith(['GOALSVAULTIV-' + payload.umbrella], 'participants');

        expect(userMainUmbrellaRepository.upsert).toBeCalledTimes(0);
    });
});
