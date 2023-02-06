import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import DescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-description';
import ImageSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-image';
import UrlSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-url';

/**
 * Хелпер для упрощенного импорта OpenGraph сюитов
 */
const suite = makeSuite('OpenGraph.', {
    story: mergeSuites(
        prepareSuite(DescriptionSuite),
        prepareSuite(ImageSuite),
        prepareSuite(UrlSuite)
    ),
});

export default suite;
