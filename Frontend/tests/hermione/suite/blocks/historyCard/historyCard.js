const selectors = require('../../../page-objects').index;

const testHistoryCard = async function({ mock }) {
    await this.browser
        .ywOpenPage('?lat=56.833624554166185&lon=60.58615077182291', {
            lang: this.lang,
            query: {
                usemock: mock,
                showmethehamster: { spa_widget_promo_force: 1 }
            },
        })
        .ywWaitForVisible(selectors.HistoryCard, 5000)
        .ywDeletePromos()
        .ywHideCamerasAndNews()
        .pause(200)
        .assertView('HistoryCard', selectors.HistoryCard);
};

describe('Блоки', function() {
    describe('Главная страница', function() {
        describe('Историческая карточка', function() {
            it.langs.full();
            it('Солнечные дни, нет осадков и ветра', async function() {
                return testHistoryCard.call(this, {
                    mock: 'history_card_no_prec_and_no_wind',
                });
            });

            it.langs.full();
            it('Нулевая и плюсовая температура', async function() {
                return testHistoryCard.call(this, {
                    mock: 'history_card_zero_temp',
                });
            });

            it('Нет солнечных дней, минусовая температура', async function() {
                return testHistoryCard.call(this, {
                    mock: 'history_card_minus_temp',
                });
            });
        });
    });
});
