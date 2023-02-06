describe('Justifier with last image in last row with fixed width:', function() {
    var Justifier = blocks['justifier'],
        params,
        thumbs,
        expectedRows,
        result;

    // http://yandex.ru/search?&text=лотос
    describe('align 4 images', function() {
        beforeEach(function() {
            params = {
                maxRowWidth: 550,
                minImageWidth: 75,
                marginBetween: 1,
                rowsLimit: 1
            };
            thumbs = [
                { width: 174 },
                { width: 170 },
                { width: 195 },
                { width: 130 }
            ];
            expectedRows = [{
                fullWidth: 672,
                thumbs: [{
                    width: 174,
                    alignedWidth: 133
                }, {
                    width: 170,
                    alignedWidth: 129
                }, {
                    width: 195,
                    alignedWidth: 155
                }, {
                    width: 130,
                    alignedWidth: 130,
                    preAligned: true
                }]
            }];
            result = Justifier.getAlignedRows(thumbs, params);
        });

        it('First row width must be equal to maximum allowed width', function() {
            expect(countAlignedRowWidth(result[0], params)).to.equal(params.maxRowWidth);
        });

        it('Unexpected result structure', function() {
            expect(result).to.deep.equal(expectedRows);
        });
    });

    describe('align 4 narrow images', function() {
        beforeEach(function() {
            params = {
                maxRowWidth: 550,
                minImageWidth: 71,
                marginBetween: 1,
                rowsLimit: 1
            };
            thumbs = [
                { width: 92 },
                { width: 174 },
                { width: 88 },
                { width: 174 },
                { width: 130 }
            ];
            expectedRows = [{
                fullWidth: 662,
                thumbs: [{
                    alignedWidth: 71,
                    preAligned: true,
                    width: 92
                }, {
                    alignedWidth: 137,
                    width: 174
                }, {
                    alignedWidth: 71,
                    preAligned: true,
                    width: 88
                }, {
                    alignedWidth: 137,
                    width: 174
                }, {
                    preAligned: true,
                    alignedWidth: 130,
                    width: 130
                }]
            }];

            result = Justifier.getAlignedRows(thumbs, params);
        });

        it('First row width must be equal to maximum allowed width', function() {
            expect(countAlignedRowWidth(result[0], params)).to.equal(params.maxRowWidth);
        });

        it('Images should not have width less then minimal width set in config', function() {
            expect(checkAllThumbsNotLessMinWidth(result[0], params)).to.be.equal(true);
        });

        it('Unexpected result structure', function() {
            expect(result).to.deep.equal(expectedRows);
        });
    });

    // https://yandex.ru/yandsearch?text=hawaii
    describe('row width become smaller while 3rd thumb shrinked', function() {
        beforeEach(function() {
            params = {
                maxRowWidth: 550,
                minImageWidth: 75,
                marginBetween: 1,
                rowsLimit: 1
            };
            thumbs = [
                { width: 208 },
                { width: 174 },
                { width: 182 },
                { width: 163 }
            ];
            expectedRows = [{
                fullWidth: 697,
                thumbs: [{
                    width: 208,
                    alignedWidth: 159
                }, {
                    width: 174,
                    alignedWidth: 125
                }, {
                    width: 182,
                    alignedWidth: 133
                }, {
                    width: 130,
                    alignedWidth: 130,
                    preAligned: true
                }]
            }];
            result = Justifier.getAlignedRows(thumbs, params);
        });

        it('First row width must be equal to maximum allowed width', function() {
            expect(countAlignedRowWidth(result[0], params)).to.equal(params.maxRowWidth);
        });

        it('Unexpected result structure', function() {
            expect(result).to.deep.equal(expectedRows);
        });
    });

    /**
     * Тут тестируется вариант, когда последний тамб в ряду оказывается меньше 130 px
     * Тогда мы пропускаем его и берём следующий, который при ужимании будет >= 130px
     */
    describe('with shrinked 3rd thumb row become narrower + last thumb is smaller 130px', function() {
        beforeEach(function() {
            params = {
                maxRowWidth: 550,
                minImageWidth: 75,
                marginBetween: 1,
                rowsLimit: 1
            };
            thumbs = [
                { width: 208 },
                { width: 174 },
                { width: 182 },
                { width: 120 },
                { width: 140 }
            ];
            expectedRows = [{
                fullWidth: 697,
                thumbs: [{
                    width: 208,
                    alignedWidth: 159
                }, {
                    width: 174,
                    alignedWidth: 125
                }, {
                    width: 182,
                    alignedWidth: 133
                }, {
                    width: 130,
                    alignedWidth: 130,
                    preAligned: true
                }]
            }];
            result = Justifier.getAlignedRows(thumbs, params);
        });

        it('First row width must be equal to maximum allowed width', function() {
            expect(countAlignedRowWidth(result[0], params)).to.equal(params.maxRowWidth);
        });

        it('Unexpected result structure', function() {
            expect(result).to.deep.equal(expectedRows);
        });
    });

    // http://yandex.ru/search?text=династар
    describe('align 2 images to 560px two rows with last fixed width thumb and last thumb is 650px long', function() {
        beforeEach(function() {
            params = {
                maxRowWidth: 550,
                minImageWidth: 75,
                marginBetween: 1,
                rowsLimit: 2
            };
            thumbs = [
                { width: 109 },
                { width: 650 }
            ];
            expectedRows = [{
                fullWidth: 240,
                thumbs: [{
                    width: 109
                }, {
                    width: 130,
                    alignedWidth: 130,
                    preAligned: true
                }]
            }];
            result = Justifier.getAlignedRows(thumbs, params);
        });

        it('First row width must be less then maximum allowed width', function() {
            expect(countAlignedRowWidth(result[0], params)).to.be.at.most(params.maxRowWidth);
        });

        it('Unexpected result structure', function() {
            expect(result).to.deep.equal(expectedRows);
        });
    });

    // Из-за бага в некоторых случаях ряд становится короче необходимого.
    // Возникал, когда последний тамб настолько сильно урезали, что это уводило в минус растраченный остаток.
    describe('3 images, last thumb is 300px long', function() {
        beforeEach(function() {
            params = {
                maxRowWidth: 560,
                minImageWidth: 75,
                marginBetween: 1,
                rowsLimit: 1
            };
            thumbs = [
                { width: 208 },
                { width: 320 },
                { width: 302 }
            ];
            expectedRows = [{
                fullWidth: 660,
                thumbs: [{
                    width: 208,
                    alignedWidth: 158
                }, {
                    width: 320,
                    alignedWidth: 270
                }, {
                    width: 130,
                    alignedWidth: 130,
                    preAligned: true
                }]
            }];
            result = Justifier.getAlignedRows(thumbs, params);
        });

        it('First row width must be less then maximum allowed width', function() {
            expect(countAlignedRowWidth(result[0], params)).to.be.at.most(params.maxRowWidth);
        });

        it('Unexpected result structure', function() {
            expect(result).to.deep.equal(expectedRows);
        });
    });

    /**
     * Посчитать ширину ряда с ужатами тамбами
     * @param {Object} row
     * @param {Object} params
     * @returns {Number}
     */
    function countAlignedRowWidth(row, params) {
        var width = row.thumbs.reduce(function(prev, thumb) {
            var thumbWidth = thumb.alignedWidth || thumb.width;
            return prev + thumbWidth + params.marginBetween;
        }, 0);

        return width - params.marginBetween;
    }

    /**
     * Провека, что все ужатые тамбы в ряду не уже установленного минимума
     * если эти тамбы ужимались
     * @param {Object} row
     * @param {Object} params
     * @returns {Boolean}
     */
    function checkAllThumbsNotLessMinWidth(row, params) {
        var thumb,
            properlyAligned = true;

        for (var i = 0, l = row.thumbs.length; i < l; i++) {
            thumb = row.thumbs[i];

            if (thumb.alignedWidth && thumb.alignedWidth < params.minImageWidth) {
                properlyAligned = false;
                break;
            }
        }

        return properlyAligned;
    }
});
