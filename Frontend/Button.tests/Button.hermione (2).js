describe('forms-components_Button', () => {
    ['cloud', 'darkCloud'].forEach(color => {
        it(`${color}-views`, function() {
            return this.browser
                .openComponent('forms-components', 'button-samples', color.toLowerCase())
                .assertView('plain', [`.Hermione.Theme_color_${color}`]);
        });
    });
});
