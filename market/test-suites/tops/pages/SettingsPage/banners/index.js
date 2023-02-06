import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {getHeadBannerSample, generateKeyByBannerParams} from '@yandex-market/kadavr/mocks/Adfox/helpers';

import HeadBannerPresenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/presence';
import HeadBannerPassageSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/passage';
import HeadBanner from '@self/platform/spec/page-objects/HeadBanner';
import {HEADBANNER_ADFOX_PARAMS, HEADBANNER_TARGET_PATH} from '@self/platform/spec/hermione/configs/banners';

export default makeSuite('Баннеры.', {
    story: mergeSuites(
        makeSuite('Баннер-растяжка.', {
            story: mergeSuites(
                makeSuite('При корректном ответе Adfox', {
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                const headBannerKey = generateKeyByBannerParams(HEADBANNER_ADFOX_PARAMS);
                                const headBannerEntity = Object.assign(
                                    getHeadBannerSample(),
                                    {
                                        entity: 'banner',
                                        ad_place: HEADBANNER_ADFOX_PARAMS.id,
                                        link: HEADBANNER_TARGET_PATH,
                                        backgroundColor: 'blue',
                                    }
                                );

                                const adfoxState = {
                                    [headBannerKey]: headBannerEntity,
                                };

                                await this.browser.setState('Adfox.data.collections.banners', adfoxState);
                                await this.browser.yaOpenPage('market:my-settings');
                            },
                        },
                        prepareSuite(HeadBannerPresenceSuite, {
                            suiteName: 'Проверка видимости.',
                            pageObjects: {
                                headBanner() {
                                    return this.createPageObject(HeadBanner);
                                },
                            },
                            meta: {
                                environment: 'kadavr',
                                id: 'marketfront-3310',
                                issue: 'MARKETVERSTKA-33081',
                            },
                        }),

                        prepareSuite(HeadBannerPassageSuite, {
                            pageObjects: {
                                headBanner() {
                                    return this.createPageObject(HeadBanner);
                                },
                            },
                            meta: {
                                environment: 'kadavr',
                                id: 'marketfront-3311',
                                issue: 'MARKETVERSTKA-33081',
                            },
                            params: {
                                expectedUrl: HEADBANNER_TARGET_PATH,
                            },
                        })
                    ),
                })
            ),
        })
    ),
});
