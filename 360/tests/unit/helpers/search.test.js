import * as searchHelper from 'helpers/search';

const buildMetrikaFor = (branch) => ['interface elements', 'search', 'click', branch];

describe('Хелпер `metrikaBranch`', () => {
    it('должен вернуть `photo` для `/photo`', () => {
        expect(searchHelper.metrikaBranch('/photo')).toEqual(buildMetrikaFor('photo'));
    });
    it('должен вернуть `album` для `/album`', () => {
        expect(searchHelper.metrikaBranch('/album')).toEqual(buildMetrikaFor('album'));
    });
    it('должен вернуть `trash` для `/trash`', () => {
        expect(searchHelper.metrikaBranch('/trash')).toEqual(buildMetrikaFor('trash'));
    });
    it('должен вернуть `history` для `/journal`', () => {
        expect(searchHelper.metrikaBranch('/journal')).toEqual(buildMetrikaFor('history'));
    });
    it('должен вернуть `attach` для `/attach`', () => {
        expect(searchHelper.metrikaBranch('/attach')).toEqual(buildMetrikaFor('attach'));
    });
    it('должен вернуть `disk` для всех остальных `idContext`', () => {
        expect(searchHelper.metrikaBranch('/disk/yoyoyo')).toEqual(buildMetrikaFor('disk'));
    });
});
