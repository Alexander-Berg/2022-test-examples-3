const got = require('got');
const { getTvmServiceTicket } = require('../helpers/tvm');

const yandexIOSkillId = 'YANDEX_IO';
const dialogsUrl = `https://dialogs.priemka.voicetech.yandex.net/api/v1/skills/${yandexIOSkillId}/callback/push-discovery`;

module.exports = async function yaAddDevicesYandexIO(devices, parentId) {
    await this.yaCrashOnReadonly('Нельзя добавлять устройства для пользователя readonly');

    await this.onRecord(async() => {
        const { uid } = await this.getMeta('tus');
        if (!uid) {
            throw new Error('Для добавления устройств пользователь должен быть авторизован через метод authAnyOnRecord');
        }

        const ticket = await getTvmServiceTicket('steelix-priemka');
        if (!ticket) {
            throw new Error('Не удалось получить тикет до steelix-priemka');
        }

        await this.setMeta('devices', devices);

        const body = {
            ts: new Date().getTime() / 1000,
            payload: {
                user_id: uid,
                devices: devices.map(device => {
                    const deviceConfig = device.getInfo();

                    return ({
                        ...deviceConfig,
                        skillId: yandexIOSkillId,
                        custom_data: {
                            ...deviceConfig.custom_data,
                            parent_endpoint_id: parentId,
                        },
                    });
                }),
            },
        };

        await got(dialogsUrl, {
            headers: {
                'X-Ya-Service-Ticket': ticket,
            },
            method: 'POST',
            https: { rejectUnauthorized: false },
            json: body,
        });
    });
};
