'use strict';

const { CUSTOM_ERROR } = require('@yandex-int/duffman').errors;
const mbodyMock = require('../../../test/mock/disk/mbody.json');

const mockParseAttachments = jest.fn();
jest.mock('../_helpers/disk/parse-attachments.js', () => mockParseAttachments);

const method = require('./disk_save_all');

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

test('отвечает 400, если не указан mid', async () => {
    expect.assertions(2);

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toBe('mid param is missing');
    }
});

describe('когда указан mid', () => {
    beforeEach(() => {
        core.params = { mid: '42' };
        mockDisk.mockResolvedValueOnce();
    });

    describe('несуществующий mid: mbody отвечает 500', () => {
        beforeEach(() => {
            mockMbody.mockRejectedValueOnce(new CUSTOM_ERROR({
                result: 'internal error',
                error: 'exception: error in forming message: Can\'t create message access by mid: unknown mid=XXX'
            }));
        });

        it('отвечает 400', async () => {
            expect.assertions(2);

            try {
                await method(core);
            } catch (e) {
                expect(e.code).toBe(400);
                expect(e.message).toInclude('Can\'t create message access by mid: unknown mid');
            }
        });
    });

    describe('mid существует', () => {
        beforeEach(() => {
            core.request.mockResolvedValueOnce();
        });

        it('дергает сервис с правильными параметрами', async () => {
            mockMbody.mockResolvedValueOnce({});
            mockParseAttachments.mockReturnValueOnce([]);

            await method(core);

            expect(mockMbody).toHaveBeenCalledWith('/message', {
                flags: 'XmlStreamerOn,XmlStreamerMobile,ShowContentMeta',
                mid: '42'
            });
        });

        describe('сервис валится', () => {
            beforeEach(() => {
                mockMbody.mockRejectedValueOnce();
            });

            it('ошибка', async () => {
                expect.assertions(1);

                try {
                    await method(core);
                } catch (err) {
                    expect(err.code).toBe(500);
                }
            });
        });

        describe('сервис отвечает', () => {
            const parsedAttachments = [
                {
                    hash: 'diskhash',
                    isDisk: true
                },
                {
                    hid: '1.1',
                    isDisk: false,
                    isInline: false
                }
            ];

            beforeEach(() => {
                mockMbody.mockResolvedValueOnce(mbodyMock);
                mockParseAttachments.mockReturnValueOnce(parsedAttachments);
            });

            it('аттачи парсятся', async () => {
                await method(core);

                expect(mockParseAttachments).toHaveBeenCalledWith(mbodyMock.attachments);
            });

            it('дергает модель', async () => {
                await method(core);

                expect(core.request.mock.calls[0][0]).toBe('do-disk-files-upload');
                expect(core.request.mock.calls[0][1]).toEqual({
                    mid: '42',
                    diskAttaches: [ parsedAttachments[0] ],
                    mailAttaches: [ parsedAttachments[1] ]
                });
            });

            it('если модель вернула ошибку', async () => {
                expect.assertions(2);
                core.request.mockReset();
                core.request.mockRejectedValueOnce({ message: 'boo' });

                try {
                    await method(core);
                } catch (err) {
                    expect(err.code).toBe(500);
                    expect(err.message).toBe('boo');
                }
            });

            it('если не передан параметр path, ходит в сервис создать системную папку', async () => {
                await method(core);

                expect(mockDisk).toHaveBeenCalledWith('/json/mksysdir', { type: 'downloads' });
            });

            it('если передан параметр path', async () => {
                core.params.path = '/disk/foo';

                await method(core);

                expect(core.request.mock.calls[0][0]).toBe('do-disk-files-upload');
                expect(core.request.mock.calls[0][1]).toMatchObject({ path: '/disk/foo' });
            });
        });
    });
});
