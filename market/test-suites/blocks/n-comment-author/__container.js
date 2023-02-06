import {makeSuite, prepareSuite} from 'ginny';

import UserProfilePopupSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/UserProfilePopupSnippet';
/**
 * Тесты на контейнер, в котором лежит аватарка и имя пользователя, оставившего комментарий
 * @param {PageObject.Comment} comment
 * @param {PageObject.ProductReviewItem} productReviewItem
 * @param {PageObject.CommentAuthor} commentAuthor
 * @param {PageObject.UserProfilePopupSnippet} userProfilePopupSnippet
 */
export default makeSuite('Контейнер, в котором лежит аватарка и имя пользователя, который оставил комментарий.', {
    feature: 'Комментарии к отзывам',
    environment: 'kadavr',
    story: {
        'Автор комментария.': prepareSuite(UserProfilePopupSnippetSuite, {
            params: {
                willTooltipOpen: true,
                hoverOnRootNode() {
                    return this.commentAuthor.hoverOnAuthorContainer();
                },
                userName: 'Pupkin Vasily',
                reviewLink: 'Написал 13 отзывов',
                isExpertiseBlockVisible: true,
            },
        }),
    },
});
