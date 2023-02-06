module.exports = function(app, raiffeisen, meta) {
    return {
        type: 'snippet',
        data_stub: function(dataBuilder) {
            return {
                num: 0,
                construct: dataBuilder.create()
                    .setAdapter('test')
                    .setTemplate('bno')
                    .add(raiffeisen)
                    .add(app)
                    .add(meta)
                    .build()
            };
        }
    };
};
