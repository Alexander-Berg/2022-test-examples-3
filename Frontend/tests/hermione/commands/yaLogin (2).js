const isWriteMode = process.argv.includes('--save') || process.argv.includes('--create');

/**
 * Позволяет залогиниться под пользователем с соответствующей кукой.
 * Нужно в основном для записи дампов
 * @deprecated - использовать yaLoginReadonly или yaLoginWritable
 */
module.exports = async function yaLogin(rawSessionId) {
    const userSession = this.url('https://any.yandex.ru');
    const sessionId = rawSessionId || process.env.HERMIONE_SESSION_ID;

    if (rawSessionId) {
        // eslint-disable-next-line no-console
        console.log(`yaLogin: Found Session_Id: ${rawSessionId}`);
    } else {
        // eslint-disable-next-line no-console
        console.log('yaLogin: Session_Id not found');
    }

    if (isWriteMode && !sessionId) {
        const errorMessage = 'Для тестов с авторизацией необходимо установить переменную окружения: export HERMIONE_SESSION_ID="<значение куки Session_id>"';

        // eslint-disable-next-line no-console
        console.log(`\u001B[31m${errorMessage}\u001B[39m`);

        // Для hermione-отчёта
        throw Error(errorMessage);
    }

    if (sessionId) {
        userSession.setCookie({
            name: 'Session_id',
            value: sessionId,
            domain: '.yandex.ru',
        });
    }

    return userSession;
};
