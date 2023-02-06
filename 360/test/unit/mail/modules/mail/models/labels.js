describe('Daria.mLabels', function() {
    beforeEach(function() {
        this.model = ns.Model.get('labels');
        setModelByMock(this.model);
    });

    afterEach(function() {
        this.model.destroy();
    });

    describe('#sortLabels', function() {
        describe('Сортировка по имени ->', function() {
            beforeEach(function() {
                this.sinon.stub(ns.Model.get('settings'), 'getSetting')
                    .withArgs('label_sort')
                    .returns('by_abc');
            });

            it('Должен отсортировать метки по алфавиту', function() {
                var labelsBefore = [
                    { name: 'яблоко' },
                    { name: 'pear' },
                    { name: 'avocado' },
                    { name: 'cherry' },
                    { name: 'lime' },
                    { name: 'PoMeLo' },
                    { name: 'PASSION_FRUIT' },
                    { name: 'банан' },
                    { name: 'апельсин' },
                    { name: 'plum' },
                    { name: '_banana_' }
                ];

                var labelsAfter = [
                    { name: '_banana_' },
                    { name: 'avocado' },
                    { name: 'cherry' },
                    { name: 'lime' },
                    { name: 'PASSION_FRUIT' },
                    { name: 'pear' },
                    { name: 'plum' },
                    { name: 'PoMeLo' },
                    { name: 'апельсин' },
                    { name: 'банан' },
                    { name: 'яблоко' }
                ];

                this.model.sortLabels(labelsBefore);

                expect(labelsBefore).to.eql(labelsAfter);
            });
        });

        describe('Сортировка по количеству ->', function() {
            beforeEach(function() {
                this.sinon.stub(ns.Model.get('settings'), 'getSetting')
                    .withArgs('label_sort')
                    .returns('by_count');
            });

            it('Должен сортировать по алфавиту, если количество одинаковое', function() {
                var labelsBefore = [
                    { count: 10, user: true, name: 'bcd' },
                    { count: 10, user: true, name: 'abc' }
                ];

                var labelsAfter = [
                    { count: 10, user: true, name: 'abc' },
                    { count: 10, user: true, name: 'bcd' }
                ];

                this.model.sortLabels(labelsBefore);

                expect(labelsBefore).to.eql(labelsAfter);
            });

            it('Должен отсортировать метки по убыванию', function() {
                var labelsBefore = [
                    { count: 0, user: false, name: 'abc' },
                    { count: 1, user: true, name: 'abc' },
                    { count: 0, user: true, name: 'abc' },
                    { count: 0, user: false, name: 'abc' },
                    { count: 2, user: true, name: 'abc' },
                    { count: 0, user: false, name: 'abc' },
                    { count: 9, user: true, name: 'abc' },
                    { count: 8, user: true, name: 'abc' }
                ];

                var labelsAfter = [
                    { count: 9, user: true, name: 'abc' },
                    { count: 8, user: true, name: 'abc' },
                    { count: 2, user: true, name: 'abc' },
                    { count: 1, user: true, name: 'abc' },
                    { count: 0, user: false, name: 'abc' },
                    { count: 0, user: false, name: 'abc' },
                    { count: 0, user: false, name: 'abc' },
                    { count: 0, user: true, name: 'abc' }
                ];

                this.model.sortLabels(labelsBefore);

                expect(labelsBefore).to.eql(labelsAfter);
            });
        });
    });

    describe('#adjustCounters', function() {
        it('Должен обновить количество в указанных метках', function() {
            var label1 = this.model.models[0].getData();
            var label2 = this.model.models[1].getData();

            // делаем удобные данные
            label1.count = 5;
            label2.count = 6;
            var adjust = {};
            adjust[label1.lid] = 1;
            adjust[label2.lid] = -1;

            this.model.adjustCounters(adjust);

            expect(this.model.getLabelById(label1.lid).count).to.be.equal(6);
            expect(this.model.getLabelById(label2.lid).count).to.be.equal(5);
        });
    });

    describe('#getLabelByName', function() {
        beforeEach(function() {
            this.mLabel = this.model.models[0];
            this.sinon.spy(this.mLabel, 'getData');
        });

        it('Должен вернуть метку по имени', function() {
            var labelData = this.mLabel.getData();

            expect(this.model.getLabelByName('test_label_name')).to.eql(labelData);
        });

        it('Должен вернуть undefined, если метки нет', function() {
            expect(this.model.getLabelByName('no_such_label_name')).to.be.equal(undefined);
        });

        it('Должен кешировать результат', function() {
            this.model.getLabelByName('test_label_name');
            this.model.getLabelByName('test_label_name');

            expect(this.mLabel.getData).to.have.callCount(1);
        });
    });

    describe('#getUserLabelByName', function() {
        beforeEach(function() {
            this.mLabel = this.model.models[0];
            this.sinon.spy(this.mLabel, 'getData');
        });

        it('Должен вернуть метку по имени', function() {
            var labelData = this.mLabel.getData();

            expect(this.model.getUserLabelByName('test_label_name')).to.eql(labelData);
        });

        it('Должен вернуть undefined, если метки нет', function() {
            expect(this.model.getUserLabelByName('no_such_label_name')).to.be.equal(undefined);
        });

        it('Должен вернуть undefined, если есть системная метка с переданным именем', function() {
            expect(this.model.getWaitingForReplyLabel()).to.be.equal(undefined);
        });

        it('Должен кешировать результат', function() {
            this.model.getUserLabelByName('test_label_name');
            this.model.getUserLabelByName('test_label_name');

            expect(this.mLabel.getData).to.have.callCount(1);
        });
    });

    describe('#getLabelById', function() {
        it('Должен вернуть метку по lid', function() {
            var labelData = this.model.models[0].getData();

            expect(this.model.getLabelById('1')).to.eql(labelData);
        });

        it('Должен вернуть null, если метки нет и ее искали по lid', function() {
            expect(this.model.getLabelById('1212121212')).to.be.equal(null);
        });
    });

    describe('#getLabelBySymbolicName', function() {
        beforeEach(function() {
            this.mLabel = this.model.models[1];
            this.sinon.spy(this.mLabel, 'getData');
        });

        it('Должен вернуть метку по symbolicName', function() {
            var labelData = this.model.models[1].getData();

            expect(this.model.getLabelBySymbolicName('test_symbol')).to.eql(labelData);
        });

        it('Должен вернуть undefined, если метки нет и ее искали по symbol', function() {
            expect(this.model.getLabelBySymbolicName('no_such_symbol')).to.be.equal(undefined);
        });

        it('Должен кешировать результат, если поиск по symbol', function() {
            this.model.getLabelBySymbolicName('test_symbol');
            this.model.getLabelBySymbolicName('test_symbol');

            expect(this.mLabel.getData).to.have.callCount(1);
        });
    });

    describe('#getImportantLID', function() {
        it('Должен вернуть lid метки "Важное"', function() {
            expect(this.model.getImportantLID()).to.be.equal('123');
        });
    });

    describe('#getUserLabelsCount', function() {
        it('Должен вернуть количество пользовательских меток', function() {
            expect(this.model.getUserLabelsCount()).to.be.equal(2);
        });
    });

    describe('#getLIDByName', function() {
        it('Должен вернуть lid по названию метки', function() {
            expect(this.model.getLIDByName('test_label_name')).to.be.equal('1');
        });
    });

    describe('#getSOLabelName', function() {
        it('Должен вернуть имя социальной метки по lid', function() {
            expect(this.model.getSOLabelName('4')).to.be.equal('SystMetkaSO:delivery');
        });

        it('Должен вернуть null, если метка не социальная', function() {
            expect(this.model.getSOLabelName('1')).to.be.equal(null);
        });
    });

    describe('#canShowEventWidget', function() {
        it('Должен вернуть true, если метка не найдена, виджет можеть быть выведен', function() {
            expect(this.model.canShowEventWidget([ '1' ])).to.be.equal(true);
        });

        it('Должен вернуть false, если метка найдена, виджет не можеть быть выведен', function() {
            expect(this.model.canShowEventWidget([ '1', '2370000000112026049' ])).to.be.equal(false);
        });
    });

    describe('#getPinnedLabel', function() {
        it('Должен вернуть метку "запиненные"', function() {
            expect(this.model.getPinnedLabel().lid).to.be.equal('2420000001823639879');
        });
    });
});
