'use strict';

const ava2 = require('./ava2.js');
const status = require('../_helpers/status');

const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockRecipients;

beforeEach(() => {
    mockRecipients = jest.fn();
    core = {
        params: {},
        service: () => mockRecipients
    };
    core.status = status(core);
});

describe('-> OK happy path', () => {
    it('один чанк в ответе сервиса', async () => {
        mockRecipients.mockResolvedValueOnce([ {
            'test@example.com': [
                {
                    is_self: false,
                    display_name: '',
                    email: 'test@example.com',
                    mono: 'TE',
                    warn: false,
                    ava_type: 'link',
                    color: '#f39718',
                    domain: 'ya.ru',
                    ava_value: 'http://pics.example.com/pic-200',
                    ava: {
                        url: 'http://pics.example.com/pic-200',
                        type: 'avatar',
                        url_mobile: 'http://pics.example.com/pic-200',
                        url_small: 'http://pics.example.com/pic-50'
                    },
                    valid: true,
                    ava_value_ie: 'http://pics.example.com/pic-50',
                    local: 'test'
                }
            ]
        } ]);

        const res = await ava2(core);

        expect(res.status.status).toBe(1);
        expect(res.catdog).toEqual({
            'test@example.com': [
                {
                    valid: true,
                    mono: 'TE',
                    color: '#f39718',
                    ava: {
                        type: 'avatar',
                        url_mobile: 'http://pics.example.com/pic-200'
                    },
                    local: 'test',
                    display_name: ''
                }
            ]
        });
    });

    it('несколько чанков в ответе сервиса', async () => {
        mockRecipients.mockResolvedValueOnce([
            {
                'test@example.com': [
                    {
                        mono: 'TE'
                    }
                ],
                'vasya@pupkin.ru': [
                    {
                        mono: 'VP'
                    }
                ]
            },
            {
                'test@example.com': [
                    {
                        mono: 'TT'
                    }
                ]
            }
        ]);

        const res = await ava2(core);

        expect(res.status.status).toBe(1);
        expect(res.catdog).toEqual({
            'test@example.com': [
                { mono: 'TE' },
                { mono: 'TT' }
            ],
            'vasya@pupkin.ru': [
                { mono: 'VP' }
            ]
        });
    });
});

test('прокидывает правильные параметры в сервис', async () => {
    core.params = { request: [ 'test@example.com' ] };
    mockRecipients.mockResolvedValueOnce([]);

    await ava2(core);

    expect(mockRecipients).toHaveBeenCalledWith('/emails', { recipientsIds: [ 'test@example.com' ] });
});

test('-> PERM_FAIL когда сервис отвечает 4xx', async () => {
    mockRecipients.mockRejectedValueOnce(httpError(400));

    const res = await ava2(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('CATDOG_REQUEST_ERROR');
});

test('-> TEMP_FAIL когда сервис отвечает 5xx', async () => {
    mockRecipients.mockRejectedValueOnce(httpError(500));

    const res = await ava2(core);

    expect(res.status.status).toBe(2);
    expect(res.status.phrase).toInclude('CATDOG_REQUEST_ERROR');
});
