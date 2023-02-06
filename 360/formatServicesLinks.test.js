'use strict';

const formatServicesLinks = require('./formatServicesLinks.js');

test('форматирует конфг ссылок сервисов', () => {
    expect(formatServicesLinks({
        service: {
            url: 'https://service',
        },
        other: {
            url: 'https://other',
        },
    })).toEqual({
        'service-url': 'https://service',
        'other-url': 'https://other',
    });
});
