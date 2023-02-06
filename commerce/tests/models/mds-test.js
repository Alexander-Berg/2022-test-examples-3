const { expect } = require('chai');
const nock = require('nock');

const MdsModel = require('models/mds');
const nockMdsServices = require('tests/helpers/mdsServices');
const nockMds = nockMdsServices.mds;
const nockAvatars = nockMdsServices.avatars;

const config = require('yandex-config');

const fs = require('fs');

describe('`mds`', () => {
    const certData = {
        service: 'direct',
        firstname: 'Наташа',
        lastname: 'Константинопольская',
        certId: '555555',
        confirmedDate: '2016-06-01T12:46:30.135Z',
        dueDate: '2017-06-01T12:46:30.135Z',
        language: 'ru',
        type: 'cert'
    };
    const dataBuffer = fs.readFileSync('tests/models/mds-test.js');

    describe('`putToMds`', () => {
        afterEach(nock.cleanAll);

        it('should put certificate into mdsService and return path', function *() {
            nockMds.success();

            const name = `${Date.now()}_${certData.certId}.pdf`;
            const path = yield MdsModel.putToMds(name, dataBuffer);

            expect(path).to.equal('603/1468925144742_555555.pdf');
        });

        it('should return null when fail to put certificate into mdsService', function *() {
            nockMds.failedId();

            const data = Object.assign({}, certData, { certId: 'failedId' });

            const name = `${Date.now()}_${data.certId}.pdf`;
            const path = yield MdsModel.putToMds(name, dataBuffer);

            expect(path).to.be.null;
        });

        it('should apply retry for mdsService when statusCode 5**', function *() {
            nockMds.retry({ code: 500, certId: 'retryId500' });

            const data = Object.assign({}, certData, { certId: 'retryId500' });
            const name = `${Date.now()}_${data.certId}`;
            const path = yield MdsModel.putToMds(name, dataBuffer);

            expect(path).to.equal('603/1468925144742_555555.pdf');
        });

        it('should not apply retry for mdsService when statusCode 4**', function *() {
            nockMds.retry({ code: 400, certId: 'retryId400' });

            const data = Object.assign({}, certData, { certId: 'retryId400' });
            const name = `${Date.now()}_${data.certId}`;
            const path = yield MdsModel.putToMds(name, dataBuffer);

            expect(path).to.be.null;
        });
    });

    describe('`putToAvatars`', () => {
        afterEach(nock.cleanAll);

        it('should put certificate into avatarsService and return path', function *() {
            nockAvatars.success();

            const name = `${Date.now()}_${certData.certId}`;
            const path = yield MdsModel.putToAvatars(name, dataBuffer);

            expect(path).to.equal('603/1468925144742_555555');
        });

        it('should return null when fail to put certificate into avatarsService', function *() {
            nockAvatars.failedId();

            const data = Object.assign({}, certData, { certId: 'failedId' });

            const name = `${Date.now()}_${data.certId}`;
            const path = yield MdsModel.putToAvatars(name, dataBuffer);

            expect(path).to.be.null;
        });

        it('should apply retry for avatarsService when statusCode 5**', function *() {
            nockAvatars.retry({ code: 500, certId: 'retryId500' });

            const data = Object.assign({}, certData, { certId: 'retryId500' });
            const name = `${Date.now()}_${data.certId}`;
            const path = yield MdsModel.putToAvatars(name, dataBuffer);

            expect(path).to.equal('603/1468925144742_555555');
        });

        it('should not apply retry for avatarsService when statusCode 4**', function *() {
            nockAvatars.retry({ code: 400, certId: 'retryId400' });

            const data = Object.assign({}, certData, { certId: 'retryId400' });
            const name = `${Date.now()}_${data.certId}`;
            const path = yield MdsModel.putToAvatars(name, dataBuffer);

            expect(path).to.be.null;
        });
    });

    describe('`getAvatarsPath`', () => {
        it('should return correct path to avatars service', () => {
            const { avatarsService } = config;
            const actualImagePath = MdsModel.getAvatarsPath('603/7483573983_55555');
            const expectedImagePath = `https://${avatarsService.read.hostname}/get-${avatarsService.namespace}` +
                `/603/7483573983_55555/orig`;

            expect(actualImagePath).to.equal(expectedImagePath);
        });

        it('should return "" when path contains null', () => {
            const actualPath = MdsModel.getAvatarsPath(null);

            expect(actualPath).to.equal('');
        });
    });

    describe('`getMdsPath`', () => {
        it('should return correct path to mds service', () => {
            const { mdsService } = config;
            const actualFilePath = MdsModel.getMdsPath('676/222222222.pdf');
            const expectedFilePath = `http://${mdsService.read.hostname}/get-${mdsService.namespace}/676/222222222.pdf`;

            expect(actualFilePath).to.equal(expectedFilePath);
        });

        it('should return "" when path contains null', () => {
            const actualPath = MdsModel.getMdsPath(null);

            expect(actualPath).to.equal('');
        });
    });

    describe('`putCert`', () => {
        const buffer = Buffer.from('buffer');

        afterEach(nock.cleanAll);

        it('should return correct certificate path for png', function *() {
            nockAvatars.success();

            const actual = yield MdsModel.putCert(buffer, certData.certId, 'png');

            expect(actual).to.equal('603/1468925144742_555555');
        });

        it('should return null when fail to put certificate into avatarsService', function *() {
            nockAvatars.failedId();

            const failedCertData = Object.assign({}, certData, { certId: 'failedId' });
            const actual = yield MdsModel.putCert(buffer, failedCertData.certId, 'png');

            expect(actual).to.be.null;
        });

        it('should return correct certificate path for pdf', function *() {
            nockMds.success();

            const actual = yield MdsModel.putCert(buffer, certData.certId, 'pdf');

            expect(actual).to.equal('603/1468925144742_555555.pdf');
        });

        it('should return null when fail to put certificate into mdsService', function *() {
            nockMds.failedId();

            const failedCertData = Object.assign({}, certData, { certId: 'failedId' });
            const actual = yield MdsModel.putCert(buffer, failedCertData.certId, 'pdf');

            expect(actual).to.be.null;
        });
    });
});
