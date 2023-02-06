const { Api } = require('../helpers/api');

module.exports = async function yaDeleteUserStorage(userStorageChange) {
    await this.onRecord(async() => {
        const api = Api(this);

        await api.iot.userStorage.delete();
    });
};
