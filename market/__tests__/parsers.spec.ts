import {Parsers} from '../parsers';

describe('Parsers', () => {
    describe('isInt()', () => {
        it("'1' should be integer", () => expect(Parsers.isInt('1')).toEqual(true));

        it("'9786545' should be integer", () => expect(Parsers.isInt('9786545')).toEqual(true));

        it("'-3' should be integer", () => expect(Parsers.isInt('-3')).toEqual(true));

        it("'1a' shouldn't be integer", () => expect(Parsers.isInt('1a')).toEqual(false));
    });
});
