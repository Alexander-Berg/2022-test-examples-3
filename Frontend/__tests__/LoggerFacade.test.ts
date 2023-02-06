import { configureLogger, createLogger } from '../index';

describe('#LoggerFacade', () => {
    it('should create logger', () => {
        const logger = { log: jest.fn(), error: jest.fn() };
        const loggerFactory = jest.fn(() => logger);

        configureLogger(loggerFactory);

        const testLogger = createLogger('');
        testLogger.log('test');

        expect(logger.log).toBeCalledTimes(1);
        expect(logger.log).toBeCalledWith('test');

        const testError = new Error('test');

        testLogger.error(testError);

        expect(logger.error).toBeCalledTimes(1);
        expect(logger.error).toBeCalledWith(testError);
    });
});
