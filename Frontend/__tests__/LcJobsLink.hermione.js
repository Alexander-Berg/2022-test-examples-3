function patchStyle() {
    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = 'body { padding: 20px !important; }';

    document.body.appendChild(style);
}

specs({
    feature: 'LcJobsLink',
}, () => {
    hermione.only.notIn(['safari13']);
    it('normal', function() {
        return this.browser
            .url('turbo?stub=lcjobslink/normal.json')
            .yaWaitForVisible(PO.lcJobsLink(), 'Ссылка не появилась')
            .execute(patchStyle)
            .assertView('default', PO.lcJobsLink());
    });
});
