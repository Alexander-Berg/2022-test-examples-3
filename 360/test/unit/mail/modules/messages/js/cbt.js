// В хоткеях переписали cbt и эти тесты надо пересмотреть
xdescribe('Daria.CBT', function() {
    var index = 0;
    function createCheckboxItem(isGroup, groupCount) {
        var groupClass = isGroup ? ' js-cbt-group' : '';
        var $item = $('<div />', {
            class: 'js-cbt-item' + groupClass
        });

        $item.append('<input id="checkbox-' + index + '" class="js-cbt-item-checkbox' + (isGroup ? ' group' : '') + '" type="checkbox" name="test" value="0" />');
        index++;
        if (isGroup) {
            if (!groupCount) {
                groupCount = 0;
            }
            var i;
            for (i = 0; i < groupCount; i++) {
                $item.append(createCheckboxItem());
            }
        }
        return $item;
    }

    beforeEach(function() {
        index = 0;
        this.$tree = $('<div class="tree" />');

        this.$tree.append(createCheckboxItem(true, 5));
        this.$tree.append(createCheckboxItem());
        this.$tree.append(createCheckboxItem());
        this.$tree.append(createCheckboxItem(true, 5));
        this.$tree.append(createCheckboxItem());

        /*
         Получившееся дерево

         <div class="tree">
             <div class="js-cbt-item js-cbt-group">
                <input id="checkbox-0" class="js-cbt-item-checkbox group" type="checkbox" name="test" value="0">

                 <div class="js-cbt-item"><input id="checkbox-1" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
                 <div class="js-cbt-item"><input id="checkbox-2" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
                 <div class="js-cbt-item"><input id="checkbox-3" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
                 <div class="js-cbt-item"><input id="checkbox-4" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
                 <div class="js-cbt-item"><input id="checkbox-5" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
             </div>

             <div class="js-cbt-item"><input id="checkbox-6" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>

             <div class="js-cbt-item"><input id="checkbox-7" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>

             <div class="js-cbt-item js-cbt-group">
                <input id="checkbox-8" class="js-cbt-item-checkbox group" type="checkbox" name="test" value="0">

                 <div class="js-cbt-item"><input id="checkbox-9" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
                 <div class="js-cbt-item"><input id="checkbox-10" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
                 <div class="js-cbt-item"><input id="checkbox-11" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
                 <div class="js-cbt-item"><input id="checkbox-12" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
                 <div class="js-cbt-item"><input id="checkbox-13" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
             </div>

             <div class="js-cbt-item"><input id="checkbox-14" class="js-cbt-item-checkbox" type="checkbox" name="test" value="0"></div>
         </div>
         */

        this.$checkboxes = this.$tree.find('input');

        this.cbt = new Daria.CBT(this.$tree);
    });

    afterEach(function() {
        this.$checkboxes.off('daria-cbt-change');
    });

    describe('Должен корректно вызвать события daria-cbt-change на чекбоксах ->', function() {
        describe('Выделение идет сверху вниз ->', function() {

            it('Начало выделения - элемент входящий в группу, конец выделения - элемент невходящий в группу', function() {
                var $startItem = this.$checkboxes.eq(1).parent();
                var $endItem = this.$checkboxes.eq(7).parent();
                var counter = 0;

                this.$checkboxes.on('daria-cbt-change', function(event, isChecked) {
                    if (isChecked) {
                        counter++;
                    }
                });

                this.cbt.startSelection($startItem);
                this.cbt.selectByRange($endItem);

                expect(counter).to.be.equal(7);
            });

            it('Начало выделения - элемент невходящий в группу, конец выделения - элемент входящий в группу', function() {
                var $startItem = this.$checkboxes.eq(7).parent();
                var $endItem = this.$checkboxes.eq(10).parent();
                var counter = 0;

                this.$checkboxes.on('daria-cbt-change', function(event, isChecked) {
                    if (isChecked) {
                        counter++;
                    }
                });

                this.cbt.startSelection($startItem);
                this.cbt.selectByRange($endItem);

                expect(counter).to.be.equal(4);
            });

            it('Начало выделения - элемент невходящий в группу, конец выделения - элемент невходящий в группу с прохождением через группу', function() {
                var $startItem = this.$checkboxes.eq(7).parent();
                var $endItem = this.$checkboxes.eq(14).parent();
                var counter = 0;

                this.$checkboxes.on('daria-cbt-change', function(event, isChecked) {
                    if (isChecked) {
                        counter++;
                    }
                });

                this.cbt.startSelection($startItem);
                this.cbt.selectByRange($endItem);

                expect(counter).to.be.equal(8);
            });

            it('Начало выделения - элемент невходящий в группу, конец выделения - элемент невходящий в группу без прохождением через группу', function() {
                var $startItem = this.$checkboxes.eq(6).parent();
                var $endItem = this.$checkboxes.eq(7).parent();
                var counter = 0;

                this.$checkboxes.on('daria-cbt-change', function(event, isChecked) {
                    if (isChecked) {
                        counter++;
                    }
                });

                this.cbt.startSelection($startItem);
                this.cbt.selectByRange($endItem);

                expect(counter).to.be.equal(2);
            });

        });

        describe('Выделение идет снизу вверх ->', function() {

            it('Начало выделения - элемент входящий в группу, конец выделения - элемент невходящий в группу', function() {
                var $startItem = this.$checkboxes.eq(7).parent();
                var $endItem = this.$checkboxes.eq(1).parent();
                var counter = 0;

                this.$checkboxes.on('daria-cbt-change', function(event, isChecked) {
                    if (isChecked) {
                        counter++;
                    }
                });

                this.cbt.startSelection($startItem);
                this.cbt.selectByRange($endItem);

                expect(counter).to.be.equal(7);
            });

            it('Начало выделения - элемент невходящий в группу, конец выделения - элемент входящий в группу', function() {
                var $startItem = this.$checkboxes.eq(10).parent();
                var $endItem = this.$checkboxes.eq(7).parent();
                var counter = 0;

                this.$checkboxes.on('daria-cbt-change', function(event, isChecked) {
                    if (isChecked) {
                        counter++;
                    }
                });

                this.cbt.startSelection($startItem);
                this.cbt.selectByRange($endItem);

                expect(counter).to.be.equal(4);
            });

            it('Начало выделения - элемент невходящий в группу, конец выделения - элемент невходящий в группу с прохождением через группу', function() {
                var $startItem = this.$checkboxes.eq(14).parent();
                var $endItem = this.$checkboxes.eq(7).parent();
                var counter = 0;

                this.$checkboxes.on('daria-cbt-change', function(event, isChecked) {
                    if (isChecked) {
                        counter++;
                    }
                });

                this.cbt.startSelection($startItem);
                this.cbt.selectByRange($endItem);

                expect(counter).to.be.equal(8);
            });

            it('Начало выделения - элемент невходящий в группу, конец выделения - элемент невходящий в группу без прохождением через группу', function() {
                var $startItem = this.$checkboxes.eq(7).parent();
                var $endItem = this.$checkboxes.eq(6).parent();
                var counter = 0;

                this.$checkboxes.on('daria-cbt-change', function(event, isChecked) {
                    if (isChecked) {
                        counter++;
                    }
                });

                this.cbt.startSelection($startItem);
                this.cbt.selectByRange($endItem);

                expect(counter).to.be.equal(2);
            });

        });
        
        it('Выделение меняет направление', function() {
            var $startItem = this.$checkboxes.eq(7).parent();
            var $endItem = this.$checkboxes.eq(14).parent();
            var counter = 0;

            this.$checkboxes.on('daria-cbt-change', function(event, isChecked) {
                if (isChecked) {
                    counter++;
                } else {
                    counter--;
                }
                $(event.target).prop('checked', isChecked);
            });

            this.cbt.startSelection($startItem);
            this.cbt.selectByRange($endItem);

            $endItem = this.$checkboxes.eq(1).parent();
            this.cbt.selectByRange($endItem);

            expect(counter).to.be.equal(7);
        });
    });

});
