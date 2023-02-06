const { expect } = require('chai');

const BaseReport = require('models/report/items/baseReport');

describe('Base report model', () => {
    describe('`getValue`', () => {
        const obj = {
            a: { d: 'value' },
            b: null
        };

        it('should return value from path if value exists', () => {
            const actual = BaseReport.getValue(obj, 'a.d', 'default');

            expect(actual).to.equal('value');
        });

        it('should return default value if path not exist', () => {
            const actual = BaseReport.getValue(obj, 'e', 'default');

            expect(actual).to.equal('default');
        });

        it('should return default value if value is null', () => {
            const actual = BaseReport.getValue(obj, 'b', 'default');

            expect(actual).to.equal('default');
        });
    });
});
