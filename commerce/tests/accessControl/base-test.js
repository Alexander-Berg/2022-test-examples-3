const BaseAccessControl = require('accessControl/base');
const { expect } = require('chai');
const catchError = require('tests/helpers/catchError').func;

describe('BaseAccessControl', () => {
    describe('`uid`', () => {
        it('should return uid from state', () => {
            const accessControl = new BaseAccessControl({ user: { uid: { value: '0711' } } });

            expect(accessControl.uid).to.equal('0711');
        });

        it('should return `undefined` when state is empty', () => {
            const accessControl = new BaseAccessControl({});

            expect(accessControl.uid).to.be.undefined;
        });
    });

    describe('`login`', () => {
        it('should return login from state', () => {
            const accessControl = new BaseAccessControl({ user: { login: 'm-smirnov' } });

            expect(accessControl.login).to.equal('m-smirnov');
        });

        it('should return `undefined` when state is empty', () => {
            const accessControl = new BaseAccessControl({});

            expect(accessControl.login).to.be.undefined;
        });
    });

    describe('`authType`', () => {
        it('should return authType from state', () => {
            const accessControl = new BaseAccessControl({ authType: 'web' });

            expect(accessControl.authType).to.equal('web');
        });

        it('should return `undefined` when state is empty', () => {
            const accessControl = new BaseAccessControl({});

            expect(accessControl.authType).to.be.undefined;
        });
    });

    describe('`normalizedLogin`', () => {
        it('should return normalizedLogin from state', () => {
            const accessControl = new BaseAccessControl({ user: { attributes: { 1008: 'smrnv-m' } } });

            expect(accessControl.normalizedLogin).to.equal('smrnv-m');
        });

        it('should return `undefined` when state is empty', () => {
            const accessControl = new BaseAccessControl({});

            expect(accessControl.normalizedLogin).to.be.undefined;
        });
    });

    describe('`authorizationRequired`', () => {
        it('should do nothing for authorized user', () => {
            const accessControl = new BaseAccessControl({ user: { uid: { value: '0711' } } });

            accessControl.authorizationRequired();
        });

        it('should throw error for unauthorized user', () => {
            const accessControl = new BaseAccessControl({});

            const error = catchError(accessControl.authorizationRequired.bind(accessControl));

            expect(error.statusCode).to.equal(401);
            expect(error.message).to.equal('User not authorized');
            expect(error.options).to.deep.equal({ internalCode: '401_UNA' });
        });
    });
});
