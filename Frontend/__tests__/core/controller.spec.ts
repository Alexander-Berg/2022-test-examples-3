import { BotController } from '../../core/controller';

import { createBot, receiveTextMessage, TestCommand } from '../helpers';

describe('constructor', () => {
    const bot = createBot();

    bot.addListener = jest.fn();

    it('starts listening text messages', () => {
        expect(new BotController(bot)).toBeInstanceOf(BotController);
        expect(bot.addListener).toBeCalledWith('text', expect.any(Function));
    });
});

describe('registerCommand', () => {
    const bot = createBot();

    bot.addListener = jest.fn();

    it('creates new command instance', () => {
        const botController = new BotController(bot);

        const commandGetNameSpy = jest.spyOn(TestCommand.prototype, 'getName');

        botController.registerCommand(TestCommand);

        expect(commandGetNameSpy).toBeCalled();

        commandGetNameSpy.mockRestore();
    });

    it('returns this', () => {
        const botController = new BotController(bot);

        expect(botController.registerCommand(TestCommand)).toBe(botController);
    });

    it('throws error if command is already exists', () => {
        const botController = new BotController(bot);

        expect(() => {
            botController.registerCommand(TestCommand);
        }).not.toThrowError();

        expect(() => {
            botController.registerCommand(TestCommand);
        }).toThrowError();
    });
});

describe('text message handler', () => {
    const bot = createBot();
    const botController = new BotController(bot);

    const testCommandRunSpy = jest.spyOn(TestCommand.prototype, 'run');

    botController.registerCommand(TestCommand);

    afterEach(() => {
        testCommandRunSpy.mockClear();
    });

    afterAll(() => {
        testCommandRunSpy.mockRestore();
    });

    it('does not run command if text does not matching', () => {
        receiveTextMessage(bot, 'any text');

        expect(testCommandRunSpy).not.toBeCalled();
    });

    it('runs registered command', () => {
        receiveTextMessage(bot, '/test a b c');

        const context = expect.objectContaining({
            bot,
            message: expect.anything(),
            command: 'test',
            args: ['a', 'b', 'c'],
        });

        expect(testCommandRunSpy).toBeCalledWith(context);
    });

    it('correctly parses command arguments', () => {
        receiveTextMessage(bot, '/test');

        expect(testCommandRunSpy).toBeCalledWith(expect.objectContaining({
            args: [],
        }));

        receiveTextMessage(bot, '/test  true  0  undefined');

        expect(testCommandRunSpy).toBeCalledWith(expect.objectContaining({
            args: ['true', '0', 'undefined'],
        }));
    });
});
