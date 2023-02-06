import Axios from 'axios';
import * as FormData from 'form-data';
import { QBot, withHeaders } from '../qbot';

import * as Fake from './data.json';

jest.mock('axios');
jest.mock('form-data');
jest.useFakeTimers();

afterEach(() => {
    jest.clearAllMocks();
});

describe('constructor', () => {
    it('creates an instance without errors', () => {
        expect(new QBot({ token: Fake.token })).toBeInstanceOf(QBot);
    });

    it('throws error if no token specified', () => {
        expect(() => {
            // @ts-ignore
            // eslint-disable-next-line no-new
            new QBot();
        }).toThrowError();
    });
});

describe('start', () => {
    it('sets webhook if passed in options', () => {
        const bot = new QBot({
            token: Fake.token,
            webhook: Fake.webhook,
        });

        const setWebhookSpy = jest.spyOn(bot, 'setWebhook');

        expect.assertions(1);

        return bot.start().then(() => {
            expect(setWebhookSpy).toBeCalledWith(Fake.webhook);
        });
    });

    it('deletes webhook and starts polling', () => {
        const bot = new QBot({
            token: Fake.token,
        });

        const setWebhookSpy = jest.spyOn(bot, 'setWebhook');
        const startPolling = jest.spyOn(bot, 'startPolling');

        expect.assertions(2);

        return bot.start().then(() => {
            expect(setWebhookSpy).toBeCalledWith(undefined);
            expect(startPolling).toBeCalled();
        });
    });
});

describe('startPolling', () => {
    it('throws error if using webhook', () => {
        const bot = new QBot({
            token: Fake.token,
            webhook: Fake.webhook,
        });

        expect(() => {
            bot.startPolling();
        }).toThrowError();
    });

    it('calls "getUpdates" only once', () => {
        const bot = new QBot({ token: Fake.token });

        const getUpdatesSpy = jest.spyOn(bot, 'getUpdates');

        bot.startPolling();
        bot.startPolling();

        expect(getUpdatesSpy).toBeCalledTimes(1);
    });

    it('calls "getUpdates" and "receiveUpdates" on starting', () => {
        const bot = new QBot({
            token: Fake.token,
            limit: Fake.limit,
        });

        const getUpdatesSpy = jest.spyOn(bot, 'getUpdates');
        const receiveUpdatesSpy = jest.spyOn(bot, 'receiveUpdates');

        expect.assertions(2);

        return bot.startPolling().then(() => {
            expect(getUpdatesSpy).toBeCalledWith(0, Fake.limit);
            expect(receiveUpdatesSpy).toBeCalledWith([]);
        });
    });

    it('starts polling with user interval', () => {
        const bot = new QBot({
            token: Fake.token,
            pollingInterval: Fake.pollingInterval,
        });

        const getUpdatesSpy = jest.spyOn(bot, 'getUpdates');

        expect.assertions(2);

        return bot.startPolling().then(() => {
            expect(setTimeout).toBeCalledWith(expect.any(Function), Fake.pollingInterval);

            getUpdatesSpy.mockClear();
            jest.runOnlyPendingTimers();

            expect(getUpdatesSpy).toBeCalled();
        });
    });
});

describe('stopPolling', () => {
    let bot: QBot;

    beforeEach(() => {
        bot = new QBot({ token: Fake.token });
    });

    it('does nothing if polling not started', () => {
        bot.stopPolling();

        expect(clearTimeout).not.toBeCalled();
    });

    it('stops polling if started', () => {
        expect.assertions(2);

        return bot.startPolling().then(() => {
            jest.runOnlyPendingTimers();

            bot.stopPolling();

            expect(clearTimeout).toBeCalled();
            expect(setTimeout).toBeCalledTimes(1);
        });
    });
});

describe('isPolling', () => {
    let bot: QBot;

    beforeEach(() => {
        bot = new QBot({ token: Fake.token });
    });

    it('returns "false" if polling is not started', () => {
        expect.assertions(2);

        expect(bot.isPolling()).toBeFalsy();

        return bot.startPolling().then(() => {
            bot.stopPolling();

            expect(bot.isPolling()).toBeFalsy();
        });
    });

    it('returns "true" if polling is started', () => {
        bot.startPolling();

        expect(bot.isPolling()).toBeTruthy();
    });
});

describe('events', () => {
    const listener = jest.fn();

    let bot: QBot;

    beforeEach(() => {
        bot = new QBot({ token: Fake.token });
    });

    it('emits "message" only once if receiving the same data', () => {
        bot.addListener('message', listener);

        bot.receiveUpdates(Fake.updates);
        bot.receiveUpdates(Fake.updates);

        expect(listener).toHaveBeenCalledTimes(Fake.updates.length);
    });

    it('not emits "message" after removing listener', () => {
        bot.addListener('message', listener);
        bot.removeListener('message', listener);

        bot.receiveUpdates(Fake.updates);

        expect(listener).not.toBeCalled();
    });

    it('not emits after removing all listeners', () => {
        bot.addListener('message', listener);
        bot.addListener('document', listener);
        bot.addListener('photo', listener);

        bot.removeAllListeners();

        bot.receiveUpdates(Fake.updates);

        expect(listener).not.toBeCalled();
    });

    it('emits "message" on receiving update', () => {
        bot.addListener('message', listener);
        bot.receiveUpdates(Fake.updates);

        expect(listener).toBeCalledWith(Fake.updates[0].message);
        expect(listener).toBeCalledTimes(4);
    });

    it('emits "document" on receiving update', () => {
        bot.addListener('document', listener);
        bot.receiveUpdates(Fake.updates);

        expect(listener).toBeCalledWith(Fake.updates[1].message);
    });

    it('emits "photo" on receiving update', () => {
        bot.addListener('photo', listener);
        bot.receiveUpdates(Fake.updates);

        expect(listener).toBeCalledWith(Fake.updates[2].message);
    });

    it('emits "text" on receiving update', () => {
        bot.addListener('text', listener);
        bot.receiveUpdates(Fake.updates);

        expect(listener).toBeCalledWith(Fake.updates[0].message);
    });

    it('emits "sticker" on receiving update', () => {
        bot.addListener('sticker', listener);
        bot.receiveUpdates(Fake.updates);

        expect(listener).toBeCalledWith(Fake.updates[3].message);
    });
});

