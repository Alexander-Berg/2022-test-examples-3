specs({
    feature: 'Колдунщик 1орг',
    experiment: 'Дивная карточка для ПП',
}, SURFACES.SEARCHAPP, () => {
    it.skip('Дефолтный', async function() {
        const result = await this.client.request('кафе Пушкин', {
            exp_flags: {
                GEO_1org_div_card: 1,
                GEO_enable_app: 1,
            },
        });
        const expected = /Кажется, вам нужно это место|Вот оно|Всё для вас|Смотрите/;
        const expectedCard = {
            body: {
                states: [{
                    blocks: [{
                        items: [{
                            type: 'div-container-block',
                            children: [{
                                image: {},
                                type: 'div-image-block'
                            }]
                        }],
                        type: 'div-gallery-block'
                    }]
                }]
            }
        };

        this.asserts.yaCheckVoiceMatch(result, expected);
        this.asserts.yaCheckTextMatch(result, expected);
        this.asserts.yaCheckCard(result, expectedCard);
    });
});
