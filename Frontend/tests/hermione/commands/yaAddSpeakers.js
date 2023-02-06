const { Api } = require('../helpers/api');
/**
 * Добавление колонок на аккаунт авторизованный с помощью yaLoginWritable
 *
 * @param speakers - массив устройств, например: [new YandexMini(), new YandexModule2()]
 * @return {Promise<void>}
 */
module.exports = async function yaAddSpeakers(speakers) {
    await this.yaCrashOnReadonly('Нельзя добавлять колонки для пользователя readonly');

    await this.setMeta('speakers', speakers);
    await this.yaChangeSpeakers(speakers, true);

    const api = Api(this);
    await api.iot.skills.updateSpeakerDevices();
};
