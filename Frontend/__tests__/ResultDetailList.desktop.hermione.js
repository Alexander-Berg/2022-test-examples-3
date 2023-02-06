hermione.only.notIn(['linux-firefox', 'win-ie11'], 'сторибук не открывается в ie, проблемы со скроллом в firefox');
describe('Другие матчи', () => {
  it('должен корректно отображать футбольные матчи', function() {
    return this.browser
      .openComponent('ResultDetailList', 'Футбол', 'desktop')
      .waitForVisible('.sport-result-detail-list')
      .assertView('plain', '.sport-result-detail-list');
  });

  it('должен корректно отображать хоккейные матчи', function() {
    return this.browser
      .openComponent('ResultDetailList', 'Хоккей', 'desktop')
      .waitForVisible('.sport-result-detail-list')
      .assertView('plain', '.sport-result-detail-list');
  });
});
