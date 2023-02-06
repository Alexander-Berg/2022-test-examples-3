/* global xflags */
(function() {

    var $inputCustomJson = document.querySelector('#customJson');
    var $expCustomJsonSelector = document.querySelector('.js-exp-custom-json-selector');

    var $inputUserJson = document.querySelector('#userJson');
    var $userJsonSelector = document.querySelector('.js-user-json-selector');

    var $result = document.querySelector('#result');
    var $error = document.querySelector('#error');

    var last_custom_json = null;
    var last_user_json = null;

    // Генерируем select-ы для выбора кастомного JSON-а и userJson-а.
    var item;
    for (var expCustomJsonId in testData.expCustomJson) {
        item = document.createElement('option');
        item.value = expCustomJsonId;
        item.innerText = expCustomJsonId;
        $expCustomJsonSelector.appendChild(item);
    }

    for (var userJsonId in testData.userJson) {
        item = document.createElement('option');
        item.value = userJsonId;
        item.innerText = userJsonId;
        $userJsonSelector.appendChild(item);
    }

    var setCustomJson = function(customJson) {
        $inputCustomJson.value = JSON.stringify(customJson, null, 4);
    };

    var setUserJson = function(userJson) {
        $inputUserJson.value = JSON.stringify(userJson, null, 4);
    };

    $expCustomJsonSelector.addEventListener('change', function() {
        setCustomJson(testData.expCustomJson[$expCustomJsonSelector.value]);
    });

    $userJsonSelector.addEventListener('change', function() {
        setUserJson(testData.userJson[$userJsonSelector.value]);
    });

    // Начальное состояние:
    setCustomJson(testData.expCustomJson['']);
    setUserJson(testData.userJson['']);

    function validate() {
        var _custom_json = $inputCustomJson.value;
        var _user_json = $inputUserJson.value;

        if (_custom_json !== last_custom_json || _user_json !== last_user_json) {
            last_custom_json = _custom_json;
            last_user_json = _user_json;

            try {
                // Сбрасывает текущие значения.
                $result.innerText = '';
                $error.innerText = '';

                // Проверяем, что customJson и userJson - валидные JSON-ы.
                _custom_json = string2obj(_custom_json, 'Невалидный кастомный JSON');
                _user_json = string2obj(_user_json, 'Невалидный userJSON');

                const values = convertToValues(_user_json);

                // Выполняем проверку.
                const checker = xflags.check(values, _custom_json.conditions);
                $result.innerHTML = checker.getValue()
                    ? '<span style="font-size: 120%; color: green;">Подходит</span>'
                    : '<span style="font-size: 120%; color: red;">Не подходит</span>';

                if (checker.getError()) {
                    $error.innerText = checker.getError().message;
                }

            } catch (ex) {
                // Сбрасывает текущие значения.
                $result.innerText = '';
                $error.innerText = '';

                // Показываем ошибку
                $error.innerText = ex;
            }
        }
        setTimeout(validate, 100);
    }

    function string2obj(str, errorMessage) {
        var tmpVar;
        try {
            eval('tmpVar=' + str); // потому что на входе может быть невалидный JSON
            if (typeof tmpVar === 'object') {
                return tmpVar;
            } else {
                throw errorMessage;
            }
        } catch (ex) {
            throw errorMessage;
        }
    }

    validate();

}());

function objToMap(obj) {
    const stringifiedPairs = Object.entries(obj)
        .filter(([, v]) => v != null)
        .map(([k, v]) => [k, v.toString()]);
    return new Map(stringifiedPairs);
}

function convertToValues(obj) {
    const { Variable } = window.xflags;
    const values = new Map();
    for (const [key, val] of Object.entries(obj)) {
        let value;
        if (Array.isArray(val)) {
            value = Variable.array(val.filter(v => v != null).map(v => v.toString()));
        } else if (key === 'settings') {
            value = Variable.map(objToMap(val));
        } else if (typeof val === 'string') {
            value = Variable.string(val);
        } else if (typeof val === 'number') {
            value = Variable.int(parseInt(val, 10));
        } else if (typeof val === 'boolean') {
            value = Variable.boolean(val);
        }
        values.set(key, value);
    }

    return values;
}
