const { Api } = require('../helpers/api');

/**
 * Удаление всех комнат с аккаунта
 * @return {Promise<void>}
 */
module.exports = async function yaRemoveRooms() {
    await this.onRecord(async() => {
        const api = Api(this);
        const { rooms } = await api.iot.rooms.getRooms();
        const promises = rooms.map(room => api.iot.rooms.removeRoom(room.id));

        await Promise.all(promises);
    });
};
