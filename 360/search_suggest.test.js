'use strict';

const method = require('./search_suggest');
const ApiError = require('../../../routes/helpers/api-error.js');
const suggestMock = require('../../../test/mock/search-suggest.json');

const validateSchema = require('../_helpers/validate-schema.js');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            client: 'aphone',
            uuid: 'deadbeef42'
        },
        res: {
            status: jest.fn(() => ({
                send: jest.fn()
            })),
            set: jest.fn()
        },
        service: () => mockService
    };
});

test('возвращает 400 без параметра client', async () => {
    expect.assertions(3);
    delete core.params.client;

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('client param is missing');
    }
});

test('добавляет в параметр side=mobile к саджесту', async () => {
    mockService.mockResolvedValueOnce({ body: [] });

    await method(core);

    expect(mockService.mock.calls[0][1]).toMatchObject({ side: 'mobile' });
});

test('добавляет в параметр highlight=1 к саджесту', async () => {
    mockService.mockResolvedValueOnce({ body: [] });

    await method(core);

    expect(mockService.mock.calls[0][1]).toMatchObject({ highlight: 1 });
});

test('не передает пустой fid', async () => {
    mockService.mockResolvedValueOnce({ body: [] });

    await method(core);

    expect(Object.keys(mockService.mock.calls[0][1])).not.toEqual(expect.arrayContaining([ 'fid' ]));
});

test('добавляет в параметр fid, если указан', async () => {
    mockService.mockResolvedValueOnce({ body: [] });
    core.params.fid = '42';

    await method(core);

    expect(mockService.mock.calls[0][1].fid).toBe('42');
});

test('передает uuid в параметре reqid, если не указан', async () => {
    mockService.mockResolvedValueOnce({ body: [] });
    delete core.params.reqid;

    await method(core);

    expect(mockService.mock.calls[0][1].reqid).toBe('deadbeef42');
});

test('передает параметр reqid, если указан', async () => {
    mockService.mockResolvedValueOnce({ body: [] });
    core.params.reqid = '424242';

    await method(core);

    expect(mockService.mock.calls[0][1].reqid).toBe('424242');
});

test('limit по-умолчанию 10', async () => {
    mockService.mockResolvedValueOnce({ body: [] });

    await method(core);

    expect(mockService.mock.calls[0][1]).toMatchObject({ limit: 10 });
});

test('limit можно передать в параметрах', async () => {
    core.params.limit = 42;
    mockService.mockResolvedValueOnce({ body: [] });

    await method(core);

    expect(mockService.mock.calls[0][1]).toMatchObject({ limit: 42 });
});

test('text можно передать в параметрах', async () => {
    core.params.text = '42';
    mockService.mockResolvedValueOnce({ body: [] });

    await method(core);

    expect(mockService.mock.calls[0][1]).toMatchObject({ request: '42' });
});

test('если передан text, то types = subject,contact', async () => {
    core.params.text = '42';
    mockService.mockResolvedValueOnce({ body: [] });

    await method(core);

    expect(mockService.mock.calls[0][1]).toMatchObject({ types: 'subject,contact' });
});

test('если не передан text, то types = history', async () => {
    mockService.mockResolvedValueOnce({ body: [] });

    await method(core);

    expect(mockService.mock.calls[0][1]).toMatchObject({ types: 'history' });
});

test('если передан status, он добавляется в параметры', async () => {
    core.params.status = 1234;
    mockService.mockResolvedValueOnce({ body: [] });

    await method(core);

    expect(mockService.mock.calls[0][1]).toMatchObject({ status: 1234 });
});

test('если сервис валится, пробрасываем 500 с message', async () => {
    expect.assertions(3);
    mockService.mockRejectedValueOnce({ message: 'foo' });

    try {
        await method(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(500);
        expect(err.message).toBe('foo');
    }
});

describe('happy path', () => {
    beforeEach(() => {
        mockService.mockResolvedValueOnce({ body: suggestMock });
    });

    it('идет в правильную ручку', async () => {
        await method(core);

        expect(mockService.mock.calls[0][0]).toBe('/api/async/mail/suggest');
    });

    it('отвечает количеством айтемов из бэка', async () => {
        const res = await method(core);

        expect(res).toHaveLength(7);
    });

    it('отвечает правильно', async () => {
        const schema = require('./_helpers/schema.json');

        const result = await method(core).then(validateSchema(schema));

        expect(result).toMatchSnapshot();
    });

    it('отвечает правильно с html-хайлайтом', async () => {
        core.params.htmlHighlight = 1;
        const schema = require('./_helpers/schema-html.json');

        const result = await method(core).then(validateSchema(schema));

        expect(result).toMatchSnapshot();
    });
});

test('дефолтный параметр twoSteps в запросе к сервису (0)', async () => {
    mockService.mockResolvedValueOnce({ body: [] });

    await method(core);

    const opts = mockService.mock.calls[0][1];
    expect(opts.twoSteps).toBe(0);
});

test('не прокидывает timeout по-дефолту', async () => {
    mockService.mockResolvedValueOnce({ body: [] });
    core.params.timeout = 42;

    await method(core);

    const opts = mockService.mock.calls[0][1];
    expect(opts.twoSteps).toBe(0);
    expect(opts.timeout).toBeUndefined();
});

describe('twoSteps mode', () => {
    beforeEach(() => {
        mockService.mockResolvedValueOnce({ headers: {}, body: [] });
        core.params.twoSteps = true;
    });

    it('добавляет параметр twoSteps в запрос к сервису', async () => {
        await method(core);

        const opts = mockService.mock.calls[0][1];
        expect(opts.twoSteps).toBe(1);
    });

    it('если сервис ответил статусом в заголовках, прокидывать его в ответ', async () => {
        mockService.mockReset();
        mockService.mockResolvedValueOnce({ headers: { status: '1234' }, body: [] });

        await method(core);

        expect(core.res.set).toHaveBeenCalledWith('msearch-status', '1234');
    });

    it('прокидывает timeout', async () => {
        core.params.timeout = 42;

        await method(core);

        const opts = mockService.mock.calls[0][1];
        expect(opts.timeout).toBe(42);
    });
});
