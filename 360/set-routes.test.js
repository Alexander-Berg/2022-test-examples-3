'use strict';

const mockRequireList = jest.fn();
const mockLog = jest.fn();
const mockSum = jest.fn();
jest.mock('express', () => ({ Router: jest.fn() }));
jest.mock('../lib/options.js', () => ({ routes: 'fake-path' }));
jest.mock('./require-list.js', () => mockRequireList);
jest.mock('./console.js', () => ({
    log: mockLog,
    error: mockLog,
    warning: mockLog
}));
jest.mock('@duffman-int/core', () => ({
    yasm: { sum: mockSum }
}));

jest.mock('./fake-path', () => 42, { virtual: true });
jest.mock('./fake-path-es6', () => ({ __esModule: true, default: 13 }), { virtual: true });

const setRoutes = require('./set-routes.js');

test('it works', () => {
    mockRequireList.mockReturnValue({
        a: {
            routes: []
        },
        b: {
            global: true,
            routes: []
        }
    });
    const app = { use: jest.fn() };
    setRoutes(app);
    expect(app.use).toHaveBeenCalledWith('/a', {});
});

test('it accepts middleware', () => {
    mockRequireList.mockReturnValue({
        a: {
            global: true,
            routes: [ {
                name: 'name-a',
                middleware: 'mw-a'
            } ]
        },
        b: {
            __esModule: true,
            default: {
                global: true,
                routes: [ {
                    name: 'name-b',
                    middleware: 'mw-b'
                } ]
            }
        }
    });
    const app = { use: jest.fn() };
    setRoutes(app);
    expect(app.use).toHaveBeenCalledWith('name-a', 'mw-a');
    expect(app.use).toHaveBeenCalledWith('name-b', 'mw-b');
});

test('it accepts path', () => {
    mockRequireList.mockReturnValue({
        b: {
            global: true,
            routes: [ {
                name: 'fake',
                path: './fake-path'
            }, {
                name: 'fake-es6',
                path: './fake-path-es6'
            } ]
        }
    });
    const app = { use: jest.fn() };
    setRoutes(app);
    expect(app.use).toHaveBeenCalledWith('fake', 42);
    expect(app.use).toHaveBeenCalledWith('fake-es6', 13);
});

test('it sends errors', () => {
    mockRequireList.mockReturnValue({
        b: {
            global: true,
            routes: [ {
                name: 'fake',
                middleware: 42
            } ]
        }
    });
    const app = { use: jest.fn(() => { throw new Error('test'); }) };
    setRoutes(app);
    expect(app.use).toHaveBeenCalledWith('fake', 42);
    expect(mockLog).toHaveBeenCalledWith('ROUTES_INIT_ERROR', expect.anything());
    expect(mockSum).toHaveBeenCalledWith('duffman_routes_init_error');
    expect(mockSum).toHaveBeenCalledWith('duffman_routes_init_error.fake');
});

test('it exits if no routes', () => {
    const mockExit = jest.spyOn(process, 'exit').mockImplementation(() => {});
    mockRequireList.mockReturnValue({});
    const app = { use: jest.fn() };
    setRoutes(app);
    expect(mockExit).toHaveBeenCalledWith(1);
    expect(mockLog).toHaveBeenCalledWith('ROUTES_INIT_ERROR', expect.anything());
});
