import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';

import ShareButtonsSuite from '@self/platform/spec/hermione/test-suites/blocks/SocialShareButtons/shareButtons';
/**
 * Тест на компонент SocialShareButtons.
 * @param {PageObject.SocialShareButtons} socialShareButtons
 */

export default makeSuite('Блок кнопок соц шаринга.', {
    feature: 'Блок кнопок соц шаринга',
    params: {
        shareInfo: 'Информация для шаринга',
    },
    story: mergeSuites(
        {
            'по-умолчанию': {
                'должен присутствовать.': makeCase({
                    test() {
                        return this.socialShareButtons
                            .isRootNodeVisible()
                            .should.eventually.be.equal(true, 'блок присутствует на странице');
                    },
                }),
            },
        },
        prepareSuite(ShareButtonsSuite)
    ),
});
