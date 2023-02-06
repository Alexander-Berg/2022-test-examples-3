/**
 * Выполнение запроса
 * @return {Promise<void>}
 */
module.exports = async function yaFetch(path, method = 'GET', body) {
    const result = await this.executeAsync(async function(path, method, body, done) {
        // Позволяет при обращении к одному и тому же урлу сохранять разные дампы, аналогично в api/middlewares/hermione.ts
        this.requestIncrement = (this.requestIncrement || 0) + 1;

        const response = await fetch(path + '?requestIncrement=' + this.requestIncrement, {
            headers: {
                'x-csrf-token': window.storage.csrfToken2,
                'Content-Type': 'application/json;charset=utf-8',
            },
            credentials: 'include',
            method,
            body,
        });

        const json = await response.json();

        if (!response.ok) {
            done(`Ошибка выполнения запроса "${method} ${path}": ${response.status} - ${response.statusText}`);
        }

        done(json);
    }, path, method, body).then(execResult => execResult.value);

    if (typeof result === 'string') {
        throw new Error(result);
    }

    if (result.status !== 'ok') {
        throw new Error(`Ошибка выполнения запроса "${method} ${path}": ${result.code} - ${result.message}`);
    }

    return result;
};
