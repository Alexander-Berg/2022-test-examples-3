'use strict';

const request_module = require('../sendbernar.js').request;
const chai = require('chai');
const expect = chai.expect;

describe('Unit tests for sendbernar.request', () => {
    it('should make flat', () => {
        const obj = {
            str: 'str',
            number: 1,
            bool: true,
            undef: undefined,
            nul: null,
            arr: [ '1', 2, true, {} ],
            child: {
                child_str: 'child_str',
                child_child: {
                    child_nubmer: 42
                }
            }
        };

        const res = request_module.flatten(obj, {});

        expect(res).to.have.property('str').to.be.equal('str');
        expect(res).to.have.property('number').to.be.equal(1);
        expect(res).to.have.property('bool').to.be.equal(true);
        expect(res).not.to.have.property('undef');
        expect(res).not.to.have.property('nul');
        expect(res).to.have.property('arr').with.lengthOf(4);
        expect(res).to.have.property('child_str').to.be.equal('child_str');
        expect(res).to.have.property('child_nubmer').to.be.equal(42);
        expect(res).not.to.have.property('child');
    });

    it('several cases', () => {
        expect(() => {
            request_module.fill_request(request_module.make_clean_request(), 'query', {
                key: {
                    name: 'val',
                    required: false
                }
            }, { another_key: 'another_val' });
        }).to.not.throw(Error);

        expect(() => {
            request_module.fill_request(request_module.make_clean_request(), 'query', {
                key: {
                    name: 'val',
                    required: true
                }
            }, { another_key: 'another_val' });
        }).to.throw(Error);

        expect(() => {
            request_module.fill_request(request_module.make_clean_request(), 'query', {
                key: {
                    name: 'val',
                    required: true
                }
            }, { key: 'another_val' });
        }).to.not.throw(Error);
    });

    it('should make common params', () => {
        const request = request_module.common_params({
            caller: 'wmi',
            uid: '1',
            requestId: 'xreqid',
            realIp: '127.0.0.1',
            originalHost: 'mail.yandex.ru',
            another_header: 'another_header'
        }, request_module.make_clean_request());

        expect(request.query).to.have.property('caller').to.be.equal('wmi');
        expect(request.query).to.have.property('uid').to.be.equal('1');
        expect(request.headers).to.have.property('X-Request-Id').to.be.equal('xreqid');
        expect(request.headers).to.have.property('X-Real-Ip').to.be.equal('127.0.0.1');
        expect(request.headers).to.have.property('X-Original-Host').to.be.equal('mail.yandex.ru');
        expect(request).to.have.property('body').to.be.deep.equal({});
        expect(request.headers).not.to.have.property('another_header');
    });

    it('should make uj params', () => {
        const request = request_module.user_journal({
            connectionId: 'connection_id',
            expBoxes: 'X-Yandex-ExpBoxes',
            enabledExpBoxes: 'X-Yandex-EnabledExpBoxes',
            clientType: 'X-Yandex-ClientType',
            clientVersion: 'X-Yandex-ClientVersion',
            yandexUid: 'yandexuid',
            iCookie: 'icookie',
            userAgent: 'User-Agent',
            another_header: 'another_header'
        }, request_module.make_clean_request());

        expect(request.headers).to.have.property('connection_id').to.be.equal('connection_id');
        expect(request.headers).to.have.property('X-Yandex-ExpBoxes').to.be.equal('X-Yandex-ExpBoxes');
        expect(request.headers).to.have.property('X-Yandex-EnabledExpBoxes').to.be.equal('X-Yandex-EnabledExpBoxes');
        expect(request.headers).to.have.property('X-Yandex-ClientType').to.be.equal('X-Yandex-ClientType');
        expect(request.headers).to.have.property('X-Yandex-ClientVersion').to.be.equal('X-Yandex-ClientVersion');
        expect(request.headers).to.have.property('yandexuid').to.be.equal('yandexuid');
        expect(request.headers).to.have.property('icookie').to.be.equal('icookie');
        expect(request.headers).to.have.property('User-Agent').to.be.equal('User-Agent');
        expect(request.headers).not.to.have.property('another_header');
        expect(request).to.have.property('query').to.be.deep.equal({});
        expect(request).to.have.property('body').to.be.deep.equal({});
    });

    it('should make empty uj params', () => {
        const request = request_module.user_journal({
            connectionId: '',
            expBoxes: '',
            enabledExpBoxes: '',
            clientType: '',
            clientVersion: '',
            yandexUid: '',
            iCookie: '',
            userAgent: '',
            another_header: 'another_header'
        }, request_module.make_clean_request());

        expect(request.headers).to.have.property('connection_id').to.be.equal('');
        expect(request.headers).to.have.property('X-Yandex-ExpBoxes').to.be.equal('');
        expect(request.headers).to.have.property('X-Yandex-EnabledExpBoxes').to.be.equal('');
        expect(request.headers).to.have.property('X-Yandex-ClientType').to.be.equal('');
        expect(request.headers).to.have.property('X-Yandex-ClientVersion').to.be.equal('');
        expect(request.headers).to.have.property('yandexuid').to.be.equal('');
        expect(request.headers).to.have.property('icookie').to.be.equal('');
        expect(request.headers).to.have.property('User-Agent').to.be.equal('');
        expect(request.headers).not.to.have.property('another_header');
        expect(request).to.have.property('query').to.be.deep.equal({});
        expect(request).to.have.property('body').to.be.deep.equal({});
    });

    it('should transform boolean', () => {
        expect(request_module.transform_object(true)).to.be.equal('yes');
        expect(request_module.transform_object(false)).to.be.equal('no');
    });

    it('should transform array', () => {
        const o = { a: 1 };
        expect(request_module.transform_object([ true, o, 'string', 1 ]))
            .to.be.eql([ 'yes', JSON.stringify(o), 'string', '1' ]);
    });

    it('should transform number', () => {
        expect(request_module.transform_object(1)).to.be.eql('1');
    });

    it('should transform object', () => {
        expect(request_module.transform_object({})).to.be.eql('{}');
    });

    it('should not transform undefined', () => {
        expect(request_module.transform_object(undefined)).to.be.equal(undefined);
    });
});
