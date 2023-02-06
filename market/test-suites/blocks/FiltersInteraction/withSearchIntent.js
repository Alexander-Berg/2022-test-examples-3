import {makeSuite, makeCase} from 'ginny';
import {waitForSuccessfulSnippetListUpdate} from '@self/platform/spec/hermione/helpers/filters';

/**
 * Тест на взаимодействие фильтров с деревом категорий.
 * @property {PageObject} filterCheckbox
 */
export default makeSuite('Взаимодействие с деревом категорий.', {
    environment: 'testing',
    params: {
        queryParamName: 'Имя параметра, которое будем проверять',
        queryParamValue: 'Значение параметра, которое будем проверять',
    },
    story: {
        'При взаимодействии': {
            'фильтры сохраняются в урле после перехода на категорию выше.': makeCase({
                id: 'marketfront-616',
                issue: 'MARKETVERSTKA-24659',
                async test() {
                    const {queryParamName, queryParamValue} = this.params;

                    const clickOnCheckbox = () => this.filterCheckbox.clickCheckbox();

                    const checkForQueryParamPresence = () =>
                        this.browser
                            .yaCheckUrlParams({[queryParamName]: queryParamValue})
                            .should.eventually.to.be.equal(true, 'Параметры присутствуют в урле');

                    const clickOnFirstIntentLink = () => this.searchIntent.clickFirstLink();
                    const waitUntilPageLoaded = () => this.browser.yaWaitForPageReady();

                    // скроллим чтобы лучше срабатывал клик
                    const filterCheckboxSelector = await this.filterCheckbox.getSelector();
                    await this.browser.scroll(filterCheckboxSelector);

                    await waitForSuccessfulSnippetListUpdate(
                        this.browser,
                        clickOnCheckbox,
                        this.snippetList
                    );
                    await checkForQueryParamPresence();
                    await clickOnFirstIntentLink();
                    await waitUntilPageLoaded();
                    return checkForQueryParamPresence();
                },
            }),
        },
    },
});
