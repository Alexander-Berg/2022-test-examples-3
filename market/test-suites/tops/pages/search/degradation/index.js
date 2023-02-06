import {makeSuite, makeCase, mergeSuites} from 'ginny';
import COOKIE_NAME from '@self/root/src/constants/cookie';
import {getCrushedEndpointSettingsCookie} from '@self/root/src/utils/resource/utils';
import {BACKENDS_NAME} from '@self/root/src/constants/backendsIdentifier';

const SearchPageDegradationSuite = makeSuite('Деградация.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Отказ сервиса pers-authors.', {
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: getCrushedEndpointSettingsCookie(BACKENDS_NAME.PERS_AUTHOR),
                },
            },
            story: {
                'Блок с результатами поиска отображается.': makeCase({
                    async test() {
                        await this.searchResults.isVisible()
                            .should
                            .eventually
                            .to
                            .be
                            .equal(true, 'Блок с результатами поиска должен отображаться.');
                    },
                }),
            },
        })
    ),
});

export default SearchPageDegradationSuite;
