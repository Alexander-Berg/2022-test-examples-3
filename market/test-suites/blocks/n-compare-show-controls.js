import {makeCase, makeSuite} from 'ginny';
import ComparisonGroup from '@self/platform/widgets/content/compare/Content/ComparisonGroup/__pageObject';

/**
 * Тесты на блок n-compare-show-controls.
 * @param {PageObject.CompareShowControls} compareShowControls
 * @param {PageObject.CompareContent} CompareContent
 */
export default makeSuite('Кнопки переключения вида показываемых характеристик.', {
    environment: 'kadavr',
    feature: 'Характеристики модели',
    id: 'marketfront-26',
    issue: 'MARKETVERSTKA-23790',
    story: {
        'По умолчанию.': {
            'Активирована кнопка "Различающиеся характеристики"': {
                test() {
                    return this.compareShowControls.isDiffActive()
                        .should.eventually.equal(true, 'Кнопка "различающиеся характеристики" активирована')

                        .then(() => this.compareShowControls.isAllActive())
                        .should.eventually.equal(false, 'Кнопка "все характеристики" не активирована');
                },

            },
        },

        'Кнопка "Все характеристики".': {
            'При нажатии': {
                'показываются одинаковые параметры в сравнении': makeCase({
                    async test() {
                        const groupsCountWithoutEqual = await this.compareContent.elems(ComparisonGroup.root)
                            .then(result => result.value.length);
                        await this.compareShowControls.clickOnAll();

                        return this.compareContent.elems(ComparisonGroup.root)
                            .then(result => result.value.length)
                            .should.eventually.to.be.above(
                                groupsCountWithoutEqual,
                                'Параметры с одинаковыми значениями отображаются'
                            );
                    },
                }),
            },
        },

        'Кнопка "Различающиеся характеристики".': {
            'При нажатии': {
                'показываются различающиеся параметры в сравнении': makeCase({
                    async test() {
                        await this.compareShowControls.clickOnAll();
                        const groupsCountWithEqual = await this.compareContent.elems(ComparisonGroup.root)
                            .then(result => result.value.length);

                        await this.compareShowControls.clickOnDiff();
                        return this.compareContent.elems(ComparisonGroup.root)
                            .then(result => result.value.length)
                            .should.eventually.to.be.below(
                                groupsCountWithEqual,
                                'Параметры с одинаковыми значениями скрыты'
                            );
                    },
                }),
            },
        },
    },
});
