import {makeCase, makeSuite, mergeSuites} from 'ginny';
import COOKIE_NAME from '@self/root/src/constants/cookie';
import {getCrushedEndpointSettingsCookie} from '@self/root/src/utils/resource/utils';
import {BACKENDS_NAME} from '@self/root/src/constants/backendsIdentifier';

const ProductDegradationSuite = makeSuite('Деградация.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Отказ сервиса pers-static.', {
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: getCrushedEndpointSettingsCookie(BACKENDS_NAME.PERS_STATIC),
                },
            },
            story: {
                'Оглавление карточки продукта отображается.': makeCase({
                    async test() {
                        await this.productCardHeader.root.isVisible()
                            .should
                            .eventually
                            .to
                            .be
                            .equal(true, 'Оглавление карточки товара должна быть видна.');
                    },
                }),
                'Контент карточки продукта отображается.': makeCase({
                    async test() {
                        await this.productCard.root.isVisible()
                            .should
                            .eventually
                            .to
                            .be
                            .equal(true, 'Контент карточки товара должен быть виден.');
                    },
                }),
            },
        })
    ),
});

export default ProductDegradationSuite;
