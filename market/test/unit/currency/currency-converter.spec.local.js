describe.skip('currency converter', () => {
    const chai = require('chai');
    const sinon = require('sinon');
    const fs = require('fs');

    const currencyConverter = require('../../../utils/currency-converter');
    const currencyLoader = require('../../../utils/currency-loader');
    const request = require('request');

    chai.should();

    const currencyXML = fs.readFileSync('test/unit/currency/all.xml', { encoding: 'utf-8' });

    let getStub;

    beforeEach((done) => {
        sinon.stub(fs, 'writeFile');
        getStub = sinon.stub(request, 'get', (url, callback) => {
            callback(null, { responseCode: 200 }, currencyXML);
        });
        currencyLoader.load().then(done);
    });

    afterEach(() => {
        request.get.restore();
        fs.writeFile.restore();
    });

    it('should get rates', () => {
        const currencies = currencyConverter.fx.rates;
        currencies.RUB.should.equal(1);
        currencies.USD.should.be.within(0.019004181, 0.019011407);
        currencies.EUR.should.be.within(0.015547264, 0.015549681);
        currencies.AZN.should.be.within(0.014883167, 0.014885383);
        currencies.UAH.should.be.within(0.3003003, 0.30120482);
        currencies.KZT.should.be.within(3.4482759, 3.5714286);
        currencies.BYR.should.be.within(204.08163, 208.33333);
        currencies.CNY.should.be.within(0.11778563, 0.11792453);
    });

    describe('convert prices', () => {
        it('USD to RUB', () => {
            currencyConverter.convertPrice(2.5, 'USD', 'RUB').should.be.within(131.53, 132.54);
        });

        it('USD to RUR', () => {
            currencyConverter.convertPrice(2.5, 'USD', 'RUR').should.within(131.53, 132.54);
        });

        it('RUB to USD', () => {
            currencyConverter.convertPrice(2600, 'RUB', 'USD').should.be.within(48.41, 49.42);
        });

        it('RUR to USD', () => {
            currencyConverter.convertPrice(2600, 'RUR', 'USD').should.be.within(48.41, 49.42);
        });

        it('BYR to RUR', () => {
            currencyConverter.convertPrice(2800000, 'BYR', 'RUR').should.within(9478.9, 13479.93);
        });

        it('RUB to BYR', () => {
            currencyConverter.convertPrice(2600, 'RUB', 'BYR').should.within(540102, 740112);
        });

        it('BYR to EUR', () => {
            currencyConverter.convertPrice(2800000, 'BYR', 'EUR').should.within(208, 210);
        });

        it('USD to BYR', () => {
            currencyConverter.convertPrice(2600, 'USD', 'BYR').should.within(28417955, 28417995);
        });
    });
});
