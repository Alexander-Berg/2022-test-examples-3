describe('Меню сюжета', () => {
  it('должено отображаться корректно', function() {
    return this.browser
      .openComponent('storysettings', 'default', 'phone')
      .yaWaitForVisible('.mg-story-menu')
      .assertView('story-menu-button', '.mg-story-menu')
      .click('.mg-story-menu__button')
      .yaShouldExist('.mg-story-menu__popup', 'Не открылся попап')
      .assertView('story-menu-popup', '.mg-story-menu__popup', { allowViewportOverflow: true });
  });
});
