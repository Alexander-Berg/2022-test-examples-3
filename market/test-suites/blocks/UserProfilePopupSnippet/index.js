import {makeSuite, makeCase, prepareSuite, mergeSuites} from 'ginny';

import PopupContentSuite from '@self/platform/spec/hermione/test-suites/blocks/UserProfilePopupSnippet/popupContent';

/**
 * Тесты на попап в профиле пользователя
 *
 * @property {PageObject.UserProfilePopupSnippet} userProfilePopupSnippet
 */
export default makeSuite('Профиль пользователя.', {
    environment: 'kadavr',
    feature: 'Тултип профиля пользователя',
    params: {
        willTooltipOpen: 'Будет ли открываться тултип',
        hoverOnRootNode: 'Функция для того чтобы активировать тултип',
        userName: 'Имя пользователя',
        publicId: 'public_id пользователя',
        reviewLink: 'Текст который будет на ссылке для отзывов',
        isExpertiseBlockVisible: 'Будет ли виден блок с ачивками поьзователя',
    },
    story: mergeSuites(
        makeSuite('При наведении', {
            story: {
                'появляется тултип профиля пользователя': makeCase({
                    id: 'marketfront-2562',
                    issue: 'MARKETVERSTKA-29249',
                    test() {
                        const checkPopupVisible = expectedValue =>
                            this.userProfilePopupSnippet.isRootVisible()
                                .should.eventually
                                .to.be.equal(expectedValue, expectedValue
                                    ? 'Тултип видно'
                                    : 'Тултип не видно'
                                );

                        return checkPopupVisible(false)
                            .then(() => this.params.hoverOnRootNode.call(this))
                            .then(() => checkPopupVisible(this.params.willTooltipOpen));
                    },
                }),
            },
        }),

        prepareSuite(PopupContentSuite, {
            hooks: {
                beforeEach() {
                    if (this.params.willTooltipOpen === false) {
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip('Попап не должен открываться, проверку контента пропускаем');
                    }
                    return this.params.hoverOnRootNode.call(this);
                },
            },
        })
    ),
});
