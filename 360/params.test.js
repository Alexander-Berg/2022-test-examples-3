'use strict';

const Params = require('./params.js');

test('должен выдавать правильные параметры при инициализации ядром с параметрами', () => {
    const req = {
        query: {
            query_test: 'query_test'
        },
        body: {
            body_test: 'body_test'
        }
    };

    expect((new Params({ req })).query_test).toEqual('query_test');
    expect((new Params({ req })).body_test).toEqual('body_test');
});

test('должен переопределять GET-параметры POST-параметрами, если есть параметры с одинаковыми названиями', () => {
    const req = {
        query: {
            query_test: 'query_test',
            test_override: 'get'
        },
        body: {
            body_test: 'body_test',
            test_override: 'post'
        }
    };

    expect((new Params({ req })).query_test).toEqual('query_test');
    expect((new Params({ req })).body_test).toEqual('body_test');
    expect((new Params({ req })).test_override).toEqual('post');
});
