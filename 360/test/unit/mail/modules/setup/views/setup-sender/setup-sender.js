describe('Daria.vSetupSender', function () {
    beforeEach(function () {
        this.view = ns.View.create('setup-sender');
        this.mAccountInformation = ns.Model.get('account-information');
    });

    describe('appendSeparator(plain)', function () {
        it('перед текстом должен быть добавлен разделитель "-- "', function () {
            expect(this.view.appendSeparator('test')).to.eql('-- \ntest');
        });

        it('если уже есть разделитель "--", то новый не ставится', function () {
            expect(this.view.appendSeparator('--\ntest')).to.eql('--\ntest');
        });

        it('поиск разделителя выполняется без учета пробелов только на наличие в строке "--"', function () {
            expect(this.view.appendSeparator(' - - \ntest')).to.eql(' - - \ntest');
        });

        it('пустые строки игнорируются, разделитель добавляется только перед не пустой строкой', function () {
            expect(this.view.appendSeparator(' \n \n \ntest')).to.eql(' \n \n \n-- \ntest');
        });
    });

    describe('appendSeparator(html)', function () {
        it('перед текстом должен быть добавлен разделитель "-- "', function () {
            expect(this.view.appendSeparator('<div>test</div>')).to.eql('<div>--&nbsp;</div><div>test</div>');
        });

        it('если уже есть разделитель "--", то новый не ставится', function () {
            expect(this.view.appendSeparator('<div>--&nbsp;</div><div>test</div>')).to.eql('<div>--&nbsp;</div><div>test</div>');
        });

        it('разделитель может оканчиваться пробелом', function () {
            expect(this.view.appendSeparator('<div>-- </div><div>test</div>')).to.eql('<div>-- </div><div>test</div>');
        });

        it('поиск разделителя выполняется без учета пробелов только на наличие в строке "--"', function () {
            expect(this.view.appendSeparator('<div> - - </div><div>test</div>')).to.eql('<div> - - </div><div>test</div>');
        });

        it('пустые теги игнорируются, разделитель добавляется только перед не пустым тегом', function () {
            expect(this.view.appendSeparator('<div></div><div><br></div><div>&nbsp;</div><div> </div><div>test</div>'))
                .to.eql('<div><br></div><div>&nbsp;</div><div> </div><div>--&nbsp;</div><div>test</div>');
        });

        it('поиск выполняется по всем тегам, поэтому каждый тег считается новой стройок, и разделитель может находится в той же строке с текстом', function () {
            expect(this.view.appendSeparator('<div><br></div><div><span>--</span>test</div>')).to.eql('<div><br></div><div><span>--</span>test</div>');
        });

        it('перед картинкой необходимо добавить разделитель', function () {
            expect(this.view.appendSeparator('<div><img src="/client/build/foo.png" /></div>')).to.eql('<div>--&nbsp;</div><div><img src="/client/build/foo.png"></div>');
        });

        it('перед таблицой необходимо добавить разделитель и добавить overflow: auto; контейнеру', function () {
            const signature = this.view.appendSeparator('<div><table></table></div>');
            expect(signature).to.eql('<div>--&nbsp;</div><div style="overflow: auto;"><table></table></div>');
        });

        it('вставляет разделитель над div, если его первый сын текст, а второй и далее могут быть элементами', function () {
            const signature = this.view.appendSeparator('<div>abc<span style="color: #f00">def</span>ghi</div>');
            expect(signature).to.eql('<div>--&nbsp;</div><div>abc<span style="color: #f00">def</span>ghi</div>');
        });
    });
});
