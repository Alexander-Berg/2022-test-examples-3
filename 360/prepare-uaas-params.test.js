'use strict';

jest.mock('./get-location.js', () => () => 'FAKE_LOCATION');

const prepareUaasParams = require('./prepare-uaas-params.js');

jest.useFakeTimers('modern');
jest.setSystemTime(1580554500751);

const core = {
    yasm: {
        sum: jest.fn()
    },
    params: {},
    config: {
        USER_IP: 'FAKE_IP'
    }
};

test('without params', () => {
    core.params = {};
    expect(prepareUaasParams(core)).toMatchSnapshot();
});

test('with uuid', () => {
    core.params = { uuid: 'deadbeef' };
    expect(prepareUaasParams(core)).toMatchSnapshot();
});

test('with strange uuid', () => {
    core.params = { uuid: { value: 'deadbeef' } };
    expect(prepareUaasParams(core)).toMatchSnapshot();
    expect(core.yasm.sum.mock.calls).toMatchSnapshot();
});

test('with test-id', () => {
    core.params = { 'test-id': 12345 };
    expect(prepareUaasParams(core)).toMatchSnapshot();
});

test('with device-id', () => {
    core.params = { 'device-id': '12345' };
    expect(prepareUaasParams(core)).toMatchSnapshot();
});

[ undefined, 'mobilemail', 'mobilepayment' ].forEach((handler) => {
    [ 'iphone', 'ipad', 'aphone', 'apad' ].forEach((client) => {
        test(`handler: ${handler}, client: ${client}`, () => {
            core.params = { handler, client };
            expect(prepareUaasParams(core)).toMatchSnapshot();
        });
    });
});
