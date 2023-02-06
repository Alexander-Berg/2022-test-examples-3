import {makeCase, makeSuite, mergeSuites} from 'ginny';
import dayjs from 'dayjs';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Notification from '@self/root/src/components/Notification/__pageObject';
import SearchForm from '@self/platform/spec/page-objects/widgets/SearchForm';
import RegionSelector from '@self/platform/spec/page-objects/widgets/content/RegionSelector';

const SAMARA_REGION_ID = 51;

/**
 * Тесты на виджет RegionPopup.
 * @param {PageObject.RegionPopup} regionPopup
 */
export default makeSuite('Блок региональной нотификации', {
    story: mergeSuites({
        beforeEach() {
            this.setPageObjects({
                regionPopup: () => this.createPageObject(RegionPopup),
                regionSelector: () => this.createPageObject(RegionSelector),
                search: () => this.createPageObject(SearchForm),
                notification: () => this.createPageObject(Notification),
            });
        },

        afterEach() {
            return this.browser.deleteCookie('yandex_gid');
        },

        'При вводе в серч запроса "женские платья в Самаре"': {
            beforeEach() {
                const queryParams = routes.index;
                return this.browser.deleteCookie('currentRegionId')
                    .then(() => this.browser.yaOpenPage('touch:index', queryParams));
            },
            'на 2 часа проставляется кука "lr" с регионом - Самара': makeCase({
                id: 'm-touch-1871',
                issue: 'MOBMARKET-6926',
                test() {
                    return Promise.resolve()
                        .then(() => this.search.suggestInputSetValue('женские платья в Самаре'))
                        .then(() => this.search.submitForm())
                        .then(() => this.browser.waitForVisible(RegionPopup.regionName, 15000))
                        .then(() => this.browser.waitUntil(() => this.browser.getCookie('lr'), 4000))
                        .then(lrCookie => {
                            if (!lrCookie || isNaN(lrCookie.value)) {
                                return false;
                            }
                            const cookieValue = Number(lrCookie.value);
                            const cookieExpireTime = dayjs.unix(lrCookie.expiry);
                            const cookieHoursLifeTime = Math.round(
                                cookieExpireTime.diff(dayjs(), 'minute') / 60
                            );
                            const isExpectedRegionCookie = cookieValue === SAMARA_REGION_ID;
                            const isExpectedCookieLifeTime = cookieHoursLifeTime === 2;

                            return isExpectedRegionCookie && isExpectedCookieLifeTime;
                        })
                        .should.eventually.be.equal(true, 'Кука "lr" c регионом Самара проставилась на 2 часа');
                },
            }),
        },
    }),
});
