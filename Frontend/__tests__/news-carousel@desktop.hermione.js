specs({
    feature: 'Карусель',
}, () => {
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=newscarousel/default.json')
            .assertView('plain', '.hermione__carousel');
    });
});
