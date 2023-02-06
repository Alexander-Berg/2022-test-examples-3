import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок Roll
 * @param {PageObject.Roll} Roll
 */
export default makeSuite('Roll (промо)', {
    params: {
        snippetsCountBeforeLoading: 'Количество сниппетов до подгрузки',
        snippetsCountAfterLoading: 'Количество сниппетов после подгрузки',
    },
    story: {
        'По умолчанию': {
            'подгружает сниппеты по клику на кнопку "Показать еще"': makeCase({
                issue: 'MOBMARKET-10918',
                id: 'm-touch-2525',
                async test() {
                    const beforeSnippetsCount = await this.roll.getSnippetsCount();

                    await this.expect(beforeSnippetsCount).to.equal(
                        this.params.snippetsCountBeforeLoading,
                        'Исходное количество сниппетов корректно'
                    );

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.roll.clickLoadMoreButton(),
                        valueGetter: () => this.roll.getSnippetsCount(),
                    });
                    const snippetsCount = await this.roll.getSnippetsCount();

                    return this.expect(snippetsCount).to.equal(
                        this.params.snippetsCountAfterLoading,
                        'Сниппеты подгружаются корректно'
                    );
                },
            }),
        },
    },
});
