const { expect } = require('chai');

const AccessControl = require('accessControl/idm');
const catchError = require('tests/helpers/catchError').func;

describe('Idm access control', () => {
    describe('`hasAccess`', () => {
        it('should success on yandex-team.ru domain and client is idm', () => {
            const accessControl = new AccessControl(
                { hostname: 'expert-admin.yandex-team.ru' },
                { tvmClient: 'idm' }
            );

            accessControl.hasAccess();
        });

        it('should failed on yandex.ru domain', () => {
            const accessControl = new AccessControl(
                { hostname: 'expert-admin.yandex.ru' },
                { tvmClient: 'idm' }
            );

            const error = catchError(() => accessControl.hasAccess());

            expect(error.message).to.equal('Application should locate on yandex-team.ru domain');
            expect(error.status).to.equal(403);
            expect(error.options).to.deep.equal({
                internalCode: '403_YTD',
                hostname: 'expert-admin.yandex.ru'
            });
        });

        it('should failed when client is not idm', () => {
            const accessControl = new AccessControl(
                { hostname: 'expert-admin.yandex-team.ru' },
                { tvmClient: 'direct' }
            );

            const error = catchError(() => accessControl.hasAccess());

            expect(error.message).to.equal('Client has no access');
            expect(error.status).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_CNA', tvmClient: 'direct' });
        });
    });
});
