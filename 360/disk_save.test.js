'use strict';

const method = require('./disk_save');
const mbodyMock = require('../../../test/mock/disk/mbody.json');

let core;
let mockMbody;
let mockDisk;

beforeEach(() => {
    mockMbody = jest.fn();
    mockDisk = jest.fn();
    core = {
        params: {},
        config: {
            services: {
                disk: {}
            }
        },
        service: (service) => (service === 'mbody' ? mockMbody : mockDisk),
        request: jest.fn()
    };
});

test('отвечает 404 если сервиса нет (корп, например)', async () => {
    expect.assertions(2);
    delete core.config.services.disk;

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(404);
        expect(err.message).toBe('disk service not available');
    }
});

test('отвечает 400, если не указан mid и hid', async () => {
    expect.assertions(2);

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toBe('mid and hid params are required');
    }
});

describe('переданы mid и hid', () => {
    beforeEach(() => {
        core.params = { mid: '42', hid: '1.3' };
        mockMbody.mockResolvedValueOnce(mbodyMock);
        mockDisk.mockResolvedValueOnce();
    });

    it('дергает модель', async () => {

        core.request.mockResolvedValueOnce();

        await method(core);

        expect(core.request).toHaveBeenCalledWith(
            'do-disk-files-upload',
            {
                mid: '42',
                diskAttaches: [],
                mailAttaches: [
                    {
                        hid: '1.3',
                        isDisk: false,
                        isInline: false,
                        name: 'Group 2.png'
                    }
                ]
            }
        );
    });

    it('если модель вернула ошибку', async () => {
        expect.assertions(2);
        core.request.mockRejectedValueOnce({ message: 'boo' });

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(500);
            expect(err.message).toBe('boo');
        }
    });

    it('если не передан параметр path, ходит в сервис создать системную папку', async () => {
        core.request.mockResolvedValueOnce();

        await method(core);

        expect(mockDisk).toHaveBeenCalledWith('/json/mksysdir', { type: 'downloads' });
    });

    it('если передан параметр path', async () => {
        core.params.path = '/disk/foo';
        core.request.mockResolvedValueOnce();

        await method(core);

        expect(core.request.mock.calls[0][0]).toBe('do-disk-files-upload');
        expect(core.request.mock.calls[0][1]).toMatchObject({ path: '/disk/foo' });
    });
});
