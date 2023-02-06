'use strict';

const PO = require('../ImageFact.page-object');

specs({ feature: 'Факт', type: 'Одной картинкой' }, function() {
    const imageCases = [
        {
            name: 'Широкая картинка',
            foreverdata: '1037394578',
            screenshotName: 'wide',
        },
        {
            name: 'Квадратная картинка',
            foreverdata: '450541026',
            screenshotName: 'square',
        },
        {
            name: 'Высокая картинка',
            foreverdata: '1324113209',
            screenshotName: 'tall',
        },
    ];

    for (const imageCase of imageCases) {
        describe(imageCase.name, function() {
            beforeEach(async function() {
                await this.browser.yaOpenSerp({
                    text: 'foreverdata',
                    foreverdata: imageCase.foreverdata,
                    data_filter: false,
                }, PO.imageFact());
            });

            it('Портретная ориентация', async function() {
                await this.browser.assertView(imageCase.screenshotName, PO.imageFact());
            });

            hermione.only.notIn('iphone');
            it('Альбомная ориентация', async function() {
                await this.browser.setOrientation('landscape');
                await this.browser.assertView(`${imageCase.screenshotName}-landscape`, PO.imageFact());
            });
        });
    }

    it('Ссылки и счётчики', async function() {
        const path = '/$page/$main/$result';

        await this.browser.yaOpenSerp({
            text: 'теплопроводность утеплителей таблица',
            hermione_disable_inline_stubs: 1,
            data_filter: 'image_fact',
        }, PO.imageFact());

        await this.browser.yaCheckBaobabServerCounter({
            path: `${path}[@wizard_name="image_fact" and @source="image_facts"]`,
        });

        await this.browser.yaCheckLink2({
            selector: PO.imageFact.fact.sourceLink(),
            baobab: { path: `${path}/title` },
            message: 'Сломана ссылка в тайтле',
        });

        await this.browser.yaCheckBaobabCounter(PO.imageFact.fact.image(), { path: `${path}/thumb` });
    });
});
