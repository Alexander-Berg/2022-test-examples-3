const got = require('got');
const { getTvmServiceTicket } = require('../helpers/tvm');

const quasarUrl = 'https://testing.quasar.yandex.ru/testing/';

/**
 * Добавление или удалание колонок на аккаунт авторизованный с помощью yaLoginWritable
 *
 * @param speakers - массив колонок, например: [new YandexMini(), new YandexModule2()]
 * @param add {boolean}
 * @return {Promise<void>}
 */
module.exports = async function yaChangeSpeakers(speakers, add) {
    await this.onRecord(async() => {
        const { uid } = await this.getMeta('tus');
        if (!uid) {
            throw new Error('Пользователь должен быть авторизован через метод yaLoginWritable');
        }

        const ticket = await getTvmServiceTicket('quasar-backend');
        if (!ticket) {
            throw new Error('Не удалось получить tvm ticket');
        }

        const body = {
            devices: speakers.map(device => device.getInfo()),
        };

        const method = add ? 'batch_register' : 'batch_unregister';
        const url = new URL(quasarUrl + method);
        url.searchParams.append('uuid', uid);

        await got(url.toString(), {
            headers: {
                'X-Ya-Service-Ticket': ticket,
            },
            method: 'POST',
            https: { rejectUnauthorized: false },
            json: body,
        });
    });
};
