describe('edu-components_Textinput_Lyceum', () => {
  it('sizes', function() {
    return this.browser
      .openComponent('edu-components', 'textinput-lyceum', 'sizes')
      .assertView('plain', ['.Gemini']);
  });

  it('states', function() {
    return this.browser
      .openComponent('edu-components', 'textinput-lyceum', 'states')
      .assertView('plain', ['.Gemini']);
  });
});

/* TODO(EDUC-130): Раскомментировать */

// ['Pandora', 'Platform'].map(service => {
//   describe(`edu-components_Textinput_${service}`, () => {
//     const page = `textinput-${service.toLowerCase()}`;
//
//     it('states', function() {
//       return this.browser
//         .openComponent('edu-components', page, 'states')
//         .setViewportSize({ width: 800, height: 600 })
//         .assertView('plain', ['body']);
//     });
//
//     it('readOnly', function() {
//       return this.browser
//         .openComponent('edu-components', page, 'read-only')
//         .setViewportSize({ width: 800, height: 600 })
//         .assertView('plain', ['body']);
//     });
//   });
// });
