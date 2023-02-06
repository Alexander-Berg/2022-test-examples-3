import { Logging } from './Logging';

describe('Logging', () => {
    beforeEach(() => {
        delete localStorage.LOG_LEVEL;

        // eslint-disable-next-line no-console
        console.debug = jest.fn();
    });

    it('Should be an instance of Logging', () => {
        expect(new Logging({ name: 'log' })).toBeInstanceOf(Logging);
    });

    it('Should support default logLevel', () => {
        const logging = new Logging({ name: 'log' });

        expect(logging.getLogLevel()).toBe(Logging.LOG_LEVEL_DEFAULT);
    });

    it('Should set log level', () => {
        const logging = new Logging({ name: 'log' });

        logging.setLogLevel('debug');

        expect(logging.getLogLevel()).toBe('debug');
    });

    it('Should get log level from localStorage', () => {
        localStorage.LOG_LEVEL = 'warn';

        expect(new Logging({ name: 'log' }).getLogLevel()).toBe('warn');

        localStorage.LOG_LEVEL = 'debug';

        expect(new Logging({ name: 'log' }).getLogLevel()).toBe('debug');
    });

    it('Should support logLevel', () => {
        // eslint-disable-next-line no-console
        console.debug = jest.fn();
        // eslint-disable-next-line no-console
        console.info = jest.fn();
        // eslint-disable-next-line no-console
        console.warn = jest.fn();
        // eslint-disable-next-line no-console
        console.error = jest.fn();

        const logging = new Logging({ name: 'log' });

        logging.setLogLevel('error');

        logging.debug('test');
        logging.info('test');
        logging.warn('test');
        logging.error('test');

        // eslint-disable-next-line no-console
        expect(console.debug).toHaveBeenCalledTimes(0);
        // eslint-disable-next-line no-console
        expect(console.info).toHaveBeenCalledTimes(0);
        // eslint-disable-next-line no-console
        expect(console.warn).toHaveBeenCalledTimes(0);
        // eslint-disable-next-line no-console
        expect(console.error).toHaveBeenCalledTimes(1);

        logging.setLogLevel('warn');

        logging.debug('test');
        logging.info('test');
        logging.warn('test');
        logging.error('test');

        // eslint-disable-next-line no-console
        expect(console.debug).toHaveBeenCalledTimes(0);
        // eslint-disable-next-line no-console
        expect(console.info).toHaveBeenCalledTimes(0);
        // eslint-disable-next-line no-console
        expect(console.warn).toHaveBeenCalledTimes(1);
        // eslint-disable-next-line no-console
        expect(console.error).toHaveBeenCalledTimes(2);

        logging.setLogLevel('info');

        logging.debug('test');
        logging.info('test');
        logging.warn('test');
        logging.error('test');

        // eslint-disable-next-line no-console
        expect(console.debug).toHaveBeenCalledTimes(0);
        // eslint-disable-next-line no-console
        expect(console.info).toHaveBeenCalledTimes(1);
        // eslint-disable-next-line no-console
        expect(console.warn).toHaveBeenCalledTimes(2);
        // eslint-disable-next-line no-console
        expect(console.error).toHaveBeenCalledTimes(3);

        logging.setLogLevel('debug');

        logging.debug('test');
        logging.info('test');
        logging.warn('test');
        logging.error('test');

        // eslint-disable-next-line no-console
        expect(console.debug).toHaveBeenCalledTimes(1);
        // eslint-disable-next-line no-console
        expect(console.info).toHaveBeenCalledTimes(2);
        // eslint-disable-next-line no-console
        expect(console.warn).toHaveBeenCalledTimes(3);
        // eslint-disable-next-line no-console
        expect(console.error).toHaveBeenCalledTimes(4);
    });

    it('Should support childing', () => {
        // eslint-disable-next-line no-console
        console.error = jest.fn();

        const logging = new Logging();

        logging.setLogLevel('error');

        logging.child({}).error('test');

        expect(console.error).toHaveBeenCalledWith('[default]', 'test');

        logging.child({ name: 'foo' }).error('test');

        expect(console.error).toHaveBeenCalledWith('[default:foo]', 'test');
    });
});
