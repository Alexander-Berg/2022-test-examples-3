const getYcridMiddleware = require('../../middleware/ycrid');

describe('getYcridMiddleware', () => {
    it('должна вызывать next', () => {
        const next = jest.fn();
        getYcridMiddleware({})({ headers: {} }, {}, next);
        expect(next).toBeCalled();
    });

    it('должна класть requestId в req', () => {
        const req = {
            headers: {
                'x-request-id': '12345'
            }
        };
        getYcridMiddleware({})(req, {}, () => {});
        expect(req.requestId).toEqual('12345');
    });

    it('должна фолбэчить requestId таймстемпом', () => {
        const req = { headers: {} };
        const originalNow = Date.now;
        Date.now = jest.fn(() => 1522405223068);
        getYcridMiddleware({})(req, {}, () => {});
        Date.now = originalNow;
        expect(req.requestId).toEqual('TIME162766c2a9c');
    });

    it('должна класть ycrid в req', () => {
        const req = {
            headers: {
                'x-request-id': '12345'
            }
        };
        getYcridMiddleware({
            prefix: 'prefix',
            nodeName: 'nodeName'
        })(req, {}, () => {});
        expect(req.ycrid).toEqual('prefix-12345-nodeName');
    });
});
