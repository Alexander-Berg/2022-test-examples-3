/* global mainPriv:true */
/**
 * Собирает стек вызовов
 * @author Shuhrat Shadmonkulov <zumra6a@yandex-team.ru>
 * @see SERP-30114
 */

// Hermione плагин selective по умолчанию выключен. Включается переменой окружения.
if (process.env.selective_enabled === 'true') {
    var autoTestData = {};
    var ajaxBemJson = [];
    let adapterCalls;
    let currentAdapterBlocks;

    const path = require('path');
    const { BufferedBus } = require('@yandex-int/ipbus');
    // Файл выполняется в контексте report-renderer, и важно использовать именно его luster.
    const lusterPath = process.env.NODE_PATH ? path.resolve(process.env.NODE_PATH, 'luster') : 'luster';
    const luster = require(lusterPath);

    const bus = BufferedBus.create();
    luster.on('disconnect', () => bus.close());
    const send = bus.send.bind(bus, 'selective.autoTestData');

    (function(oldMainPriv) {
        mainPriv = function(data) {
            var expFlags = data.reqdata.flags;

            autoTestData.testId = null;
            if ('autotest_id' in expFlags) {
                autoTestData.entry = data.entry;
                autoTestData.testId = expFlags.autotest_id;
                autoTestData.timestamp = Date.now();
                autoTestData.privBlocks = [];
                autoTestData.privTree = [{
                    name: 'root',
                    children: []
                }];
                autoTestData.bemjsonBlocks = [];
                autoTestData.clarifyBlockNames = [];
                autoTestData.noSelectors = {};

                if (expFlags.autotest_selector) {
                    // .t-construct-adapter__companies, .t-construct-adapter__address-ymaps ->
                    // [t-construct-adapter__companies, t-construct-adapter__address-ymaps]
                    var blocksBySelectors = _.uniq(expFlags.autotest_selector.split(',').map(function(selector) {
                        return selector.replace('.', '');
                    }));

                    autoTestData.clarifyBlockNames = autoTestData.clarifyBlockNames.concat(blocksBySelectors);
                }
            }

            // логируем блоки в адаптерах только для hermione тестов, исключая pre-search
            if (expFlags.test_tool === 'hermione' && data.entry !== 'pre-search') {
                adapterCalls = {};
            }

            var bemjson = oldMainPriv(data);

            blocks['dump-autotest-data'](data, bemjson);
            blocks['dump-blocks-usage'](data, adapterCalls);
            adapterCalls = undefined;

            return bemjson;
        };
    })(mainPriv);

    (function(wrapToTryCatch) {
        blocks['wrap-block-try-catch'] = skipWrap(function(block, blockName) {
            if (block._wrapped) return block;

            var wrappedBlock = wrapToTryCatch(block, blockName),
                result = function() {
                    var privTree = autoTestData.privTree;
                    if (privTree) {
                        var priv = {
                            name: blockName,
                            children: []
                        };
                        // пушим последнему элементу массива дочерний элемент для создания ветки вызовов
                        _.last(privTree).children.push(priv);
                        // добавляем объект прива в общий массив, делая его родителем для следующего прива
                        privTree.push(priv);
                    }

                    // Собираем вызовы блоков в адаптерах
                    let isAdapter;
                    if (adapterCalls) {
                        // если вызван конструкторский блок -
                        // ожидаем context со св-вом adapterType первым аргументом
                        const contextAdapterType = (arguments[0] || {}).adapterType;
                        isAdapter = blocks['dump-blocks-usage__is-adapter'](blockName, contextAdapterType);
                        if (currentAdapterBlocks) {
                            blocks['dump-blocks-usage__add-block'](blockName, currentAdapterBlocks);
                        } else if (isAdapter) {
                            let adapterName = blockName === 'construct' ?
                                'adapter-' + contextAdapterType :
                                blocks['dump-blocks-usage__block-name'](blockName);
                            adapterName = blocks['adapter__normalize-type-string'](adapterName);
                            currentAdapterBlocks = adapterCalls[adapterName] = adapterCalls[adapterName] || [];
                        } else if (contextAdapterType) {
                            let adapterName = 'adapter-' + contextAdapterType;
                            adapterName = blocks['adapter__normalize-type-string'](adapterName);
                            adapterCalls[adapterName] = adapterCalls[adapterName] || [];
                            blocks['dump-blocks-usage__add-block'](blockName, adapterCalls[adapterName]);
                        }
                    }

                    var wrappedBlockResult = wrappedBlock.apply(this, arguments);

                    // Перехватываем bemJson из блоков до того, как он превратился в HTML
                    if (wrappedBlockResult && wrappedBlockResult.construct && wrappedBlockResult.content) {
                        ajaxBemJson = ajaxBemJson.concat(wrappedBlockResult.content);
                    }

                    if (wrappedBlockResult && blockName === 'feedback') {
                        ajaxBemJson = ajaxBemJson.concat(wrappedBlockResult);
                    }

                    isAdapter && (currentAdapterBlocks = undefined);

                    privTree && privTree.pop();

                    return wrappedBlockResult;
                };

            result._wrapped = true;

            return result;
        });
    })(blocks['wrap-block-try-catch']);

    blocks['dump-autotest-data'] = function(data, bemjson) {
        var currBemJson = (ajaxBemJson.length && ajaxBemJson) || bemjson;
        var exports = {}; // чтобы нижеследующий инклуд не гадил в глобальный exports

        /*borschik:include:../../../../../node_modules/@yandex-int/si.utils/Regress/Regress.js*/

        if (autoTestData.testId) {
            var Regress = exports.Regress,
                // Нормализует имена адаптеров, начинающихся с t-construct-adapter
                getNormalizedAdapterName = function(selector) {
                    var matched = selector.match(/t-construct-adapter__(.*)/);
                    if (matched) {
                        var adapterType = matched[1];
                        return 'adapter-' + adapterType;
                    }
                    return selector;
                },
                // Дополняет имя селектора адаптером. Если в тесте был передан селектор sport-livescore, то в
                // зависимости нужно добавить и adapter-sport-livescore
                // @see https://st.yandex-team.ru/MQ-1231
                getPrivNames = function(selector) {
                    const normalized = getNormalizedAdapterName(selector);
                    const res = [normalized];
                    if (!normalized.startsWith('adapter-')) {
                        res.push('adapter-' + normalized);
                    }
                    return res;
                };

            // фильтруем блоки, если такие присутсвуют
            if (autoTestData.clarifyBlockNames) {
                var clarifyBlockNames = autoTestData.clarifyBlockNames;

                let bemJsonBlocksUsage = {};

                autoTestData.bemjsonBlocks = _(clarifyBlockNames)
                    .flatMap(function(blockName) {
                        let result = Regress.extractFlattenNodeClasses(currBemJson, blockName);

                        // Собираем использования блоков в адаптерах
                        let privName = blocks['dump-blocks-usage__block-name'](getNormalizedAdapterName(blockName));
                        if (blocks['dump-blocks-usage__is-adapter'](privName) && result.length) {
                            privName = blocks['adapter__normalize-type-string'](privName);
                            bemJsonBlocksUsage[privName] = [];
                            result.forEach(className =>
                                blocks['dump-blocks-usage__add-block'](className, bemJsonBlocksUsage[privName])
                            );
                        }

                        return result;
                    })
                    .uniq()
                    .value();

                autoTestData.privBlocks = _(clarifyBlockNames)
                    .flatMap(getPrivNames)
                    .flatMap(function(blockName) {
                        return Regress.extractPrivList(autoTestData.privTree, blockName);
                    })
                    .uniq()
                    .value();

                // Дока: логируем статистику использования блоков в адаптерах по bemjson
                blocks['dump-blocks-usage'](data, bemJsonBlocksUsage);

                const isBemjsonEmpty = _.isEmpty(autoTestData.bemjsonBlocks);
                const isPrivEmpty = _.isEmpty(autoTestData.privBlocks);
                autoTestData.noSelectors[autoTestData.entry] = isBemjsonEmpty && isPrivEmpty;

                // если не смогли найти в bemjson нужный блок, то сохраняем все блоки из bemjson
                if (isBemjsonEmpty) {
                    autoTestData.bemjsonBlocks = _(Regress.extractClassesRecursively(currBemJson))
                        .flatten()
                        .uniq()
                        .value();
                }

                // если не смогли найти в дереве привов нужный, то сохраняем привы всего дерева
                if (isPrivEmpty) {
                    autoTestData.privBlocks = Regress.flattenTreeList(autoTestData.privTree[0]);
                }
            }

            autoTestData.url = RequestCtx.GlobalContext.cgi.url(); // Ссылка на страницу
            autoTestData.query = data.query.text; // Текст запроса
            autoTestData.platform = blocks['i-global__regress_platform']; // Название платформы

            // В массиве оставить только уникальные элементы
            autoTestData = _.mapValues(autoTestData, function(value) {
                return _.isArray(value) ? _.uniq(value) : value;
            });

            delete autoTestData.privTree; // не нужно хранить дерево привов
            ajaxBemJson = [];

            send(Buffer.from(JSON.stringify(autoTestData)));
        }
    };

    blocks['dump-blocks-usage'] = function(data, blocksUsage) {
        if (!_.isEmpty(blocksUsage)) {
            Util.getLog('blocks-usage')(JSON.stringify(blocksUsage) + '\n');
        }
    };

    /**
     * Обрезает элементы и модификаторы, оставляя только имя блока.
     *
     * @param {String} fullName - имя сущности, включая блоки, элементы и модификаторы
     *
     * @returns {String} имя блока
     */
    blocks['dump-blocks-usage__block-name'] = skipWrap(function(fullName) {
        return fullName.split('_').shift();
    });

    /**
     * Определяет, является ли блок адаптером
     *
     * Адаптеры возвращают конструкторский dataset, который переводится в bemjson в блоке construct.
     * Поэтому считаем адаптером и блок adapter-*, и construct
     *
     * @param {String} fullName - имя сущности, включая блоки, элементы и модификаторы
     * @param {ConstructPair} [blockArguments[0]] - аргументы, переданные блоку.
     *      Используется для определения адаптера при вызове блока 'construct'
     *
     * @returns {Boolean}
     */
    blocks['dump-blocks-usage__is-adapter'] = skipWrap(function(fullName, contextAdapterType) {
        return (fullName === 'construct' && contextAdapterType) ||
            (fullName.startsWith('adapter-') && !fullName.includes('__'));
    });

    /**
     * Добавляет имя блока к массиву блоков. Элементы и модификаторы обрезаются. Сохраняет уникальность в массиве
     *
     * @param {String} fullName - имя сущности, включая блоки, элементы и модификаторы
     * @param {String[]} [blockList] - массив блоков
     *
     * @returns {String[]} дополненный массив блоков
     */
    blocks['dump-blocks-usage__add-block'] = skipWrap(function(fullName, blockList = []) {
        const excludeBlocks = ['construct', 'adapter-legacy', 't-construct-adapter', 'composite'];
        const blockName = blocks['dump-blocks-usage__block-name'](fullName);
        const denyPush = !blockName ||
            excludeBlocks.includes(blockName) ||
            blockList.includes(blockName) ||
            blocks['dump-blocks-usage__is-adapter'](blockName);

        if (!denyPush) {
            blockList.push(blockName);
        }

        return blockList;
    });
}
