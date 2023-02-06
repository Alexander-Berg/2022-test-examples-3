/**
 * Выполнение запроса
 * @return {Promise<void>}
 */
module.exports = async function yaFetch(path, method = 'GET', body) {
    const result = await this.executeAsync(async function(path, method, body, done) {
        // Позволяет при обращении к одному и тому же урлу сохранять разные дампы, похожая реализация в api/middlewares/hermione.ts
        if (!this.requestIncrement) {
            this.requestIncrement = {};
        }
        const requestIncrement = this.requestIncrement[path + method] || 0;
        this.requestIncrement[path + method] = requestIncrement + 1;

        const url = new URL(location.origin + path);
        url.searchParams.append('requestIncrement', requestIncrement);

        const response = await fetch(url.toString(), {
            headers: {
                'x-csrf-token': window.storage.csrfToken2,
                'Content-Type': 'application/json;charset=utf-8',
            },
            credentials: 'include',
            method,
            body,
        });

        if (!response.ok) {
            done(`Ошибка выполнения запроса "${method} ${path}": ${response.status} - ${response.statusText}`);
        }

        const json = await response.json();

        done(json);
    }, path, method, body);

    if (typeof result === 'string') {
        throw new Error(result);
    }

    if (result.status !== 'ok') {
        throw new Error(`Ошибка выполнения запроса "${method} ${path}": ${result.code} - ${result.message}`);
    }

    return result;
};
