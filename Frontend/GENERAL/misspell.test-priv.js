describeBlock('misspell', function(block) {
    let data;

    stubBlocks(
        'RequestCtx',
        'misspell__messages',
        'misspell__messages_pumpkin_yes'
    );

    beforeEach(function() {
        data = stubData('experiments', 'counters');

        blocks['misspell__messages'].returns({});
        blocks['misspell__messages_pumpkin_yes'].returns([]);
    });

    it('should call "misspell__messages" in normal mode', function() {
        block(data);

        assert.calledOnce(blocks['misspell__messages']);
        assert.notCalled(blocks['misspell__messages_pumpkin_yes']);
    });

    it('should call "misspell__messages_pumpkin_yes" in pumpkin mode', function() {
        RequestCtx.GlobalContext.isPumpkin = true;

        block(data);

        assert.notCalled(blocks['misspell__messages']);
        assert.calledOnce(blocks['misspell__messages_pumpkin_yes']);
    });

    it('should add messages to bottom and call "misspell__messages" once', function() {
        blocks['misspell__messages'].returns({ top: [], bottom: [{ content: 'anti-mirror' }] });

        block(data);

        assert.equal(data._misspellBottom.length, 1);
        // обнуляем количество сообщений, чтобы не формировался bemjson
        data._misspellBottom = [];

        block(data, { bottom: true });
        assert.calledOnce(blocks['misspell__messages']);
    });

    it('should add data-cid parameter for each misspell', function() {
        blocks['misspell__messages'].returns([
            { type: 'web_misspell', content: 'web_misspell' },
            { type: 'reask', content: 'reask' },
            { type: 'oblivion', content: 'oblivion' }
        ]);

        const result = block(data);
        const misspellBlock = Array.isArray(result) ? result.find(b => b.block === 'misspell') : result;

        assert.equal(misspellBlock && misspellBlock.block, 'misspell', 'Имеется блок misspell');

        const resultMessages = misspellBlock.content[1];

        // eslint-disable-next-line no-undef
        const dataCids = new Set();

        resultMessages.forEach(message => {
            assert.property(message.attrs, 'data-cid');
            dataCids.add(message.attrs['data-cid']);
        });

        // для каждого опечаточника должен быть свой неповторяющийся data-cid
        assert.equal(dataCids.size, 3);
    });
});

describeBlock('misspell__mark-corrections', function(block) {
    it('should leave plain text unchanged', function() {
        const text = 'lorem ipsum dolor';
        assert.equal(block(text), text);
    });

    it('should leave extra spaces unchanged', function() {
        const text = '  lorem  ipsum  dolor  ';
        assert.equal(block(text), text);
    });

    it('should mark words with corrected symbols', function() {
        const text = 'l\u0007[o\u0007]rem ipsum dolor';
        assert.equal(
            block(text),
            '<span class="misspell__error misspell__error_type_bold">l' +
            '<span class="misspell__error">o</span>' +
            'rem</span> ipsum dolor'
        );
    });

    it('should mark all words and separate them by comma', function() {
        const text = 'lorem ipsum dolor';
        assert.equal(
            block(text, null, { bold: true }),
            '<span class="misspell__error misspell__error_type_bold">' +
            'lorem, ipsum, dolor' +
            '</span>'
        );
    });

    it('should mark all words', function() {
        const text = 'lorem ipsum dolor';
        assert.equal(
            block(text, null, { bold: true, phrase: true }),
            '<span class="misspell__error misspell__error_type_bold">' +
            'lorem ipsum dolor' +
            '</span>'
        );
    });

    // тест на проверку выделения ошибки при длинном запросе https://st.yandex-team.ru/SERP-64941
    it('should mark words with corrected symbols in long query SERP-64941', function() {
        const text = 'скольк\u0007[л\u0007] оста\u0007[д\u0007]ось ди\u0007[р\u0007]ломатов';
        assert.equal(
            block(text),
            '<span class="misspell__error misspell__error_type_bold">скольк<span class="misspell__error">л</span>' +
            '</span> <span class="misspell__error misspell__error_type_bold">оста<span class="misspell__error">' +
            'д</span>ось</span> <span class="misspell__error misspell__error_type_bold">ди' +
            '<span class="misspell__error">р</span>ломатов</span>'
        );
    });
});
