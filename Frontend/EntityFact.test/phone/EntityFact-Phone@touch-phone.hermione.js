const PO = require('../../EntityFact.page-object/');

specs({ feature: 'Факт', type: 'Объектный' }, () => {
    it('Кнопка позвонить', async function() {
        await this.browser.yaOpenSerp({
            text: 'музей техники телефон',
            data_filter: false,
        }, PO.EntityFact());

        await this.browser.assertView('plain', PO.EntityFact());

        await this.browser.yaCheckLink2({
            selector: PO.EntityFact.fact.phoneButton(),
            target: '',
            url: {
                href: {
                    protocol: 'tel:',
                },
                ignore: ['hostname', 'pathname', 'query'],
            },
            baobab: {
                path: '/$page/$main/$result/phone-button',
            },
        });
    });
});
