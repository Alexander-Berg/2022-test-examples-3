import {makeSuite, makeCase, mergeSuites} from 'ginny';
import COOKIE_NAME from '@self/root/src/constants/cookie';
import {getCrushedEndpointSettingsCookie} from '@self/root/src/utils/resource/utils';
import {BACKENDS_NAME} from '@self/root/src/constants/backendsIdentifier';

export const ShopReviewsDegradationSuite = makeSuite('Деградация.', {
    story: mergeSuites(
        makeSuite('Отказ сервиса с отзывами.', {
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: getCrushedEndpointSettingsCookie(BACKENDS_NAME.PERS_STATIC),
                },
            },
            story: {
                'Блок "Ни одного отзыва" отображается.': makeCase({
                    async test() {
                        await this.zeroState.root.isVisible()
                            .should
                            .eventually
                            .to
                            .be
                            .equal(true, 'Блок "Ни одного отзыва" должен отображаться.');
                    },
                }),
            },
        })
    ),
});
export default ShopReviewsDegradationSuite;
