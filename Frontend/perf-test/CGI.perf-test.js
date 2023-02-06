const Benchmark = require('benchmark');
const { CGI } = require('../CGI/CGI');

const url = 'http://yandex.ua:1111/yandsearch?text=%D0%BB%D0%B8%D1%81%D0%B0+%D0%90%D0%BB%D0%B8%D1%81%D0%B0&exp_flags=flag1&exp_flags=flag2';
let cgi = CGI(url);
let i = 0;
let obj = cgi.clone();

const suite = new Benchmark.Suite();

/*
Baseline (конечно же, в другом окружении результаты будут другими):
ctor                    x 84,139 ops/sec ±1.83%
parseUrl                x 83,227 ops/sec ±1.48%
queryString             x 366,378 ops/sec ±1.65%
link                    x 363,176 ops/sec ±1.10%
link(true)              x 100,862 ops/sec ±1.46%
replacePath             x 127,818 ops/sec ±1.98%
clone                   x 28,437,425 ops/sec ±1.89%
replace                 x 258,577 ops/sec ±1.59%
replace two params      x 225,282 ops/sec ±1.37%
remove                  x 290,277 ops/sec ±0.81%
url                     x 340,469 ops/sec ±1.41%
complex url             x 131,638 ops/sec ±1.17%
complex url(true)       x 84,367 ops/sec ±1.06%
*/

suite
    .add('ctor', function() {
        cgi = CGI(url);
    })
    .add('parseUrl', function() {
        cgi.parseUrl(url);
    })
    .add('queryString', function() {
        cgi.queryString();
    })
    .add('link', function() {
        cgi.link();
    })
    .add('link(true)', function() {
        cgi.link(true);
    })
    .add('replacePath', function() {
        cgi.replacePath('/yaca/cat/1.html');
    })
    .add('clone', function() {
        obj = obj.clone();
    })
    .add('replace', function() {
        cgi.replace('param1', i++);
    })
    .add('replace two params', function() {
        cgi.replace('param1', i++, 'param2', i++);
    })
    .add('remove', function() {
        cgi.remove('exp_flags');
    })
    .add('url', function() {
        cgi.url();
    })
    .add('complex url', function() {
        cgi.replace('param1', '1').remove('exp_flags').url();
    })
    .add('complex url(true)', function() {
        cgi.replace('param1', '1').remove('exp_flags').url(true);
    })
    .on('cycle', function(event) {
        console.log(String(event.target)); // eslint-disable-line no-console
    })
    .run({ async: false });
