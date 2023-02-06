const { Api } = require('../helpers/api');

/**
 * Удаляет дома с аккаунта
 * @return {Promise<void>}
 */
module.exports = async function yaClearHouseholds() {
    await this.onRecord(async() => {
        const api = Api(this);

        const { households } = await api.iot.households.get();
        const promises = households
            .filter(household => household.name !== 'Мой дом')
            .map(household => api.iot.households.removeHousehold(household.id));

        await Promise.all(promises);
    });
};
