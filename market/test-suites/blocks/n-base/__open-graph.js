import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import DescriptionSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-description';
import ImageSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-image';
import SiteNameSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-site-name';
import TitleSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-title';
import TypeSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-type';
import UrlSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__open-graph-url';

/**
 * Хелпер для упрощенного импорта OpenGraph сюитов
 */
const suite = makeSuite('OpenGraph.', {
    story: mergeSuites(
        prepareSuite(DescriptionSuite),
        prepareSuite(ImageSuite),
        prepareSuite(SiteNameSuite),
        prepareSuite(TitleSuite),
        prepareSuite(TypeSuite),
        prepareSuite(UrlSuite)
    ),
});

export default suite;
