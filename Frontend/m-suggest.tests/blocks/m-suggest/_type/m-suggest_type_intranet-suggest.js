BEM.DOM.decl({block: 'm-suggest', modName: 'type', modVal: 'intranet-suggest'}, {
    _sendRequest: function() {
        this._processResult({
            people: [
                {
                    department: 'Служба общих компонентов',
                    phone: 6660,
                    login: 'kuznecov',
                    href: '//staff.yandex-team.ru/kuznecov',
                    title: 'Денис'
                }
            ]
        });
    }
});

