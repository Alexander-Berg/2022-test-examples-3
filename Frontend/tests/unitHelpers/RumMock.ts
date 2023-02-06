export const mockRum = () => {
    const mockInst = {
        logError: jest.fn(),
        ERROR_LEVEL: {
            INFO: 'info',
            DEBUG: 'debug',
            WARN: 'warn',
            ERROR: 'error',
            FATAL: 'fatal',
        },
    };

    return {
        mockInst,
        mock: mockInst as unknown as typeof Ya.Rum,
    };
};
