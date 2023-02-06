const nock = require('nock');
const request = require('supertest');

const { nockTvmtool, nockBlackbox } = require('tests/mocks');

const app = require('app');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

describe('AccountMail controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findOne', () => {
        it('should return an mail', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const templateId = {
                id: 5,
                title: 'Статус по мероприятию {{eventName}}',
                text: 'Сорян, {{userName}}! Вы не попали в число участников на {{eventName}}',
            };
            const data = {
                id: 11,
                sentAt: null,
                wasSent: false,
                title: 'Статус по мероприятию Fronttalks',
                variables: { eventName: 'Fronttalks' },
                accountId: { id: 7 },
                distributionId: { id: 5, templateId },
            };

            await factory.accountMail.create(data);

            await request(app.listen())
                .get('/v1/admin/accountMails/11')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({
                    id: 11,
                    accountId: 7,
                    distributionId: 5,
                    sentAt: null,
                    wasSent: false,
                    variables: { eventName: 'Fronttalks' },
                    title: 'Статус по мероприятию Fronttalks',
                    text: 'Сорян, <<userName>>! Вы не попали в число участников на Fronttalks',
                });
        });

        it('should throw error when mailId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/accountMails/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'AccountMail ID is invalid',
                    value: 'inv@lid',
                });
        });
    });
});
