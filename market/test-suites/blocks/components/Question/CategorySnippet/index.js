import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Сниппет категории в категорийных вопросах.', {
    params: {
        expectedLink: 'Ожидаемая ссылка в хлебной крошке',
    },
    story: {
        'Хлебная крошка.': {
            'По умолчанию': {
                'содержит корректную ссылку': makeCase({
                    id: 'm-touch-3029',
                    issue: 'MARKETFRONT-5119',
                    async test() {
                        await this.browser.yaWaitForChangeUrl(() => this.categorySnippet.clickLink());

                        const currentUrl = await this.browser.getUrl();
                        return this.expect(currentUrl).to.be.link(
                            this.params.expectedLink,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                    },
                }),
            },
        },
    },
});
