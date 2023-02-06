'use strict';

const hideParamInLog = require('./hide-param-in-log.js');

const clone = (obj) => JSON.parse(JSON.stringify(obj));

const getCtx = () => ({
    core: {
        params: {
            '_model.0': 'do-collector-check',
            'password.0': '123456',
            '_model.1': 'do-filters-edit',
            'password.1': '1234test',
            'field.1': 'normal',
            _uid: '122344'
        }
    },
    model: 'do-collector-check',
    params: { password: '123456' },
});

test('если передали параметр для вырезания, то он должен вырезаться из итоговых параметров в логах', function() {
    const { core, params, model } = getCtx();
    hideParamInLog(core, params, model, 'password');

    expect(clone(params)).toEqual({});
    expect(core.params).not.toHaveProperty('password.0');
});

test('если не передали параметр, то набор параметров не меняется', function() {
    const { core, params, model } = getCtx();
    hideParamInLog(core, params, model);

    expect(clone(params)).toEqual(getCtx().params);
    expect(clone(core.params)).toEqual(getCtx().core.params);
});

test('не должен убирать одноименный параметр у других моделей', function() {
    const { core, params, model } = getCtx();
    hideParamInLog(core, params, model, 'password');

    expect(core.params).toHaveProperty([ 'password.1' ], '1234test');
});

test('должен нормально фильтровать два параметра в одной модели', function() {
    const params = {
        'a-password': '1234',
        'b-password': '1232'
    };
    const core = {
        params: {
            '_model.0': 'do-collector-check',
            'a-password.0': '1234',
            'b-password.0': '1232'
        }
    };
    const model = 'do-collector-check';

    hideParamInLog(core, params, model, 'a-password');
    hideParamInLog(core, params, model, 'b-password');

    expect(clone(params)).toEqual({});
    expect(core.params).not.toHaveProperty('a-password.0');
    expect(core.params).not.toHaveProperty('b-password.0');
});

test('если передали placeholder, то он должен быть вместо значения', function() {
    const { core, params, model } = getCtx();
    hideParamInLog(core, params, model, 'password', 'XXX');

    expect(clone(params)).toHaveProperty('password', 'XXX');
    expect(clone(core.params)).toHaveProperty([ 'password.0' ], 'XXX');
});

test('если не передали модель, то не меняет core.params', function() {
    const { core, params } = getCtx();
    hideParamInLog(core, params, null, 'password');

    expect(clone(params)).not.toHaveProperty('password');
    expect(core.params).toHaveProperty([ 'password.0' ], '123456');
});
