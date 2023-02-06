describe.skip('currency loader', () => {
    const chai = require('chai');
    const sinon = require('sinon');
    const fs = require('fs');

    const currencyLoader = require('../../../utils/currency-loader');
    const request = require('request');

    chai.should();

    const currencyXML = fs.readFileSync('test/unit/currency/all.xml', { encoding: 'utf-8' });

    let getStub;

    beforeEach((done) => {
        getStub = sinon.stub(request, 'get', (url, callback) => {
            callback(null, { responseCode: 200 }, currencyXML);
        });
        currencyLoader.load().then(done);
    });

    afterEach(() => {
        request.get.restore();
    });

    it('should rerun loader with retry interval, if we have an error', (done) => {
        const timeStub = sinon.stub(global, 'setTimeout');

        request.get.restore();
        const getStubWithError = sinon.stub(request, 'get', (url, callback) => {
            callback('Error!', { responseCode: 200 }, currencyXML);
        });

        currencyLoader.load().then(() => {
            timeStub.calledWith(currencyLoader.load, 10 * 60 * 1000).should.equal(true);

            global.setTimeout.restore();

            done();
        });
    });

    it('should rerun loader with reload interval, if we have an error', (done) => {
        const timeStub = sinon.stub(global, 'setTimeout');

        currencyLoader.load().then(() => {
            timeStub.calledWith(currencyLoader.load, 60 * 60 * 1000).should.equal(true);

            global.setTimeout.restore();

            done();
        });
    });

    it('should contain currencies', () => {
        const currencies = currencyLoader.getRates();
        currencies.should.contain.keys('USD', 'EUR', 'TL', 'RUB', 'UAH', 'KZT', 'AZN');
    });

    it('should have correct rates for one-to-one currencies', () => {
        const currencies = currencyLoader.getRates();
        currencies.RUB.should.equal(1);
        currencies.USD.should.be.within(0.019004181, 0.019011407);
        currencies.EUR.should.be.within(0.015547264, 0.015549681);
        currencies.AZN.should.be.within(0.014883167, 0.014885383);
    });

    it('should have correct rates for one-to-more currencies', () => {
        const currencies = currencyLoader.getRates();
        currencies.UAH.should.be.within(0.3003003, 0.30120482);
        currencies.KZT.should.be.within(3.4482759, 3.5714286);
        currencies.BYR.should.be.within(204.08163, 208.33333);
        currencies.CNY.should.be.within(0.11778563, 0.11792453);
    });

    it('should run callbacks after loading rates', (done) => {
        const callback1 = sinon.spy();
        const callback2 = sinon.spy();

        currencyLoader.onLoad(callback1);
        currencyLoader.onLoad(callback2);

        currencyLoader.load().then(() => {
            callback1.calledOnce.should.be.true;
            callback2.calledOnce.should.be.true;
            done();
        });
    });
});
