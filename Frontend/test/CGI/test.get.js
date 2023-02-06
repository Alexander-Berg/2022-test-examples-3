var should = require('should'),
    CGI = require('../../CGI/CGI').CGI,
    data = {
        scheme: 'http',
        host: 'yandex.ua:1111',
        hostname: 'yandex.ua',
        port: '1111',
        path: 'yandsearch',
        directory: 'yandsearch',
        args: {
            text: 'лиса Алиса',
            exp_flags: [
                'flag1',
                'flag2'
            ]
        }
    },
    url = 'http://yandex.ua:1111/yandsearch?text=%D0%BB%D0%B8%D1%81%D0%B0+%D0%90%D0%BB%D0%B8%D1%81%D0%B0&exp_flags=flag1&exp_flags=flag2';

describe('CGI', function() {
    var cgi,
        args = data.args,
        paramName = 'param',
        paramValue = 'value';

    beforeEach(function() {
        cgi = CGI(url);
    });

    describe('p', function() {
        it('should return value', function() {
            cgi.p('text').should.equal(args.text);
        });

        it('should return "undefined" for unknown', function() {
            should.not.exist(cgi.p('unknown'));
        });

        it('should return first value of array', function() {
            cgi.p('exp_flags').should.equal(args.exp_flags[0]);
        });

        it('should return second value of array', function() {
            cgi.p('exp_flags', 1).should.equal(args.exp_flags[1]);
        });

        it('should return first value of array if out of range', function() {
            cgi.p('exp_flags', 3).should.equal(args.exp_flags[0]);
        });
    });

    describe('getParam', function() {
        it('should return array for single param', function() {
            cgi.getParam('text').should.eql([args.text]);
        });

        it('should return array for multiple param', function() {
            cgi.getParam('exp_flags').should.eql(args.exp_flags);
        });

        it('should return undefined for unknown', function() {
            should.not.exist(cgi.getParam('unknown'));
        });
    });

    describe('getParams', function() {
        it('should return unchangeable params', function() {
            var cgi = CGI(url),
                params = cgi.getParams();

            params.exp_flags[0] = 'flag0';
            cgi.getParams().should.not.eql(params);
        });
    });

    describe('has', function() {
        it('should return true', function() {
            cgi.has('text').should.equal(1);
        });

        it('should return undefined for unknown', function() {
            should.not.exist(cgi.has('unknown'));
        });
    });

    describe('add', function() {
        it('should add param', function() {
            cgi.add(paramName, paramValue).p(paramName).should.equal(paramValue);
        });

        it('should add falsy param', function() {
            cgi.add(paramName, '').p(paramName).should.equal('');
            cgi.add(paramName, 0).p(paramName).should.equal(0);
            cgi.add(paramName, '0').p(paramName).should.equal('0');
        });
    });

    describe('remove', function() {
        it('should remove param', function() {
            should.not.exist(cgi.remove('text').p('text'));
        });

        it('should change order of parameters', function() {
            cgi
                .remove('text')
                .add('text', 0)
                .queryString()
                .should.equal('exp_flags=flag1&exp_flags=flag2&text=0', 'text should be the last');
        });

        // В _removeValue с 2014 года баг - порядок меняется только для первого параметра в remove
        xit('should change order when removing multiple parameters at once', function() {
            cgi
                .remove('non-existent', 'text')
                .add('text', 0)
                .queryString()
                .should.equal('exp_flags=flag1&exp_flags=flag2&text=0', 'text should be the last');
        });
    });

    describe('replace', function() {
        it('should replace existing param', function() {
            cgi.replace('text', paramValue).p('text').should.equal(paramValue);
        });

        it('should replace array', function() {
            cgi.replace('exp_flags', paramValue).p('exp_flags').should.equal(paramValue);
        });

        it('should add param if it does not already exist', function() {
            cgi.replace('unknown', paramValue).p('unknown').should.equal(paramValue);
        });
    });

    describe('replacePath', function() {
        it('should replace current path', function() {
            cgi.replacePath('/search/touch').path().should.equal('search/touch');
        });

        it('should replace current path even without slash at start of input path', function() {
            cgi.replacePath('search/touch').path().should.equal('search/touch');
        });

        it('should return updated directory on replaced path', function() {
            cgi.replacePath('/search/touch').directory().should.equal('search/touch');
        });

        it('should return updated file on replaced path', function() {
            cgi.replacePath('/yaca/cat/1.html').file().should.equal('1.html');
        });

        it('shouldn\'t replace origin path', function() {
            cgi.replacePath('/search/touch');
            cgi.path().should.not.equal('/search/touch');
        });
    });

    describe('replaceFile', function() {
        it('should add file', function() {
            CGI('http://yandex.ru/yaca/cat/').replaceFile('1.html').file().should.equal('1.html');
        });

        it('should add slash after a directory', function() {
            CGI('http://yandex.ru/yaca').replaceFile('1.html').path().should.equal('yaca/1.html');
        });

        it('should replace file', function() {
            var url = CGI('http://yandex.ru/yaca/cat/1.html').replaceFile('2.html');
            url.file().should.equal('2.html');
            url.path().should.equal('yaca/cat/2.html');
        });

        it('shouldn\'t replace origin file', function() {
            cgi.replaceFile('1.html');
            cgi.file().should.not.equal('1.html');
        });
    });

    describe('clone', function() {
        it('should return different object', function() {
            cgi.clone().should.not.equal(cgi);
        });

        it('should clone ajax parameters', function() {
            const clone = cgi.clone();
            clone.ajaxParams.join().should.equal(cgi.ajaxParams.join());
            clone.ajaxParams.should.not.equal(cgi.ajaxParams);
        });
    });

    describe('setAjaxParams', function() {
        it('should replace current ajax parameters', function() {
            const ajaxParams = ['z', 'a', 'b'];
            cgi.setAjaxParams(ajaxParams);
            cgi.ajaxParams.join().should.equal('z,a,b');
            cgi.ajaxParams.should.not.equal(ajaxParams, 'should make copy of an array');
        });
    });

    describe('queryString', function() {
        it('should return query string', function() {
            cgi.queryString().should.equal('text=%D0%BB%D0%B8%D1%81%D0%B0+%D0%90%D0%BB%D0%B8%D1%81%D0%B0&exp_flags=flag1&exp_flags=flag2');
        });

        it('should escape symbols "!\'()" in query parameter names', function() {
            const expected = [
                'blah%21=1',
                '%27blah%27=2',
                'blah%20%3A%28=3',
                'blah%20%3A%29=4'
            ].join('&');

            CGI('https://yandex.by/search/')
                .add('blah!', 1, '\'blah\'', 2, 'blah :(', 3, 'blah :)', 4)
                .queryString()
                .should.equal(expected);
        });

        it('should escape symbols "!\'()" in query parameter values', function() {
            const expected = [
                'text=blah%21',
                'text=%27blah%27',
                'text=blah%20%3A%28',
                'text=blah%20%3A%29',
                'text=blah%3A'
            ].join('&');

            CGI('https://yandex.by/search/')
                .add('text', ['blah!', '\'blah\'', 'blah :(', 'blah :)', 'blah:'])
                .queryString()
                .should.equal(expected);
        });
    });

    describe('path', function() {
        it('should return path', function() {
            cgi.path().should.equal(data.path);
        });

        it('should return empty string', function() {
            cgi = CGI('http://yandex.ua/');
            cgi.path().should.equal('');
        });

        it('should return path for nested directories', function() {
            cgi = CGI('http://yandex.ua/settings/configure?userID=123');
            cgi.path().should.equal('settings/configure');
        });
    });

    describe('directory', function() {
        it('should return directory', function() {
            cgi.directory().should.equal(data.directory);
        });

        it('should return empty string', function() {
            cgi = CGI('http://yandex.ua/');
            cgi.directory().should.equal('');
        });

        it('should return directory without file for nested directories', function() {
            cgi = CGI('http://yandex.ua/yaca/cat/1.html?text=wikipedia');
            cgi.directory().should.equal('yaca/cat/');
        });
    });

    describe('file', function() {
        it('should return empty string for the path without dot', function() {
            cgi = CGI('http://yandex.ua/yaca/cat?text=ya.ru');
            cgi.file().should.equal('');
        });

        it('should return file', function() {
            cgi = CGI('http://yandex.ua/yaca/cat/1.html?text=wikipedia');
            cgi.file().should.equal('1.html');
        });
    });

    describe('scheme', function() {
        it('should return http', function() {
            var url = 'http://yandex.ua/';
            cgi = CGI(url);

            cgi.scheme().should.equal('http');
            cgi.url().should.equal(url);
        });

        it('should return https', function() {
            var url = 'https://yandex.ua/';
            cgi = CGI(url);

            cgi.scheme().should.equal('https');
            cgi.url().should.equal(url);
        });

        it('should return undefined', function() {
            var url = '//yandex.ua/';
            cgi = CGI(url);

            should.not.exist(cgi.scheme());
            cgi.url().should.equal(url);
        });
    });

    describe('hostname', function() {
        it('should get hostname #1', function() {
            cgi.hostname().should.equal(data.hostname);
        });

        it('should get hostname #2', function() {
            var url = data.hostname + '/search?text=test';
            cgi = CGI(url);

            should.not.exist(cgi.scheme());
            cgi.hostname().should.equal(data.hostname);
        });

        it('should get hostname from url with scheme in the middle', function() {
            var url = 'yandex.ua/http://yandex.ua';
            cgi = CGI(url);
            cgi.url().should.equal('//yandex.ua/http://yandex.ua');
        });
    });

    describe('host', function() {
        it('should get hostname with port', function() {
            cgi.host().should.equal(data.host);
        });

        it('should remove 80 port from result', function() {
            var url = 'http://yandex.ua:80/text?test=1';
            cgi = CGI(url);

            cgi.host().should.equal('yandex.ua');
        });
    });

    describe('port', function() {
        it('should get port', function() {
            cgi.port().should.equal(data.port);
        });
    });

    describe('setHostname', function() {
        it('should set hostname', function() {
            var hostName = 'yandex.ua';
            cgi.setHostname(hostName).hostname().should.equal(hostName);
        });
    });

    describe('link', function() {
        it('should return link #1', function() {
            cgi.link().should.equal('/yandsearch?text=%D0%BB%D0%B8%D1%81%D0%B0+%D0%90%D0%BB%D0%B8%D1%81%D0%B0&exp_flags=flag1&exp_flags=flag2');
        });

        it('should return link #2', function() {
            cgi = CGI('http://yandex.ua/');
            cgi.link().should.equal('/');
        });

        it('should remove ajax params', function() {
            cgi = CGI('http://yandex.ua/?callback=foo&yu=baz');
            cgi.link(true).should.equal('/');
        });

        it('should not remove ajax params', function() {
            cgi = CGI('http://yandex.ua/?callback=foo&yu=baz');
            cgi.link().should.equal('/?callback=foo&yu=baz');
        });
    });

    describe('url', function() {
        it('should return url', function() {
            cgi.url().should.equal('http://yandex.ua:1111/yandsearch?text=%D0%BB%D0%B8%D1%81%D0%B0+%D0%90%D0%BB%D0%B8%D1%81%D0%B0&exp_flags=flag1&exp_flags=flag2');
        });

        it('should remove 80 port from result', function() {
            var url = 'http://yandex.ua:80/search/?text=1';
            cgi = CGI(url);

            cgi.url().should.equal('http://yandex.ua/search/?text=1');
        });

        it('should process multiple slash', function() {
            var url = 'yandex.ua///text//test////ololo';
            cgi = CGI(url);
            cgi.url().should.equal('//yandex.ua/text//test////ololo');
        });
    });

    describe('specific query params', function() {
        ['constructor', 'toString', 'hasOwnProperty'].forEach(function(param) {
            it('should properly work with «' + param + '» param name', function() {
                const url = 'https://yandex.ru/search/?text=%D0%BB%D0%B8%D1%81%D0%B0&' + param + '=you+and+i&reask=1';
                cgi = CGI(url);
                cgi.url().should.equal(url);
            });
        });

        it('should parse consecutive ampersands', function() {
            cgi = CGI('https://yandex.ru/search/?&&');
            cgi.url().should.equal('https://yandex.ru/search/');
        });

        it('should parse trailing ampersand', function() {
            cgi = CGI('https://yandex.ru/search/?text=test&lr=1&');
            cgi.url().should.equal('https://yandex.ru/search/?text=test&lr=1');
        });

        it('should parse leading ampersand', function() {
            cgi = CGI('https://yandex.ru/search/?&text=test&lr=1');
            cgi.url().should.equal('https://yandex.ru/search/?text=test&lr=1');
        });

        it('should parse param with empty key', function() {
            cgi = CGI('https://yandex.ru/search/?text=test&=123&lr=2');
            cgi.url().should.equal('https://yandex.ru/search/?text=test&lr=2');
        });

        it('should parse param with empty value', function() {
            cgi = CGI('https://yandex.ru/search/?text=&lr=');
            cgi.url().should.equal('https://yandex.ru/search/?text=&lr=');
        });

        it('should parse params without value', function() {
            cgi = CGI('https://yandex.ru/search/?text&lr&x');
            cgi.url().should.equal('https://yandex.ru/search/?text=&lr=&x=');
        });

        it('should parse param with broken unicode value', function() {
            cgi = CGI('https://webgorlovka.com.ua/index.php?do=tags&tag=%CA%E8%E5%E2');

            cgi.p('do').should.equal('tags');
            cgi.getParam('do').should.deepEqual(['tags']);

            cgi.p('tag').should.equal('%CA%E8%E5%E2');
            cgi.getParam('tag').should.deepEqual(['%CA%E8%E5%E2']);

            // Problem: additional escape for broken value
            // cgi.url().should.equal('https://webgorlovka.com.ua/index.php?do=tags&tag=%CA%E8%E5%E2');
            cgi.url().should.equal('https://webgorlovka.com.ua/index.php?do=tags&tag=%25CA%25E8%25E5%25E2');
        });
    });
});
