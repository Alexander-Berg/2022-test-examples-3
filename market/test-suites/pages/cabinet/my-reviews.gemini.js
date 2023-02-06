import utils from '@yandex-market/gemini-extended-actions/';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';

import cookies from '@self/platform/constants/cookies';
import PersonalCabinetCard from '@self/platform/components/PersonalCabinetCard/__pageObject';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

const MY_REVIEWS_URL = '/my/reviews';

export default {
    suiteName: 'MyReviews',
    url: MY_REVIEWS_URL,
    before(actions) {
        utils.setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: cookies.USER_ACHIEVEMENTS_ONBOARDING_COOKIE,
                value: '1',
            },
            {
                name: cookies.LKOB_COOKIE,
                value: '1',
            },
        ]);
        // Не авторизуемся тут потому что есть тест для незалогина и тесты для разных логинов
    },
    childSuites: [
        {
            ...MainSuite,
            suiteName: 'UnathorizedReviews',
        },
        {
            ...MainSuite,
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.reviewsfortest.login,
                    password: profiles.reviewsfortest.password,
                    url: MY_REVIEWS_URL,
                });
                MainSuite.before(actions);
            },
            after(actions) {
                utils.logout.call(actions);
            },
        },
        {
            ...MainSuite,
            suiteName: 'NoReviews',
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.emptyWishlist.login,
                    password: profiles.emptyWishlist.password,
                    url: MY_REVIEWS_URL,
                });
                MainSuite.before(actions);
            },
            after(actions) {
                utils.logout.call(actions);
            },
        },
        {
            // Хотим видеть определённый отзыв первым в списке, прокидываем гет-параметр
            suiteName: 'firstReviewId-Should-Be-Greenfield-Review',
            selector: `${PersonalCabinetCard.root}:nth-child(1)`,
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.authorCabinet.login,
                    password: profiles.authorCabinet.password,
                    url: `${MY_REVIEWS_URL}?firstReviewId=93298550`,
                });
                // Здесь нужно ожидание, т.к. страница автоскроллится и без него скринтест захватывает
                // неправильную область
                actions.wait(1000);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            capture() {
            },
        },
    ],
};
