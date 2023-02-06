const helperElements = require('helpers/elements');

const WIDTH_ITEM = 240;
const HEIGHT_ITEM = 240;

const len = 1000;
const widthViewport = 1200;
const elements = generateElements(widthViewport, len);

const testViewports = [{
    top: 0,
    bottom: 600,
    expect: [0, 14]
}, {
    top: 250,
    bottom: 600,
    expect: [5, 14]
}, {
    top: 490,
    bottom: 600,
    expect: [10, 14]
}, {
    top: 0,
    bottom: 740,
    expect: [0, 19]
}, {
    top: 250,
    bottom: 740,
    expect: [5, 19]
}];

/**
 * @param viewportWidth
 * @param len
 */
function generateElements(viewportWidth, len) {
    const itemsInRow = Math.floor(viewportWidth / WIDTH_ITEM);
    let row = -1;
    const items = new Array(len);
    let i = -1;

    while (++i < len) {
        if (i % itemsInRow === 0) {
            row++;
        }

        const top = HEIGHT_ITEM * row;
        items[i] = {
            top: top,
            bottom: top + HEIGHT_ITEM
        };
    }
    return items;
}

/**
 * @param rect
 */
function getRect(rect) {
    return rect;
}

/**
 * @param testViewport
 */
function runTest(testViewport) {
    const res = helperElements.findInRect({
        elements: elements,
        rect: testViewport,
        fnGetRectEl: getRect
    });

    it('in viewport ' + testViewport.top + 'x' + testViewport.bottom, () => {
        expect(res.from).to.be.eql(testViewport.expect[0]);
        expect(res.to).to.be.eql(testViewport.expect[1]);
    });
}

describe('helperElements', () => {
    testViewports.forEach(runTest);

    describe('#isElInRect', () => {
        beforeEach(function() {
            this.el = $('<div />');
            this.rectViewPort = {
                top: 100,
                bottom: 300,
                left: 100,
                right: 300
            };
        });

        afterEach(function() {
            delete this.el;
            delete this.rectViewPort;
        });

        /**
         *      ____
         *  _  |    |
         * |_| |    |
         *     |____|
         */
        it('элемент находится слева от вьюпорт', function() {
            const rectEl = {
                top: 100,
                bottom: 300,
                left: 50,
                right: 90
            };

            expect(helperElements.isElInRect({
                el: this.el,
                rect: this.rectViewPort,
                fnGetRectEl: function() {
                    return rectEl;
                }
            })).not.to.be.ok();
        });

        /**
         *      ____
         *  ___|_   |
         * |___|_|  |
         *     |____|
         */
        it('элемент зашёл слева во вьюпорт', function() {
            const rectEl = {
                top: 100,
                bottom: 300,
                left: 90,
                right: 120
            };

            expect(helperElements.isElInRect({
                el: this.el,
                rect: this.rectViewPort,
                fnGetRectEl: function() {
                    return rectEl;
                }
            })).to.be.ok();
        });

        /**
         *      _____
         *     |  _  |
         *     | |_| |
         *     |_____|
         */
        it('элемент находится полностью во вьюпорте', function() {
            const rectEl = {
                top: 100,
                bottom: 300,
                left: 110,
                right: 200
            };

            expect(helperElements.isElInRect({
                el: this.el,
                rect: this.rectViewPort,
                fnGetRectEl: function() {
                    return rectEl;
                }
            })).to.be.ok();
        });

        /**
         *      ____
         *     |   _|__
         *     |  |_|__|
         *     |____|
         */
        it('элемент зашёл справа во вьюпорт', function() {
            const rectEl = {
                top: 100,
                bottom: 300,
                left: 200,
                right: 320
            };

            expect(helperElements.isElInRect({
                el: this.el,
                rect: this.rectViewPort,
                fnGetRectEl: function() {
                    return rectEl;
                }
            })).to.be.ok();
        });

        /**
         *      ____
         *  ___|____|__
         * |___|____|__|
         *     |____|
         */
        it('элемент больше вью порта и находится в нём', function() {
            const rectEl = {
                top: 100,
                bottom: 300,
                left: 50,
                right: 350
            };

            expect(helperElements.isElInRect({
                el: this.el,
                rect: this.rectViewPort,
                fnGetRectEl: function() {
                    return rectEl;
                }
            })).to.be.ok();
        });

        /**
         *      ____
         *     |    |  ___
         *     |    | |___|
         *     |____|
         */
        it('элемент находится справа от вьюпорта', function() {
            const rectEl = {
                top: 100,
                bottom: 300,
                left: 310,
                right: 350
            };

            expect(helperElements.isElInRect({
                el: this.el,
                rect: this.rectViewPort,
                fnGetRectEl: function() {
                    return rectEl;
                }
            })).not.to.be.ok();
        });
    });
});
