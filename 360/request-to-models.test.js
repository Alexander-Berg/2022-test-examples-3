'use strict';

const mockCastParams = jest.fn();
const request2Models = require('./request-to-models.js');

jest.mock('./cast-params', () => mockCastParams);

test('использует params.models при их наличии', () => {
    const models = request2Models({ models: [ { name: 'do-1' } ] });

    expect(models).toEqual([ { name: 'do-1' } ]);
    expect(mockCastParams).toHaveBeenCalledWith({ name: 'do-1' });
});

test('обрезает модели, если их больше 100', () => {
    const requestModels = [];
    for (let i = 1; i <= 101; i++) {
        requestModels.push({ name: 'do-' + i });
    }
    const models = request2Models({ models: requestModels });

    expect(models[0]).toEqual({ name: 'do-1' });
    expect(models[99]).toEqual({ name: 'do-100' });
    expect(models).toHaveLength(100);
    expect(mockCastParams).toHaveBeenCalledTimes(100);
});

describe('создает правильное число моделей по параметрам', () => {
    it('если нет параметров', () => {
        const models = request2Models({});

        expect(models).toHaveLength(0);
    });

    it('для одного индекса', () => {
        const models = request2Models({
            '_model.0': 'do-1',
            'param.0': 'value'
        });

        expect(models).toHaveLength(1);
    });

    it('для нескольких индексов', () => {
        const models = request2Models({
            '_model.0': 'do-1',
            'param.1': 'value'
        });

        expect(models).toHaveLength(2);
    });

    it('для индекса > 100', () => {
        const models = request2Models({
            '_model.101': 'do-101'
        });

        expect(models).toHaveLength(0);
    });

    it('для неправильного запроса', () => {
        const models = request2Models({
            '_model.0': 'do-1',
            'param.': 'value',
            'param.a': 'value'
        });

        expect(models).toHaveLength(1);
    });
});

test('устанавливает параметр в params', () => {
    const models = request2Models({
        '_model.0': 'model1',
        'param.0': '123'
    });

    expect(models[0].params).toEqual({
        param: '123'
    });
});

describe('правильно обрабатывает параметры.', () => {

    it('обрабатывает `_model` как имя а не как параметр', () => {
        const models = request2Models({
            '_model.0': 'do-1'
        });

        expect(models[0].name).toEqual('do-1');
    });

    it('игнорирует параметры с `_` префиксом', () => {
        const models = request2Models({
            '_model.0': 'do-1',
            '_param.0': 'value'
        });

        expect(models[0].params).not.toHaveProperty('_param');
    });
});

describe('правильно обрабатывает мусорные параметры', () => {

    it('отбрасывает параметры без модели', () => {
        const models = request2Models({
            'param.0': 'p0',
            '_model.1': 'model1',
            'param.1': 'p1',
            'param.2': 'p2'
        });

        expect(models).toEqual([
            undefined,
            {
                name: 'model1',
                params: {
                    param: 'p1'
                }
            },
            undefined
        ]);
    });

    it('отбрасывает параметры без модели для json-запросов и модели из exclude', () => {
        const models = request2Models({
            models: [
                {
                    name: ''
                },
                {
                    name: 'model1',
                    params: {
                        param: 'p1'
                    }
                },
                {
                    params: {
                        param: 'v2'
                    }
                },
                {
                    name: null
                },
                null,
                {
                    name: 'model2'
                }
            ]
        }, [ 'model2' ]);

        expect(models).toEqual([
            undefined,
            {
                name: 'model1',
                params: {
                    param: 'p1'
                }
            },
            undefined,
            undefined,
            undefined,
            undefined
        ]);
    });
});
