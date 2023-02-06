describe('ns.View.', function() {

    describe('#updateByLayout', function() {

        // один раз определяем тестовые виды
        before(function() {
            ns.View.define('view-app', {yateModule: 'common'});
            ns.View.define('view1', {yateModule: 'common'});
            ns.View.define('view1-tab', {yateModule: 'common'});
            ns.View.define('view1-tab-1', {
                'params+': {
                    'tab': '1'
                },
                yateModule: 'common'
            });
            ns.View.define('view1-tab-2', {
                'params+': {
                    'tab': '2'
                },
                yateModule: 'common'
            });

            var subPageLayout = {
                'view1-tab-box@': function(params) {
                    return 'view1-tab-' + params.tab;
                }
            };

            ns.layout.define('page', {
                'view-app': {
                    'view1': {
                        'view1-tab': subPageLayout
                    }
                }
            });

            ns.layout.define('subpage', {
                'view1-tab': subPageLayout
            });

        });

        beforeEach(function() {
            this.sinon.stub(ns.Update.prototype, 'log');

            this.view = ns.View.create('view-app');

            this.params1 = {ids: '1', tab: '1'};
            this.params2 = {ids: '1', tab: '2'};

            var layout = ns.layout.page('page', this.params1);
            return new ns.Update(this.view, layout, this.params1).render();
        });

        // синтетический тест из найденного бага
        it('должен правильно сохранять ссылки на layout. (global -> updateByLayout -> localUpdate)', function() {
            var that = this;
            var layout = ns.layout.page('page', this.params1);

            var parentView = that.view.views['view1'];
            var childView = parentView.views['view1-tab'];

            // делаем global-update
            return new ns.Update(this.view, layout, this.params1)
                .render()
                .then(function() {
                    // изменяем layout внутреннего вида
                    // tab-1 (становится скрытым в боксе) -> tab-2 (становится видимым в боксе)
                    return childView.updateByLayout('subpage', that.params2);
                })
                .then(function() {
                    // вызываем update на родителе
                    return parentView.update();
                })
                .then(function() {
                    // по идее он должен сохранить layout и tab-2 останется видным
                    expect(that.view.node.querySelectorAll('.ns-view-view1-tab-2')).to.have.length(1);
                });
        });

        // синтетический тест из найденного бага
        it('должен правильно сохранять ссылки на layout. (updateByLayout -> updateByLayout -> localUpdate)', function() {
            var that = this;

            var parentView = that.view.views['view1'];
            var childView = parentView.views['view1-tab'];

            // изменяем layout внутреннего вида
            // tab-1 (становится скрытым в боксе) -> tab-2 (становится видимым в боксе)
            return childView.updateByLayout('subpage', that.params2)
                .then(function() {
                    // изменяем layout внутреннего вида
                    // tab-2 (становится скрытым в боксе) -> tab-1 (становится видимым в боксе)
                    return childView.updateByLayout('subpage', that.params1);
                })
                .then(function() {
                    // вызываем update на родителе
                    return parentView.update();
                })
                .then(function() {
                    // по идее он должен сохранить layout и tab-2 останется видным
                    expect(that.view.node.querySelectorAll('.ns-view-view1-tab-1')).to.have.length(1);
                });
        });

    });

});
