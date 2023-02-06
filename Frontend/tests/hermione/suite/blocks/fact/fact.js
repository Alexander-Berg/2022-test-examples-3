const selectors = require('../../../page-objects');

const testFact = async function({ mock }) {
    const { index, header } = selectors;

    await this.browser
        .ywOpenPage('?lat=56.833624554166185&lon=60.58615077182291', {
            lang: this.lang,
            query: {
                usemock: mock,
            },
        })
        .ywWaitForVisible(index.Fact, 5000)
        .pause(50) // possible layout shift
        .ywHideCamerasAndNews()
        .assertView('fact', index.Fact, {
            invisibleElements: [header.White, index.MapImg, index.MapAlert]
        });
};

describe('Блоки', function() {
    describe('Главная страница', function() {
        describe('Факт и почасовой', function() {
            it.langs.full();
            it('Везде отрицательная температура', async function() {
                return testFact.call(this, {
                    mock: 'fact_minus_temp',
                });
            });

            it.langs.full();
            it('Везде нулевая температура', async function() {
                return testFact.call(this, {
                    mock: 'fact_zero_temp',
                });
            });

            it.langs.full();
            it('Сильный дождь, гроза', async function() {
                return testFact.call(this, {
                    mock: 'long_condition_name',
                });
            });
        });
    });
});
