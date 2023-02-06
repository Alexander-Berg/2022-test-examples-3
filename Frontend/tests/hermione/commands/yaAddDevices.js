const got = require('got');
const { getToken } = require('../helpers/secrets');

// Так как секрет нужен только на момент записи дампа, доступ к нему требуется только разработчику теста
const secretId = 'sec-01fzt66qe8sp3q8y1kvns15ryy';
const secretKeyName = 'steelix-ui-quality-skill-oauth-token';
const hermioneSkillId = 'eacb68b3-27dc-4d8d-bdbb-b4f6fb7babd2';
const dialogsUrl = `https://dialogs.priemka.voicetech.yandex.net/api/v1/skills/${hermioneSkillId}/callback/push-discovery`;

/**
 * Добавление устройств на аккаунт авторизованный с помощью yaLoginWritable
 *
 * @param devices - массив устройств, например: [new Light('Лампочка'), new Socket()]
 * @return {Promise<void>}
 */
module.exports = async function yaAddDevices(devices) {
    await this.yaCrashOnReadonly('Нельзя добавлять устройства для пользователя readonly');

    await this.onRecord(async() => {
        const { uid } = await this.getMeta('tus');
        if (!uid) {
            throw new Error('Для добавления устройств пользователь должен быть авторизован через метод authAnyOnRecord');
        }

        const authToken = await getToken(secretId, secretKeyName);
        if (!authToken) {
            throw new Error('Не удалось получить OAuth токен для добавления устройств');
        }

        await this.setMeta('devices', devices);

        const body = {
            ts: new Date().getTime() / 1000,
            payload: {
                user_id: uid,
                devices: devices.map(device => device.getInfo()),
            },
        };

        await got(dialogsUrl, {
            headers: {
                Authorization: `OAuth ${authToken}`,
            },
            method: 'POST',
            https: { rejectUnauthorized: false },
            json: body,
        });
    });
};
