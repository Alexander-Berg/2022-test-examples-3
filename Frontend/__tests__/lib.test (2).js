const pathToLib = '../../lib_cjs';

describe('#lib test', () => {
    let origWindow;
    beforeAll(() => {
        origWindow = global.window;
    });

    afterEach(() => {
        global.window = origWindow;
    });

    it('should not access window on import', () => {
        delete global.window;

        expect(() => require(pathToLib)).not.toThrow();
    });
});
