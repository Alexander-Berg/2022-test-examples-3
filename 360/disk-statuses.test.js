'use strict';

const statusFilter = require('./disk-statuses').statusFilter;

describe('statusFilter', () => {
    const OPERATIONS = [
        { status: 'DONE' },
        { status: 'DONE' },
        { status: 'EXECUTING' },
        { status: 'FAILED' }
    ];

    it('без агрументов отдает все', () => {
        expect(OPERATIONS.filter(statusFilter())).toHaveLength(OPERATIONS.length);
    });

    it('работает с одним аргументом', () => {
        expect(OPERATIONS.filter(statusFilter('DONE'))).toHaveLength(2);
    });

    it('принимает в качестве агрумента массив', () => {
        expect(OPERATIONS.filter(statusFilter([ 'REJECTED', 'ABORTED' ]))).toHaveLength(0);
    });

    it('работает с несколькими аргументами', () => {
        expect(OPERATIONS.filter(statusFilter('DONE', 'FAILED', 'EXECUTING'))).toHaveLength(4);
    });
});
