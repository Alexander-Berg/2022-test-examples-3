const { appendQueryParameter } = require('../../utils/url');

module.exports = function(path, { expFlags, ...params } = {}) {
    if (expFlags) {
        // Экспериментальные флаги добавляются query-параметром, откуда попадут в контекст
        // @see https://github.yandex-team.ru/search-interfaces/frontend/tree/master/packages/frontend-kotik#мидлвар-для-добавления-экспериментальных-флагов
        path = appendQueryParameter(path, 'expFlags', JSON.stringify(expFlags));
    }

    Object.keys(params).forEach(key => {
        const value = params[key];
        path = appendQueryParameter(path, key, value);
    });

    return this
        .url(path)
        .yaWaitForLoadPage();
};
