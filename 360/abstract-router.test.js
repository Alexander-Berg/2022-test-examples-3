'use strict';

const AssertionError = require('assert').AssertionError;

const AbstractRouter = require('./abstract-router.js');
const ApiError = require('./api-error.js');
const overrideMethods = require('../../test/helpers/override-methods.js');

let core;
let route;
let sendResponse;

beforeEach(() => {
    sendResponse = jest.fn();

    core = {
        req: {},
        res: {
            set: jest.fn(),
            status: jest.fn(() => ({
                send: sendResponse
            }))
        },
        timing: {
            start: jest.fn(),
            stop: jest.fn()
        },
        yasm: {
            sum: jest.fn(),
            hist: jest.fn()
        },
        console: {
            error: jest.fn()
        }
    };

    class MyRouter extends AbstractRouter {
        prepareCore() {
            return core;
        }
    }

    route = new MyRouter('test', null);
});

describe('#constructor', () => {
    it('должен выдавать ошибку, если не передано имя', () => {
        let route;

        try {
            route = new AbstractRouter();
        } catch (err) {
            expect(err).toBeInstanceOf(AssertionError);
            expect(err.message).toEqual('`name` parameter is required');
        }

        if (route) {
            throw new Error('должен выдавать ошибку');
        }
    });

    it('должен принимать дефолтный mime type, если type не передан', () => {
        const route = new AbstractRouter('new test');
        expect(route.type).toEqual('application/json');
    });

    it('должен выдавать ошибку, если передан не корректный type', () => {
        let route;

        try {
            route = new AbstractRouter('new test', 'test type');
        } catch (err) {
            expect(err).toBeInstanceOf(AssertionError);
            expect(err.message).toEqual('invalid mime type');
        }

        if (route) {
            throw new Error('должен выдавать ошибку');
        }
    });
});

describe('#route', () => {
    it('если в #_route возникла не обработанная ошибка, то она должна обрабатываться в #_exception', async () => {
        const err = new Error('test error');

        overrideMethods(
            route,
            [ '_exception', '_headers', '_error', '_end', '_finish' ],
            {
                _headers: () => {
                    throw new Error('pass error');
                },
                _finish: () => {
                    throw err;
                }
            }
        );
        jest.spyOn(route, '_exception');

        await route.router(core.req, core.res);

        expect(route._exception).toBeCalledWith(err);
    });
});

describe('#_router', () => {
    beforeEach(() => {
        overrideMethods(route, [ '_headers', '_end', '_finish', '_error', '_escape', 'prepare' ]);
        [ '_headers', 'prepare', 'process', '_end', '_finish', '_error', '_escape' ].reduce((acc, method) => {
            acc[method] = jest.spyOn(route, method);
            return acc;
        }, {});
    });

    it('должен вызвать все необходимые функции при нормальном запросе', async () => {
        await route._router(core.req, core.res);

        [ '_headers', 'prepare', 'process', '_end', '_finish' ].forEach((funcName) => {
            expect(route[funcName]).toHaveBeenCalled();
        });
    });

    it('если возникла ошибка при обработке запроса, то должен вызвать #_error', async () => {
        route._headers = () => {
            throw new Error('test error');
        };

        await route._router(core.req, core.res);

        expect(route._error).toHaveBeenCalled();
    });

    it('если возникла ошибка при вызове #_error, то должен послать 500', async () => {
        route._headers = route._error = () => {
            throw new Error('test error');
        };

        await route._router(core.req, core.res);

        expect(core.res.status).toHaveBeenCalledWith(500);
        // expect(core.res.send).to.be.calledWith({
        //     error: 'COMMON_REQUEST_EXCEPTION'
        // });
    });

    it('должен верно регистрировать сообщение об ошибках', async () => {
        route._headers = route._error = route._end = () => {
            throw new Error('test error');
        };

        await route._router(core.req, core.res);

        const args = route._finish.mock.calls[0];
        // Второй аргумент при вызове #_finish - сообщение об ошибке
        expect(args[1]).toEqual('test error test error test error');
    });
});

