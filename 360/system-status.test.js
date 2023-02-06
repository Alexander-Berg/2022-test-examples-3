'use strict';

const systemStatus = require('./system-status');

let core;

beforeEach(function() {
    core = {
        request: jest.fn()
    };
});

test('correct fetches data from working services', function() {
    core.request
        .mockImplementationOnce(() => Promise.resolve({ problem: '', reload_timestamp: 'Mars' }))
        .mockImplementationOnce(() => Promise.resolve({ db_status: 'Sun' }));

    return systemStatus({}, core)
        .then((res) => {
            expect(core.request.mock.calls).toEqual([
                [ 'bunker-status' ],
                [ 'db-ro-status' ]
            ]);

            expect(res).toEqual({
                'problem': '',
                'db-status': 'Sun',
                'reload_timestamp': 'Mars'
            });
        });
});

test('not throws an error when any of services is down', function() {
    core.request.mockImplementationOnce(() => Promise.reject())
        .mockImplementationOnce(() => Promise.reject());

    return expect(systemStatus({}, core)).resolves.toEqual({
        'problem': '',
        'db-status': 'rw',
        'reload_timestamp': undefined
    });
});

test('answers with problem when db-ro-status is \'ro\'', function() {
    core.request
        .mockImplementationOnce(() => Promise.resolve({ problem: '', reload_timestamp: '135' }))
        .mockImplementationOnce(() => Promise.resolve({ db_status: 'ro' }));

    return expect(systemStatus({}, core)).resolves.toEqual({
        'problem': '%db_ro',
        'db-status': 'ro',
        'reload_timestamp': '135'
    });
});
