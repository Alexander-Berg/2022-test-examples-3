describe('Сообщение об опечатке', () => {
  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должено отображаться корректно, когда предлагается посмотреть исправленный запрос', function() {
    return this.browser
      .openComponent('typowarn', 'recommendation', 'desktop')
      .setViewportSize({ width: 800, height: 600 })
      .yaWaitForVisible('.news-typo-warn')
      .assertView('typo-warn-recommendation', '.news-typo-warn');
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должено отображаться корректно, когда опечатка исправлена', function() {
    return this.browser
      .openComponent('typowarn', 'fixed', 'desktop')
      .setViewportSize({ width: 800, height: 600 })
      .yaWaitForVisible('.news-typo-warn')
      .assertView('typo-warn-fixed', '.news-typo-warn');
  });
});
