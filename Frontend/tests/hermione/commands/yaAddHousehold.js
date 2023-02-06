const { Api } = require('../helpers/api');

/**
 * Добавление дома на аккаунт
 * @return {Promise<void>}
 */
module.exports = async function yaAddHousehold(name, address) {
    await this.yaCrashOnReadonly('Нельзя добавлять дома для пользователя readonly');

    const api = Api(this);
    await api.iot.households.addHousehold({ name, address: address || undefined });
};
