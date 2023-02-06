describe('b-outboard-controls', function() {
    var clock;

    beforeEach(function() {
        clock = sinon.useFakeTimers();
    });

    afterEach(function() {
        clock.restore();
    });

    //блок-заглушка, реализующий i-outboard-controls
    BEM.DOM.decl({ block: 'i-outboard-controls-impl', implements: 'i-outboard-controls' }, {
        prepareToShow: sinon.spy(),
        provideData: sinon.spy(),
        declineChange: sinon.spy()
    });

    describe('b-outboard-controls', function() {

        var blockParams = { paramName: 'paramValue' },
            tree,
            outboard,
            switcherNode,
            outboardImpl,
            popupNode,
            getButton;

        beforeEach(function() {
            tree = u.getDOMTree([
                {
                    block: 'b-outboard-controls',
                    js: { id: 'control' },
                    content: {
                        elem: 'popup',
                        acceptButtonText: 'acceptText',
                        innerBlock: BEMHTML.apply({ block: 'i-outboard-controls-impl' })
                    }
                },
                {
                    block: 'b-outboard-controls',
                    js: { id: 'control' },
                    content: {
                        elem: 'switcher',
                        js: { innerBlockParams: blockParams }
                    }
                }
            ]),
            outboard = function() {
                return $(tree[0]).bem('b-outboard-controls');
            },
            switcherNode = $(tree[1]).find('.b-outboard-controls__switcher-button'),
            outboardImpl = $(tree[0]).find('.i-outboard-controls-impl').bem('i-outboard-controls-impl'),
            popupNode = $(tree[0]).find('.popup'),
            getButton = function(type) {
                return popupNode.find('.b-outboard-controls__' + type + '-button');
            };

            $('body').append(tree);

            BEM.DOM.init(tree);
        });

        afterEach(function() {
            popupNode.bem('popup').destruct();
            BEM.DOM.destruct(tree);
        });

        it('must call prepareToShow on switcher click', function() {
            switcherNode.click();

            expect(outboardImpl.prepareToShow.called).to.be.equal(true);
        });

        it('must pass proper params when calling prepareToShow', function() {
            switcherNode.click();

            expect(outboardImpl.prepareToShow.calledWith(blockParams)).to.be.equal(true);
        });

        it('must call provideData on accept button click', function() {
            switcherNode.click();
            getButton('accept').bem('button').trigger('click'); //todo dom click?

            expect(outboardImpl.provideData.called).to.be.equal(true);
        });

        it('must call provideData on accept call', function() {
            outboard().accept();

            expect(outboardImpl.provideData.called).to.be.equal(true);
        });

        it('must call declineChange on decline button click', function() {
            switcherNode.click();
            $('.b-outboard-controls__decline-button').bem('button').trigger('click');

            expect(outboardImpl.declineChange.called).to.be.equal(true);
        });

        it('must call declineChange on decline call', function() {
            outboard().decline();

            expect(outboardImpl.declineChange.called).to.be.equal(true);
        });

        it('getInnerBlock must return inner block', function() {
            expect(outboard().getInnerBlock()).to.be.equal(outboardImpl);
        });

        it('getAcceptButtonText must return text on accept button', function() {
            expect(outboard().getAcceptButtonText()).to.be.equal('acceptText');
        });

        it('setAcceptButtonText must set text on accept button', function() {
            outboard().setAcceptButtonText('new text');
            expect(outboard().getAcceptButtonText()).to.be.equal('new text');
        });

        it('must disable it accept buttons when inner block triggers state with { canSave: false }', function() {
            switcherNode.click();
            outboardImpl.trigger('state', { canSave: false });

            expect(outboard().acceptButton.every(function(button) { return button.hasMod('disabled', 'yes') })).to.be.equal(true);
        });

        it('must enable it accept buttons when inner block triggers state with { canSave: true }', function() {
            switcherNode.click();
            outboardImpl.trigger('state', { canSave: true });

            expect(outboard().acceptButton.some(function(button) { return button.hasMod('disabled', 'yes') })).to.be.equal(false);
        });

        //todo несколько свитчеров/попапов
    });
});
