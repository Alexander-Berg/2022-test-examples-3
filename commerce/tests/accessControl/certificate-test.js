const CertificateAccessControl = require('accessControl/certificate');
const catchError = require('tests/helpers/catchError').func;
const { expect } = require('chai');

describe('Certificate access control', () => {
    describe('`hasAccessForFindCertificate`', () => {
        const accessControl = new CertificateAccessControl();

        it('should throw 400 when certificate it not number', () => {
            const error = catchError(accessControl.hasAccessForFindCertificate.bind(accessControl, 'not number'));

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('Certificate id is invalid');
            expect(error.options).to.deep.equal({
                internalCode: '400_CII',
                certId: 'not number'
            });
        });

        it('should throw 400 when lastname is empty', () => {
            const error = catchError(accessControl.hasAccessForFindCertificate.bind(accessControl, 1, ''));

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('Lastname is empty');
            expect(error.options).to.deep.equal({ internalCode: '400_LNE' });
        });

        it('should throw 400 when lastname contains invalid characters', () => {
            const error = catchError(
                accessControl.hasAccessForFindCertificate.bind(
                    accessControl,
                    1,
                    'drop database;'
                )
            );

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('Lastname contains invalid characters');
            expect(error.options).to.deep.equal({ internalCode: '400_LIC' });
        });

        it('should success when id is number and lastname is valid', () => {
            accessControl.hasAccessForFindCertificate(1, 'Ivanova (Petrov\'a)');
        });
    });

    describe('`hasAccessForFindAllCertificates`', () => {
        const accessControl = new CertificateAccessControl();

        it('should throw 400 when user id contains invalid characters', () => {
            const error = catchError(
                accessControl.hasAccessForFindAllCertificates.bind(
                    accessControl,
                    'invalidSymbols'
                )
            );

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('User id contains invalid characters');
            expect(error.options).to.deep.equal({ internalCode: '400_UIC' });
        });

        it('should success when id is number and lastname is valid', () => {
            accessControl.hasAccessForFindAllCertificates('AX4uf9cp');
        });
    });

    describe('`hasAccessForGetUserCertificates`', () => {
        it('should throw 400 when exam slug is invalid', () => {
            const sut = new CertificateAccessControl({ user: { uid: { value: '1234567890' } } });

            const error = catchError(sut.hasAccessForGetUserCertificates.bind(sut, ['drop database;']));

            expect(error.message).to.equal('Exam slug contains invalid characters');
            expect(error.statusCode).to.equal(400);
            expect(error.options).to.deep.equal({ internalCode: '400_EIC', exam: 'drop database;' });
        });

        it('should throw 401 when user not authorized', () => {
            const sut = new CertificateAccessControl();

            const error = catchError(sut.hasAccessForGetUserCertificates.bind(sut));

            expect(error.message).to.equal('User not authorized');
            expect(error.statusCode).to.equal(401);
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });

        it('should success', () => {
            const sut = new CertificateAccessControl({ user: { uid: { value: '1234567890' } } });

            sut.hasAccessForGetUserCertificates(['direct']);
        });
    });
});
