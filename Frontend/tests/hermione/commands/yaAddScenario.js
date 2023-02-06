const { Api } = require('../helpers/api/');

/**
 * Добавление сценария на аккаунт
 * @return {Promise<void>}
 */
module.exports = async function yaAddScenario(scenario) {
    await this.yaCrashOnReadonly('Нельзя добавлять сценарии для пользователя readonly');

    const api = Api(this);
    return await api.iot.scenariosV3.createScenario(scenario.getInfo());
};
