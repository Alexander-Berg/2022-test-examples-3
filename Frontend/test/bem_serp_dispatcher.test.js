const assert = require('assert');
const dispatcher = require('../ext/dispatchers/json2bem_serp');

describe('Серповый диспетчер json2bem_serp', function() {
    it('Должен удалять висячие пробелы из переводов при выгрузке', function() {
        const data = JSON.parse(dispatcher.setTplData({
            a: ' aaa\n\n',
            b: '\n\nbbb  ',
        }, s => s).data);

        assert(data.a === 'aaa');
        assert(data.b === 'bbb');
    });
});
