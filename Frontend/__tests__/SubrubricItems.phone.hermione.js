describe('Подрубрики', () => {
  it('должны отображаться корректно (default)', function() {
    return this.browser
      .openComponent('subrubricitems', 'default', 'phone')
      .yaAssertOuterView('subrubrics-default', '.news-subrubric-items', { paddings: 5 });
  });

  it('должны отображаться корректно (last-active)', function() {
    return this.browser
      .openComponent('subrubricitems', 'last-active', 'phone')
      .yaAssertOuterView('subrubrics-last-active', '.news-subrubric-items', { paddings: 5 })
      .click('.Button2:nth-of-type(6).news-subrubric-items__button')
      .yaAssertOuterView('subrubrics-second-last-active', '.news-subrubric-items', { paddings: 5 });
  });
});
