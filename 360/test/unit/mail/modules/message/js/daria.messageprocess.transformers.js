describe('Daria.MessageProcess.BODY_TRANSFORMERS', function() {
    describe('Преобразование src у img', function() {
        beforeEach(function() {
            this.transformer = Daria.MessageProcess.BODY_TRANSFORMERS.regs[0];
            this.transformerRegExp = this.transformer[0];
            this.transformerCallback = this.transformer[1];
        });

        it('если у img есть src, то должен преобразовать его в data-fake-img-src', function() {
            const content = [
                {
                    start: '<img',
                    srcName: 'src',
                    src: 'test-url',
                    end: '>'
                },
                {
                    start: '<img',
                    srcName: 'src',
                    src: 'test-url',
                    end: '/>'
                },
                {
                    start: '<img alt="test-alt"',
                    srcName: 'src',
                    src: 'test-url',
                    end: '/>'
                },
                {
                    start: '<div></div><img alt="test-alt"',
                    srcName: 'src',
                    src: 'test-url',
                    end: '><div></div>'
                },
                {
                    start: '<div></div><img alt="test-alt"',
                    srcName: 'src',
                    src: 'test-url',
                    end: '/><span></span>'
                }
            ];

            content.forEach(({ start, srcName, src, end }) => {
                const sample = `${start} ${srcName}="${src}" ${end}`;

                expect(sample.replace(this.transformerRegExp, this.transformerCallback))
                    .equal(`${start} data-fake-img-src="${src}" ${end}`);
            });
        });

        it('если у img нет src, то ничего не должен делать', function() {
            const content = [ '<img>', '<img />', '<img alt="test-alt">', '<img alt="test-alt" />' ];

            content.forEach(
                (sample) => expect(sample.replace(this.transformerRegExp, this.transformerCallback)).equal(sample)
            );
        });

        it('если у img нет src, а следом идет другой элемент с атрибутом src, то ничего не должен делать', function() {
            const content = [
                '<img><div data-interactive-src="test"></div>',
                '<img alt="test-alt" /> <span data-interactive-transaction-src="test"></span>'
            ];

            content.forEach(
                (sample) => expect(sample.replace(this.transformerRegExp, this.transformerCallback)).equal(sample)
            );
        });
    });

    describe('Преобразование атрибута background ->', function() {

        beforeEach(function() {
            this.transformer = Daria.MessageProcess.BODY_TRANSFORMERS.dom[1];
        });

        it('должен преобразовать @backround в style="background-image:" на дочерних элементах', function() {
            var body = $('<div><div background="https://yandex.ru/1.png"></div></div>')[0];
            this.transformer(body);

            expect(body.outerHTML).to.be.equal('<div><div style="background-image: url(&quot;https://yandex.ru/1.png&quot;);"></div></div>');
        });

        it('должен преобразовать @backround в style="background-image:" на самом теле', function() {
            var body = $('<div background="https://yandex.ru/1.png"/>')[0];
            this.transformer(body);

            expect(body.outerHTML).to.be.equal('<div style="background-image: url(&quot;https://yandex.ru/1.png&quot;);"></div>');
        });

        it('должен удалить @backround, если он пустой', function() {
            var body = $('<div background=""/>')[0];
            this.transformer(body);

            expect(body.outerHTML).to.be.equal('<div></div>');
        });

    });

    describe('#attachmentsSelect', function() {
        describe('->', function() {
            beforeEach(function() {
                this.attachmentsSelect = Daria.MessageProcess.attachmentsSelect;
                this.sinon.spy(Range.prototype, 'surroundContents');
            });

            it('Не ищет в цитировании и ссылках', function() {
                var $node = $('<div><a>attach 2</a><blockquote>attach 3</blockquote></div>');
                this.attachmentsSelect($node[0]);
                expect(Range.prototype.surroundContents).to.have.callCount(0);
            });

            it('Ищет правильно во вложенных нодах внутри заданной #1', function() {
                var $node = $('<div>attach<div>attached</div><div>прилагаю файл</div></div>');
                this.attachmentsSelect($node[0]);
                expect(Range.prototype.surroundContents).to.have.callCount(3);
            });

            it('Ищет правильно во вложенных нодах внутри заданной #2', function() {
                var $node = $('<div>attach<div>attached</div><div>приаттаченный аттач</div><div>прилагаю файл</div></div>');
                this.attachmentsSelect($node[0]);
                expect(Range.prototype.surroundContents).to.have.callCount(4);
            });

            it('Вызывается метрика для вложений', function() {
                var $node = $('<div>посмотри-прилагаю файл. Во вложении</div>');
                this.sinon.stub(Jane, 'c');
                this.attachmentsSelect($node[0]);
                expect(Jane.c.withArgs('Аттачи в тексте письма', 'Показ ссылки')).to.have.callCount(2);
            });
            it('Если вхождения вообще есть, то показывается метрика количество писем с подсвеченными ссылками', function() {
                var $node = $('<div>посмотри-прилагаю файл. Во вложении</div>');
                this.sinon.stub(Jane, 'c');
                this.attachmentsSelect($node[0]);
                expect(Jane.c).to.have.calledWith('Аттачи в тексте письма', 'Количество открытий писем с подсвеченными ссылками');
            });
        });
    });

});
