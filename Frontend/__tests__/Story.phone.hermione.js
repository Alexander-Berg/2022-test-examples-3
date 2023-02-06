describe('Сюжет', () => {
  hermione.only.notIn(['appium-chrome-phone'], 'расскипать в NERPA-11625');
  it('должен корректно отображать все поля', function() {
    return this.browser
      .openComponent('Story', 'все поля', 'phone')
      .waitForVisible('.sport-story')
      .assertView('plain', '#container');
  });

  it('должен корректно отображать минимум полей', function() {
    return this.browser
      .openComponent('Story', 'минимум полей', 'phone')
      .waitForVisible('.sport-story')
      .assertView('plain', '#container');
  });

  hermione.only.notIn(['appium-chrome-phone'], 'расскипать в NERPA-11625');
  it('должен отключать комментарии', function() {
    return this.browser
      .openComponent('Story', 'выключенные комментарии', 'phone')
      .waitForVisible('.sport-story')
      .assertView('plain', '#container');
  });
});
