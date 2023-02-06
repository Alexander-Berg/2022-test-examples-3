'use strict';

const mockTskv = jest.fn();
jest.mock('./helpers/tskv.js', () => mockTskv);

jest.mock('./helpers/debug.js');

const Logger = require('./abstract-logger.js');
let logger, logSpy;

beforeEach(() => {
    mockTskv.mockReturnValue('tskv\tmock=true');

    Logger.setDefaults(4, false);

    logger = new Logger('JEST', 'jest');
    logger._prepareArgs = (reason, args) => ({ reason, ...args });
    logSpy = jest.spyOn(logger, '_log');
});

test('calls tskv and syslog', () => {
    const posix = require('posix');

    logger.error('reason', { args: 1 });

    expect(logSpy).toHaveBeenCalledWith('err', 'reason', { args: 1 });
    expect(mockTskv).toHaveBeenCalledWith('JEST', 'err', { reason: 'reason', args: 1 });
    expect(posix.syslog).toHaveBeenCalledWith('err', 'tskv\tmock=true');
});

const LOG_METHODS = [
    'crit',
    'error',
    'warn',
    'info',
    'log',
    'debug',
];

const POSIX_LEVELS = [
    'emerg', // 0
    'alert',
    'crit',
    'err', // 3
    'warning',
    'notice',
    'info',
    'debug' // 7
];

// verbosity, called priority, skipped priority
test.each([
    [ 0, POSIX_LEVELS.slice(2, 3), POSIX_LEVELS.slice(3) ],
    [ 1, POSIX_LEVELS.slice(2, 4), POSIX_LEVELS.slice(4) ],
    [ 2, POSIX_LEVELS.slice(2, 5), POSIX_LEVELS.slice(5) ],
    [ 3, POSIX_LEVELS.slice(2, 7), POSIX_LEVELS.slice(7) ],
    [ 4, POSIX_LEVELS.slice(2), [] ]
])('verbosity: %d called: %p skipped: %p', (verbose, called, skipped) => {
    logger.verbose = verbose;
    LOG_METHODS.forEach((method) => logger[method]('a', {}));

    called.forEach((priority) => {
        expect(logSpy).toHaveBeenCalledWith(
            priority,
            expect.any(String),
            expect.any(Object)
        );
    });

    skipped.forEach((priority) => {
        expect(logSpy).not.toHaveBeenCalledWith(
            priority,
            expect.any(String),
            expect.any(Object)
        );
    });
});

test('apply prototype.verbose', () => {
    Logger.setDefaults(0, false);

    logger.info('a', {});
    expect(logSpy).not.toHaveBeenCalled();
});

test('call debug', () => {
    logger.dev = true;
    logger._debug = jest.fn(() => jest.fn());
    logger.error('a', {});
    expect(logger._debug).toHaveBeenCalledWith('err');
});

test('call debug if prototype.dev is true', () => {
    Logger.setDefaults(4, true);
    logger._debug = jest.fn(() => jest.fn());
    logger.error('a', {});
    expect(logger._debug).toHaveBeenCalledWith('err');
});

test('_prepareArgs should be implemented', () => {
    expect(Logger.prototype._prepareArgs).toThrow(/Should be implemented/);
});
