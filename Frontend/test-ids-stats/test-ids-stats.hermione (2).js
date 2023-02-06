specs('Серверные счетчики test-ids', function() {
    it('Записываются при открытии страницы', function() {
        return this.browser
            .yaOpenSerp({ text: 'test', 'test-id': 38901 })
            .yaCheckServerCounter(
                { path: '/test-ids', vars: { value: '38901' } },
                { allowMultipleTriggering: true }
            )
            .yaCheckBaobabServerCounter(
                { path: '/$page[@tech@test-ids="38901"]' }
            );
    });
});
