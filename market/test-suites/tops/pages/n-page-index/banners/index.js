import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {getHeadBannerSample, generateKeyByBannerParams} from '@yandex-market/kadavr/mocks/Adfox/helpers';

import {HEADBANNER_ADFOX_PARAMS, HEADBANNER_TARGET_PATH} from '@self/platform/spec/hermione/configs/banners';
import AdfoxBannerSuite from '@self/platform/spec/hermione/test-suites/blocks/AdfoxBanner';
import HeadBannerPresenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/presence';
import HeadBannerPassageSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/passage';
import StaticBanner from '@self/platform/spec/page-objects/Banner/StaticBanner';
import Banner from '@self/root/src/components/Banner/__pageObject';
import HeadBanner from '@self/platform/spec/page-objects/HeadBanner';

export default makeSuite('Баннеры.', {
    story: mergeSuites(
        {
            beforeEach() {
                return this.browser.yaOpenPage('market:index');
            },
        },
        makeSuite('Cтатический баннер.', {
            story: prepareSuite(AdfoxBannerSuite, {
                pageObjects: {
                    banner() {
                        return this.createPageObject(StaticBanner);
                    },
                },
            }),
        }),
        makeSuite('Боковая панель с баннерами.', {
            story: prepareSuite(AdfoxBannerSuite, {
                pageObjects: {
                    banner() {
                        return this.createPageObject(Banner);
                    },
                },
            }),
        }),
        makeSuite('Баннер-растяжка.', {
            environment: 'kadavr',
            story: mergeSuites(
                makeSuite('При корректном ответе Adfox', {
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                const headBannerKey = generateKeyByBannerParams(HEADBANNER_ADFOX_PARAMS);
                                const headBannerEntity = Object.assign(
                                    {},
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
                                await this.browser.yaOpenPage('market:index');
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
                                id: 'marketfront-3298',
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
                                id: 'marketfront-3299',
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
