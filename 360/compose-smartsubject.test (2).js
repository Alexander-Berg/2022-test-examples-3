'use strict';

const composeSmartsubject = require('./compose-smartsubject');

jest.mock('./_filters/compose-smartsubject');

const context = {};

describe('compose-smartsubject model', () => {
    beforeEach(() => {
        context.service = jest.fn();
        context.core = {
            config: {
                locale: 'ru'
            },
            hideParamInLog: jest.fn(),
            service: jest.fn().mockReturnValue(context.service)
        };
    });

    it('передаём дефолтные параметры', async () => {
        await composeSmartsubject({ id: 'reqId' }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('передаём и недефолтные параметры тоже', async () => {
        await composeSmartsubject({
            id: 'reqId',
            message_id: '__MESSAGE_ID__',
            thread_id: '__THREAD_ID__',
            from: '{"email": "__EMAIL__", "name": "__NAME__"}',
            attaches: '[]'
        }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                messageId: '__MESSAGE_ID__',
                threadId: '__THREAD_ID__',
                sender: { email: '__EMAIL__', displayName: '__NAME__' },
                text: '',
                recipients: [],
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('position - popup', async () => {
        await composeSmartsubject({ id: 'reqId', position: 'popup' }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('position - line', async () => {
        await composeSmartsubject({ id: 'reqId', position: 'line' }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'line' } }
        );
    });

    it('position - левый', async () => {
        await composeSmartsubject({ id: 'reqId', position: 'not_supported' }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('text - лимитирован в 20к', async () => {
        await composeSmartsubject({ id: 'reqId', text: '$'.repeat(30 * 1024) }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '$'.repeat(20 * 1024),
                recipients: [],
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('парсим адресатов в to', async () => {
        await composeSmartsubject({ id: 'reqId', to: JSON.stringify([
            { email: 'user1@yandex.com', name: 'user1', someExtraField: 'someExtraValue' }
        ]) }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [
                    { email: 'user1@yandex.com', displayName: 'user1' }
                ],
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('дефолтим имена адресатов в to', async () => {
        await composeSmartsubject({ id: 'reqId', to: JSON.stringify([
            { email: 'user1@yandex.com' }
        ]) }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [
                    { email: 'user1@yandex.com', displayName: '' }
                ],
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('дефолтим адреса адресатов в to', async () => {
        await composeSmartsubject({ id: 'reqId', to: JSON.stringify([
            { name: 'user1' }
        ]) }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [
                    { email: '', displayName: 'user1' }
                ],
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('парсим максимум 100 адресатов в to', async () => {
        await composeSmartsubject({
            id: 'reqId',
            to: JSON.stringify([ ...' '.repeat(300) ].map(
                (_, idx) => ({ email: `user${idx}@yandex.be`, name: `user${idx}` })
            ))
        }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [ ...' '.repeat(100) ].map(
                    (_, idx) => ({ email: `user${idx}@yandex.be`, displayName: `user${idx}` })
                ),
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('парсим адресатов в cc', async () => {
        await composeSmartsubject({ id: 'reqId', cc: JSON.stringify([
            { email: 'user1@yandex.com', name: 'user1', someExtraField: 'someExtraValue' }
        ]) }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [
                    { email: 'user1@yandex.com', displayName: 'user1' }
                ],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('дефолтим имена адресатов в cc', async () => {
        await composeSmartsubject({ id: 'reqId', cc: [
            { email: 'user1@yandex.com' }
        ] }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [
                    { email: 'user1@yandex.com', displayName: '' }
                ],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('дефолтим адреса адресатов в cc', async () => {
        await composeSmartsubject({ id: 'reqId', cc: [
            { name: 'user1' }
        ] }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [
                    { email: '', displayName: 'user1' }
                ],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('парсим максимум 100 адресатов в cc', async () => {
        await composeSmartsubject({
            id: 'reqId',
            cc: [ ...' '.repeat(300) ].map(
                (_, idx) => ({ email: `user${idx}@yandex.be`, name: `user${idx}` })
            )
        }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [ ...' '.repeat(100) ].map(
                    (_, idx) => ({ email: `user${idx}@yandex.be`, displayName: `user${idx}` })
                ),
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('парсим адресатов в bcc', async () => {
        await composeSmartsubject({ id: 'reqId', bcc: [
            { email: 'user1@yandex.com', name: 'user1', someExtraField: 'someExtraValue' }
        ] }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [],
                bcc: [
                    { email: 'user1@yandex.com', displayName: 'user1' }
                ],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('дефолтим имена адресатов в bcc', async () => {
        await composeSmartsubject({ id: 'reqId', bcc: [
            { email: 'user1@yandex.com' }
        ] }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [],
                bcc: [
                    { email: 'user1@yandex.com', displayName: '' }
                ],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('дефолтим адреса адресатов в bcc', async () => {
        await composeSmartsubject({ id: 'reqId', bcc: [
            { name: 'user1' }
        ] }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [],
                bcc: [
                    { email: '', displayName: 'user1' }
                ],
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('парсим максимум 100 адресатов в bcc', async () => {
        await composeSmartsubject({
            id: 'reqId',
            bcc: [ ...' '.repeat(300) ].map(
                (_, idx) => ({ email: `user${idx}@yandex.be`, name: `user${idx}` })
            )
        }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [],
                bcc: [ ...' '.repeat(100) ].map(
                    (_, idx) => ({ email: `user${idx}@yandex.be`, displayName: `user${idx}` })
                ),
                attachments: [],
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('парсим аттачи', async () => {
        await composeSmartsubject({
            id: 'reqId',
            attaches: [ ...' '.repeat(300) ].map(
                (_, idx) => ({ id: `attach_${idx}`, name: `attach${idx}`, mediaType: 'image' })
            )
        }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [],
                bcc: [],
                attachments: [ ...' '.repeat(100) ].map(
                    (_, idx) => ({ attachmentId: `attach_${idx}`, name: `attach${idx}`, type: 'image' })
                ),
                lang: 'ru'
            },
            { query: { position: 'popup' } }
        );
    });

    it('прокидываем extra_params в query сервиса', async () => {
        await composeSmartsubject({ id: 'reqId',
            extra_params: 'extraOne=1&extraTwo=2'
        }, context.core);

        expect(context.service).toBeCalledWith(
            '/subject',
            {
                requestId: 'reqId',
                text: '',
                recipients: [],
                cc: [],
                bcc: [],
                attachments: [],
                lang: 'ru'
            },
            {
                query: {
                    position: 'popup',
                    extraOne: '1',
                    extraTwo: '2'
                }
            }
        );
    });
});
