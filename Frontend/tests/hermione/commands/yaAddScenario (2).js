const { Api } = require('../helpers/api/');

/**
 * Добавление сценария на аккаунт
 * @return {Promise<void>}
 */
module.exports = async function yaAddScenario(scenario) {
    const api = Api(this);
    return await api.iot.scenariosV3.createScenario(scenario.getInfo());
};
