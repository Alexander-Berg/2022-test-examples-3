require('co-mocha');

const { expect } = require('chai');

const QRcodeController = require('models/draw/qr_code');

const CertificateModel = require('models/certificate');

const certificateFactory = require('tests/factory/certificatesFactory');

describe('`draw qr code`', () => {
    it('should return url for qr code', () => {
        const drawData = { service: 'direct',
            certId: 1001,
            lastname: 'Urltov'
        };

        const qrUrl = QRcodeController._getUrl(drawData);

        expect(qrUrl[0]).to.equal(`https://yandex.ru/adv/expert/certificates?certId=${drawData.certId}&lastname=${drawData.lastname}`);
    });

    it('should return src for img qr code for pdf format', function *() {
        const drawData = { service: 'direct',
            certId: 1002,
            lastname: 'Pdftov'
        };

        const srcQRcodeImg = yield QRcodeController.createQRcodeUrl(drawData, 'pdf');

        expect(srcQRcodeImg).to.not.be.empty;
    });

    it('should return src for img qr code for png format', function *() {
        const drawData = { service: 'direct',
            certId: 1003,
            lastname: 'Pngtov'
        };

        const srcQRcodeImg = yield QRcodeController.createQRcodeUrl(drawData, 'png');

        expect(srcQRcodeImg).to.not.be.empty;
    });

    it('should return src for img qr code for get drow data', function *() {
        const dueDate = new Date(2027, 1, 2, 3, 4, 5);
        const finished = new Date(2016, 1, 2, 3, 4, 5);
        const service = { id: 3, code: 'direct', title: 'Yandex.Direct' };
        const type = { id: 4, code: 'cert' };
        const trialTemplate = { id: 5, previewImagePath: '2345/345667' };
        const trial = { id: 6 };
        const cert = {
            id: 2,
            firstname: 'Ivan',
            lastname: 'Ivanov',
            dueDate,
            active: 1,
            confirmedDate: finished,
            imagePath: '255/38472434872_13',
            pdfPath: '255/38472434872_13.pdf'
        };

        yield certificateFactory.createWithRelations(
            cert,
            { trial, trialTemplate, service, type }
        );

        const certData = yield CertificateModel.find(2, 'Ivanov');

        const typeCode = 1;
        const format = 'pdf';

        const drawData = yield CertificateModel._getDrawData({ trial, certData, typeCode, format });

        console.log('drawData ', drawData);
        expect(drawData.qrUrl).to.not.be.empty;
    });
});
