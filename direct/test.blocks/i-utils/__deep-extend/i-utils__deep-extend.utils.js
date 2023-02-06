u.register({
    deepExtend: function deepExtend(extendable, extension) {
        var result = {};

        // todo@dima117a нужен рефакторинг https://st.yandex-team.ru/DIRECT-65106

        for (var n in extendable) extendable.hasOwnProperty(n) && (result[n] = extendable[n]);

        for (var property in extension) {
            if (typeof extension[property] === 'object' &&
                !Array.isArray(extension[property]) &&
                extension[property] !== null ) {

                result[property] = deepExtend(result[property] || {}, extension[property]);
            } else {
                result[property] = extension[property];
            }
        }

        return result;
    }
});
