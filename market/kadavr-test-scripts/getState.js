const {run} = require(`${__dirname}/utils/basic`);

const OPTIONS = {
    removeSession: false,
    createSession: false,
};

run(async ({browser, logger}) => {
    // в кадавре нет ручке получения всего стейта, потому что там сейчас реализуется несколько хранилищ (redis)
    // поэтому тут костыли через setState, а setState возврает значение ключа

    const keys = [
        'report',
        'schema',
    ];

    const fakeSetState = key => {
        // тут ставим фейковое значение key._, в ответ вернется полное значение key
        const fakeKey = `${key}._it_is_debug_baby`;
        const data = {};
        return browser.setState(fakeKey, data)
            .then(result => {
                try {
                    return {
                        [key]: JSON.parse(result),
                    };
                } catch {
                    return {
                        [key]: result,
                    };
                }
            });
    };

    const promises = keys.map(fakeSetState);

    const results = await Promise.all(promises);

    const state = results.reduce((stateObject, result) => ({
        ...stateObject,
        ...result,
    }), {});

    logger.info(JSON.stringify(state, null, 2));
}, OPTIONS);
