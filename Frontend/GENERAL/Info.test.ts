import { shallow } from 'enzyme';
import { parseData } from './Info';

const testParseData = (input: object, name: string) => {
    parseData(input).map((el, idx) => {
        it(name + idx, () => {
            expect(shallow(el)).toMatchSnapshot();
        });
    });
};

describe('Bug', () => {
    testParseData({ text: '1', url: '1' }, 'parse to link');
    testParseData({ text: '1', urm: '1' }, 'false parse to link');
    testParseData(1 as unknown as object, 'number');
    testParseData('1' as unknown as object, 'string');
    testParseData(true as unknown as object, 'boolean');
    testParseData([1, 2, 3, 4], 'array of number');
    testParseData(['1', '2', '3'], 'array of string');
    testParseData([true, false, true], 'array of boolean');
    testParseData([{ text: '1', url: '1' }, { text: '2', url: '1' }, { text: '3', url: '1' }], 'array parse to links');
    testParseData([{ text: '1', url: '1' }, { text: '2', urm: '1' }, { text: '3', url: '1' }], 'array parse some links');
    testParseData([true, { text: '2', urm: '1' }, { text: '3', url: '1' }, 1, 'hello world', 1.2], 'all mixed up');
});
