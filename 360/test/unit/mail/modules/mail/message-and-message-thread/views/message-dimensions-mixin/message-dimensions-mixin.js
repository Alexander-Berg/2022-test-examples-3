describe('Daria.vMessageDimensionsMixin', function() {

    beforeEach(function() {
        this.view = ns.View.create('message-dimensions-mixin');
    });

    describe('#isVisibleInScroller', function() {

        beforeEach(function() {
            this.sinon.stub(this.view, 'getDimensions');
        });

        it('должен вернуть true, если видна верхняя граница', function() {
            this.view.getDimensions.returns({
                top: 10,
                bottom: 500,
                height: 490
            });

            expect(this.view.isVisibleInScroller({
                top: 5,
                bottom: 100
            })).to.be.equal(true);
        });

        it('должен вернуть true, если видна нижняя граница', function() {
            this.view.getDimensions.returns({
                top: 110,
                bottom: 500,
                height: 390
            });

            expect(this.view.isVisibleInScroller({
                top: 100,
                bottom: 200
            })).to.be.equal(true);
        });

        it('должен вернуть true, если длинное письмо находится внутри (границы за ним)', function() {
            this.view.getDimensions.returns({
                top: 110,
                bottom: 600,
                height: 490
            });

            expect(this.view.isVisibleInScroller({
                top: 90,
                bottom: 300
            })).to.be.equal(true);
        });

        it('должен вернуть false, если письмо не внутри', function() {
            this.view.getDimensions.returns({
                top: 10,
                bottom: 200,
                height: 190
            });

            expect(this.view.isVisibleInScroller({
                top: 210,
                bottom: 500
            })).to.be.equal(false);
        });

        it('должен вернуть false, если высота === 0', function() {
            this.view.getDimensions.returns({
                top: 10,
                bottom: 500,
                height: 0
            });

            expect(this.view.isVisibleInScroller({
                top: 5,
                bottom: 100
            })).to.be.equal(false);
        });

    });

    describe('#getDimensions3Pane', function() {
        it('должен вернуть размеры по-умолчанию, если у письма нет ноды', function() {
            this.view.node = null;

            expect(this.view.getDimensions3Pane()).to.be.eql({
                bottom: 0,
                height: 0,
                top: 0
            });
        });

        it('должен вернуть размеры, рассчитанные от тела письма, если передали флаг и тело письма есть', function() {
            this.view.node = {
                offsetTop: 200,
                getElementsByClassName: function() {
                    return [
                        {
                            offsetHeight: 50,
                            offsetTop: 50
                        }
                    ];
                }
            };

            expect(this.view.getDimensions3Pane(true)).to.be.eql({
                bottom: 300,
                height: 50,
                top: 250
            });
        });

        it('должен вернуть размеры по-умолчанию, если передали флаг и тела письма нет', function() {
            this.view.node = {
                getElementsByClassName: function() {
                    return [];
                }
            };

            expect(this.view.getDimensions3Pane(true)).to.be.eql({
                bottom: 0,
                height: 0,
                top: 0
            });
        });

        it('должен вернуть размеры, рассчитанные от шапки письма, если не передали флаг', function() {
            this.view.node = {
                offsetHeight: 100,
                offsetTop: 100
            };

            expect(this.view.getDimensions3Pane()).to.be.eql({
                bottom: 200,
                height: 100,
                top: 100
            });
        });
    });
});
