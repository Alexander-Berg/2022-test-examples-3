const { Api } = require('../helpers/api');

/**
 * Добавление комнаты на аккаунт
 * @return {Promise<void>}
 */
module.exports = async function yaAddRooms(name, householdId, devicesIds) {
    const api = Api(this);
    await api.iot.rooms.createRoom({
        name,
        householdId,
        devicesIds,
    });
};
