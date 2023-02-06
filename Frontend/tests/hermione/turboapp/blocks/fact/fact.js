const selectors = require('../../../page-objects');

const testFact = async function({ mock, url }) {
    const { index, header } = selectors;

    await this.browser
        .ywOpenPage(url, {
            lang: this.lang,
            query: {
                usemock: mock,
            },
        })
        .ywWaitForVisible(index.HourlyMain, 5000)
        .ywDeleteAdvs()
        .ywHideCamerasAndNews()
        .ywHidePopup()
        .assertView('fact2', index.Fact, {
            invisibleElements: [header.White, index.MapImg, index.MapAlert]
        });
};

describe('Блоки', function() {
    describe('Главная страница', function() {
        describe('Факт и почасовой', function() {
            it.langs.full();
            it('В домашней локации в абсорбации', async function() {
                return testFact.call(this, {
                    mock: 'fact_minus_temp_home_location',
                    url: '/161833?startScreen=1',
                });
            });

            it.langs.full();
            it('В абсорбации', async function() {
                return testFact.call(this, {
                    mock: 'fact_zero_temp',
                    url: 'tver',
                });
            });
        });
    });
});
