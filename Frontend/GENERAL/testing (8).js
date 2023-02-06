/**
 * Конфиг для деплоя статики в тестовом окружении.
 */
module.exports = {
    // https://abc.yandex-team.ru/services/report-templates/resources/?view=consuming&layout=table&supplier=895&show-resource=3925622
    bucket: 'serp-static-testing',
    // https://abc.yandex-team.ru/services/goods
    service: 'goods',
    // Секреты robot-frontend для тестинга-бакета поискового отдела
    // хранятся в https://sandbox.yandex-team.ru/admin/vault
    // Доступ: https://idm.yandex-team.ru/user/robot-frontend/roles#role=31970266
    s3: {
        // Аналог логина.
        accessKeyId: process.env.GOODS_TESTING_S3_ACCESS_KEY_ID,
        // Аналог пароля.
        secretAccessKey: process.env.GOODS_TESTING_S3_SECRET_ACCESS_KEY,
        // Используем https://serp-static-testing.s3.yandex.net
        usePublicUrl: true,
    },
};
