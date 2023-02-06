describe('b-campaign-optimizer-request', function() {
    var block,
        sandbox,
        campaigns = [
            {
                id: '2134',
                clicks: '2342',
                left: 423524,
                login: 'login',
                name: 'name'
            },
            {
                id: '21342',
                clicks: '23422',
                left: 4235242,
                login: 'login2',
                name: 'name2'
            }
        ],
        currencyNameShort = 'RUB';
    
    u.currencies = {
        getName: function(val) { return val }
    };
    window.CONSTS.SCRIPT_OBJECT = {
        host: "11289.beta4.direct.yandex.ru",
        path: "/registered/main.pl",
        protocol: "https"
    };

    /**
     * Создание блока, который тестируется
     */
    function createBlock() {
        return u.createBlock({
            block: 'b-campaign-optimizer-request',
            campaigns: campaigns,
            currencyNameShort: currencyNameShort
        });
    }

    beforeEach(function() {
        block = createBlock();
    });

    afterEach(function() {
        BEM.DOM.destruct(block.domElem);
    });

    it('при создании блока, все checkbox выбора кампаний - не выбранны', function() {
        var btns = block.findBlocksInside('list-col-checkbox', 'checkbox'),
            allOffCheck = btns.every(function(btn) { return !btn.isChecked()});

        expect(allOffCheck).to.be.true;
    });

    it('не выбранны кампании - кнопка "Заказать услугу" не активна', function() {
        var btn = block.findBlockInside('order', 'button');

        expect(btn.hasMod('disabled', 'yes')).to.be.true;
    });

    it('при выборе checkbox кампании, кнопка "Заказать услугу" становится доступной для клика', function() {
        var btn = block.findBlockInside('list-col-checkbox', 'checkbox');

        btn.setMod('checked', 'yes');
        expect(btn.hasMod('disabled', 'yes')).to.be.false;
    });

    describe('метод isValid', function() {

        it('возвращает false, если ни одна кампания не выбрана', function() {
            var btns = block.findBlocksInside('list-col-checkbox', 'checkbox');

            btns.forEach(function(btn) {
                btn.isChecked() && btn.delMod('checked');
            });
            expect(block.isValid()).to.be.false;
        });

        it('возвращает true, если есть выбраная кампания', function() {
            var btn = block.findBlockInside('list-col-checkbox', 'checkbox');

            btn.setMod('checked', 'yes');
            expect(block.isValid()).to.be.true;
        });
    });
});
