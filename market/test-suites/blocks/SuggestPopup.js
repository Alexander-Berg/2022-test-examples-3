import {makeCase} from 'ginny';

/**
 * Тесты на блок mini-suggest__popup.
 * @param {PageObject.MiniSuggestPopup} miniSuggestPopup
 */
export default {
    'Блок mini-suggest__popup': {
        'по умолчанию': {
            'не должен быть виден на странице': makeCase({
                id: 'm-touch-1866',
                issue: 'MOBMARKET-6843',
                test() {
                    return this.miniSuggestPopup
                        .isVisibleInViewPort()
                        .should.eventually.to.equal(false, 'Попап не виден во вьюпорте');
                },
            }),
        },
    },
};
