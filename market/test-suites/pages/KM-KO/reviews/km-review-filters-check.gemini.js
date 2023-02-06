import ProductReviewsList from '@self/platform/spec/page-objects/components/ProductReviewsList';
import ProductRatingStat from '@self/platform/spec/page-objects/components/ProductRatingStat';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'KM-reviews-filter-check',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideProductTabs(actions);
        // Вынести сюда click и ожидание - нельзя, иначе получим StaleElementReference.
    },
    childSuites: [
        {
            suiteName: 'shouldFilterModificationReviewsWithOneStar',
            url: {
                pathname: '/product--noutbuk-hp-pavilion/7720766/reviews',
                query: {
                    sort_desc: 'asc',
                    sort_by: 'date',
                },
            },
            selector: ProductReviewsList.root,
            capture(actions, find) {
                actions.click(find(`${ProductRatingStat.reviewItem}:last-child`));
                // Здесь нужно ожидание т.к. оверлей может появиться и исчезнуть очень быстро
                actions.wait(500);
                actions.waitForElementToHide(ProductReviewsList.overlay, 5000);
                // Здесь нужно ожидание т.к. комменты подгружаются не сразу
                actions.wait(500);
            },
        },
        {
            suiteName: 'shouldFilterGuruReviewsWithOneStar',
            url: {
                pathname: '/product--planshet-samsung-galaxy-tab-p1000-16gb/6407300/reviews',
                query: {
                    sort_desc: 'asc',
                    sort_by: 'date',
                },
            },
            selector: ProductReviewsList.root,
            capture(actions, find) {
                actions.click(find(`${ProductRatingStat.reviewItem}:last-child`));
                // Здесь нужно ожидание т.к. оверлей может появиться и исчезнуть очень быстро, и ожидание его
                actions.wait(500);
                actions.waitForElementToHide(ProductReviewsList.overlay, 5000);
                // Здесь нужно ожидание т.к. комменты подгружаются не сразу
                actions.wait(500);
            },
        },
    ],
};
