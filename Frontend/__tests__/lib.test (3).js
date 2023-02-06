const exec = require('child_process').execSync;
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

    it('should import ui Block', () => {
        expect(require(pathToLib).blockUIFactory).toBeDefined();
    });

    it('should import ui Popup', () => {
        expect(require(pathToLib).popupUIFactory).toBeDefined();
    });

    it('should import ui Support', () => {
        expect(require(pathToLib).supportPopupUIFactory).toBeDefined();
        expect(require(pathToLib).supportButtonUIFactory).toBeDefined();
    });

    it('should import ui Button', () => {
        expect(require(pathToLib).buttonUIFactory).toBeDefined();
    });

    it('should import without errors', () => {
        exec('node src/__tests__/requireImportLib.js', (err) => {
            expect(err).not.toBe();
        });
    });
});
