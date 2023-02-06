const db = require('db');

const destroyOptions = { where: {}, force: true };

module.exports = async() => {
    try {
        await db.heartbeat.destroy(destroyOptions);
        await db.event.destroy(destroyOptions);
        await db.groups.destroy(destroyOptions);
        await db.eventsGroup.destroy(destroyOptions);
        await db.eventLocation.destroy(destroyOptions);
        await db.eventBroadcast.destroy(destroyOptions);
        await db.userRole.destroy(destroyOptions);
        await db.history.destroy(destroyOptions);
        await db.tag.destroy(destroyOptions);
        await db.tagCategory.destroy(destroyOptions);
        await db.programItem.destroy(destroyOptions);
        await db.section.destroy(destroyOptions);
        await db.account.destroy(destroyOptions);
        await db.image.destroy(destroyOptions);
        await db.presentation.destroy(destroyOptions);
        await db.video.destroy(destroyOptions);
        await db.registration.destroy(destroyOptions);
        await db.badgeTemplate.destroy(destroyOptions);
        await db.distribution.destroy(destroyOptions);
        await db.accountMail.destroy(destroyOptions);
        await db.mailTemplate.destroy(destroyOptions);
        await db.eventMailTemplatePreset.destroy(destroyOptions);
        await db.speaker.destroy(destroyOptions);
        await db.subscription.destroy(destroyOptions);
    } catch (error) {
        console.error('Clean db error >>>', error);

        await db.sequelize.sync({ force: true });
    }
};
