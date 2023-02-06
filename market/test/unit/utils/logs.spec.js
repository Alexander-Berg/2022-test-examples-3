'use strict';

const path = require('path');

const { readLatestLines, latestRequests } = require('./../../../utils/logs');
const logsJSON = require('./logs-expected.json');

describe('logs', () => {
    describe.skip('read last lines', () => {
        it('should return lines with equal \'req_id\'', () => {
            for (let i = 0; i < logsJSON.length; i++) {
                const filepath = path.join(__dirname, logsJSON[i].path).toString();
                const expected = logsJSON[i].expected;
                expect(readLatestLines(filepath)).toEqual(expected);
            }
        });
    });

    describe.skip('latest requests', () => {
        it('should return latest requests', () => {
            const expectedArray = latestRequests();

            expect(Array.isArray(expectedArray)).toBeTruthy();
        });
    });
});
