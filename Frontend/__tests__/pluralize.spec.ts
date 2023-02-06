import { pluralize } from '../pluralize';

describe('Выбирает правильную форму для слова', () => {
    it('38', () => {
        expect(pluralize(38, 'попугай', 'попугая', 'попугаев'))
            .toEqual('попугаев');
    });

    it('1', () => {
        expect(pluralize(1, 'попугай', 'попугая', 'попугаев'))
            .toEqual('попугай');
    });

    it('122', () => {
        expect(pluralize(122, 'попугай', 'попугая', 'попугаев'))
            .toEqual('попугая');
    });
});
