import cloneDeep from 'lodash/cloneDeep';
import {makeSuite, mergeSuites, prepareSuite} from '@yandex-market/ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import COOKIES from '@self/root/src/constants/cookie';
import * as mocks from '@self/platform/spec/hermione/fixtures/product/productWithJumpTables';
import {routes} from '@self/platform/spec/hermione/configs/routes';

import secondParameterScrenshot from './secondParameterScrenshot';

const SMART_BANNER2_CLOSED_COOKIE = {
    name: COOKIES.SMART_BANNER2_CLOSED,
    value: '1',
};

const storyData =
    ['grid', 'list'].flatMap(view =>
        [1, 2, 10].flatMap(count =>
            ['detailed', 'quick', 'visual'].flatMap(templateType => {
                const snippetConfiguration = cloneDeep(mocks.templatorTarantinoMock);
                snippetConfiguration.snippets[0].template.type = templateType;
                snippetConfiguration.snippets[0].template.viewtype = view;

                const viewName = view === 'grid' ? 'Гридовый' : 'Листовой';

                return ({
                    description: `Шаблон ${templateType}. ${viewName} сниппет с параметрами второго рода. ${count} параметров`,
                    params: {
                        mock: mocks[`productWithJumpTable${count}`],
                        view,
                        snippetConfiguration,
                    },
                });
            })
        )
    );

export default makeSuite('Параметры 2-го рода', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-65216',
    story: mergeSuites(
        {
            async beforeEach() {
                this.browser.yaSetCookie(SMART_BANNER2_CLOSED_COOKIE);
            },
        },
        createStories(storyData, ({params}) => prepareSuite(secondParameterScrenshot, {
            hooks: {
                async beforeEach() {
                    const {mock, view, snippetConfiguration} = params;
                    const reportState = mergeState([
                        mock,
                        {
                            data: {
                                search: {
                                    'total': 1,
                                    'totalOffers': 1,
                                    view,
                                },
                            },
                        },
                    ]);
                    await this.browser.setState('report', reportState);
                    await this.browser.setState('Tarantino.data.result', [snippetConfiguration]);
                    return this.browser.yaOpenPage('touch:search', routes.search.default);
                },
            },
            params,
        }))
    ),
});
