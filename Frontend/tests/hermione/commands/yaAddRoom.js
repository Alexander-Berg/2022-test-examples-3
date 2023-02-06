const { Api } = require('../helpers/api');

/**
 * Добавление комнаты на аккаунт
 * @return {Promise<void>}
 */
module.exports = async function yaAddRoom(name, householdId, devicesIds) {
    await this.yaCrashOnReadonly('Нельзя добавлять комнаты для пользователя readonly');

    await this.setMeta('rooms', true);

    const api = Api(this);
    await api.iot.rooms.createRoom({
        name,
        householdId,
        devicesIds,
    });
};
