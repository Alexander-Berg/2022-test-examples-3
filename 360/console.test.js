'use strict';

function MockConsole() {
    this.core = {
        hideParamInLog: jest.fn()
    };
}

const prep = MockConsole.prototype._prepareArgs = jest.fn();

jest.mock('@yandex-int/duffman', () => ({
    Core: {
        Console: MockConsole
    }
}));

const Console = require('./console.js');
const c = new Console();

describe('Console#_prepareArgs', () => {
    it('calls super', () => {
        c._prepareArgs('reason', { args: 1 });
        expect(prep).toBeCalledWith('reason', { args: 1 });
    });

    it.each([
        [ 'MODEL_RESOLVED', 'save-draft' ],
        [ 'MODEL_REJECTED', 'save-draft' ],
        [ 'MODEL_RESOLVED', 'send-message' ],
        [ 'MODEL_REJECTED', 'send-message' ]
    ])('Hide message reason: %s, model: %s', (reason, name) => {
        c._prepareArgs(reason, { name, params: {} });
        expect(c.core.hideParamInLog).toBeCalledWith({}, null, 'message', '[HiddenParam message]');
    });

    it.each([
        [ 'MODEL_RESOLVED', 'settings' ],
        [ 'CUSTOM_REASON', 'save-draft' ]
    ])('Do not hide message reason: %s, model: %s', (reason, name) => {
        c._prepareArgs(reason, { name, params: {} });
        expect(c.core.hideParamInLog).not.toBeCalled();
        expect(prep).toBeCalledWith(reason, { name, params: {} });
    });
});
