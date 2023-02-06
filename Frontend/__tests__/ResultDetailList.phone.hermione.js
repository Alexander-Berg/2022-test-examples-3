describe('Другие матчи', () => {
  it('должен корректно отображать футбольные матчи', function() {
    return this.browser
      .openComponent('ResultDetailList', 'Футбол', 'phone')
      .waitForVisible('.sport-result-detail-list')
      .assertView('plain', '.sport-result-detail-list');
  });

  it('должен корректно отображать хоккейные матчи', function() {
    return this.browser
      .openComponent('ResultDetailList', 'Хоккей', 'phone')
      .waitForVisible('.sport-result-detail-list')
      .assertView('plain', '.sport-result-detail-list');
  });
});
