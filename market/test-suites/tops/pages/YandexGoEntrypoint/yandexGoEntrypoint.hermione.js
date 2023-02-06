import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

// page objects
import YandexGoCatalog from '@self/root/src/widgets/content/YandexGoCatalog/__pageObject';

// suites
import YandexGoEntrypointCatalog from '@self/platform/spec/hermione/test-suites/blocks/YandexGoEntrypointCatalog';
import yandexGoEntrypointCmsMarkup
    from '@self/root/src/spec/hermione/kadavr-mock/tarantino/yandexGoEntrypoint';
import navigationTree from '@self/root/src/spec/hermione/kadavr-mock/cataloger/expressNavigationTree';

// fixtures
import {minimalWarehouses} from '@self/root/src/spec/hermione/kadavr-mock/report/warehouses';

const regionId = 213;
const gpsCoordinate = '37.541773,55.749461';
const nid = 23281830;

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница Yandex Go Entrypoint.', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                yandexGoCatalog: () => this.createPageObject(YandexGoCatalog),
            });
        },
        'Авторизованный пользователь': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [yandexGoEntrypointCmsMarkup]
                    );
                    await this.browser.setState(
                        'Cataloger.tree', navigationTree
                    );

                    await this.browser.setState('report', minimalWarehouses);

                    await this.browser.yaProfile('testachi1', 'market:yandex-go-entrypoint', {
                        lr: regionId,
                        gps: gpsCoordinate,
                    });
                },
            },
            prepareSuite(YandexGoEntrypointCatalog, {
                params: {
                    nid,
                    regionId,
                    gpsCoordinate,
                },
            })
        ),
    },
});
