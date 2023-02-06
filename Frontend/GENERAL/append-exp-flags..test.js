const { URL } = require('url');
const { promisify } = require('util');
const sinon = require('sinon');
const chai = require('chai');

const { expect } = chai.use(require('chai-sinon'));
chai.use(require('chai-as-promised'));

const createMiddleware = require('./append-exp-flags');

describe('append-exp-flags', () => {
    let middleware;
    let kotik;
    let req;
    let res;

    beforeEach(() => {
        middleware = promisify(createMiddleware().fn);

        kotik = {
            ctx: {
                getItems: sinon.stub(),
            },
            parsedURL: new URL('http://some.test/pathname?query=1'),
        };
        req = { kotik };
        res = {};
    });

    it('should do nothing if no expFlags set', async() => {
        await middleware(req, res);

        return expect(kotik.ctx.getItems).not.to.have.been.called;
    });

    it('should throw error if no items with type "experiments" were added', async() => {
        kotik.ctx.getItems.returns([]);
        kotik.parsedURL.searchParams.append('expFlags', JSON.stringify({ test: 1 }));

        await expect(middleware(req, res)).to.eventually.be.rejectedWith('append-exp-flags: в контексте нет данных от источника экспериментов');
    });

    it('should create flags field if it was not created', async() => {
        const expItem = { binary: {} };
        kotik.ctx.getItems.returns([expItem]);
        kotik.parsedURL.searchParams.append('expFlags', JSON.stringify({ test: 1 }));

        await middleware(req, res);

        expect(expItem.binary).to.deep.include({ flags: { test: 1 } });
    });

    it('should throw error if appending flags that are already in context', async() => {
        kotik.ctx.getItems.returns([{ binary: { flags: { test: 2, other: 'abc' } } }]);
        kotik.parsedURL.searchParams.append('expFlags', JSON.stringify({ test: 1, other: 'abc', third: true }));

        await expect(middleware(req, res)).to.eventually.be.rejectedWith('append-exp-flags: нужно переопределить флаги test, other, но они уже определены в контексте');
    });

    it('should throw error if setting same flag in different query parameters', async() => {
        kotik.ctx.getItems.returns([{ binary: { flags: {} } }]);
        kotik.parsedURL.searchParams.append('expFlags', JSON.stringify({ test: 1 }));
        kotik.parsedURL.searchParams.append('expFlags', JSON.stringify({ test: 1 }));

        await expect(middleware(req, res)).to.eventually.be.rejectedWith('append-exp-flags: нужно переопределить флаги test, но они уже определены в контексте');
    });

    it('should append flags from single query parameter "expFlags" in context', async() => {
        const expItem = { binary: { flags: { existing: 1 } } };
        kotik.ctx.getItems.returns([expItem]);
        kotik.parsedURL.searchParams.append('expFlags', JSON.stringify({ test: 1 }));

        await middleware(req, res);

        expect(expItem.binary).to.deep.equal({ flags: { test: 1, existing: 1 } });
    });

    it('should append flags from all query parameters "expFlags" in context', async() => {
        const expItem = { binary: { flags: { existing: 1 } } };
        kotik.ctx.getItems.returns([expItem]);
        kotik.parsedURL.searchParams.append('expFlags', JSON.stringify({ test: 1 }));
        kotik.parsedURL.searchParams.append('expFlags', JSON.stringify({ other: 1 }));

        await middleware(req, res);

        expect(expItem.binary).to.deep.equal({ flags: { test: 1, existing: 1, other: 1 } });
    });

    it('should append flags in every found item', async() => {
        const expItem1 = { binary: { flags: { existing: 1 } } };
        const expItem2 = { binary: { flags: { other: 1 } } };
        kotik.ctx.getItems.returns([expItem1, expItem2]);
        kotik.parsedURL.searchParams.append('expFlags', JSON.stringify({ test: 1 }));

        await middleware(req, res);

        expect(expItem1.binary).to.deep.equal({ flags: { test: 1, existing: 1 } });
        expect(expItem2.binary).to.deep.equal({ flags: { test: 1, other: 1 } });
    });
});
