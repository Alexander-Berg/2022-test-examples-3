import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на проверку содержания слагов.
 * @param {string|undefined} this.params.entity — сущность
 * @param {string} this.params.selector — CSS селектор
 */
export default makeSuite('Проверка ссылок на слаг.', {
    environment: 'kadavr',
    params: {
        entity: 'Cущность',
        selector: 'CSS селектор',
    },
    story: {
        'По умолчанию': {
            'ссылки содержат слаг': makeCase({
                async test() {
                    /**
                     * Проверка, которая позволит не падать тесту
                     */
                    const hasElements = await this.browser.elements(this.params.selector)
                        .then(({value}) => value.length > 0);

                    if (!hasElements) {
                        return this.browser.allure.runStep('Нет ссылок, которые надо проверить', () => true);
                    }

                    const hrefs = await this.browser.getAttribute(this.params.selector, 'href');
                    const arrayHrefs = Array.isArray(hrefs) ? hrefs : [hrefs];
                    const checkHrefs = arrayHrefs
                        // Фильтруем ссылку с добавление отзыва, потому что в ней нет слага
                        .filter(href => !href.includes('reviews/add'))
                        .map(href => this.expect(href, 'Ссылка должна содержать слаг')
                            .to.be.link({
                                pathname: new RegExp(`^\\/${this.params.entity || '[\\w]'}+--[\\w-]+(/)?`),
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            })
                        );

                    return Promise.all(checkHrefs);
                },
            }),
        },
    },
});
