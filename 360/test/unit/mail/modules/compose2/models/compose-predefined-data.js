describe('Daria.mComposePredefinedData', function() {

    beforeEach(function() {
        this.model = ns.Model.get('compose-predefined-data');
    });

    describe('#_onInit', function() {
        beforeEach(function() {
            this.sinon.stub(this.model, '_createDataFromUrl');
            this.sinon.stub(this.model, 'setData');
            this.sinon.stub(this.model, '_cleanUrl');
        });

        it('Должен сформировать данные из URL', function() {
            this.model._onInit();
            expect(this.model._createDataFromUrl).to.have.callCount(1);
        });

        it('Должен установить данные, полученные из URL', function() {
            var data = { 'test': 'test' };
            this.model._createDataFromUrl.returns(data);
            this.model._onInit();
            expect(this.model.setData).to.be.calledWithExactly(data);
        });

        it('Должен удалять данные из URL, если они были найдены', function() {
            var data = { 'test': 'test' };
            this.model._createDataFromUrl.returns(data);
            this.model._onInit();
            expect(this.model._cleanUrl).to.have.callCount(1);
        });

        it('Не должен удалять данные из URL, если ничего не найдено', function() {
            var data = {};
            this.model._createDataFromUrl.returns(data);
            this.model._onInit();
            expect(this.model._cleanUrl).to.have.callCount(0);
        });
    });

    describe('#request', function() {
        it('Должен возвращать зарезолвленный промис', function() {
            return this.model.request();
        });
    });

    describe('#_createDataFromUrl', function() {
        beforeEach(function() {
            this.sinon.stub(ns.page.current, 'page').value('compose2');
            this.sinon.stub(this.model, '_getBody').returns('expected');
            this.sinon.spy(this.model, '_replaceParamsCallback');
        });

        it('Должен вызвать преобраование данных', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                'mailto': 'test@ya.ru'
            });

            this.model._createDataFromUrl();
            expect(this.model._replaceParamsCallback.called).to.be.equal(true);
        });

        it('Должен заменить ключи из _replaceParams', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                'mailto': 'test@ya.ru',
                'body': "expected",
                'subject': 'test subj'
            });

            var data = this.model._createDataFromUrl();
            expect(data).to.be.eql({
                'to': '<test@ya.ru>',
                'send': 'expected',
                'subj': 'test subj'
            });
        });

        it('Должен выбрать только разрешенные данные', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                'mailto': 'test@ya.ru',
                'body': 'test<br>body<script>alert("XSS");</script>',
                'subject': 'test subj',
                'cc': 'testcc@ya.ru',
                'bcc': 'testbcc@ya.ru',
                'param1': 'value1',
                'param2': 'value2'
            });

            var data = this.model._createDataFromUrl();
            expect(data).to.be.eql({
                'to': '<test@ya.ru>',
                'send': 'expected',
                'subj': 'test subj',
                'cc': '<testcc@ya.ru>',
                'bcc': '<testbcc@ya.ru>'
            });
        });

        it('Должен нормализовать email-ы и не допустить фишинг', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                'mailto': 'super@trusted_to1.com<mallory@evil_to1.com>,super@trusted_to2.ru<mallory@evil_to2.com>',
                'cc': 'super@trusted_cc1.com<mallory@evil_cc1.com>,super@trusted_cc2.ru<mallory@evil_cc2.com>',
                'bcc': 'super@trusted_bcc1.com<mallory@evil_bcc1.com>,super@trusted_bcc2.ru<mallory@evil_bcc2.com>'
            });

            var data = this.model._createDataFromUrl();
            expect(data).to.be.eql({
                'to': '"mallory@evil_to1.com" <mallory@evil_to1.com>, "mallory@evil_to2.com" <mallory@evil_to2.com>',
                'cc': '"mallory@evil_cc1.com" <mallory@evil_cc1.com>, "mallory@evil_cc2.com" <mallory@evil_cc2.com>',
                'bcc': '"mallory@evil_bcc1.com" <mallory@evil_bcc1.com>, "mallory@evil_bcc2.com" <mallory@evil_bcc2.com>',
            });
        });
    });

    describe('#_getBody', function() {
        it('Должен вырезать html и вернуть plain text', function() {
            this.sinon.stub(this.model, '_shouldReturnPlainText').returns(true);
            this.sinon.stub(this.model, '_isHtml').returns(true);

            const result = this.model._getBody("foo\nbar&baz<br>");
            expect(result).to.equal("foo bar&baz");
        });

        it('Должен вернуть plain text', function() {
            this.sinon.stub(this.model, '_shouldReturnPlainText').returns(true);
            this.sinon.stub(this.model, '_isHtml').returns(false);

            const result = this.model._getBody("foo\nbar&baz<br");
            expect(result).to.equal("foo\nbar&baz<br");
        });

        it('Должен вернуть html-разметку', function() {
            this.sinon.stub(this.model, '_shouldReturnPlainText').returns(false);
            this.sinon.stub(this.model, '_isHtml').returns(true);

            const result = this.model._getBody("foo\nbar&baz<br>");
            expect(result).to.equal("foo\nbar&amp;baz<br>");
        });

        it('Должен вернуть html-разметку, которая будет выглядеть как исходный plain text', function() {
            this.sinon.stub(this.model, '_shouldReturnPlainText').returns(false);
            this.sinon.stub(this.model, '_isHtml').returns(false);

            const result = this.model._getBody("foo\nbar&baz<br>");
            expect(result).to.equal("foo<br>bar&amp;baz&lt;br&gt;");
        });
    });

    describe('#_cleanUrl', function() {
        beforeEach(function() {
            this.sinon.stub(ns.page, 'replacePage');
            this.sinon.stub(ns.page.current, 'page').value('compose2');
        });

        it('Должен удалить из URL разрешенные параметры + параметры до замены', function() {
            this.sinon.stub(ns.page.current, 'params').value({
                'mailto': 'test@ya.ru',
                'body': 'test body',
                'subject': 'test subj',
                'cc': 'testcc@ya.ru',
                'bcc': 'testbcc@ya.ru',
                'to': 'test@ya.ru',
                'send': 'test body',
                'subj': 'test subj',
                'param1': 'value1',
                'param2': 'value2'
            });

            this.model._cleanUrl();
            expect(ns.page.replacePage).to.be.calledWithExactly(ns.page.current.page, {
                'param1': 'value1',
                'param2': 'value2'
            });
        });
    });
});

