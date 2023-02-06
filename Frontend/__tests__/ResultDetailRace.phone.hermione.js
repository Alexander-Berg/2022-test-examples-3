describe('Шапка матча (гонка)', () => {
  [
    'Биатлон',
    'Биатлон. Национальные участники',
    'Биатлон. Без результатов',
    'Биатлон. Предстоящая гонка в ближайшее время',
    'Биатлон. Предстоящая гонка не в ближайшее время',
  ].forEach((storyText) => {
    it(storyText, function() {
      return this.browser
        .openComponent('ResultDetailRace', storyText, 'phone')
        .waitForVisible('.sport-result-detail-race')
        .assertView('plain', '.sport-result-detail-race');
    });
  });
});
