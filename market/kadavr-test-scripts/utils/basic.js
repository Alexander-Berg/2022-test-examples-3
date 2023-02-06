const {createSession, deleteSession, setState} = require('@yandex-market/kadavr/client');
const options = require('@yandex-market/kadavr/options');

const logger = console;

const createKadavrSession = async sessionId => {
    logger.info('=> createSession', sessionId);
    const response = await createSession(sessionId);
    logger.info('<= createSession', response);
};

const deleteKadavrSession = async sessionId => {
    logger.info('=> deleteSession', sessionId);
    const response = await deleteSession(sessionId);
    logger.info('<= deleteSession', response);
};

const defaultRunOptions = {
    deleteSession: true,
    createSession: true,
};

const run = async (cb, runOptions = defaultRunOptions) => {
    try {
        const sessionId = process.env.KADAVR_SESSION_ID || process.env.USER;

        if (!sessionId) {
            logger.error('KADAVR_SESSION_ID or USER env variables not set');
            process.exit(1);
        }

        logger.info(`kadavrHost = ${options.host}`);
        logger.info(`kadavrPort = ${options.port}`);
        logger.info(`sessionId = ${sessionId}`);

        logger.info(
            '\nset cookie in your browser:',
            `\n\n document.cookie = 'kadavr_session_id=${sessionId};  path=/';`,
            '\n\nexecute for removing:',
            '\n\n document.cookie = \'kadavr_session_id=; expires=\' + (new Date()).toGMTString() + \'; path=/\';',
            '\n\n'
        );

        const browser = {
            setState: async (path, data) => {
                logger.info('=> setState:', 'path=', path);
                const response = await setState(sessionId, path, data);
                logger.info('<= setState:', 'response=', response);

                return response;
            },
        };

        logger.info('LOG:');

        if (runOptions.deleteSession) {
            await deleteKadavrSession(sessionId);
        }

        if (runOptions.createSession) {
            await createKadavrSession(sessionId);
        }

        await cb({browser, logger});
    } catch (error) {
        logger.error(error);
        process.exit(1);
    }
};

module.exports = {
    run,
};
