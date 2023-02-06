(function() {
    /**
     * Глобально стабит ns.request.models и кидает ошибку, если нет данных.
     * @param {object} context
     */
    function nsRequestMock(context) {
        context.sinon.stub(ns.request, 'models').callsFake(function(models) {
            for (var i = 0; i < models.length; i++) {
                var model = models[i];
                if (!model.isValid()) {
                    if (model.isDo()) {
                        // пропускаем do-модели без предупреждения
                        try {
                            setModelByMock(model);
                        } catch(e) {
                            return new Vow.Promise();
                        }
                    } else {
                        setModelByMock(model);
                    }
                }
            }

            return vow.resolve(models);
        });
    }

    /**
     * Очищает контекст после тестов
     * DARIA-37332
     */
    var clearTestContext = function(context) {
        if (!context || typeof context !== 'object') {
            return;
        }

        for (var property in context) {
            if (context.hasOwnProperty(property)) {
                delete context[property];
            }
        }
    };

    /**
     * Стабит метод `getModel` вида так, чтобы он возвращал экземпляры моделей.
     *
     * Также добавляет модели в контекст по паттерну: compose-message → this['mComposeMessage']
     * для быстрого доступа в тесте.
     *
     * Если передана строка, то модель конструируется без параметров и возвращается из стаба.
     * Если передана модель, то она и возвращается из стаба.
     *
     * У модели ОБЯЗАТЕЛЬНО должно быть свойство `id` с ее именем.
     * @param {ns.View} view
     * @param {string|string[]|ns.Model[]} models
     */
    function stubGetModel(view, models) {
        models = _.isString(models) ? [ models ] : models;

        this.getModel = this.sinon.stub(view, 'getModel');

        _.each(models, function(model) {
            var modelName;

            if (_.isString(model)) {
                modelName = model;
                model = ns.Model.get(model);
            } else {
                modelName = model.id;
            }

            this['m' + _.capitalize(_.camelCase(modelName))] = model;

            this.getModel.withArgs(model.id).returns(model);
        }, this);
    }

    /**
     * Метод стабит все переданные методы объекта и опционально сохраняет их в контексте с переданными именами
     * @param {object} obj
     * @param {string|string[]|object} methods
     */
    function stubMethods(obj, methods) {
        methods = _.isString(methods) ? [ methods ] : methods;
        if (_.isArray(methods)) {
            var stubNames = [];
            // создаем массив из undefined длины массива методов
            stubNames.length = methods.length;
            methods = _.zipObject(methods, stubNames);
        }
        _.each(methods, function(stubName, methodName) {
            var stub = this.sinon.stub(obj, methodName);
            if (stubName) {
                this[stubName] = stub;
            }
        }, this);
    }

    function stubScenarioManager(owner) {
        const scenario = {
            id: 'testid',
            getTimeFromStart: this.sinon.stub(),
            logStep: this.sinon.stub(),
            logError: this.sinon.stub()
        };
        const scenarioManager = {
            startScenario: this.sinon.stub().callsFake(() => scenario),
            startParallelScenario: this.sinon.stub().callsFake(() => scenario),
            hasActiveScenario: this.sinon.stub(),
            getActiveScenario: this.sinon.stub(),
            finishScenario: this.sinon.stub(),
            finishScenarioIfActive: this.sinon.stub(),
            stubScenario: scenario
        };
        const property = owner.scenarioManager ? 'scenarioManager' : '_scenarioManager';
        this.sinon.stub(owner, property).value(scenarioManager);
        return scenarioManager;
    }

    beforeEach(function() {
        ns.Model.get('module-compose').setData(true);
        ns.Model.get('module-message').setData(true);

        window.requestAnimationFrame = function(cb) {
            cb();
        };

        var requests = this.requests = [];

        this.sinon = sinon.sandbox.create();

        this.sinon.stubMethods = stubMethods.bind(this);
        this.sinon.stubGetModel = stubGetModel.bind(this);
        this.sinon.stubScenarioManager = stubScenarioManager.bind(this);

        this.sinon.stub(ns, 'DEBUG').value(false);

        // кидаем исключение, чтобы они не просто так в консоль сыпались
        this.sinon.stub(Jane.ErrorLog, 'sendException').callsFake(function(name, err) {
            throw err;
        });

        this.xhr = this.sinon.useFakeXMLHttpRequest();
        this.xhr.onCreate = function(xhr) {
            requests.push(xhr);
        };

        // стабим дебаунсы, чтобы избежать выполнения асинхронного кода между тестами
        this.sinon.stub(_, 'debounce').callsFake(function(fn) {
            return fn;
        });

        /**
         * Хелпер метод для стабинга метода _.debounce.
         * Нужен, если хочется использовать this.sinon.useFakeTimers() и управлять отложенными вызовами
         * дебаунсенных методов.
         */
        this.stubDebounce = function() {
            _.debounce.restore();

            // иначе никак
            // https://github.com/lodash/lodash/issues/304
            this.sinon.stub(_, 'debounce').callsFake(function(fn, delay) {
                var timeout;
                return function() {
                    window.clearTimeout(timeout);
                    timeout = window.setTimeout(fn.bind(this), delay);
                };
            });
        };

        this.sinon.stub(_, 'throttle').callsFake(function(fn) {
            return fn;
        });

        this.sinon.stub(_, 'defer').callsFake(function(fn) {
            fn.apply(undefined, Array.prototype.slice.call(arguments, 1));
            return 0;
        });

        nsRequestMock(this);
    });

    afterEach(function() {
        this.sinon.restore();
        clearTestContext(this);

        delete window.requestAnimationFrame;

        // сохраняем кнопки, они определяются один раз
        var toolbarButtons = [];
        ns.Model.traverse('toolbar-button', function(model) {
            toolbarButtons.push([ model.getData(), model.params ]);
        });

        ns.Model._clear();
        ns.request._reset();

        // восстанавливаем кнопки
        toolbarButtons.forEach(function(data) {
            ns.Model.get('toolbar-button', data[1]).setData(data[0]);
        });
    });
})();
