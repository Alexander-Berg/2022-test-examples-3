/* TODO(EDUC-130): Раскомментировать */

// ['Pandora', 'Platform'].map(service => {
//   describe(`edu-components_Select_${service}`, () => {
//     const page = `select-${service.toLowerCase()}`;
//
//     it('states', function() {
//       return this.browser
//         .openComponent('edu-components', page, 'states')
//         .setViewportSize({ width: 800, height: 600 })
//         .assertView('plain', ['body'])
//         .moveToObject('[role=listbox]')
//         .assertView('hovered', ['body']);
//     });
//
//     it('single choice', function() {
//       const wideOption = '[role=option]:nth-child(2)'; // Охотник
//       const narrowOption = '[role=option]:nth-child(4)'; // Знать
//
//       return this.browser
//         .openComponent('edu-components', page, 'single-choice')
//         .setViewportSize({ width: 800, height: 600 })
//         .assertView('closed', ['body'])
//         .click('[role=listbox]')
//         .pause(200)
//         .assertView('opened', ['body'])
//         .moveToObject(wideOption)
//         .click(wideOption)
//         .assertView('wide option selected', ['body'])
//         .click('[role=listbox]')
//         .pause(200)
//         .moveToObject(narrowOption)
//         .click(narrowOption)
//         .assertView('narrow option selected', ['body']);
//     });
//
//     it('multiple choice', function() {
//       const option = '[role=option]:nth-child(3)';
//
//       return this.browser
//         .openComponent('edu-components', page, 'multiple-choice')
//         .setViewportSize({ width: 800, height: 600 })
//         .assertView('closed', ['body'])
//         .click('[role=listbox]')
//         .pause(200)
//         .assertView('opened', ['body'])
//         .moveToObject(option)
//         .click(option)
//         .assertView('deselected', ['body'])
//         .click(option)
//         .assertView('selected', ['body']);
//     });
//
//     it('scroll', function() {
//       return this.browser
//         .openComponent('edu-components', page, 'single-choice')
//         .setViewportSize({ width: 800, height: 200 })
//         .assertView('closed', ['body'])
//         .click('[role=listbox]')
//         .pause(200)
//         .assertView('opened', ['body']);
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
