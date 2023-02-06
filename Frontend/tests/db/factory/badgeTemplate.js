const BaseFactory = require('tests/db/factory/base');

class BadgeTemplateFactory extends BaseFactory {
    static get defaultData() {
        return {
            id: this.id,
            slug: 'badge_template',
            data: [{ content: '{username}' }],
            createdAt: new Date(),
            updatedAt: new Date(),
            createdByLogin: 'art00',
            updatedByLogin: 'art00',
        };
    }

    static get table() {
        return require('db').badgeTemplate;
    }
}

module.exports = BadgeTemplateFactory;
