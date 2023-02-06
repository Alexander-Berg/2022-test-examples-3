const assert = require('assert');
const nock = require('nock');
const config = require('yandex-cfg');

const db = require('db');
const { DbType } = require('db/constants');
const { getRegistrationQrCodeUrl } = require('lib/utils');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

const MailDistributionService = require('worker/mailDistributionService');

const testDbType = DbType.internal;
const mockId = '00000000-0000-0000-0000-000000000000';

describe('Worker mail distribution service', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('start', () => {
        it('should create email distribution by filters', async() => {
            const filters = {
                and: [
                    { invitationStatus: 'invite' },
                    { or: [{ answerChoice: 'yes' }, { answerChoice: 'mat be yes' }] },
                ],
            };
            const eventId = 10;
            const feedbackFormId = 99;
            const secretKey = mockId;
            const answersYes1 = { choice: { value: 'yes' } };
            const answersYes2 = { choice: { value: 'mat be yes' } };
            const answersNo = { choice: { value: 'no' } };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.eventLocation.create({
                place: ' ул. Льва Толстого, 16',
                eventId: {
                    id: eventId,
                    title: 'Front Talks 2019',
                    slug: 'front-talks',
                    startDate: '2019-02-16T12:00:00.000Z',
                    feedbackFormId,
                },
            });

            const defaultFields = { secretKey, eventId, ...filters.and[0], isEmailConfirmed: true };

            await factory.registration.create([
                { id: 1, accountId: { id: 1, email: '1@m.m' }, ...defaultFields, answers: answersYes1 },
                { id: 2, accountId: { id: 2, email: '2@m.m' }, ...defaultFields, answers: answersYes2 },
                {
                    id: 3,
                    accountId: { id: 3, email: '3@m.m' },
                    isEmailConfirmed: true,
                    eventId,
                    ...filters.and[0],
                    answers: answersNo,
                },
                { id: 4, accountId: { id: 4, email: '4@m.m' }, isEmailConfirmed: true, eventId },
            ]);
            await factory.distribution.create({
                id: 5,
                eventId,
                templateId: { id: 1, externalSlug: 'WA8CHB23-SFU1167' },
                filters,
                variables: { withQr: true },
            });

            await MailDistributionService.start({ entityId: 5, dbType: testDbType });

            const distribution = await db.distribution.findOne({ where: { id: 5 } });

            assert.deepStrictEqual(distribution.recipientsCount, 2);
            assert.strictEqual(distribution.status, config.schema.distributionStatusEnum.inProgress);

            const accountMails = await db.accountMail.findAll({
                raw: true,
                order: [['accountId']],
                attributes: { exclude: ['id'] },
            });

            assert.deepStrictEqual(accountMails, [
                {
                    accountId: 1,
                    distributionId: 5,
                    sentAt: null,
                    wasSent: false,
                    skipSend: false,
                    title: null,
                    text: null,
                    variables: {
                        email: '1@m.m',
                        campaignSlug: 'WA8CHB23-SFU1167',
                        place: ' ул. Льва Толстого, 16',
                        withQr: getRegistrationQrCodeUrl({ registrationId: 1, eventId, secretKey }),
                        eventId,
                        eventUrl: `${config.frontend.endpoint.replace('{tld}', 'ru')}events/front-talks`,
                        lastName: 'Скайуокер',
                        eventName: 'Front Talks 2019',
                        subjectEventName: 'Front Talks 2019',
                        firstName: 'Энакин',
                        startDate: '16.02.2019',
                        startTime: '12:30',
                        startWeekday: 'в субботу',
                        hasOnlineBroadcast: false,
                        startRegistrationTime: '12:00',
                        onlineBroadcastStartTime: '12:00',
                        secretKey,
                        registrationId: 1,
                        feedbackform: `${config.forms.frontUrl}/${feedbackFormId}`,
                    },
                }, {
                    accountId: 2,
                    distributionId: 5,
                    sentAt: null,
                    wasSent: false,
                    skipSend: false,
                    title: null,
                    text: null,
                    variables: {
                        email: '2@m.m',
                        campaignSlug: 'WA8CHB23-SFU1167',
                        place: ' ул. Льва Толстого, 16',
                        withQr: getRegistrationQrCodeUrl({ registrationId: 2, eventId, secretKey }),
                        eventId,
                        eventUrl: `${config.frontend.endpoint.replace('{tld}', 'ru')}events/front-talks`,
                        lastName: 'Скайуокер',
                        eventName: 'Front Talks 2019',
                        subjectEventName: 'Front Talks 2019',
                        firstName: 'Энакин',
                        startDate: '16.02.2019',
                        startTime: '12:30',
                        startWeekday: 'в субботу',
                        hasOnlineBroadcast: false,
                        startRegistrationTime: '12:00',
                        onlineBroadcastStartTime: '12:00',
                        secretKey,
                        registrationId: 2,
                        feedbackform: `${config.forms.frontUrl}/${feedbackFormId}`,
                    },
                },
            ]);
        });

        it('should create email distribution with variables', async() => {
            const variables = {
                eventName: 'Кастомное имя события для рассылки',
                place: 'В новосибирском офисе Яндекса в БЦ Гринвич',
            };

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.registration.create({
                id: 1,
                accountId: { id: 1, email: '1@m.m' },
                eventId: { id: 10, title: 'Front Talks 2019' },
                invitationStatus: 'invite',
                isEmailConfirmed: true,
            });
            await factory.distribution.create({
                id: 5,
                eventId: 10,
                templateId: { id: 1 },
                filters: { and: [{ invitationStatus: 'invite' }] },
                variables,
            });

            await MailDistributionService.start({ entityId: 5, dbType: testDbType });

            const [accountMail] = await db.accountMail.findAll({
                raw: true,
                order: [['accountId']],
                attributes: { exclude: ['id'] },
            });

            assert.strictEqual(accountMail.variables.eventName, variables.eventName);
            assert.strictEqual(accountMail.variables.place, variables.place);
        });

        it('should create email distribution by filters was sent template', async() => {
            const filters = { and: [{ invitationStatus: 'invite' }] };
            const eventId = 10;
            const secretKey = mockId;

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.eventLocation.create({
                place: ' ул. Льва Толстого, 16',
                eventId: {
                    id: eventId,
                    title: 'Front Talks 2019',
                    slug: 'front-talks',
                    startDate: '2019-02-16T12:00:00.000Z',
                },
            });

            const defaultFields = { secretKey, eventId, ...filters.and[0], isEmailConfirmed: true };

            await factory.registration.create([
                { id: 1, accountId: { id: 1, email: '1@m.m' }, ...defaultFields },
                { id: 2, accountId: { id: 2, email: '2@m.m' }, ...defaultFields },
                { id: 3, accountId: { id: 3, email: '3@m.m' }, ...defaultFields },
            ]);
            await factory.distribution.create({
                id: 5,
                eventId,
                templateId: { id: 1, externalId: 123 },
                filters,
                variables: { withQr: true },
                status: config.schema.distributionStatusEnum.inProgress,
                recipientsCount: 2,
            });

            await factory.accountMail.create([
                {
                    accountId: 1,
                    distributionId: 5,
                },
                {
                    accountId: 2,
                    distributionId: 5,
                },
            ]);

            await factory.distribution.create({
                id: 55,
                eventId,
                templateId: { id: 1, externalId: 123 },
                filters,
                variables: { withQr: true },
                byPreviousDistributionType: config.schema.distributionSendToEnum.wasSent,
                previousSentTemplateId: 1,
            });

            await MailDistributionService.start({ entityId: 55, dbType: testDbType });

            const distribution = await db.distribution.findOne({ where: { id: 55 } });

            assert.deepStrictEqual(distribution.recipientsCount, 2);
            assert.strictEqual(
                distribution.status, config.schema.distributionStatusEnum.inProgress);

            const accountMails = await db.accountMail.findAll({
                raw: true,
                where: { distributionId: 55 },
                order: [['accountId']],
                attributes: { exclude: ['id'] },
            });

            assert.equal(accountMails[0].accountId, 1);
            assert.equal(accountMails[0].distributionId, 55);
            assert.equal(accountMails[1].accountId, 2);
            assert.equal(accountMails[1].distributionId, 55);
        });

        it('should create email distribution by filters was not sent template', async() => {
            const filters = { and: [{ invitationStatus: 'invite' }] };
            const eventId = 10;
            const secretKey = mockId;

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.eventLocation.create({
                place: ' ул. Льва Толстого, 16',
                eventId: {
                    id: eventId,
                    title: 'Front Talks 2019',
                    slug: 'front-talks',
                    startDate: '2019-02-16T12:00:00.000Z',
                },
            });

            const defaultFields = { secretKey, eventId, ...filters.and[0], isEmailConfirmed: true };

            await factory.registration.create([
                { id: 1, accountId: { id: 1, email: '1@m.m' }, ...defaultFields },
                { id: 2, accountId: { id: 2, email: '2@m.m' }, ...defaultFields },
                { id: 3, accountId: { id: 3, email: '3@m.m' }, ...defaultFields },
            ]);
            await factory.distribution.create({
                id: 5,
                eventId,
                templateId: { id: 1 },
                filters,
                variables: { withQr: true },
                status: config.schema.distributionStatusEnum.inProgress,
                recipientsCount: 2,
            });

            await factory.accountMail.create([
                {
                    accountId: 1,
                    distributionId: 5,
                },
            ]);

            await factory.distribution.create({
                id: 55,
                eventId,
                templateId: { id: 1 },
                filters,
                variables: { withQr: true },
                byPreviousDistributionType: config.schema.distributionSendToEnum.wasNotSent,
                previousSentTemplateId: 1,
            });

            await MailDistributionService.start({ entityId: 55, dbType: testDbType });

            const distribution = await db.distribution.findOne({ where: { id: 55 } });

            assert.deepStrictEqual(distribution.recipientsCount, 2);
            assert.strictEqual(
                distribution.status, config.schema.distributionStatusEnum.inProgress);

            const accountMails = await db.accountMail.findAll({
                raw: true,
                where: { distributionId: 55 },
                order: [['accountId']],
                attributes: { exclude: ['id'] },
            });

            assert.equal(accountMails[0].accountId, 2);
            assert.equal(accountMails[0].distributionId, 55);
            assert.equal(accountMails[1].accountId, 3);
            assert.equal(accountMails[1].distributionId, 55);
        });
    });

    describe('finish', () => {
        it('should finish distribution if all mails were sent', async() => {
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.distribution.create({
                id: 5,
                eventId: { id: 10, slug: 'codefest-x' },
                templateId: { id: 1, externalId: 12, externalSlug: 'WA8CHB23-SFU2' },
                recipientsCount: 2,
                status: config.schema.distributionStatusEnum.inProgress,
            });
            await factory.accountMail.create([
                { id: 1, distributionId: 5, wasSent: true },
                { id: 2, distributionId: 5, wasSent: true },
                { id: 3, distributionId: { id: 7 }, wasSent: true },
                { id: 3, distributionId: { id: 7 }, wasSent: false },
            ]);

            const { finished } = await MailDistributionService.finish({ entityId: 5, dbType: testDbType });

            assert(finished);
        });

        it('should not finish distribution if not all mails were sent', async() => {
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.distribution.create({
                id: 5,
                eventId: { id: 10, slug: 'codefest-x' },
                templateId: { id: 1, externalId: 155, externalSlug: 'WA8CHB23-SFU2' },
                recipientsCount: 2,
                status: config.schema.distributionStatusEnum.inProgress,
            });
            await factory.accountMail.create([
                { id: 1, distributionId: 5, wasSent: true },
                { id: 2, distributionId: 5, wasSent: false },
                { id: 3, distributionId: { id: 7 }, wasSent: true },
                { id: 3, distributionId: { id: 7 }, wasSent: false },
            ]);

            const { finished } = await MailDistributionService.finish({ entityId: 5, dbType: testDbType });

            assert(!finished);
        });
    });

    describe('sync', () => {
        it('should sync email distribution', async() => {
            const statuses = config.schema.distributionStatusEnum;

            // Рассылка, которую нужно запустить
            const filters = {
                and: [
                    { invitationStatus: 'invite' },
                    { or: [{ answerChoice_123: 'да' }, { answerChoice_123: 'может быть' }] },
                ],
            };
            const eventId = 10;
            const answers1 = { choice_123: { value: 'Да' } };
            const answers2 = { choice_123: { value: 'Может быть' } };
            const answersNo = { choice_123: { value: 'Нет' } };
            const defaultFields = { isEmailConfirmed: true, eventId };

            await factory.eventLocation.create({
                place: ' ул. Льва Толстого, 16',
                eventId: {
                    id: eventId,
                    title: 'Front Talks 2019',
                    slug: 'front-talks',
                    startDate: '2019-02-16T12:00:00.000Z',
                },
            });
            await factory.registration.create([
                { id: 1, accountId: { id: 1, email: '1@m.m' }, ...defaultFields, ...filters.and[0], answers: answers1 },
                { id: 2, accountId: { id: 2, email: '2@m.m' }, ...defaultFields, ...filters.and[0], answers: answers2 },
                { id: 3, accountId: { id: 3, email: '3@m.m' }, ...defaultFields, answers: answersNo },
            ]);
            await factory.distribution.create({
                id: 5,
                eventId,
                templateId: { id: 1, externalId: 209 },
                filters,
                sendAt: '2017-01-10T01:00:00.000Z',
                status: config.schema.distributionStatusEnum.new,
            });

            // Рассылка, которую нужно завершить
            await factory.distribution.create({
                id: 6,
                eventId: { id: 11, slug: 'codefest-x' },
                templateId: { id: 2, externalSlug: 'WA8CHB23-SFU2' },
                recipientsCount: 2,
                status: config.schema.distributionStatusEnum.inProgress,
            });
            await factory.accountMail.create([
                { id: 1, distributionId: 6, wasSent: true },
                { id: 2, distributionId: 6, wasSent: true },
            ]);

            await MailDistributionService.sync({ dbType: testDbType });

            const distributions = await db.distribution.findAll({ order: [['id']], raw: true });

            assert.strictEqual(distributions[0].status, statuses.inProgress);
            assert.strictEqual(distributions[0].recipientsCount, 2);
            assert.strictEqual(distributions[1].status, statuses.finished);
        });
    });
});
