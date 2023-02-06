import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

// suites
import LogoCarouselSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/parts/LogoCarousel';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import LogoCarousel from '@self/platform/spec/page-objects/widgets/parts/LogoCarousel';

import brandPage from './fixtures/brand-page';

export default makeSuite('Бренд', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Страница бренда', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('Tarantino.data.result', [brandPage]);
                        await this.browser.yaOpenPage('touch:brands', {
                            brandId: 153043,
                            slug: 'apple',
                        });
                        await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
                prepareSuite(LogoCarouselSuite, {
                    pageObjects: {
                        logoCarousel() {
                            return this.createPageObject(LogoCarousel);
                        },
                    },
                })
            ),
        })
    ),
});
