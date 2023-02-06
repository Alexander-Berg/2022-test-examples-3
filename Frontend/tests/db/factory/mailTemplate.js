const { schema } = require('yandex-cfg');
const BaseFactory = require('tests/db/factory/base');

const { registrationInvitationStatusEnum } = schema;

class MailTemplateFactory extends BaseFactory {
    static get externalId() {
        this._externalId = this._externalId || 11596;

        return this._externalId++;
    }

    static get externalSlug() {
        this._externalSlug = this._externalSlug || 0;

        return `WA8CHB23-SFU1${this._externalSlug++}`;
    }

    static get defaultData() {
        const createdAt = '2018-12-13T11:20:25.581Z';

        return {
            id: this.id,
            name: 'Тестовый',
            externalId: this.externalId,
            externalLetterId: 13285,
            externalSlug: this.externalSlug,
            systemAction: null,
            withStatuses: [registrationInvitationStatusEnum.invite],
            text: null,
            title: null,
            createdAt,
            isDefault: false,
        };
    }

    static get table() {
        return require('db').mailTemplate;
    }
}

module.exports = MailTemplateFactory;
