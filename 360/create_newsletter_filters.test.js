'use strict';

const method = require('./create_newsletter_filters.js');
const ApiError = require('../../../routes/helpers/api-error.js');

let core;

beforeEach(() => {
    core = {
        params: {
            newsletterFilters: [
                {
                    email: 'foo@example.com',
                    displayName: 'Awesome newsletter',
                    messageType: 13,
                    folderId: '3'
                }
            ]
        },
        request: jest.fn()
    };
});

test('вызывает create-newsletter-filters/v1', async () => {
    core.request.mockResolvedValueOnce({});

    await method.call(core);

    expect(core.request).toHaveBeenCalledWith('create-newsletter-filters/v1', {
        newsletterFilters: [
            {
                email: 'foo@example.com',
                displayName: 'Awesome newsletter',
                messageType: 13,
                folderId: '3'
            }
        ]
    });
});

test('проверяет схему', async () => {
    expect.assertions(3);

    core.params.newsletterFilters[0].messageType = '13';

    try {
        await method.call(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('invalid params schema');
    }
});

test('обрабатывает ошибки', async () => {
    expect.assertions(3);
    core.request.mockRejectedValueOnce({ message: 'foo' });

    try {
        await method.call(core);
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
        expect(err.message).toBe('foo');
    }
});
