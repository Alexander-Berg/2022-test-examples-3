function patchStyle() {
    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = 'body { padding: 20px !important; }';

    document.body.appendChild(style);
}

specs({
    feature: 'LcJobsSkillLevel',
}, () => {
    hermione.only.notIn(['safari13']);
    it('normal', function() {
        return this.browser
            .url('turbo?stub=lcjobsskilllevel/normal.json')
            .yaWaitForVisible(PO.lcJobsSkillLevel(), 'Блок не появился')
            .execute(patchStyle)
            .assertView('default', PO.lcJobsSkillLevel());
    });
});
