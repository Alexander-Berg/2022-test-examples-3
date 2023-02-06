before(function() {
    window.mock = {};

    function findMock(modelId) {
        var modelData = mock[modelId];
        if (!modelData) {
            throw new Error('No mock for model "' + modelId + '"');
        }

        if (!Array.isArray(modelData)) {
            throw new Error('Mock for model "' + modelId + '" must be array');
        }

        return modelData;
    }

    /**
     * Возвращает именованные данные для модели
     * @param {string} modelId
     * @param {string} name
     * @returns {*}
     */
    window.getModelMockByName = function(modelId, name) {
        var modelMocks = findMock(modelId);
        if (!name) {
            throw new Error('getModelMockByName: no name given');
        }

        for (var k = 0; k < modelMocks.length; k++) {
            var modelMock = modelMocks[k];
            if (modelMock.name === name) {
                return _.cloneDeep(modelMock.data);
            }
        }

        throw new Error('Can\'t find data "' + name + '" for model "' + modelId + '"');
    };

    /**
     * Возвращает из mock данные для модели
     * @param {ns.Model} model
     * @returns {*}
     */
    window.getModelMock = function(model) {
        var modelId = model.id;
        var modelMocks = findMock(modelId);

        for (var k = 0; k < modelMocks.length; k++) {
            var modelMock = modelMocks[k];
            var modelKey = ns.Model.key(modelId, modelMock.params);
            if (modelKey === model.key) {
                return _.cloneDeep(modelMock.data);
            }
        }

        throw new Error('Can\'t find data for key "' + model.key + '"');
    };

    /**
     * Сохраняет данные из mock в модель
     * @param {ns.Model} model
     * @returns {*}
     */
    window.setModelByMock = function(model) {
        var data = getModelMock(model);
        model.setData(data);
    };

    /**
     * Сохраняет все данные из mock в модели
     * @param {string} modelId
     */
    window.setModelsByMock = function(modelId) {
        var modelMocks = findMock(modelId);
        modelMocks.forEach(function(modelMock) {
            if ('params' in modelMock) {
                var model = ns.Model.get(modelId, modelMock.params);
                model.setData(_.cloneDeep(modelMock.data));
            }
        });
    };

    window.CKEDITOR = {};
});
