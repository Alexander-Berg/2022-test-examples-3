import { createDepsForLint } from './create-deps';
import { DepStatus } from './types';

jest.mock('@yandex-int/frontend.ci.utils', () => ({
    Env: {
        getCheckoutConfig: jest.fn()
            .mockReturnValue({ base: { commit: 'baseHash' }, head: { commit: 'headHash' } }),
    },
    getBaseCommit: jest.fn().mockReturnValue('baseHash'),
    getContentByCommit: jest.fn()
        .mockReturnValueOnce({ dependencies: { p1: 'v1', p2: 'v1' }, devDependencies: { p3: 'v1', p4: 'v1' } }),
}));

jest.mock('fs', () => ({
    readFileSync: jest.fn()
        .mockReturnValueOnce('{"dependencies":{"p1":"v2","p2":"v1"},"devDependencies":{"p3":"v1","p4":"v1.2"}}')
        .mockReturnValue('{"dependencies":{"p1":"v1","p2":"v1"},"devDependencies":{"p3":"v1","p4":"v1"}}'),
}));

const packageJsonPath = '/path/to/package.json';
const lintedPackages = ['p1', 'p2', 'p3', 'p4', 'p5'];
const log = () => {
};

describe('Create deps list for linting', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should return only changed deps if package.json deps is changed', () => {
        expect(createDepsForLint(packageJsonPath, lintedPackages, true, log))
            .toStrictEqual({
                p1: { version: 'v2', status: DepStatus.Changed },
                p4: { version: 'v1.2', status: DepStatus.Changed },
            });
    });

    it('should return all deps list if package.json is new', () => {
        expect(createDepsForLint(packageJsonPath, lintedPackages, true, log))
            .toStrictEqual({
                p1: { version: 'v1', status: DepStatus.New },
                p2: { version: 'v1', status: DepStatus.New },
                p3: { version: 'v1', status: DepStatus.New },
                p4: { version: 'v1', status: DepStatus.New },
            });
    });
});
