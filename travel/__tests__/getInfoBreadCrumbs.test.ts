import IParentChildStaticPage from '../../../interfaces/state/info/IParentChildStaticPage';
import Tld from '../../../interfaces/Tld';
import Lang from '../../../interfaces/Lang';

import getInfoBreadCrumbs from '../getInfoBreadCrumbs';
import getMainPage from '../../url/crumble/getMainPage';

const tld = Tld.ru;
const language = Lang.ru;

describe('getInfoBreadCrumbs', () => {
    it('Случай с отсутствием родительских статей', () => {
        const parents: IParentChildStaticPage[] = [];
        const title = 'currentTitle';

        expect(
            getInfoBreadCrumbs({
                title,
                parents,
                tld,
                language,
            }),
        ).toEqual([getMainPage('/'), {name: title}]);
    });

    it('Случай с присутствием родительских статей', () => {
        const parents: IParentChildStaticPage[] = [
            {
                id: 1,
                slug: 'slug1',
                title: 'title1',
            },
            {
                id: 2,
                title: 'title2',
            },
        ];
        const title = 'currentTitle';

        expect(
            getInfoBreadCrumbs({
                title,
                parents,
                tld,
                language,
            }),
        ).toEqual([
            getMainPage('/'),
            {name: 'title1', url: '/info/slug1'},
            {name: 'title2', url: '/info/2'},
            {name: title},
        ]);
    });
});
