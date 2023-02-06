'use strict';

jest.mock('./settings_boolean.json', () => [ 'testBooleanSettingsName' ]);
jest.mock('./settings_number.json', () => [ 'testNumberSettingsName' ]);
jest.mock('./settings_json.json', () => [ 'testJsonSettingsName', 'testJsonSettingsNameInvalid' ]);
jest.mock('./settings_remove.json', () => [ 'disable_etickets', 'testRemoveSettingName' ]);

const filter = require('./filter-all.js');

let core;
let settings;

beforeEach(function() {
    core = {
        req: {
            cookies: {}
        }
    };
});

describe('приведение настройки "color_scheme" ->', function() {
    it('тема "mac" должна превращаться в "colorful"', function() {
        const result = filter(core, {
            color_scheme: 'mac'
        });

        expect(result.color_scheme).toEqual('colorful');
        expect(result['colorful-theme-skin']).toEqual('gray');
    });

    it('тема "white" должна превращаться в "colorful"', function() {
        const result = filter(core, {
            color_scheme: 'white'
        });

        expect(result.color_scheme).toEqual('colorful');
        expect(result['colorful-theme-skin']).toEqual('white');
    });
});

describe('приведение настройки "colorful-theme-skin"', function() {
    it('если выставлена настройка скина mac, то сразу ставим ее в gray', function() {
        const result = filter(core, {
            'colorful-theme-skin': 'mac'
        });
        expect(result['colorful-theme-skin']).toEqual('gray');
    });
    it('если стоял любой скин, отличный от mac, ничего не меняем', function() {
        const result = filter(core, {
            'colorful-theme-skin': 'green'
        });
        expect(result['colorful-theme-skin']).toEqual('green');
    });
});

describe('приведение настройки "disable_inboxattachs" ->', function() {

    it('преобразует disable_inboxattachs="experiment-on" в ""', function() {
        const result = filter(core, {
            disable_inboxattachs: 'experiment-on'
        });

        expect(result).toEqual({
            disable_inboxattachs: false
        });
    });

});

describe('приведение к 2 новым виджетным настройкам ->', function() {

    describe('нет настройки "disable_etickets" ->', function() {

        beforeEach(function() {
            settings = {
                disable_aviaeticket: 'on'
            };
        });

        it('должен заменить "disable_aviaeticket" на 2 новые настройки', function() {
            expect(filter(core, settings)).toEqual({
                show_widgets_decor: false,
                show_widgets_buttons: false
            });
        });

    });

    describe('есть настройка "disable_etickets" ->', function() {

        beforeEach(function() {
            settings = {
                disable_aviaeticket: 'on',
                disable_etickets: 'on'
            };
        });

        it('должен удалить "disable_aviaeticket" и "disable_etickets" из выдачи', function() {
            expect(filter(core, settings)).toEqual({
                show_widgets_decor: false,
                show_widgets_buttons: false
            });
        });

    });

    describe('есть настройка "disable_etickets" c пустым значением ->', function() {

        beforeEach(function() {
            settings = {
                disable_etickets: ''
            };
        });

        it('должен удалить "disable_aviaeticket" и "disable_etickets" из выдачи', function() {
            expect(filter(core, settings)).toEqual({});
        });

    });

});

describe('удаляет настройки', () => {
    describe('для тестирования', function() {
        beforeEach(function() {
            settings = {
                'some-test-setting': true,
                'anotherOne': 'isSet',
                'last_setting': 'value'
            };
        });

        it('список настроек не должен измениться, если нет куки debug-settings-remove', function() {
            expect(filter(core, settings)).toEqual(settings);
        });

        it('список настроек не должен измениться, если нет кука debug-settings-remove не имеет значения', function() {
            core.req.cookies['debug-settings-delete'] = '';
            expect(filter(core, settings)).toEqual(settings);
        });

        it('из списка настроек должна удалиться настройка, ключ которой равен куке debug-settings-remove', function() {
            core.req.cookies['debug-settings-delete'] = 'some-test-setting';
            const filteredSettings = Object.assign({}, settings);
            delete filteredSettings['some-test-setting'];

            expect(filter(core, settings)).toEqual(filteredSettings);
        });

        it('из списка настроек должны удалиться настройки, ключи которых в куке debug-settings-remove', function() {
            core.req.cookies['debug-settings-delete'] = 'some-test-setting,anotherOne';
            const filteredSettings = Object.assign({}, settings);
            delete filteredSettings['some-test-setting'];
            delete filteredSettings.anotherOne;

            expect(filter(core, settings)).toEqual(filteredSettings);
        });
    });

    it('из settings_remove.json', () => {
        const result = filter(core, {
            good: 'on',
            testRemoveSettingName: 'on'
        });

        expect(result).toEqual({
            good: 'on'
        });
    });

    it('переданные в filters', () => {
        const result = filter(core, {
            good: 'on',
            oneMoreTestRemoveSettingName: 'on'
        }, {
            exclude: [ 'oneMoreTestRemoveSettingName' ]
        });

        expect(result).toEqual({
            good: 'on'
        });
    });

    it('которые никто не просит', () => {
        const result = filter(core, {
            good: 777,
            bad: 666
        }, {
            include: [ 'good' ]
        });

        expect(result).toEqual({
            good: 777
        });
    });
});

test('приводит к нужному формату', () => {
    const result = filter(core, {
        testBooleanSettingsName: 'on',
        testNumberSettingsName: '123',
        testJsonSettingsName: encodeURIComponent('{"some": "value"}'),
        testJsonSettingsNameInvalid: 'invalid json'
    });

    expect(result.testBooleanSettingsName).toBeTruthy();
    expect(result.testNumberSettingsName).toEqual(123);

    expect(result.testJsonSettingsName).toEqual({ some: 'value' });
    expect(result.testJsonSettingsNameInvalid).toBeNull();
});
