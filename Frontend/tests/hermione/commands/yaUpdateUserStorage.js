const { Api } = require('../helpers/api');

module.exports = async function yaUpdateUserStorage(userStorageChange) {
    await this.onRecord(async() => {
        const api = Api(this);

        const userStorage = await api.iot.userStorage.get().config;
        await api.iot.userStorage.set({ ...userStorage, ...userStorageChange });
    });
};
