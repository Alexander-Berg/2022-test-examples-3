import CreateLeafWizard from '../index';
import { createNewLeaf } from '../utils/createNewLeaf';
import { prepareLeaf } from '../utils/prepareLeaf';

jest.mock('inquirer', () => ({
    prompt: jest.fn(() => ({
        type: 'package',
        name: 'test',
    })),
}));
jest.mock('../utils/createNewLeaf', () => ({
    createNewLeaf: jest.fn().mockReturnValue('pathToLeaf'),
}));

jest.mock('../utils/prepareLeaf', () => ({
    prepareLeaf: jest.fn(),
}));
jest.mock('../constants', () => ({
    isTTY: true,
}));

describe('CreateLeafWizard', () => {
    beforeEach(() => {
        jest
            .spyOn(process.stdout, 'write');
    });

    afterEach(() => jest.restoreAllMocks());

    it('default', async() => {
        await CreateLeafWizard.run(['--config', 'src/tests/config.json']);

        expect(createNewLeaf).toHaveBeenCalledWith(
            {
                package: {
                    path: './packages/stub/',
                    blackListPaths: [
                        '/__reports',
                        '/node_modules',
                        '/package-lock.json',
                        '/build',
                    ],
                },
            },
            { name: 'test', type: 'package' },
        );
        expect(createNewLeaf).toHaveReturnedWith('pathToLeaf');

        expect(prepareLeaf).toHaveBeenCalledWith('pathToLeaf', { name: 'test', type: 'package' }, undefined);
        expect(prepareLeaf).toHaveBeenCalledTimes(1);

        expect(process.stdout.write).not.toBeCalled();
    });
});
