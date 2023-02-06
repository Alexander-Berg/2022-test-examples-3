import {mockLocation, mockRouterFabric} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';
import {getCookie} from '@self/root/src/utils/cookie';

const WIDGET_PATH = '@self/root/src/widgets/content/search/ViewTypeSelector';
/** @type {import('@yandex-market/testament/mirror').default} */
let mirror;
/** @type {import('@yandex-market/testament/mirror/layers/jest').default} */
let jestLayer;
/** @type {import('@yandex-market/testament/mirror/layers/mandrel').default} */
let mandrelLayer;
/** @type {import('@yandex-market/testament/mirror/layers/apiary').default} */
let apiaryLayer;


beforeAll(async () => {
    mockLocation();
    mockRouterFabric(require.resolve('@self/root/src/utils/router'))();
    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');
});

afterAll(() => {
    mirror.destroy();
});

describe('Куки viewtype', () => {
    it.each(['list', 'grid'])('должна быть куки viewtype - %s', async type => {
        await jestLayer.backend.runCode(resultsViewType => {
            const visibleSearchResultId = '1';
            // eslint-disable-next-line global-require
            const SearchByInitialParams = require('@self/root/src/resolvers/search/resolveSearchByInitialParams');
            // eslint-disable-next-line global-require
            const resolveSearchView = require('@self/root/src/resolvers/search/resolveSearchView');
            jest.spyOn(SearchByInitialParams, 'default').mockResolvedValue({
                result: visibleSearchResultId,
                collections: {visibleSearchResult: {
                    [visibleSearchResultId]: {
                        id: visibleSearchResultId, currentPageIds: [1, 2], resultsViewType,
                    },
                    searchResult: {
                        sr1: {id: 'sr1', visibleEntityIds: ['ve5']},
                    },
                }},
            });
            jest.spyOn(resolveSearchView, 'default').mockResolvedValue({
                result: visibleSearchResultId,
                collections: {
                    visualSearch: {},
                    searchView: {
                        [visibleSearchResultId]: {viewtype: resultsViewType},
                    },
                },
            });
        }, [type]);
        await mandrelLayer.initContext({
            params: {
                viewtype: type,
            },
        });
        await apiaryLayer.mountWidget(WIDGET_PATH, {searchPlace: 'test'});
        expect(getCookie('viewtype')).toEqual(type);
    });
});
