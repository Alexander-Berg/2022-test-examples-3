var CGI = require('../../CGI/CGI').CGI,
    url = 'http://yandex.ua/yandsearch?text=test&debug=hamster';

describe('Mutable CGI', function() {
    var cgi,
        paramName = 'param',
        paramValue = 'value';

    beforeEach(function() {
        cgi = CGI(url);
    });

    describe('add', function() {
        it('should add param', function() {
            var link = cgi.add(paramName, paramValue).link();
            link.should.equal('/yandsearch?text=test&debug=hamster&param=value');
        });

        it('should add param once', function() {
            var link = cgi.add(paramName, paramValue).add(paramName, paramValue).link();
            link.should.equal('/yandsearch?text=test&debug=hamster&param=value');
        });

        it('should not mutate global', function() {
            cgi.add('debug', 'eventlog').link();
            var link2 = cgi.add(paramName, paramValue).link();
            link2.should.equal('/yandsearch?text=test&debug=hamster&param=value');
        });

        it('should add ""', function() {
            var link = cgi.add(paramName, '').link();
            link.should.equal('/yandsearch?text=test&debug=hamster&param=');
        });

        it('should add 0', function() {
            var link = cgi.add(paramName, 0).link();
            link.should.equal('/yandsearch?text=test&debug=hamster&param=0');
        });

        it('should add "0"', function() {
            var link = cgi.add(paramName, '0').link();
            link.should.equal('/yandsearch?text=test&debug=hamster&param=0');
        });

        it('should remove and add', function() {
            var link = cgi.remove('text').add(paramName, 0).link();
            link.should.equal('/yandsearch?debug=hamster&param=0');
        });

        it('should remove and add the same param', function() {
            var link = cgi.remove('text').add('text', 'new-value').link();
            link.should.equal('/yandsearch?debug=hamster&text=new-value');
        });
    });
});
