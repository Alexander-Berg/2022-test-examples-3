import { QBot, TextMessage } from '@yandex-int/qbot';

import { BotCommand } from '../core/command';
import { BotContext } from '../core/controller';

export class TestCommand extends BotCommand {
    public getName() {
        return 'test';
    }

    public parse() {
        return {};
    }

    public async handle() {
        return 'Hello';
    }
}

export function createBot(): QBot {
    return new QBot({ token: 'some auth token' });
}

export function createTextMessage(text: string): TextMessage {
    const date = Date.now();

    return {
        from: {
            is_bot: false,
            id: '9fd6cb46f12a49e581db4437db026e75',
            uid: 1120000000025555,
        },
        text,
        chat: {
            id: '542a6702-00fc-41dd-b4eb-29cf19e072cd_9fd6cb46-f12a-49e5-81db-4437db026e75',
        },
        date,
        message_id: date * 1000,
    };
}

export function createContext(command: string, args: string[]): BotContext {
    return {
        bot: createBot(),
        message: createTextMessage(`/${command} ${args.join(' ')}`.trim()),
        command,
        args,
    };
}

let updateId = 63096;

export function receiveTextMessage(bot: QBot, text: string) {
    bot.receiveUpdates([
        {
            message: createTextMessage(text),
            update_id: updateId++,
        },
    ]);
}
