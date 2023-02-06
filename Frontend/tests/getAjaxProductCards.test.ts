import sinon from 'sinon';
import { getAjaxProductCards } from '../.';

describe('Product cards ajax helper', () => {
    const server = sinon.fakeServer.create();
    // @ts-ignore
    global.XMLHttpRequest = sinon.FakeXMLHttpRequest;

    it('should call success callback', () => {
        const onSuccessCallback = sinon.stub();
        const onErrorCallback = sinon.stub();

        getAjaxProductCards({ lr: 12, queryText: 'text', isTest: false }, onSuccessCallback, onErrorCallback);

        server.respondWith('{ "data": 1 }');
        server.respond();

        sinon.assert.calledOnce(onSuccessCallback);
        sinon.assert.notCalled(onErrorCallback);
    });

    it('should retry request', () => {
        const onSuccessCallback = sinon.stub();
        const onErrorCallback = sinon.stub();

        getAjaxProductCards({ lr: 12, queryText: 'text', isTest: false }, onSuccessCallback, onErrorCallback);

        server.respondWith('{invalid server side json');
        server.respond();

        server.respondWith('{ "data": 1 }');
        server.respond();

        sinon.assert.calledOnce(onSuccessCallback);
        sinon.assert.notCalled(onErrorCallback);
    });

    it('should retry request, if server respond without field "data"', () => {
        const onSuccessCallback = sinon.stub();
        const onErrorCallback = sinon.stub();

        getAjaxProductCards({ lr: 12, queryText: 'text', isTest: false }, onSuccessCallback, onErrorCallback);

        server.respondWith('{}');
        server.respond();

        server.respondWith('{ "data": 1 }');
        server.respond();

        sinon.assert.calledOnce(onSuccessCallback);
        sinon.assert.notCalled(onErrorCallback);
    });

    it('should call failure callback', () => {
        const onSuccessCallback = sinon.stub();
        const onErrorCallback = sinon.stub();

        getAjaxProductCards({ lr: 12, queryText: 'text', isTest: false }, onSuccessCallback, onErrorCallback);

        server.respondWith('{invalid server side json');
        server.respond();

        server.respondWith([500, {}, '']);
        server.respond();

        server.respondWith([400, {}, '']);
        server.respond();

        server.respondWith('{invalid server side json');
        server.respond();

        sinon.assert.notCalled(onSuccessCallback);
        sinon.assert.calledOnce(onErrorCallback);
    });
});
