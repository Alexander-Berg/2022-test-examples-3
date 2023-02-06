'use strict';

jest.mock('./protected-settings.json', () => [ 'testProtectedSettingName' ]);

const setRequest = require('./request-set.js');

test('возвращает ожидаемую структуру', () => {
    expect(setRequest({})).toEqual({
        update_params: {},
        update_profile: {}
    });
});

test('возвращает false если есть невалидные параметры', () => {
    expect(setRequest({
        'name=': 'test'
    })).toEqual(false);
});

test('возвращает false если есть несериализуемый параметр', () => {
    expect(setRequest({
        name: Symbol('test')
    })).toEqual(false);
});

test('отфильтровывает profile параметры', () => {
    expect(setRequest({
        emails: 'test',
        name: 'test'
    })).toEqual({
        update_params: { name: 'test' },
        update_profile: { emails: 'test' }
    });
});

test('обнуляет signature, если сохраняются подписи', () => {
    expect(setRequest({
        signs: []
    })).toEqual({
        update_params: {},
        update_profile: {
            signs: [],
            signature: ''
        }
    });
});

test('игнориреут настройки, изменение которых запрещено', () => {
    expect(setRequest({
        emails: 'testEmail',
        name: 'testName',
        testProtectedSettingName: 'protected!'
    })).toEqual({
        update_params: {
            name: 'testName'
        },
        update_profile: {
            emails: 'testEmail'
        }
    });
});
