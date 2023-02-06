const { canFileNameBeParsed, canLayoutsBeSpread } = require('../../../src/server/utils/can-layouts-be-spread');

describe('utils/can-layouts-be-spread', () => {
    describe('canFileNameBeParsed', () => {
        describe('should be true', () => {
            const values = [
                '10_0.png', '10-0.png', '23-43.png',
                '23_43.png', '32-bad.png', '54_bad.jpg',
            ];

            values.forEach((val) => it(val, () => assert.isTrue(canFileNameBeParsed(val))));
        });

        describe('should be false', () => {
            const values = [
                '10_-0.png', '10$0.png', '10-%0.png',
                '2943-342432.png', '234234-3.png', '23-3.',
                '234234-bad.png', '-34.png', '23-.png',
                '-Bad.png', '32-good.png', '32-Bad.gif',
                '23-.png', '22-12,png', ' ', '123', 'sad',
            ];

            values.forEach((val) => it(val, () => assert.isFalse(canFileNameBeParsed(val))));
        });
    });

    describe('canLayoutsBeSpread', () => {
        it('should be true', () => {
            const values = [
                [
                    { '1-1.jpeg': '1' },
                    { '10-2.jpeg': '1' },
                    { '10-1.jpeg': '1' },
                    { '1-2.jpeg': '1' },
                ],
                [
                    { '1-1.jpeg': '1' },
                    { '10-2.jpeg': '1' },
                    { '10-bad.jpeg': '1' },
                    { '1-bad.jpeg': '1' },
                ],
            ];

            values.forEach((val) => assert.isTrue(canLayoutsBeSpread(val)));
        });

        it('should be false', () => {
            const values = [
                [
                    { '43052873-1.jpg': '1' },
                    { '43052873-2.jpg': '1' },
                ],
                [
                    { '1-1.jpeg': '1' },
                    { '10-2.jpeg': '1' },
                    { '10-bad.jpeg': '1' },
                    { '43052873-2.jpg': '1' },
                ],
            ];

            values.forEach((val) => assert.isFalse(canLayoutsBeSpread(val)));
        });
    });
});
