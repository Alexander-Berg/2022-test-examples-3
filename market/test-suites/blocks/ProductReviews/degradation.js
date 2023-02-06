import {makeSuite, makeCase, mergeSuites} from 'ginny';
import COOKIE_NAME from '@self/root/src/constants/cookie';
import {getCrushedEndpointSettingsCookie} from '@self/root/src/utils/resource/utils';
import {BACKENDS_NAME} from '@self/root/src/constants/backendsIdentifier';

export default makeSuite('Деградация.', {
    story: mergeSuites(
        makeSuite('Отказ сервиса с отзывами.', {
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: getCrushedEndpointSettingsCookie(BACKENDS_NAME.PERS_STATIC),
                },
            },
            story: {
                'Блок "Не удалось загрузить отзывы" должен отображаться.': makeCase({
                    async test() {
                        await this.productReviewsPage.getDegradationView.isVisible()
                            .should
                            .eventually
                            .to
                            .be
                            .equal(true, 'Блок "Не удалось загрузить отзывы" должен отображаться.');
                    },
                }),
            },
        })
    ),
});

