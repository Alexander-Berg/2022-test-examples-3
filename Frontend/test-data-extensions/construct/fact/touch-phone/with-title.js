module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            type: 'test',
            template: 'test',
            blocks: [
                {
                    block: 'fact',
                    answer: 'Рожь',
                    description: [
                        'Доминантсептаккорд выстраивает самодостаточный флэнжер, это понятие создано ' +
                        'по аналогии с термином Ю.Н.Холопова "многозначная тональность" ',
                        { block: 'link', url: '//ya.ru', text: 'Читать дальше' }
                    ]
                }
            ]
        }
    }
};
