import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import CommentAuthorContainerSuite from '@self/platform/spec/hermione/test-suites/blocks/n-comment-author/__container';
/**
 * Тесты на блок фильтра
 * @property {PageObject.FilterBlock} this.filterBlock - блок фильтра
 */
export default makeSuite('Блок комментариев в отзывах.', {
    story: mergeSuites(
        prepareSuite(CommentAuthorContainerSuite)
    ),
});
