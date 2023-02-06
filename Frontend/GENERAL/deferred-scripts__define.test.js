describe('Ya.define', function() {
    var getUid = (function() {
        var uid = 0;

        return function() {
            uid++;
            return 'uid' + uid;
        };
    })();

    function getUniqueModuleName() {
        return 'module-' + getUid();
    }

    it('should immediately execute module without dependencies', function(done) {
        var moduleExecutionLog = '',
            moduleName = getUniqueModuleName();

        Ya.define(moduleName, [], function() {
            moduleExecutionLog += '1';

            assert.equal(moduleExecutionLog, '1', 'should execute module exactly once');
            assert.strictEqual(Ya.define.modules[moduleName], undefined, 'module name should be removed from list of unresolved modules');
            done();
        });
    });

    it('should execute module when dependency is resolved', function(done) {
        var moduleExecutionLog = '',
            dependency = getUniqueModuleName();

        Ya.define(getUniqueModuleName(), [dependency], function() {
            moduleExecutionLog += '1';
            assert.equal(moduleExecutionLog, '1', 'should execute module when dependency is resolved');
            done();
        });
        assert.equal(moduleExecutionLog, '', 'should not execute module with unresolved dependency');

        Ya.define(dependency);
    });

    it('should resolve nested dependencies', function(done) {
        var moduleExecutionLog = '';

        // В этом тесте важны имена модулей - от этого зависит порядок их перечисления
        // при разрешении зависимостей
        Ya.define('test-module-2', ['test-module-1'], function() {
            moduleExecutionLog += '2';
        });

        Ya.define('test-module-3', ['test-module-2'], function() {
            moduleExecutionLog += '3';

            assert.equal(moduleExecutionLog, '123',
                'should resolve nested dependencies in order test-module-1->test-module-2->test-module-3');
            done();
        });

        Ya.define('test-module-1', [], function() {
            moduleExecutionLog += '1';
        });
    });

    it('should ignore error in one of modules', function(done) {
        var moduleExecutionLog = '';

        Ya.define(getUniqueModuleName(), [], function() {
            throw new Error('Intentional error to check error handling');
        });
        Ya.define(getUniqueModuleName(), [], function() {
            moduleExecutionLog += '2';

            assert.equal(moduleExecutionLog, '2', 'should execute second module');
            done();
        });
    });

    it('should execute dependencies only when module is finished', function(done) {
        var moduleExecutionLog = '',
            moduleName1 = getUniqueModuleName(),
            moduleName2 = getUniqueModuleName(),
            moduleName3 = getUniqueModuleName();

        Ya.define(moduleName3, [moduleName1, moduleName2], function() {
            moduleExecutionLog += '3';

            assert.equal(moduleExecutionLog, '1-start1-finish3', 'should execute third module only when first is finished');
            done();
        });

        Ya.define(moduleName1, [], function() {
            moduleExecutionLog += '1-start';
            Ya.define(moduleName2);
            moduleExecutionLog += '1-finish';
        });
    });
});
