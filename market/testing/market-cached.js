'use strict';

/**
 * Расширенное окружение для стенда с кэшированием ответов бекенда.
 *
 * @see https://st.yandex-team.ru/MARKETVERSTKA-16235
 */
module.exports = {
    servant: {
        buker: {
            useCacheProxy: true,
        },
        cataloger: {
            useCacheProxy: true,
        },
        guru: {
            useCacheProxy: true,
        },
        report: {
            useCacheProxy: true,
        },
    },
};
