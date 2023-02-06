import IConfig from '@oclif/config';

import RunSelective from './run';

describe('RunSelective', () => {
    // eslint-disable-next-line
    let instance:any;

    beforeEach(() => {
        instance = new RunSelective([], {} as IConfig.Config);
    });

    describe('run', () => {
        it('should run execute command', async() => {
            jest.spyOn(instance, 'runCommand').mockImplementation(() => {});
            jest
                .spyOn(instance, 'getConfig')
                .mockReturnValue({
                    adapter: {
                        changedFiles: [],
                        run: () => ({ plainBlocksList: ['Button'] }),
                    },
                });
            jest
                .spyOn(instance, 'parse')
                .mockReturnValue({ flags: { execute: 'npm run build && npm run hermione' } });

            await instance.run();

            expect(instance.runCommand).toBeCalledWith('npm run build && npm run hermione', 'Button');
        });

        it('should return short list by default', async() => {
            const expected = { directlyAffected: 'Button', plainBlocksList: ['Button', 'Select'], affectedDependants: ['Select'] };
            jest
                .spyOn(instance, 'getConfig')
                .mockReturnValue({
                    adapter: {
                        changedFiles: [],
                        run: () => expected,
                    },
                });
            jest
                .spyOn(instance, 'parse')
                .mockReturnValue({ flags: {} });

            const actual = await instance.run();

            expect(actual).toEqual(expected.plainBlocksList);
        });

        it('should return extended output', async() => {
            const expected = { directlyAffected: 'Button', plainBlocksList: ['Button', 'Select'], affectedDependants: ['Select'] };
            jest
                .spyOn(instance, 'getConfig')
                .mockReturnValue({
                    adapter: {
                        changedFiles: [],
                        run: () => expected,
                    },
                });
            jest
                .spyOn(instance, 'parse')
                .mockReturnValue({ flags: { extended: true } });

            const actual = await instance.run();

            expect(actual).toEqual(expected);
        });

        it('should not throw if command succeeds', async() => {
            expect(() => instance.runCommand('echo', ['Button'])).not.toThrow('');
        });

        it('should throw if command fails', async() => {
            expect(() => instance.runCommand('exit 12 && echo', ['Button'])).toThrow('Command "exit 12 && echo -- Button" failed with code: 12');
        });
    });
});
