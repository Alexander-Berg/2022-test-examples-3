describe('Футер на десктопах', () => {
  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должен отображаться корректно при ширине экрана 1024', function() {
    return this.browser
      .setViewportSize({ width: 1024, height: 768 })
      .openComponent('footer', 'default', 'desktop')
      .yaAssertOuterView('footer-desktop-default', '.news-footer');
  });

  hermione.only.notIn('win-ie11', 'сторибук не открывается в ie');
  it('должен отображаться корректно при ширине экрана 1280', function() {
    return this.browser
      .setViewportSize({ width: 1280, height: 768 })
      .openComponent('footer', 'default', 'desktop')
      .yaAssertOuterView('footer-desktop-default-1280', '.news-footer');
  });
});
