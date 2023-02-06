'use strict';

specs({
    feature: 'Feature-name',
    experiment: 'Experiment-1'
}, function() {
    it('Should check something', async function() {
        await this.browser.yaOpenSerp({
            text: 'some-text',
            exp_flags: 'new_flag=22'
        }, this.PO.selector());
    });
});
