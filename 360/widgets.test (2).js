'use strict';

const model = require('./widgets.js');
const filter = require('./_filters/widgets.js');

jest.mock('./_filters/widgets.js');

const taksa = jest.fn();

const core = {
    service: () => taksa,
    yasm: {
        sum: jest.fn()
    },
    console: {
        log: jest.fn(),
        error: jest.fn()
    }
};

describe('should call filter with correct params', function() {
    it('if envelopes is array and not empty', function() {
        taksa.mockResolvedValueOnce({ widgets: [ { mid: 1, info: {} } ] });

        return model({ data: { envelopes: [ 'message' ] } }, core).then(() => {
            expect(taksa).toHaveBeenCalledWith('/api/list', { envelopes: [ 'message' ] });
            expect(filter).toHaveBeenCalledWith({
                widgets: [ { mid: 1, info: {} } ]
            }, core.console);
        });
    });

    it('if params.data raised an exception', function() {
        return model({ data: Promise.reject() }, core).then(() => {
            expect(filter).toHaveBeenCalledWith({}, core.console);
        });
    });

    it('if envelopes is empty array', function() {
        taksa.mockRejectedValueOnce();

        return model({ data: { envelopes: [] } }, core).then(() => {
            expect(filter).toHaveBeenCalledWith({}, core.console);
        });
    });

    it('if an exception occured', function() {
        taksa.mockRejectedValueOnce(new Error());

        return model({ data: { envelopes: [ 'message' ] } }, core).then(() => {
            expect(filter).toHaveBeenCalledWith({}, core.console);
        });
    });
});
