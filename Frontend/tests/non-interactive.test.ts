import { prompt } from 'inquirer';
import CreateLeafWizard from '../index';
import { createNewLeaf } from '../utils/createNewLeaf';
import { prepareLeaf } from '../utils/prepareLeaf';

jest.mock('inquirer');

jest.mock('../utils/createNewLeaf', () => ({
    createNewLeaf: jest.fn().mockReturnValue('pathToLeaf'),
}));

jest.mock('../utils/prepareLeaf', () => ({
    prepareLeaf: jest.fn(),
}));
jest.mock('../constants', () => ({
    isTTY: false,
}));

describe('CreateLeafWizard', () => {
    beforeEach(() => {
        jest
            .spyOn(process.stdout, 'write');
    });

    afterEach(() => jest.restoreAllMocks());

    describe('requires to pass config params', () => {
        const baseConfig = ['--config', 'src/tests/config.json', '--type', 'package', '--name', 'test2'];
        for (const param of ['type', 'name']) {
            it(`requires to pass ${param}`, async() => {
                const config = [...baseConfig];
                config.splice(baseConfig.indexOf(`--${param}`), 2);
                await expect(CreateLeafWizard.run(config)).rejects.toHaveProperty('flag.name', param);
                expect(prompt).not.toBeCalled();
            });
        }
    });

    it('default', async() => {
        await CreateLeafWizard.run(['--config', 'src/tests/config.json', '--type', 'package', '--name', 'test2']);

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
            { name: 'test2', type: 'package' },
        );
        expect(createNewLeaf).toHaveReturnedWith('pathToLeaf');

        expect(prepareLeaf).toHaveBeenCalledWith('pathToLeaf', { name: 'test2', type: 'package' }, undefined);
        expect(prepareLeaf).toHaveBeenCalledTimes(1);
        expect(prompt).not.toBeCalled();

        expect(process.stdout.write).not.toBeCalled();
    });
});
