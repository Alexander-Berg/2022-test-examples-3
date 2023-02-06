/**
 * transporter
 * ===
 *
 * Последовательно (как конвейер) обрабатывает файлы заданными обработчиками и склеивает
 * результаты обработки в один файл.
 *
 *
 * **Опции**
 *
 * * *String* **target** — Результирующий таргет. По умолчанию `?.merged.js`.
 * * *Array* **apply** — Массив обработчиков с параметрами. По умолчанию [].
 * * *Array* **applyResult** — Обработчик массива преобразованных файлов в финальный результат. Обязательный параметр.
 *
 *
 * **Пример**
 *
 * ```javascript
 *  var transporter = require('./enb-techs/tests/transporter');
 *
 *  nodeConfig.addTech(transporter.createTech('js'), {
 *      target: '?.merged.js',
 *      sourceSuffixes: ['js', 'utils.js', 'vm.js'] ,
 *      apply: [
 *          { handler: 'istanbul', grep: 'b-example-block' },
 *          { handler: 'wrap', begin: '// begin: {relativePath} \n', end: '\n// end: {relativePath}' },
 *          { handler: 'my-handler', param1: 'value1', param2: 'value2' },
 *      ],
 *      applyResult: {
 *          handler: 'join',
 *          separator: '\n;\n'
 *      }
 *  });
 * ```
 *
 * **Опции обработчика**
 *
 * * *String* **handler** — Название обработчика (под котрым он был зарегистрирован).
 * * *String* **grep** — Регулярное выражение для фильтрации списка файлов, к которым должен быть
 * применен обработчик. Не обязательный параметр. Если параметр отсутствует или === undefined,
 * то обработчик применяется ко всем файлам.
 *
 * Кроме параметров handler и grep можно указывать любые другие параметры, которые будут доступны
 * при выполнении обработчика.
 *
 *
 * **Добавление обработчика**
 *
 * ```javascript
 *  var transporter = require('./enb-techs/tests/transporter');
 *
 *  transporter.setHandler('my-handler', function(content, path, options, node) {
 *      ...
 *      return newContent;
 *  });
 * ```
 *
 * **Входные параметры функции обработчика**
 *
 * * *String* **prev** —  Контент файла (на момент вызова ф-ии он уже обработан предыдущими обработчиками из списка).
 * * *String* **path** — Абсолютный путь к обрабатываемому файлу.
 * * *Object* **options** — Опции обработчика, указанные в параметре *apply* при добавлении технологии.
 * * *Object* **node** — Текущая нода.
 *
 * **Опции финального обработчика**
 *
 * * *String* **handler** — Название обработчика (под которым он был зарегистрирован).
 *
 * **Добавление финального обработчика**
 *
 * ```javascript
 *  var transporter = require('./enb-techs/tests/transporter');
 *
 *  transporter.setResultHandler('my-handler', function(files, options) {
 *      ...
 *      return finalContent;
 *  });
 * ```
 *
 * **Входные параметры функции финального обработчика**
 *
 * * *String* **files** —  Массив содержимого файлов, обработанных функциями из apply.
 * * *Object* **options** — Опции обработчика, указанные в параметре *applyResult* при добавлении технологии.
 */

var vow = require('vow'),
    vowFs = require('enb/lib/fs/async-fs'),
    istanbul = require('istanbul'),
    _ = require('lodash'),
    instrumenter = new istanbul.Instrumenter(),
    buildFlow = require('enb/lib/build-flow'),
    extendObject = function(obj) {
        return [].slice.call(arguments).reduce(function(prev, arg) {
            return Object.keys(arg).reduce(function(prev, field) {
                prev[field] = arg[field];
                return prev;
            }, prev);
        }, obj);
    },
    applyFormat = function(format, obj) {
        return Object.keys(obj).reduce(function(prev, field) {
            return prev.replace('{' + field + '}', obj[field]);
        }, format);
    },
    handlers = {
        wrap: function(obj, path, options, node) {
            var relativePath = node.relativePath(path),
                args = { path: path, relativePath: relativePath },
                opts = extendObject({
                    begin: '',
                    end: ''
                }, options);

            opts.begin = applyFormat(opts.begin, args);
            opts.end = applyFormat(opts.end, args);

            return '' + opts.begin + obj + opts.end;
        },
        istanbul: function(obj, path, options, node) {
            return instrumenter.instrumentSync(obj + '', path);
        },
        istanbulCoverage: function(obj, path, options, node) {
            var fileInstrumenter = new istanbul.Instrumenter({ embedSource: true });

            fileInstrumenter.instrumentSync(obj + '', path);

            return fileInstrumenter.lastFileCoverage();
        }
    },
    resultHandlers = {
        join: function(files, opts) {
            return files.join(opts.separator);
        }
    };

module.exports = {
    setHandler: function(name, fn) {
        handlers[name] = fn;
    },
    setResultHandler: function(name, fn) {
        resultHandlers[name] = fn;
    },
    createTech: function(ext) {
        return buildFlow.create()
            .name('transporter')
            .target('target', '?.merged.js')
            .defineOption('filterFiles', _.constant(true))
            .defineOption('apply', [])
            .defineRequiredOption('applyResult')
            .useFileList(ext)
            .builder(function(files) {
                var node = this.node,
                    apply = this._apply.filter(function(obj) {
                        return !!obj;
                    }).map(function(obj) {
                        var handler = typeof obj === 'string' ? { handler: obj, grep: undefined } : obj;
                        handler.grep = handler.grep !== undefined ? new RegExp(handler.grep) : undefined;
                        return handler;
                    }),
                    applyResult = _.isString(this._applyResult) ?
                        { handler: this._applyResult } :
                        this._applyResult;

                return vow.all(files.filter(this._filterFiles).map(function(file) {
                    return vowFs.read(file.fullname, 'utf8').then(function(content) {
                        return { path: file.fullname, content: content };
                    });
                }))
                .then(function(files) {
                    var transformedFiles = files.map(function(file) {
                        return apply.filter(function(obj) {
                            return obj.grep === undefined || obj.grep.test(file.path);
                        }).reduce(function(prev, obj) {

                            if (!handlers[obj.handler])
                                throw new Error('unknown handler: ' + obj.handler);
                            return handlers[obj.handler](prev, file.path, obj, node);
                        }, file.content);
                    });

                    return resultHandlers[applyResult.handler](transformedFiles, applyResult);
                });
            })
            .createTech();
    }
}



