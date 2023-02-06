module.exports = {
    blocks: {
        'b-page': function() {
            return { block: 'b-page' };
        },
        'b-promo-page': function() {
            return { block: 'b-promo-page' };
        },
        'i-debug': function() {
            return { block: 'i-debug' };
        }
    },
    setData: function() {
    },
    BEMHTML: function() {
        if (this.broke) {
            throw new Error('broken block "' + this.block + '"!');
        }

        return this.block + ',';
    }
};
