describe('edu-components_MarkerSeparator', () => {
  describe('separators', () => {
    [
      'placeholder',
      'together',
      'separate',
      'comma',
      'dot',
      'new-line',
      'hyphen',
      'colon',
      'semi-colon',
      'question',
      'exclamation',
      'minus',
      'open-quote',
      'close-quote',
      'open-brace',
      'close-brace',
      'double-quote',
      'dash',
    ].map(separator =>
      it(separator, function() {
        return this.browser
          .setViewportSize({ width: 500, height: 500 })
          .openComponent('edu-components', 'markerseparator', 'separators')
          .assertView('plain', [`.${separator}`]);
      }),
    );
  });

  describe('flavors', () => {
    ['blueberry'].map(flavor =>
      it(flavor, function() {
        return this.browser
          .setViewportSize({ width: 800, height: 600 })
          .openComponent('edu-components', 'markerseparator', 'flavors')
          .assertView('plain', [`.${flavor}`]);
      }),
    );
  });
});
