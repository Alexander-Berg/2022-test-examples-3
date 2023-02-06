/**
 * Конфиг для деплоя статики
 * @see https://github.yandex-team.ru/search-interfaces/frontend/tree/master/packages/static-uploader
 */

module.exports = {
    bucket: 'serp-static-testing',
    service: 'ecom-sins',
    static: {
        path: 'build/static/turbo/pages/spa/ecom',
        sources: ['**'],
        throwOnOverwrite: false,
        overwrite: true,
    },
    freeze: {
        path: 'dev/null',
    },
    s3: {
        // Креденшиалы robot-frontend для турбо-бакетов в монорепозитории называются
        // TURBO_S3_ACCESS_KEY_ID и TURBO_S3_SECRET_ACCESS_KEY
        // Выданы роботу robot-frontend в сервисе Разработка интерфейсов Серпа (service_id=2173) с правами admin
        accessKeyId: process.env.TURBO_S3_ACCESS_KEY_ID || process.env.SEARCH_INTERFACES_S3_ACCESS_KEY_ID,
        secretAccessKey: process.env.TURBO_S3_SECRET_ACCESS_KEY || process.env.SEARCH_INTERFACES_S3_SECRET_ACCESS_KEY,
        // Используем https://serp-static-testing.s3.yandex.net/
        usePublicUrl: true,
    },
};
