const { Api } = require('../helpers/api');

/**
 * Удаление комнат, домов, устройств и колонок с аккаунта
 * @return {Promise<void>}
 */
module.exports = async function yaClearAccount() {
    await this.onRecord(async() => {
        const readonly = await this.getMeta('readonly');
        const tus = await this.getMeta('tus');

        if (readonly || !tus) {
            return;
        }

        try {
            const devices = await this.getMeta('devices');
            if (devices) {
                await this.yaUnlinkDevices();
            }

            const speakers = await this.getMeta('speakers');
            if (speakers) {
                await this.yaChangeSpeakers(speakers, false);
            }

            const api = Api(this);
            const response = await api.iot.devicesV3.getDevices();
            const promises = [];

            response.households.forEach(household => {
                household.all.forEach((device) => {
                    promises.push(api.iot.devices.removeDevice(device.id));
                });

                // household.rooms.forEach(room => {
                //     console.log('room', room.id);
                //     promises.push(api.iot.rooms.removeRoom(room.id));
                // });

                if (household.name !== 'Мой дом') {
                    promises.push(api.iot.households.removeHousehold({ id: household.id }));
                }
            });

            await Promise.all(promises);

            // Пустые комнаты не отдаются в devicesV3, поэтому удаляем отдельно
            const rooms = await this.getMeta('rooms', true);
            if (rooms) {
                await this.yaRemoveRooms();
            }

            await this.yaDeleteUserStorage();
        } catch (e) {
            console.error(e);
            throw new Error(e);
        }
    });
};
