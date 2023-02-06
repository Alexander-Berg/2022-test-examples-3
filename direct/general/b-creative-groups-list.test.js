describe('b-creative-groups-list', function() {
    var block,
        sandbox,
        groupStub = {
            group_id: '111',
            name: 'G1',
            // id креативов, которые содержатся в группе. Если будем делать валидацию по buisness_type на севере, то buisness_type присылать не надо будет
            creatives_data: [{ id: 222, buisness_type: 'MMM' }, { id: 333, buisness_type: 'MMM' }],
            group_creatives_count: 1,
            //данные по первому креативу, нужные для отрисовки картинки
            width: 240,
            height: 400,
            alt_text: 'Bla',
            preview_url: 'https://bayan2cdn.xmlrpc.http.yandex.net/file/476643?token=f',
            preview_scale: 0.5
        },
        groupsListStub = [
            {
                group_id: '111',
                name: 'G1',
                // id креативов, которые содержатся в группе. Если будем делать валидацию по buisness_type на севере, то buisness_type присылать не надо будет
                creatives_data: [{ id: 222, buisness_type: 'MMM' }, { id: 333, buisness_type: 'MMM' }],
                group_creatives_count: 1,
                //данные по первому креативу, нужные для отрисовки картинки
                width: 240,
                height: 400,
                alt_text: 'Bla',
                preview_url: 'https://bayan2cdn.xmlrpc.http.yandex.net/file/476643?token=f',
                preview_scale: 0.5
            },
            {
                group_id: '222',
                name: 'G2',
                // id креативов, которые содержатся в группе. Если будем делать валидацию по buisness_type на севере, то buisness_type присылать не надо будет
                creatives_data: [{ id: 222, buisness_type: 'MMM' }, { id: 333, buisness_type: 'MMM' }],
                group_creatives_count: 1,
                //данные по первому креативу, нужные для отрисовки картинки
                width: 240,
                height: 400,
                alt_text: 'Bla',
                preview_url: 'https://bayan2cdn.xmlrpc.http.yandex.net/file/476643?token=f',
                preview_scale: 0.5
            },
            {
                group_id: '333',
                name: 'G1',
                // id креативов, которые содержатся в группе. Если будем делать валидацию по buisness_type на севере, то buisness_type присылать не надо будет
                creatives_data: [{ id: 222, buisness_type: 'MMM' }, { id: 333, buisness_type: 'MMM' }],
                group_creatives_count: 1,
                //данные по первому креативу, нужные для отрисовки картинки
                width: 240,
                height: 400,
                alt_text: 'Bla',
                preview_url: 'https://bayan2cdn.xmlrpc.http.yandex.net/file/476643?token=f',
                preview_scale: 0.5
            },
            {
                group_id: '444',
                name: 'G2',
                // id креативов, которые содержатся в группе. Если будем делать валидацию по buisness_type на севере, то buisness_type присылать не надо будет
                creatives_data: [{ id: 222, buisness_type: 'MMM' }, { id: 333, buisness_type: 'MMM' }],
                group_creatives_count: 1,
                //данные по первому креативу, нужные для отрисовки картинки
                width: 240,
                height: 400,
                alt_text: 'Bla',
                preview_url: 'https://bayan2cdn.xmlrpc.http.yandex.net/file/476643?token=f',
                preview_scale: 0.5
            }
        ];

    beforeEach(function() {
        sandbox = sinon.sandbox.create({
            useFakeTimers: true
        });
    });

    afterEach(function() {
        sandbox.restore();
        block && block.destruct();
    });

    it('Если не передан флаг isSearch и пустой список групп, должен отобразиться текст "Поиск не дал результатов"', function() {
        block = u.createBlock({
            block: 'b-creative-groups-list',
            items: []
        });

        expect(block.elem('empty-message').text()).to.be.equal('Поиск не дал результатов');
    });

    it('Если передан флаг isSearch и пустой список групп, должен отобразиться текст "По вашему запросу ничего не найдено"', function() {
        block = u.createBlock({
            block: 'b-creative-groups-list',
            isSearch: true,
            items: []
        });

        expect(block.elem('empty-message').text()).to.be.equal('По вашему запросу ничего не найдено');
    });

    it('Метод addItems должен добавлять в список элементы b-creative-group', function() {
        block = u.createBlock({
            block: 'b-creative-groups-list',
            items: [groupStub]
        }, { inject: true });

        block.addItems([groupStub, groupStub, groupStub]);

        expect(block.findBlocksInside('b-creative-group').length).to.be.equal(4);
    });
});