describe('#error', () => {
    it('должен залогировать ошибку и отправить 500', () => {
        const err = { message: 'test msg', stack: 'test stack' };

        route.error(core, err);

        expect(core.console.error).toHaveBeenCalledWith('COMMON_REQUEST_ERROR', err);

        expect(core.res.status).toHaveBeenCalledWith(500);
        expect(sendResponse).toHaveBeenCalledWith({
            error: 'COMMON_REQUEST_ERROR'
        });
    });

    it('должен вернуть поле "code" при возникновении соответствующей ApiError', () => {
        const err = new ApiError(403, {
            errorCode: 666,
            message: 'test msg',
            stack: 'test stack',
            reason: 'test reason'
        });

        route.error(core, err);

        expect(core.console.error)
            .toHaveBeenCalledWith('API_ERROR', { message: 'test msg', reason: 'test reason' });

        expect(core.res.status).toHaveBeenCalledWith(403);
        expect(sendResponse).toHaveBeenCalledWith({
            error: 'API_ERROR',
            code: 666,
            message: 'test msg'
        });
    });
});

describe('#_error', () => {
    let err;

    beforeEach(() => {
        err = { message: 'test msg', stack: 'test stack' };
    });

    it('должен вызвать #error текущего экземпляра, если он есть', () => {
        const a = new AbstractRouter('new test');
        a.error = () => true;

        jest.spyOn(a, 'error');
        jest.spyOn(AbstractRouter.prototype, 'error');

        a._error(core, err);

        expect(a.error).toHaveBeenCalled();
        expect(AbstractRouter.prototype.error).not.toHaveBeenCalled();
    });

    it('должен закончить цепочку вызовов, если дошёл до AbstractRouter.prototype', () => {
        // eslint-disable-next-line no-proto
        AbstractRouter.prototype.__proto__ = {
            error() {
                return true;
            }
        };

        AbstractRouter.prototype.error = jest.fn(() => false);
        // eslint-disable-next-line no-proto
        jest.spyOn(AbstractRouter.prototype.__proto__, 'error');

        const a = new AbstractRouter('new test');
        a._error(core, err);

        expect(AbstractRouter.prototype.error).toHaveBeenCalled();
        // eslint-disable-next-line no-proto
        expect(AbstractRouter.prototype.__proto__.error).not.toHaveBeenCalled();
    });

    it('должен вызвать #error из прототипа', () => {
        const a = new AbstractRouter('new test');

        jest.spyOn(AbstractRouter.prototype, 'error');

        a._error(core, err);

        expect(AbstractRouter.prototype.error).toHaveBeenCalled();
    });

    it('должен вызвать #error из ближайшего прототипа', () => {
        class B extends AbstractRouter {
            error() {
                return true;
            }
        }

        const b = new B('test b');

        jest.spyOn(AbstractRouter.prototype, 'error');
        jest.spyOn(B.prototype, 'error');

        b._error(core, err);

        expect(B.prototype.error).toHaveBeenCalled();
        expect(AbstractRouter.prototype.error).not.toHaveBeenCalled();
    });

    it('должен идти по #error прототипов пока не получит true', () => {
        class B extends AbstractRouter {
            error() {
                return true;
            }
        }

        class C extends B {
            error() {
                return false;
            }
        }

        class D extends C {
            error() {
                return false;
            }
        }

        jest.spyOn(AbstractRouter.prototype, 'error');
        jest.spyOn(B.prototype, 'error');
        jest.spyOn(C.prototype, 'error');
        jest.spyOn(D.prototype, 'error');

        const d = new D('new test');
        d._error(core, err);

        expect(D.prototype.error).toHaveBeenCalled();
        expect(C.prototype.error).toHaveBeenCalled();
        expect(B.prototype.error).toHaveBeenCalled();
        expect(AbstractRouter.prototype.error).not.toHaveBeenCalled();
    });

    it('должен перешагивать через прототипы, у которых нет #error', () => {
        class B extends AbstractRouter {
            error() {
                return true;
            }
        }

        class C extends B {}

        class D extends C {
            error() {
                return false;
            }
        }

        jest.spyOn(AbstractRouter.prototype, 'error');
        jest.spyOn(B.prototype, 'error');
        jest.spyOn(D.prototype, 'error');

        const d = new D('new test');
        d._error(core, err);

        expect(D.prototype.error).toHaveBeenCalled();
        expect(B.prototype.error).toHaveBeenCalled();
        expect(AbstractRouter.prototype.error).not.toHaveBeenCalled();
    });
});

describe('#_headers', () => {
    it('должен вернуть правильные заголовки', () => {
        expect(route._headers()).toEqual({
            'Content-Type': 'application/json; charset=utf-8',
            'Cache-Control': 'max-age=0, must-revalidate, proxy-revalidate, no-cache, no-store, private',
            'Expires': 'Thu, 01 Jan 1970 00:00:01 GMT',
            'Pragma': 'no-cache'
        });
    });
});
