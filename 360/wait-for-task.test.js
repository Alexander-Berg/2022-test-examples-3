'use strict';

jest.mock('@ps-int/mail-lib');
jest.mock('../../lib/sleep.js');

const WaitForTask = require('./wait-for-task.js');
const sleep = require('../../lib/sleep.js');

describe('WaitForTask', () => {
    let waitForTask;
    let exec;
    const params = {
        taskGroupId: 'id',
        timeout: 1000
    };

    beforeEach(() => {
        waitForTask = new WaitForTask();
        exec = jest.fn(() => ({ tasks: [] }));
        waitForTask.calls = { exec };
        sleep.mockClear();
    });

    it('returns immediately if there is no pending tasks', async () => {
        const result = await waitForTask.fetch({}, params);
        expect(result).toEqual({ completed: true });
    });

    it('waits for task to complete', async () => {
        exec.mockResolvedValueOnce({ tasks: [ { taskGroupId: 'id' } ] });
        exec.mockResolvedValueOnce({ tasks: [ { taskGroupId: 'id' } ] });
        exec.mockResolvedValueOnce({ tasks: [ { taskGroupId: 'id' } ] });
        const result = await waitForTask.fetch({}, params);
        expect(result).toEqual({ completed: true });
        expect(exec).toHaveBeenCalledTimes(4);
        expect(sleep).toHaveBeenCalledTimes(3);
        expect(sleep).toHaveBeenCalledWith(50);
        expect(sleep).toHaveBeenCalledWith(200);
    });

    it('stop waiting if timeout exceeded', async () => {
        exec.mockResolvedValue({ tasks: [ { taskGroupId: 'id' } ] });
        let N = 0;
        const mockNow = jest.spyOn(Date, 'now').mockImplementation(() => {
            N += 50;
            return 155e10 + N;
        });
        const result = await waitForTask.fetch({}, params);
        mockNow.mockRestore();
        expect(result).toEqual({ completed: false });
        expect(sleep).toHaveBeenCalledWith(1000);
        expect(exec.mock.calls.length).toBeGreaterThan(10);
    });
});
