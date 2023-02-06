describe('tools-components_Button-desktop', () => {
    const mapper = x => ({ darkCloud: 'cloud-dark' }[x] || x);

    ['cloud', 'darkCloud'].forEach(view => {
        const color = mapper(view);

        hermione.skip.in(/.+/, 'https://st.yandex-team.ru/FEI-20384');
        it(`${view}-views`, function() {
            return (
                this.browser
                    // tools-components-button-samples--cloud-desktop
                    .openComponent('tools-components', 'button-samples', view.toLowerCase() + '-desktop')
                    .assertView('plain', [`.Hermione.Theme_color_${color}`])
            );
        });
    });
});
