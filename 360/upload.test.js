'use strict';

const upload = require('./upload.js');
const status = require('../_helpers/status');

let core;
let mockService;

const file = {
    originalname: 'test.zip',
    buffer: Buffer.alloc(1024),
    mimetype: 'application/zip'
};

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            uuid: 'deadbeef42'
        },
        config: {
            urls: {
                webattach: 'http://webattach'
            }
        },
        service: () => mockService,
        req: {
            file
        },
        status: status(core)
    };
});

test('-> PERM_FAIL без uid', async () => {
    delete core.params.uuid;

    const res = await upload(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('uuid is mandatory');
});

test('передает верные параметры в сервис', async () => {
    mockService.mockResolvedValueOnce({ status: 'ok', object: { id: 'abcd42' } });

    await upload(core);

    expect(mockService).toHaveBeenCalledWith('write_attachment', {
        filename: file.originalname,
        body: file.buffer
    }, { form: false });
});

test('-> OK', async () => {
    mockService.mockResolvedValueOnce({ status: 'ok', object: { id: 'abcd42', hash: 'deadbeef==' } });

    const res = await upload(core);

    expect(res.status.status).toBe(1);
    expect(res.type).toBe('general');
    expect(res.content_type).toBe('application/zip');
    expect(res.id).toBe('abcd42');
    expect(res.hash).toBe('deadbeef==');
    expect(res.url).toInclude('abcd42');
    expect(res.url).toInclude('test.zip');
});

test('добавляет для изображений thumb и view', async () => {
    mockService.mockResolvedValueOnce({ status: 'ok', object: { id: 'abcd42' } });
    core.req.file = {
        originalname: 'test.jpg',
        buffer: Buffer.alloc(1024),
        mimetype: 'image/jpeg'
    };

    const res = await upload(core);

    expect(res.type).toBe('image');
    expect(res.content_type).toBe('image/jpeg');
    expect(res.thumb).toInclude('thumb=y');
    expect(res.view).toInclude('no_disposition=y');
});

test('-> TMP_FAIL', async () => {
    mockService.mockResolvedValueOnce({ status: 'not ok' });

    const res = await upload(core);

    expect(res.status.status).toBe(2);
});
