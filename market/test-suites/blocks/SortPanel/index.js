import {makeCase, makeSuite} from 'ginny';

export default makeSuite('Иконка карты', {
    feature: 'Переход на карту',
    story: {
        'По умолчанию': {
            'содержит корректную ссылку': makeCase({
                params: {
                    route: 'Роут',
                    routeParams: 'Параметры роута',
                },
                test() {
                    return this.browser.allure.runStep('Проверяем ссылку', async () => {
                        const link = await this.sortPanel.getMapLink();
                        const expectedLink = this.browser.yaBuildURL(this.params.route, this.params.routeParams);

                        const comparisonParams = {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        };

                        return this.expect(link).to.be.link(expectedLink, comparisonParams);
                    });
                },
            }),
        },
    },
});
