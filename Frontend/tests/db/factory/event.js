const BaseFactory = require('tests/db/factory/base');
const config = require('yandex-cfg');

class EventFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            slug: 'noop',
            startDate: new Date('2018-02-16T12:00:00.000Z'),
            endDate: new Date('2018-02-16T18:00:00.000Z'),
            dateIsConfirmed: false,
            redirectUrl: null,
            registrationStartDate: new Date('2018-02-01T12:00:00.000Z'),
            registrationEndDate: new Date('2018-02-15T12:00:00.000Z'),
            registrationDateIsConfirmed: false,
            registrationStatus: 'open',
            registrationRedirectUrl: 'https://somewhere.net/register',
            registrationFormId: null,
            registrationFormIdHashed: null,
            feedbackFormId: null,
            feedbackFormIdHashed: null,
            title: 'NoOp',
            city: 'Evercity',
            timezone: config.defaultTimezone,
            description: 'Come here and take part',
            shortDescription: 'Come!',
            image: null,
            lpcPagePath: '.sandbox/events/test',
            lpcPageImage: null,

            isOnline: false,
            isPublished: false,
            isVisible: false,
            areMaterialsPublished: false,
            askUserToSubscribe: false,
            autoControlRegistration: false,
            isAcademy: false,
            isMigrated: false,

            translationStatus: config.broadcastStatus.OFF,
            videoStatus: config.videoStatus.HIDE,
            broadcastWillBe: false,
            broadcastIsStarted: false,
            broadcastIsFinished: false,
            createdAt: new Date('2018-02-16T11:30:00'),
            badgeTemplateId: null,
            autoSendingMailTemplates: {
                ...config.sender.mailIds.internal,
                registrationCreateError: config.sender.mailIds.registrationCreateError,
            },
        };
    }

    static get table() {
        return require('db').event;
    }

    static get subFactories() {
        return {
            badgeTemplateId: require('tests/db/factory/badgeTemplate'),
        };
    }
}

module.exports = EventFactory;
