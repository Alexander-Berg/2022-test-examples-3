hermione.only.notIn(['linux-firefox', 'win-ie11'], 'сторибук не открывается в ie, проблемы со скроллом в firefox');
describe('Сюжет', () => {
  it('должен корректно отображать все поля', function() {
    return this.browser
      .openComponent('Story', 'все поля', 'desktop')
      .waitForVisible('.sport-story')
      .assertView('plain', '#container');
  });

  it('должен корректно отображать минимум полей', function() {
    return this.browser
      .openComponent('Story', 'минимум полей', 'desktop')
      .waitForVisible('.sport-story')
      .assertView('plain', '#container');
  });

  it('должен отключать комментарии', function() {
    return this.browser
      .openComponent('Story', 'выключенные комментарии', 'desktop')
      .waitForVisible('.sport-story')
      .assertView('plain', '#container');
  });
});
