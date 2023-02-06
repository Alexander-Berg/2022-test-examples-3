describeBlock('adapter-entity-card__cinema-get-partner-price', function(block) {
    let context;
    let licenses;

    const avod = { monetization_model: 'AVOD' };
    const svod = { monetization_model: 'SVOD' };
    const tvod = { monetization_model: 'TVOD', price: 99 };
    const est = { monetization_model: 'EST', price: 299 };

    beforeEach(function() {
        context = { tld: 'ru' };
        licenses = [];
    });

    it('does not exist if there is no `licenses`', function() {
        licenses = null;
        assert.isUndefined(block(context, licenses));
    });

    it('does not exist if there is empty `licenses`', function() {
        assert.isUndefined(block(context, licenses));
    });

    it('`avod` if exists', function() {
        licenses = [avod, svod, tvod, est];
        assert.equal(block(context, licenses).text.key, 'смотреть с рекламой');
    });

    it('`tvod` if exists', function() {
        licenses = [svod, tvod, est];
        assert.equal(block(context, licenses).text.minValue, 99);
    });

    it('`est` if exists', function() {
        licenses = [svod, est];
        assert.equal(block(context, licenses).text.value, 299);
    });

    it('`svod` if exists', function() {
        licenses = [svod];
        assert.equal(block(context, licenses).text.key, 'по подписке');
    });
});

describeBlock('adapter-entity-card__cinema-get-kinopoisk-price', function(block) {
    let context;
    let legal;
    const tvod = { price: 99 };
    const est = { price: 299 };
    const tvodHasDiscount = { price: 99, discount_price: 7, promocode: 'TESTCODE' };
    const estHasDiscount = { price: 299, discount_price: 199, promocode: 'TESTCODE' };

    // датасинк удаляет потраченный промокод но не удаляет discount_price
    const tvodDeletedPromocode = { price: 99, discount_price: 7 };
    const estDeletedPromocode = { price: 299, discount_price: 199 };

    beforeEach(function() {
        context = {};
        legal = {};
    });

    it('does not exist if there is no `legal`', function() {
        legal = null;
        assert.isUndefined(block(context, legal));
    });

    it('does not exist if there is no `tvod` and `est`', function() {
        assert.isUndefined(block(context, legal));
    });

    it('`tvod` if exists', function() {
        legal = { tvod };
        assert.equal(block(context, legal).minValue, 99);
    });

    it('`tvodHasDiscount` if exists', function() {
        legal = { tvod: tvodHasDiscount };
        assert.equal(block(context, legal).minValue, 7);
    });

    it('must not have discount for `tvod` if promocode is missing', function() {
        legal = { tvod: tvodDeletedPromocode };
        assert.equal(block(context, legal).minValue, 99);
    });

    it('`est` if exists', function() {
        legal = { est };
        assert.equal(block(context, legal).value, 299);
    });

    it('`estHasDiscount` if exists', function() {
        legal = { est: estHasDiscount };
        assert.equal(block(context, legal).value, 199);
    });

    it('must not have discount for `est` if promocode is missing', function() {
        legal = { est: estDeletedPromocode };
        assert.equal(block(context, legal).value, 299);
    });
});
