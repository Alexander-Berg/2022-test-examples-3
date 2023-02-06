const { Api } = require('../helpers/api');

/**
 * Удаление всех сценариев с аккаунта
 * @return {Promise<void>}
 */
module.exports = async function yaRemoveScenarios() {
    await this.onRecord(async() => {
        const api = Api(this);
        const { scenarios } = await api.iot.scenarios.getScenarios();
        const promises = scenarios.map(scenario => api.iot.scenarios.deleteScenario(scenario.id));

        await Promise.all(promises);
    });
};
