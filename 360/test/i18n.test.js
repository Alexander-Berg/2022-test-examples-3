var webpack = require('webpack');

require('chai/register-expect');
var chai = require("chai");
var sinon = require("sinon");
sinon.assert.expose(chai.assert, { prefix: "" });

describe('webpack-i18n-plugin', function() {
    it('works', function(done) {
        var config = require('../example/webpack.config.js');

        webpack(config, (err, stats) => {
            if(err) return done(err);

            stats = stats.toJson({
                errorDetails: true
            });

            if(stats.errors.length > 0) {
                return done(new Error(stats.errors[0]));
            }

            done();
        });
    });

    it('works fine', function() {
        this.sinon = sinon.sandbox.create();
        var out = []
        this.sinon.stub(console, 'log').callsFake(function(d) {
            out.push(d)
        });

        require('../example/output/en.js');
        require('../example/output/ru.js');
        require('../example/output/de.js');
        this.sinon.restore();

        var expected = [
            'One attached file',
            '2 attached file',
            '5 attached file',
            '23 attached file',
            'file',
            'file',
            'file',
            'file',
            'My attachments',
            'August 25, 2017',
            '22 messages selected',
            'Один прикреплённый файл',
            '2 прикреплённых файла',
            '5 прикрёпленных файлов',
            '23 прикреплённых файла',
            'файл',
            'файла',
            'файла',
            'файлов',
            'Мои вложения',
            '25 августа 2017 года',
            'Выбрано 22 письма',
            'One attached file',
            '2 attached files',
            '5 attached files',
            '23 attached files',
            'file',
            'files',
            'files',
            'files',
            'My attachments',
            'August 25, 2017',
            '22 messages selected'
        ];

        out.forEach(function(str, idx) {
            expect(str).to.be.equal(expected[idx]);
        });
    });
});
