'use strict';

jest.mock('./convert-params.js', () => jest.fn((params, method) => ({ params, method })));
jest.mock('./convert-result.js', () => (result) => ({ object: result }));

describe('routes/send/route', () => {
    let uploadAttachments;
    let send;
    const core = { request: jest.fn() };
    core.request.safe = jest.fn();
    const next = jest.fn();

    beforeAll(() => {
        [ uploadAttachments, send ] = require('./route.js');
    });

    describe('#uploadAttachments', () => {
        it('Skip if no file', async () => {
            await uploadAttachments({ files: [], core }, {}, next);
            expect(next).toHaveBeenCalledTimes(1);
            expect(core.request).not.toHaveBeenCalled();
        });

        it('Uploads files', async () => {
            core.request.mockImplementation(
                (methods) => methods.map(
                    ({ params }) => ({
                        object: { id: `id-${params.filename}` }
                    })
                )
            );
            const body = {};
            await uploadAttachments({
                core,
                body,
                files: [
                    { originalname: 'f1', buffer: {} },
                    { originalname: 'f2', buffer: {} }
                ]
            }, {}, next);
            expect(body).toEqual({ att_ids: [ 'id-f1', 'id-f2' ] });
            expect(core.request).toHaveBeenCalledTimes(1);
        });
    });

    describe('#send', () => {
        it('save-draft', async () => {
            const req = { core, body: { a: 1 } };
            const res = { json: jest.fn() };
            await send(req, res);
            expect(require('./convert-params.js')).toHaveBeenCalledWith(req.body, 'save-draft');
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({ nosend: 'yes' }));
            expect(next).not.toHaveBeenCalled();
        });

        it('send-message', async () => {
            const req = { core, body: { doit: '1' } };
            const res = { json: jest.fn() };
            await send(req, res);
            expect(require('./convert-params.js')).toHaveBeenCalledWith(req.body, 'send-message');
            expect(res.json).toHaveBeenCalledWith(expect.objectContaining({}));
            expect(next).not.toHaveBeenCalled();
        });
    });
});
