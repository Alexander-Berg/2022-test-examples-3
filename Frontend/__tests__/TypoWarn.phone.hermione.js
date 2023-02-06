describe('Сообщение об опечатке', () => {
  it('должено отображаться корректно, когда предлагается посмотреть исправленный запрос', function() {
    return this.browser
      .openComponent('typowarn', 'recommendation', 'phone')
      .yaWaitForVisible('.news-typo-warn')
      .assertView('typo-warn-recommendation', '.news-typo-warn');
  });

  it('должено отображаться корректно, когда опечатка исправлена', function() {
    return this.browser
      .openComponent('typowarn', 'fixed', 'phone')
      .yaWaitForVisible('.news-typo-warn')
      .assertView('typo-warn-fixed', '.news-typo-warn');
  });
});
