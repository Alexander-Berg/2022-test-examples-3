describe('edu-components_CompoundTable', () => {
  function runCompoundTableTest(browser, screen) {
    return browser
      .openComponent('edu-components', 'compoundtable', screen)
      .setViewportSize({ width: 1366, height: 768 })
      .assertView('without-scroll', ['.Gemini'])
      .setViewportSize({ width: 500, height: 700 })
      .assertView('scroll-left', ['.Gemini'])
      .execute(function() {
        var rightParts = document.querySelectorAll('.CompoundTable-Right');

        for (var i = 0; i < rightParts.length; i = i + 1) {
          var rightPart = rightParts[i];
          rightPart.scrollLeft = (rightPart.scrollWidth - rightPart.clientWidth) / 2;
        }
      })
      .assertView('scroll-center', ['.Gemini'])
      .execute(function() {
        var rightParts = document.querySelectorAll('.CompoundTable-Right');

        for (var i = 0; i < rightParts.length; i = i + 1) {
          var rightPart = rightParts[i];
          rightPart.scrollLeft = rightPart.scrollWidth - rightPart.clientWidth;
        }
      })
      .assertView('scroll-right', ['.Gemini']);
  }

  it('should have closed controls', function() {
    return runCompoundTableTest(this.browser, 'closed-controls');
  });

  it('should have opened controls', function() {
    return runCompoundTableTest(this.browser, 'opened-controls');
  });
});
