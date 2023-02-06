hermione.only.notIn(['win-ie11'], 'сторибук не открывается в ie');
describe('Шапка матча (новый дизайн)', () => {
  [
    'Хоккей. Прошедший матч',
    'Хоккей. Матч в процессе',
    'Хоккей. Матч не в ближайшем будущем',
    'Хоккей. Матч в ближайшем будущем',
    'Хоккей. Победа в овертайме',
    'Хоккей. Победа по буллитам',
    'Футбол. Перенесенный матч',
    'Футбол. Победа по пенальти',
    'Баскетбол',
  ].forEach((storyText) => {
    it(storyText, function() {
      return this.browser
        .openComponent('MatchHeader', storyText, 'desktop')
        .setViewportSize({ width: 648, height: 480 })
        .assertView('plain', '.sport-match-header');
    });
  });
});
