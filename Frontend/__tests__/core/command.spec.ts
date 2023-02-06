import { createContext, TestCommand } from '../helpers';

describe('BotCommand#getDescription', () => {
    it('returns empty string', () => {
        const testCommand = new TestCommand();

        expect(testCommand.getDescription()).toBe('');
    });
});

describe('BotCommand#getDefaultParams', () => {
    it('returns empty object if method is not overloaded', () => {
        const testCommand = new TestCommand();

        expect(testCommand.getDefaultParams()).toEqual({});
    });
});

describe('BotCommand#run', () => {
    const context = createContext('test', ['a', 'b', 'c']);

    const testCommand = new TestCommand();

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('calls "parse" method with raw arguments and default params', () => {
        const parseSpy = jest.spyOn(TestCommand.prototype, 'parse');

        testCommand.run(context);

        const defaultParams = testCommand.getDefaultParams();

        expect(parseSpy).toBeCalledWith(context.args, defaultParams);
    });

    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('sends description if parse method thrown an error and description is not empty', () => {
        const sendMessageSpy = jest.spyOn(context.bot, 'sendMessage')
            .mockImplementation(jest.fn());

        jest.spyOn(testCommand, 'parse').mockImplementation(() => {
            throw new Error('Invalid args');
        });

        testCommand.run(context);

        expect(sendMessageSpy).not.toBeCalled();

        testCommand.run(context);

        const text = 'some description';

        jest.spyOn(testCommand, 'getDescription').mockImplementation(() => text);

        expect(sendMessageSpy).toBeCalledWith(context.message.chat.id, text);
    });

    it('calls "handle" method with parsed params', () => {
        const handleSpy = jest.spyOn(TestCommand.prototype, 'handle');

        testCommand.run(context);

        const params = testCommand.parse();

        expect(handleSpy).toBeCalledWith(context, params);
    });
});
