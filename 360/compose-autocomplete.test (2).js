'use strict';

const composeAutocomplete = require('./compose-autocomplete');

jest.mock('./_filters/compose-autocomplete');

const context = {};

describe('compose-autocomplete model', () => {
    beforeEach(() => {
        context.service = jest.fn();
        context.core = {
            hideParamInLog: jest.fn(),
            service: jest.fn().mockReturnValue(context.service)
        };
    });

    it('если есть thread_id, то передаём tid', async () => {
        await composeAutocomplete({ id: 'sid', thread_id: 'tid', allow_pers_data: 'on' }, context.core);

        expect(context.service).toBeCalledWith(
            '/compose',
            { text: '' },
            { query: { sid: 'sid', tid: 'tid' } }
        );
    });

    it('если нет thread_id, то не передаём ничего в качестве tid', async () => {
        await composeAutocomplete({ id: 'sid', allow_pers_data: 'on' }, context.core);

        expect(context.service).toBeCalledWith(
            '/compose',
            { text: '' },
            { query: { sid: 'sid' } }
        );
    });

    it('если есть message_id, то передаём mid', async () => {
        await composeAutocomplete(
            { id: 'sid', thread_id: 'tid', message_id: 'mid', allow_pers_data: 'on' },
            context.core
        );

        expect(context.service).toBeCalledWith(
            '/compose',
            { text: '' },
            { query: { sid: 'sid', tid: 'tid', mid: 'mid' } }
        );
    });

    it('если передаём init_session, то вызываем ручку init_session методом get без парсинга ответа', async () => {
        await composeAutocomplete({ init_session: 'true', id: 'sid', allow_pers_data: 'on' }, context.core);

        expect(context.service).toBeCalledWith(
            '/init_session',
            {},
            { query: { sid: 'sid' }, method: 'get', allowPlain: true }
        );
    });

    it('если передаём init_session, то не передаём текст ', async () => {
        await composeAutocomplete(
            { init_session: 'true', id: 'sid', text: 'some_text', allow_pers_data: 'on' },
            context.core
        );

        expect(context.service).toBeCalledWith(
            '/init_session',
            {},
            { query: { sid: 'sid' }, method: 'get', allowPlain: true }
        );
    });

    it('если передаём init_session, то сбрасываем body в undefined', async () => {
        await composeAutocomplete(
            { init_session: 'true', id: 'sid', text: 'some_text', allow_pers_data: 'on' },
            context.core
        );

        expect(context.service).toBeCalledWith(
            '/init_session',
            {},
            { query: { sid: 'sid' }, method: 'get', allowPlain: true, body: undefined }
        );
    });

    it('если передаём thread_id в init_session, то передаём tid', async () => {
        await composeAutocomplete(
            { init_session: 'true', id: 'sid', thread_id: 'tid', allow_pers_data: 'on' },
            context.core
        );

        expect(context.service).toBeCalledWith(
            '/init_session',
            {},
            { query: { sid: 'sid', tid: 'tid' }, method: 'get', allowPlain: true }
        );
    });

    it('если есть text_limit больше MAX, сбрасываем его в DEFAULT', async () => {
        await composeAutocomplete(
            { id: 'sid', text: 'a'.repeat(8000), text_limit: 8000, allow_pers_data: 'on' },
            context.core
        );

        expect(context.service).toBeCalledWith(
            '/compose',
            { text: 'a'.repeat(1024) },
            { query: { sid: 'sid' } }
        );
    });

    it('если text_limit валидный, используем его', async () => {
        await composeAutocomplete(
            { id: 'sid', text: 'a'.repeat(8000), text_limit: 2000, allow_pers_data: 'on' },
            context.core
        );

        expect(context.service).toBeCalledWith(
            '/compose',
            { text: 'a'.repeat(2000) },
            { query: { sid: 'sid' } }
        );
    });

    it('если text_limit не число, сбрасываем его в DEFAULT', async () => {
        await composeAutocomplete(
            { id: 'sid', text: 'a'.repeat(8000), text_limit: 'yandex', allow_pers_data: 'on' },
            context.core
        );

        expect(context.service).toBeCalledWith(
            '/compose',
            { text: 'a'.repeat(1024) },
            { query: { sid: 'sid' } }
        );
    });

    it('если есть extra_params, то добавляем их в query', async () => {
        await composeAutocomplete(
            { id: 'sid', extra_params: 'version=1.0.0&tag=important', allow_pers_data: 'on' },
            context.core
        );

        expect(context.service).toBeCalledWith(
            '/compose',
            { text: '' },
            { query: { sid: 'sid', version: '1.0.0', tag: 'important' } }
        );
    });

    it('если нет allow_pers_data, то добавляем prod_no_pers в query', async () => {
        await composeAutocomplete(
            { id: 'sid' },
            context.core
        );

        expect(context.service).toBeCalledWith(
            '/compose',
            { text: '' },
            { query: { sid: 'sid', exp: 'prod_no_pers' } }
        );
    });
});
