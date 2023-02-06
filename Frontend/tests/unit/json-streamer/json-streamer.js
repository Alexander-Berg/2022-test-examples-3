const jsonStreamer = require('../../../src/server/json-streamer');
const through = require('through');

describe('jsonStreamer', function() {
    let stream;
    let input;
    let result;

    beforeEach(function() {
        stream = jsonStreamer();
        input = through();
        result = '';
        input.pipe(stream);
    });

    it('должен правильно стримить коллекцию из нескольких объектов', function() {
        stream
            .on('data', (data) => {
                result = result + data;
            })
            .on('end', () => assert.equal(result, '[{"prop0":"val0"},{"prop1":"val1"}]'));

        input
            .queue({ prop0: 'val0' })
            .queue({ prop1: 'val1' })
            .queue(null);
    });

    it('должен обернуть поток, в котором нет объектов, в массив', function() {
        stream
            .on('data', (data) => {
                result = result + data;
            })
            .on('end', () => assert.equal(result, '[]'));

        input.queue(null);
    });

    it('должен применить функцию модификации к каждому переданному в поток объекту', function() {
        stream = jsonStreamer((obj) => {
            obj.prop = 'modified';
            return obj;
        });

        input = through();
        result = '';

        input.pipe(stream);

        stream
            .on('data', (data) => {
                result = result + data;
            })
            .on('end', () => assert.equal(result, '[{"prop":"modified"},{"prop":"modified"}]'));

        input
            .queue({ prop: 'val0' })
            .queue({ prop: 'val1' })
            .queue(null);
    });
});
