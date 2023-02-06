describe('b-chart', function() {
    var sandbox,
        block,
        params;

    function createBlock(params) {
        block = u.getInitedBlock({
            block: 'b-chart',
            mods: params.mods || {},
            js: {
                series: params.series || [],
                options: params.options || {}
            }
        }, false)
    }
    beforeEach(function() {
        sandbox = sinon.sandbox.create();
        params = {
            series: [
                {
                    column: "shows",
                    name: "Показы",
                    data: [
                        {
                            x: 1487538000000,
                            y: 1016508
                        },
                        {
                            x: 1487624400000,
                            y: 948559
                        }
                    ]
                },
                {
                    column: "clicks",
                    name: "Клики",
                    data: [
                        {
                            x: 1487538000000,
                            y: 906508
                        },
                        {
                            x: 1487624400000,
                            y: 558559
                        }
                    ]
                }
            ],
            options: {
                groupByDate: "day"
            }
        };
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('Зависимость параметра stacking от модификатора stacking', function(){

        afterEach(function() {
            block.destruct && block.destruct();
        });

        describe('Мод stacking отсутствует', function(){
            ['column', 'line', 'area'].forEach(function(view) {
                it('Вид '+ view +', в опциях гафика параметр stacking должен быть false', function() {
                    params.mods = { view: view };
                    createBlock(params);

                    expect(block._chart.options).to.have.deep.property('plotOptions.'+ view +'.stacking', false);
                });
            });
        })

        describe('Мод stacking: yes', function(){
            ['column', 'line', 'area'].forEach(function(view) {
                it('Вид '+ view +', в опциях гафика параметр stacking должен быть true', function() {
                    params.mods = { view: view, stacking: 'yes' };
                    createBlock(params);

                    expect(block._chart.options).to.have.deep.property('plotOptions.'+ view +'.stacking', true);
                });
            });
        })

    });

    describe('Методы', function(){

        it('showLoading', function() {
            params.mods = { view: 'column' };
            createBlock(params);

            block.showLoading();

            expect(block).to.haveBlock('spin2');
        });

    });
});

