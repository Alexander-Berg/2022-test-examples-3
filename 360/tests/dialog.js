const { dirname } = require('../lib/path');
const { getClientResourceDialogUrl } = require('../lib/dialog');

describe('dialog', () => {
    describe('getClientResourceDialogUrl', () => {
        const tests = {
            '/disk/folder/file.JPG': '/client/disk/folder|select/disk/folder/file.JPG',
            '/files/ололо/файл': '/client/files/ололо|select/files/ололо/файл',
            '/files/%%<>': '/client/files|select/files/%%<>',
            '/': '/client/|select/',
            '': '/client/|select'
        };

        Object.keys(tests).forEach((input) => {
            const testName = `${input} should return "${tests[input]}"`;

            it(testName, () => {
                expect(getClientResourceDialogUrl(dirname(input), input, 'select')).toBe(tests[input]);
            });
        });
    });
});
