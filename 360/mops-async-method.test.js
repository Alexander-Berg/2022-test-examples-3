'use strict';

jest.mock('@ps-int/mail-lib');

const MopsAsyncMethod = require('./mops-async-method.js');

describe('WaitForTask', () => {
    let method;
    let exec;

    beforeEach(() => {
        method = new MopsAsyncMethod();
        exec = jest.fn(() => ({ completed: true }));
        method.calls = { exec };
    });

    it('returns immediately for sync task', async () => {
        exec.mockReturnValueOnce({ taskType: 'sync' });
        const result = await method.fetch({}, {});
        expect(result).toEqual({ completed: true });
        expect(exec).toHaveBeenCalledTimes(1);
        expect(exec).toHaveBeenCalledWith('default', {}, {});
    });

    it('calls wait for async task', async () => {
        exec.mockReturnValueOnce({ taskType: 'async', taskGroupId: 'id' });
        const result = await method.fetch({}, {});
        expect(result).toEqual({ completed: true });
        expect(exec).toHaveBeenCalledTimes(2);
        expect(exec).toHaveBeenCalledWith('default', {}, {});
        expect(exec).toHaveBeenCalledWith('wait', {}, { taskType: 'async', taskGroupId: 'id' });
    });
});
