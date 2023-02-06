import {makeSuite, makeCase} from 'ginny';

const TOOLTIP_IMPLEMENTATION_SELECTOR = 'body > .tooltip';

/**
 * Тесты на иконку рейтинга
 * @property {PageObject.Rating} this.rating - иконка рейтинга
 * @property {PageObject.Tooltip} this.tooltip - тултип с выводом рейтинга
 */

export default makeSuite('Визитка карточки модели', {
    feature: 'Визитка',
    story: {
        'Тултип на иконке рейтинга': {
            'по умолчанию': {
                'не отображается': makeCase({
                    id: 'marketfront-1495',
                    test() {
                        // жесткая завязка на текущую ужасную реализацию тултипов
                        return this.browser.isExisting(TOOLTIP_IMPLEMENTATION_SELECTOR)
                            .should.eventually.equal(false, 'Тултип не отображается');
                    },
                }),
            },
            'при наведении на иконку рейтинга': {
                beforeEach() {
                    return this.rating.getSelector()
                        .then(selector => this.browser.moveToObject(selector));
                },
                'отображается и содержит ожидаемый текст': makeCase({
                    id: 'marketfront-1493',
                    test() {
                        return this.browser.isVisible(TOOLTIP_IMPLEMENTATION_SELECTOR)
                            .should.eventually.equal(true, 'После наведения на иконку тултип становится видим')
                            .then(() => this.browser.getText(TOOLTIP_IMPLEMENTATION_SELECTOR))
                            .should.eventually.match(
                                /Рейтинг модели \d(\W\d+)? из 5/,
                                'Тултип содержит ожидаемый текст'
                            );
                    },
                }),
            },
        },
    },
});
