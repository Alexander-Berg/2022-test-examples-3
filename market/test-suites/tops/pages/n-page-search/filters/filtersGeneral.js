// flow

import {makeSuite, prepareSuite} from 'ginny';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import FiltersGeneralSuite from '@self/platform/spec/hermione/test-suites/blocks/FiltersAside/filtersGeneral';
import SearchFiltersAside from '@self/platform/spec/page-objects/SearchFiltersAside';

export default makeSuite('Блок фильтров на разных страницах выдачи', {
    environment: 'testing',
    feature: 'Фильтры',
    issue: 'MARKETFRONT-3727',
    story: createStories(
        [
            {
                id: 'marketfront-3721',
                description: 'Гуру-выдача',
                route: routes.list.phones,
            },
            {
                id: 'marketfront-3722',
                description: 'Гуру-лайт выдача',
                route: routes.list.dresses,
            },
            {
                id: 'marketfront-3723',
                description: 'Кластерная выдача',
                route: routes.list.stockings,
            },
            {
                id: 'marketfront-3724',
                description: 'Выдача книг',
                route: routes.list.book,
            },
            {
                id: 'marketfront-3725',
                description: 'Поисковая выдача',
                route: routes.search.adult,
            },
            {
                id: 'marketfront-3726',
                description: 'Мультивыдача',
                route: routes.geo.cats,
            },
        ],
        ({description, route}) => {
            const suiteParams = {
                pageObjects: {
                    filtersAside() {
                        return this.createPageObject(SearchFiltersAside);
                    },
                },
                params: {description, route},
            };
            return prepareSuite(FiltersGeneralSuite, suiteParams);
        }
    ),
});
