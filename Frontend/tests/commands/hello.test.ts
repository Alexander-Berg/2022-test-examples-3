import Hello from '../../commands/hello';

describe('Hello', () => {
    let result: string[];

    beforeEach(() => {
        result = [];
        jest
            .spyOn(process.stdout, 'write')
            // @ts-ignore
            .mockImplementation(val => result.push(val));
    });

    afterEach(() => jest.restoreAllMocks());

    it('default', async() => {
        await Hello.run(['--name', 'test']);

        expect(result).toStrictEqual(['hello test from ./src/commands/hello.ts\n']);
    });
});
