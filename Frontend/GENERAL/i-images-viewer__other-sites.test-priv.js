describeBlock('i-images-viewer__other-sites', function(block) {
    let context;
    let image;
    let url;

    beforeEach(function() {
        context = {};

        image = {
            passages: [
                {
                    bi: '9cae7fda322f1e500bc48c48ef6d43e3-l',
                    big_thmb_h: '365',
                    big_thmb_href: '//im0-tub-ru.yandex.net/i?id=9cae7fda322f1e500bc48c48ef6d43e3-l',
                    big_thmb_w: '648',
                    bq: '254',
                    crc: '10007035290424411270',
                    di: 0,
                    global_img_id: '9818af7260aca01698f35971dbdcc1e6',
                    html_href: 'http://fonday.ru/info/16710-416710e7a5b.html',
                    img_h: '365',
                    img_href: 'http://fonday.ru/images/tmp/16/7/648x365/16710fBjLzqnJlMXhoFHAG.jpg',
                    img_size_bytes: '113589',
                    img_type: 'jpg',
                    img_w: '648',
                    lang: 1,
                    smart_crop: '16x0+79x75',
                    smart_crop_noaspect: '13x1+86x64',
                    text: 'Обои корзинка, котик, котята, котики, картинки, фото.',
                    thmb_h_orig: '270',
                    thmb_href: '//im0-tub-ru.yandex.net/i?id=9818af7260aca01698f35971dbdcc1e6',
                    thmb_w_orig: '480',
                    title: 'Обои корзинка, котик, котята, котики',
                    txt_img_sim: '0.6856'
                }
            ]
        };

        url = sinon.stub(RequestCtx, 'url').returns({
            hostname: sinon.stub().returns('domain')
        });
    });

    it('should return correct sites data', function() {
        assert.deepEqual(block(context, image), [
            {
                siteURL: 'http://fonday.ru/info/16710-416710e7a5b.html',
                title: 'Обои корзинка, котик, котята, котики',
                greenURL: 'domain'
            }
        ]);
    });

    it('should return empty array if no passages', function() {
        image.passages = [];
        assert.deepEqual(block(context, image), []);
    });

    it('should return sites count by passages count', function() {
        image.passages.push({
            html_href: 'http://site2',
            img_href: 'http://site2_img',
            title: 'site2 title',
            text: 'site2 description'
        });
        var sites = block(context, image);

        assert.strictEqual(sites.length, 2);
    });

    it('should return maximum 5 sites', function() {
        const item = {
            html_href: 'http://site2',
            img_href: 'http://site2_img',
            title: 'site2 title',
            text: 'site2 description'
        };

        image.passages = image.passages.concat([item, item, item, item, item, item]);
        var sites = block(context, image);

        assert.strictEqual(sites.length, 5);
    });

    afterEach(function() {
        url.restore();
    });
});
