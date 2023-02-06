!BEM && (BEM = {});

BEM.I18N = function(block, key) { return key; };

function iget(str, data) {
    var args = arguments,
        i = 1,
        mark = '___SPLITER___MARK___',
        splitter = function(sub) { return mark + sub + mark; },
        replacer = function(part) {
            var r = part.match(/%\{([a-zA-Z0-9\-_]*)#?([^}]*)?\}/);

            if (r === null) return part;

            // name = r[1]; // ключ
            // replacer = r[2]; // подстановка
            var obj = data[r[1]];
            
            return typeof obj === 'function' ? obj() : obj;
        };

    if (args.length === 2 && typeof data === 'object')
    {
        return str
            .replace(/(%\{[a-zA-Z0-9\-_]*#?[^}]*?\})/g, splitter)
            .split(mark)
            .map(replacer);
    }

    return str.replace(/%s/g, function() {
        return args[i++];
    });
}

/**
 *
 * @param keyset
 * @param key
 * @param str
 * @param data
 * @returns {*}
 */
// iget2(common-i18n, str)
// iget2(keyset, key, str)
// iget2(common-i18n, str, data)
// iget2(keyset, key, str, data)

function iget2(keyset, key, str, data) {
    var args = arguments,
        i = 1,
        mark = '___SPLITER___MARK___',
        splitter = function(sub) { return mark + sub + mark; };

    if (args.length === 2) {
        return key;
    }

    if (typeof data === 'object') {
        return str
            .replace(/(\{[a-zA-Z0-9\-_]*\})/g, splitter)
            .split(mark)
            .filter(function(key) { return key != ''; })
            .map(function(part) {
                var r = part.match(/\{([a-zA-Z0-9\-_]*)\}/);

                if (r === null) return part;

                var obj = data[r[1]];

                if (typeof obj === 'function') {
                    return obj();
                } else {
                    obj.block || (obj.block = keyset);
                    return BEMHTML.apply(obj);
                }
            })
            .join('');
    }

    return str.replace(/%s/g, function() {
        return args[i++];
    });
}
