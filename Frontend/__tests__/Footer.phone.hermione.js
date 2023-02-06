describe('Футер на тачах', () => {
  it('должен отображаться корректно', function() {
    return this.browser
      .openComponent('footer', 'default', 'phone')
      .yaAssertOuterView('footer-phone-default', '.news-footer');
  });
});
