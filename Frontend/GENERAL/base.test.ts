import debugFactory, { IDebugger } from 'debug';
import { Request } from 'express';

import BaseController from './base';

class TestBaseController extends BaseController {
    getDebugWithTrace(req: Request) {
        return this._getDebugWithTrace(req);
    }
}

describe('BaseController', () => {
    let debug: IDebugger;

    beforeEach(() => {
        debug = debugFactory('test');
    });

    describe('.getHandler', () => {
        it('should throw Error with "not implemented" message', () => {
            const controller = new TestBaseController({ debug });

            const expected = 'not implemented';

            expect(() => controller.getHandler()).toThrow(expected);
        });
    });

    describe('._getDebugWithTrace', () => {
        it('should return function', () => {
            const controller = new TestBaseController({ debug });

            const expected = 'function';

            const actual = controller.getDebugWithTrace({ id: 'fake-id' } as Request);

            expect(typeof actual).toEqual(expected);
        });

        it('should return extended debug', () => {
            jest.spyOn(debug, 'extend');

            const controller = new TestBaseController({ debug });

            const expected = 'test:fake-id';

            const actual = controller.getDebugWithTrace({ id: 'fake-id' } as Request);

            expect(debug.namespace).toEqual('test');
            expect(debug.extend).toHaveBeenCalledTimes(1);
            expect(debug.extend).toHaveBeenCalledWith('fake-id');

            expect(actual).not.toEqual(debug);
            expect(actual.namespace).toEqual(expected);
        });

        it('should return new debug instance on every call', () => {
            jest.spyOn(debug, 'extend');

            const controller = new TestBaseController({ debug });

            const expected1 = 'test:fake-id-1';
            const expected2 = 'test:fake-id-2';

            const actual1 = controller.getDebugWithTrace({ id: 'fake-id-1' } as Request);
            const actual2 = controller.getDebugWithTrace({ id: 'fake-id-2' } as Request);

            expect(debug.extend).toHaveBeenCalledTimes(2);
            expect(debug.extend).toHaveBeenCalledWith('fake-id-1');
            expect(debug.extend).toHaveBeenCalledWith('fake-id-2');

            expect(actual1).not.toEqual(actual2);

            expect(actual1.namespace).toEqual(expected1);
            expect(actual2.namespace).toEqual(expected2);
        });
    });
});
