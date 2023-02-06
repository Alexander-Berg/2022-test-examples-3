(function(metrikaConfig, metrikaId) {
    metrikaConfig.id = metrikaId;

    let instances = {};

    function addInstance(metrika, id) {
        let sameId = instances[id];

        if (!sameId) {
            instances[id] = metrika;
        } else if (Array.isArray(sameId)) {
            sameId.push(metrika);
        } else {
            instances[id] = [sameId, metrika];
        }
    }

    /**
     * Если объект {a: undefined} из execute передать в контекст теста
     * в хроме получится {}
     * в firefox, ie11, edge {a: null}
     * Эта функция берет сереализацию в свои руки, чтобы поведение в разных браузерах совпадало.
     *
     * @param {Object} obj - объект, который нужно сереализовать/десереализовать
     * @returns {Object} obj - сереализованный/десереализованный объект
     */
    function fixHermioneSerealization(obj) {
        return JSON.parse(JSON.stringify(obj));
    }

    let Metrika = function(data) {
        this._initData = data;
        this._funcalls = [];

        addInstance(this, data ? data.id || 0 : 0);
    };

    Metrika.prototype = {
        hit: function() {
            this._log('hit', arguments);
        },

        reachGoal: function() {
            this._log('goal', arguments);
        },

        _log: function(name, args) {
            this._funcalls.push({ func: name, args: Array.prototype.slice.call(args) });
        },

        /**
         * Возвращает данные, переданные конструктору
         *
         * @returns {Object}
         */
        getInitData: function() {
            return fixHermioneSerealization(this._initData);
        },

        getHits: function() {
            return fixHermioneSerealization(this._getFuncalls('hit'));
        },

        getGoals: function() {
            return this._getFuncalls('goal');
        },

        getGoalIds: function() {
            const goals = this.getGoals();
            return Array.isArray(goals) ? goals.map(function(goal) { return goal[0] }) : goals;
        },

        getExperiments: function() {
            return this._getFuncalls('experiments');
        },

        /**
         * Возвращает список вызовов функций.
         * Если указан funName, вовращает список аргументов функций с таким именем.
         *
         * @param {String} [funName] - название функции, для которой вернуть список аргументов
         * @returns {Array<Object>} - список вызовов функций
         */
        _getFuncalls: function(funName) {
            if (!funName) {
                return this._funcalls;
            }

            return this._funcalls
                .filter(function(funcall) {
                    return funcall.func === funName;
                })
                .map(function(funcall) {
                    return funcall.args;
                });
        },

        experiments: function() {
            this._log('experiments', arguments);
        },
    };

    /**
     * Возвращает все экземпляры метрики. Если указан id, возвращает только метрики данного счетчика.
     * Выбрасывает ошибку, если по данному id нет счетчика.
     *
     * @param {Number} [id] - идентификатор метрики
     * @returns {Array<Metrika>} - массив экземпляров метрики
     */
    Metrika.getInstances = function(id) {
        return id === undefined ? getAllInstances() : getInstancesById(id);
    };

    function getAllInstances() {
        return Object.values(instances).reduce(function(result, metrikas) {
            return result.concat(metrikas);
        }, []);
    }

    function getInstancesById(id) {
        let counters = instances[id];
        if (!counters) {
            throw new Error('Нет счетчиков с таким id: ' + id);
        }

        return [].concat(counters);
    }

    /**
     * Возвращает экземпляр метрики по id.
     * Если указан index, возвращает экземпляр по id с заданным порядковым номером
     * Выбрасывает ошибку, если
     *   - нет счетчика с указанным id
     *   - пользователь указал лишь id, но было создано несколько метрик
     *   - у счетчика с таким id не было создано метрики с порядковым номером index
     *
     * @param {Number} id - идентификатор метрики
     * @param {Number} [index] - порядковый номер метрики с указанным id
     * @returns {Metrika} - экземпляр метрики
     */
    Metrika.getInstance = function(id, index) {
        let metrika = instances[id];

        if (!metrika) {
            throw new Error('Нет счетчика с таким id: ' + id);
        }
        if (Array.isArray(metrika) && index === undefined) {
            throw new Error('По данному id = ' + id + ' создано несколько метрик');
        }

        if (index > 0 || Array.isArray(metrika) && index === 0) {
            metrika = metrika[index];
            if (!metrika) {
                throw new Error('У счетчика id = ' + id + ' нет метрики с порядковым номером ' + index);
            }
        }

        return metrika;
    };

    /*
     * Шорткаты
     */

    Metrika.getHitsFor = function(id, index) {
        return Metrika.getInstance(id, index).getHits();
    };

    Metrika.getGoalsFor = function(id, index) {
        return Metrika.getInstance(id, index).getGoals();
    };

    Metrika.getGoalIdsFor = function(id, index) {
        return Metrika.getInstance(id, index).getGoalIds();
    };

    Metrika.getInitDataFor = function(id, index) {
        return Metrika.getInstance(id, index).getInitData();
    };

    Metrika.getExperimentsFor = function(id, index) {
        return Metrika.getInstance(id, index).getExperiments();
    };

    window.ym = function() {
        let counterId = arguments[0];
        let method = arguments[1];
        let other = Array.prototype.slice.call(arguments, 2);

        Metrika.getInstance(counterId)[method](...other);
    };

    window.Ya = window.Ya || {};

    window.Ya.Metrika = Metrika;
    window.Ya.Metrika2 = Metrika;

    let _counter = new Metrika(metrikaConfig);
})({}, 84153601);
