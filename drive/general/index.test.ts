import { prepareKeyToMDS } from './prepareKeyToMDS';

const PATH_TO_FILE = 'path/to/file.png';
const SEPARATE_INDEX = 5;
describe('Prepare key to MDS', () => {
    it('wrong first slash', () => {
        const key = prepareKeyToMDS(`/${PATH_TO_FILE}`);
        expect(key).toEqual(PATH_TO_FILE);
    });

    it('clear spaces in path', () => {
        const key = prepareKeyToMDS(`${PATH_TO_FILE.split('').join(' ')}`);
        expect(key).toEqual(PATH_TO_FILE);
    });

    it('check combo', () => {
        const key = prepareKeyToMDS(`/${PATH_TO_FILE.substr(0, SEPARATE_INDEX)}  `
            + ` ${PATH_TO_FILE.substr(SEPARATE_INDEX)}`);
        expect(key).toEqual(PATH_TO_FILE);
    });
});
