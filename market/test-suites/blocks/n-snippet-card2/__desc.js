import {makeSuite, makeCase} from 'ginny';
import _ from 'lodash';

/**
 * Тесты на элемент desc, блока n-snippet-card2
 * @param {PageObject.SnippetCard2} snippetCard2
 */
export default makeSuite('Описание.', {
    environment: 'kadavr',
    feature: 'Сниппет ко/км',
    params: {
        type: 'Тип категории',
        count: 'Количество пунктов характеристик на сниппете',
        paramLength: 'Максимальная длина параметра в символах',
        region: 'Регион',
        specs: 'Особенности продукта',
    },
    story: {
        'По умолчанию': {
            'соответствует параметрам из репорта': makeCase({
                issue: 'MARKETVERSTKA-25904',
                id: 'marketfront-1391',
                async test() {
                    await this.snippetCard2.descriptionItems.waitForVisible();

                    return this
                        .snippetCard2
                        .descriptionItems
                        .then(elems => elems.value.length)
                        .should.eventually.be.within(1, this.params.count, 'Количество параметров на сниппете')
                        .then(() => {
                            if (this.params.type !== 'CLUSTER') {
                                const reportSpecs = _.cloneDeep(this.params.specs.friendly);

                                this.browser.allure.createAttachment(
                                    'Параметры из репорта',
                                    JSON.stringify(reportSpecs, null, 2),
                                    'application/json'
                                );

                                const slicedReportSpecs = _.slice(reportSpecs, 0, this.params.count);
                                return checkDescription.call(this, slicedReportSpecs);
                            }

                            return undefined;
                        });
                },
            }),
        },
    },
});

const checkDescription = function (reportSpecs) {
    const checkParameterReducer = (acc, [pageSpec, reportSpec]) => {
        const actual = reportSpec.length <= this.params.paramLength ? pageSpec : _.trimEnd('…', pageSpec);

        return acc && (_.startsWith(reportSpec, actual));
    };

    return this
        .snippetCard2
        .descriptionItems
        .getText()
        .then(pageSpecs => {
            this.browser.allure.createAttachment(
                'Параметры со страницы',
                JSON.stringify(pageSpecs, null, 2),
                'application/json'
            );
            return pageSpecs;
        })
        .then(pageSpecs => _.reduce(
            _.zip(pageSpecs, reportSpecs),
            checkParameterReducer,
            true
        ))
        .should.eventually.be.equal(
            true,
            `Первые ${this.params.count} параметров модели из репорта отображены на странице`
        );
};
