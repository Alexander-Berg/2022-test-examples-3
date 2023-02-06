const path = require('path');
const {
    newTool,
    oldTool,
} = require('./fixture/toolMock');

describe('loadPageObjects()', () => {
    describe('hermione 2.0', () => {
        describe('загружает вложенные PO', () => {
            beforeAll(() => {
                jest.resetModules();

                const pluginOptions = {
                    targetDir: path.resolve(__dirname, 'fixture/page-objects'),
                };

                require('../lib')(newTool, pluginOptions);
            });

            it('PO "A" загружен', () => {
                const {PageObject} = require('../lib');
                expect(PageObject.get('A'))
                    .toBe(require('./fixture/page-objects/A'));
            });

            it('PO "B" загружен', () => {
                const {PageObject} = require('../lib');
                expect(PageObject.get('B'))
                    .toBe(require('./fixture/page-objects/B'));
            });

            it('PO "H/C" загружен', () => {
                const {PageObject} = require('../lib');
                expect(PageObject.get('H/C'))
                    .toBe(require('./fixture/page-objects/H/C'));
            });

            it('PO "H/D" загружен', () => {
                const {PageObject} = require('../lib');
                expect(PageObject.get('H/D'))
                    .toBe(require('./fixture/page-objects/H/D'));
            });
        });
    });

    describe('hermione < 2.0', () => {
        describe('загружает вложенные PO', () => {
            beforeAll(() => {
                jest.resetModules();

                const pluginOptions = {
                    targetDir: path.resolve(__dirname, 'fixture/page-objects'),
                };

                require('../lib')(oldTool, pluginOptions);
            });

            it('PO "A" загружен', () => {
                const {PageObject} = require('../lib');
                expect(PageObject.get('A'))
                    .toBe(require('./fixture/page-objects/A'));
            });

            it('PO "B" загружен', () => {
                const {PageObject} = require('../lib');
                expect(PageObject.get('B'))
                    .toBe(require('./fixture/page-objects/B'));
            });

            it('PO "H/C" загружен', () => {
                const {PageObject} = require('../lib');
                expect(PageObject.get('H/C'))
                    .toBe(require('./fixture/page-objects/H/C'));
            });

            it('PO "H/D" загружен', () => {
                const {PageObject} = require('../lib');
                expect(PageObject.get('H/D'))
                    .toBe(require('./fixture/page-objects/H/D'));
            });
        });
    });
});
