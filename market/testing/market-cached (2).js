// Расширенное окружение для стенда с кэшированием ответов бэкенда
// https://st.yandex-team.ru/MARKETVERSTKA-16235
module.exports = {
    servant: {
        cataloger: {
            useCacheProxy: true,
        },
        report: {
            useCacheProxy: true,
        },
        buker: {
            useCacheProxy: true,
        },
    },
};
