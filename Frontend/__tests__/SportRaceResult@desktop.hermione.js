specs({
    feature: 'sportRaceResult',
}, () => {
    it('Внешний вид блока', function() {
        const selector = '.sport-race-result';

        return this.browser
            .url('/turbo?stub=sportraceresult/default.json')
            .execute(resetBodyMinWidth)
            .assertView('plain', selector)
            .windowHandleSize({ width: 600, height: 1500 })
            .assertView('plain-600', selector);
    });
});

function resetBodyMinWidth() {
    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = '.page { min-width: auto; }';

    document.body.appendChild(style);

    return true;
}