describe('API requests', () => {
    const bot = new QBot({ token: Fake.token });
    const postSpy = jest.spyOn(Axios, 'post');

    it('request "getMe" with params', () => {
        bot.getMe();

        expect(postSpy).toBeCalledWith('bot/getMe/', undefined, undefined);
    });

    it('request "getFile" with params', () => {
        bot.getFile(Fake.fileId);

        expect(postSpy).toBeCalledWith('bot/getFile/', {
            file_id: Fake.fileId,
        }, undefined);
    });

    it('request "setWebhook" with params', () => {
        bot.setWebhook();
        bot.setWebhook(Fake.webhook);

        const url = 'team/update/';

        expect(postSpy).nthCalledWith(1, url, {
            webhook_url: null,
        }, undefined);

        expect(postSpy).nthCalledWith(2, url, {
            webhook_url: Fake.webhook,
        }, undefined);
    });

    it('request "getUpdates" with params', () => {
        bot.getUpdates();
        bot.getUpdates(1);
        bot.getUpdates(1, 100);

        const url = 'bot/telegram_lite/getUpdates/';

        expect(postSpy).nthCalledWith(1, url, {
            offset: undefined,
            limit: undefined,
        }, undefined);

        expect(postSpy).nthCalledWith(2, url, {
            offset: 1,
            limit: undefined,
        }, undefined);

        expect(postSpy).nthCalledWith(3, url, {
            offset: 1,
            limit: 100,
        }, undefined);
    });

    it('request "sendTyping" with params', () => {
        bot.sendTyping(Fake.chatId);

        expect(postSpy).toBeCalledWith('bot/telegram_lite/sendTyping/', {
            chat_id: Fake.chatId,
        }, undefined);
    });

    it('request "sendSeenMarker" with params', () => {
        bot.sendSeenMarker(Fake.chatId, Fake.messageId);

        expect(postSpy).toBeCalledWith('bot/telegram_lite/sendSeenMarker/', {
            chat_id: Fake.chatId,
            message_id: Fake.messageId,
        }, undefined);
    });

    it('request "sendMessage" with params', () => {
        bot.sendMessage(Fake.chatId, Fake.text);
        bot.sendMessage(Fake.chatId, Fake.text, {
            inline_keyboard: Fake.keyboard,
        });

        const url = 'bot/telegram_lite/sendMessage/';

        expect(postSpy).nthCalledWith(1, url, {
            chat_id: Fake.chatId,
            text: Fake.text,
        }, undefined);

        expect(postSpy).nthCalledWith(2, url, {
            chat_id: Fake.chatId,
            text: Fake.text,
            reply_markup: {
                inline_keyboard: Fake.keyboard,
            },
        }, undefined);
    });

    it('request "sendPhoto" with params', async() => {
        const file = Buffer.alloc(10, 'q');
        const filename = 'image.jpg';

        await bot.sendPhoto(Fake.chatId, file, filename);

        const url = 'bot/telegram_lite/sendPhoto/';

        const data = new FormData();

        data.append('chat_id', Fake.chatId);
        data.append('photo', file, { filename });

        const config = expect.objectContaining({
            headers: expect.anything(),
        });

        expect(postSpy).toBeCalledWith(url, expect.any(FormData), config);

        // @ts-ignore
        expect(postSpy.mock.calls[0][1].getBuffer()).toEqual(data.getBuffer());
    });

    it('request "sendDocument" with params', async() => {
        const file = Buffer.alloc(10, 'q');
        const filename = 'file.pdf';

        await bot.sendDocument(Fake.chatId, file, filename);

        const url = 'bot/telegram_lite/sendDocument/';

        const data = new FormData();

        data.append('chat_id', Fake.chatId);
        data.append('document', file, { filename });

        const config = expect.objectContaining({
            headers: expect.anything(),
        });

        expect(postSpy).toBeCalledWith(url, expect.any(FormData), config);

        // @ts-ignore
        expect(postSpy.mock.calls[0][1].getBuffer()).toEqual(data.getBuffer());
    });
});

describe('withHeaders', () => {
    it('reject if received an error', async() => {
        const data = new FormData();

        jest.spyOn(data, 'getLength').mockImplementation(callback => {
            callback(new Error('some error'), 0);
        });

        await expect(withHeaders(data)).rejects.toThrowError();
    });

    it('resolve correct headers', async() => {
        const data = new FormData();

        data.append('file', Buffer.alloc(10, 'q'));

        const headers = expect.objectContaining({
            'content-type': expect.stringContaining('multipart/form-data; boundary='),
            'content-length': expect.any(Number),
        });

        await expect(withHeaders(data)).resolves.toEqual(headers);
    });
});
