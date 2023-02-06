describe('b-import-xls', function() {
    var action = 'yandex.ru',
        campaigns = [
            { "total_units":"16000","name":"Холодильники - 2","sum_units":"16000","total":53.78,"currency":"YND_FIXED","mediaType":"text","sum_spent":"15946.300000","sum_spent_units":"0","cid":261,"sum":"16000.084552" },
            { "total_units":"0","name":"Бытовая техника - 2","sum_units":"68963","total":0,"currency":"YND_FIXED","mediaType":"text","sum_spent":"68962.945155","sum_spent_units":"68963","cid":263,"sum":"68962.945155" },
            { "total_units":"29","name":"Эдил","sum_units":"295","total":29.61,"currency":"YND_FIXED","mediaType":"text","sum_spent":"265.780000","sum_spent_units":"266","cid":1578,"sum":"295.395000" },
            { "total_units":"49","name":"Посудомоечные машины","sum_units":"29926","total":48.85,"currency":"YND_FIXED","mediaType":"text","sum_spent":"29876.831931","sum_spent_units":"29877","cid":1580,"sum":"29925.681574" },
            { "total_units":"0","name":"Шашлык","sum_units":"0","total":0.06,"currency":"YND_FIXED","mediaType":"text","sum_spent":"49.940000","sum_spent_units":"0","cid":2824,"sum":"50.000000" },
            { "total_units":"61","name":"Техника для кухни","sum_units":"10539","total":61.07,"currency":"YND_FIXED","mediaType":"text","sum_spent":"10477.722941","sum_spent_units":"10478","cid":6728,"sum":"10538.788894" },
            { "total_units":"60","name":"Техника для дома","sum_units":"2902","total":59.77,"currency":"YND_FIXED","mediaType":"text","sum_spent":"2842.290000","sum_spent_units":"2842","cid":6729,"sum":"2902.055398" },
            { "total_units":"47","name":"Встраиваемая бытовая техника","sum_units":"2802","total":47.51,"currency":"YND_FIXED","mediaType":"text","sum_spent":"2754.970000","sum_spent_units":"2755","cid":6733,"sum":"2802.479286" },
            { "total_units":"58","name":"Керхер","sum_units":"1516","total":58.23,"currency":"YND_FIXED","mediaType":"text","sum_spent":"1457.530000","sum_spent_units":"1458","cid":6748,"sum":"1515.763848" },
            { "total_units":"108","name":"ОПТОВАЯ","sum_units":"3369","total":107.77,"currency":"YND_FIXED","mediaType":"text","sum_spent":"3261.020000","sum_spent_units":"3261","cid":52045,"sum":"3368.786695" },
            { "total_units":"162","name":"Холодильники - Директ справа","sum_units":"74832","total":161.75,"currency":"YND_FIXED","mediaType":"text","sum_spent":"74670.032439","sum_spent_units":"74670","cid":106418,"sum":"74831.782439" },
            { "total_units":"7085","name":"Беззаботные Холодильники","sum_units":"1365270","total":7084.78,"currency":"YND_FIXED","mediaType":"text","sum_spent":"1358184.743175","sum_spent_units":"1358185","cid":118252,"sum":"1365269.524827" },
            { "total_units":"301","name":"Стиральные машины -2","sum_units":"867105","total":300.81,"currency":"YND_FIXED","mediaType":"text","sum_spent":"866803.794817","sum_spent_units":"866804","cid":454301,"sum":"867104.604817" },
            { "total_units":"237","name":"Плиты-2","sum_units":"354332","total":236.86,"currency":"YND_FIXED","mediaType":"text","sum_spent":"354095.349490","sum_spent_units":"354095","cid":570694,"sum":"354332.209490" },
            { "total_units":"161","name":"Винный Бутик_Либхерршоп_Бошшоп","sum_units":"52803","total":160.45,"currency":"YND_FIXED","mediaType":"text","sum_spent":"52642.182745","sum_spent_units":"52642","cid":802840,"sum":"52802.632039" }
        ],
        agencies = [
            { login: 'login1@ya.ru', agency_name: 'agency 1 name' },
            { login: 'login2@ya.ru', agency_name: 'agency 2 name' },
            { login: 'login3@ya.ru', agency_name: 'agency 3 name' }
        ],
        formProperties = {
            cmd: 'importCampXLS',
            ulogin: 'holodilnikru',
            retpath: action,
            svars_name: ''
        },
        uploadProperties = {
            cmd: 'preImportCampXLS',
            ulogin: 'holodilnikru',
            import_format: 'xls',
            json: 1
        },
        uploads = {
            exampleCampaign: {
                warnings: [],
                has_empty_geo: 0,
                errors: [],
                has_oversized_banners: 1,
                camp_comments: ['Кампания № 123456 не существует'],
                parse_warnings_for_exists_camp: {
                    lost_phrases: {
                        title: 'lost phrases title',
                        items: ['lp-desc1', 'lp-desc2', 'lp-desc3', 'lp-desc4', 'lp-desc5', 'lp-desc6', 'lp-desc7', 'lp-desc8', 'lp-desc9', 'lp-desc10', 'lp-desc11', 'lp-desc12', 'lp-desc13']
                    },
                    lost_banners: 'lost banners title:lb-desc1,lb-desc2,lb-desc3,lb-desc4,lb-desc5,lb-desc6,lb-desc7,lb-desc8,lb-desc9,lb-desc10,lb-desc11,lb-desc12,lb-desc13',
                    lost_groups: 'lost groups title:lg-desc1,lg-desc2,lg-desc3,lg-desc4,lg-desc5,lg-desc6,lg-desc7,lg-desc8,lg-desc9,lg-desc10,lg-desc11,lg-desc12,lg-desc13',
                    changes_minus_words: 'warning про минус-слова',
                    without_any_bids: 'В файле отсутствуют ID объявлений. Импорт такого файла может привести к дублированию существующих объявлений и фраз. Вы уверены, что хотите продолжить импорт данного файла?',
                    banners_without_bids: 'warning про отсутствие ид баннеров'
                },
                svars_name: '8758279655364782403',
                from_cid: '123456',
                geo_errors: [],
                mediaType: 'text'
            },
            existingCampaign: {
                warnings: [],
                has_empty_geo: 0,
                errors: [],
                has_oversized_banners: 0,
                camp_name: 'Винный Бутик_Либхерршоп_Бошшоп',
                camp_comments: [],
                parse_warnings_for_exists_camp: {},
                svars_name: '12151608930517873639',
                from_cid: '802840',
                cid: '802840',
                mediaType: 'text',
                geo_errors: []
            },
            errorCampaign: {
                parse_warnings_for_exists_camp: null,
                warnings: null,
                errors: ['Неправильный формат файла. Допускается использование файлов только формата .xls.'],
                from_cid: null,
                geo_errors: [],
                mediaType: 'text'
            }
        };

    BEM.DOM.decl({ block: 'b-import-xls', elem: 'region-box' }, {
        toggleVisibility: sinon.spy()
    });

    describe('Проверка формы (можно в медиаплан и создавать новые):', function() {
        var block,
            filePreloader,
            regionCheckbox,
            regionBox,
            actions,
            submitButton,
            radioGroup,
            changeCampaignRow,
            changeCampaignHidden;

        beforeEach(function() {
            block = u.getInitedBlock(getJSON({
                action: action,
                agencies: [],
                campaigns: campaigns,
                allow: {
                    createBySubclient: false,
                    createCampaign: true,
                    loadToMediaPlan: true
                },
                service: {
                    bySelf: false,
                    byManager: false
                },
                notAgencyControl: true,
                isAnyClient: false,
                hasMediaControl: false,
                hasSupportControl: false,
                formProperties: formProperties,
                uploadProperties: uploadProperties,
                campLimitError: ''
            }));

            filePreloader = block.findBlockInside('b-file-preloader');
            regionCheckbox = block.elemInstance('region-checkbox').findBlockOn('checkbox');
            regionBox = block.elemInstance('region-box');
            actions = block.elemInstance('actions');
            submitButton = block.findBlockInside({ block: 'button', modName: 'type', modVal: 'submit' });
            radioGroup = block.findBlockInside('radiobox');
            changeCampaignRow = block.elemInstance('action', 'change', 'campaign');
            changeCampaignHidden = block.elem('hidden', 'related', 'old');
        });

        afterEach(function() {
            block.destruct();
        });

        describe('в исходном состоянии', function() {
            it('прелоадер файла _js_inited', function() {
                expect(filePreloader.hasMod('js', 'inited')).to.be.equal(true);
            });

            it('чекбокс выбора региона _disabled_yes', function() {
                expect(regionCheckbox.hasMod('disabled', 'yes')).to.be.equal(true);
            });

            it('блок выбора региона _hidden_yes', function() {
                expect(regionBox.hasMod('hidden', 'yes')).to.be.equal(true);
            });

            it('блок действий _disabled_yes', function() {
                expect(actions.hasMod('disabled', 'yes')).to.be.equal(true);
            });

            it('кнопка button_type_submit _disabled_yes', function() {
                expect(submitButton.hasMod('disabled', 'yes')).to.be.equal(true);
            });
        });

        describe('после загрузки файла для несуществующей кампании', function() {
            var warningsBlock;

            beforeEach(function() {
                warningsBlock = block.elemInstance('warnings');
                filePreloader.trigger('uploaded', uploads.exampleCampaign);
            });

            it('чекбокс выбора региона НЕ _disabled_yes', function() {
                expect(regionCheckbox.hasMod('disabled', 'yes')).to.be.equal(false);
            });

            it('блок с радио на изменение кампании _hidden_yes', function() {
                expect(changeCampaignRow.hasMod('hidden', 'yes')).to.be.equal(true);
            });

            it('hidden поле для изменения кампании disabled', function() {
                expect(changeCampaignHidden.get(0).disabled).to.be.equal(true);
            });

            it('блок действий НЕ _disabled_yes', function() {
                expect(actions.hasMod('disabled', 'yes')).to.be.equal(false);
            });

            it('кнопка button_type_submit НЕ _disabled_yes', function() {
                expect(submitButton.hasMod('disabled', 'yes')).to.be.equal(false);
            });

            it('hidden поле svars_name меняет свое значение на значение svars_name загруженного файла', function() {
                expect(block.getFormProperty('svars_name')).to.be.equal(uploads.exampleCampaign.svars_name);
            });

            it('radioGroup действий НЕ _disabled_yes', function() {
                expect(radioGroup.hasMod('disabled', 'yes')).to.be.equal(false);
            });

            it('radioGroup.val() === "new"', function() {
                expect(radioGroup.val()).to.be.equal('new');

            });

            it('hidden поле cmd имеет значение importCampXLS', function() {
                expect(block.getFormProperty('cmd')).to.be.equal('importCampXLS');
            });

            ['media', 'new', 'other'].forEach(function(value) {
                describe('при переключении радиокнопок в значение ' + value, function() {
                    var cmd = { media: 'importCampToMediaplanXLS', _default: 'importCampXLS' },
                        relatedCheckboxes,
                        relatedSelects;

                    beforeEach(function() {
                        relatedCheckboxes = block.findBlocksInside({
                            block: 'checkbox',
                            modName: 'related',
                            modVal: 'destination'
                        });
                        relatedSelects = block.findBlocksInside('select');

                        radioGroup.val(value);
                    });

                    it('чекбоксы destination блокируются только при значении радио media', function() {
                        relatedCheckboxes.forEach(function(checkbox) {
                            var hasMod = checkbox.hasMod('disabled', 'yes');

                            value === 'media' ?
                                expect(hasMod).to.be.equal(true) :
                                expect(hasMod).to.be.equal(false)
                        });
                    });

                    it('из зависимых селектов включенным остается только тот, чей радио выбран', function() {
                        relatedSelects.forEach(function(select) {
                            var hasMod = select.hasMod('disabled', 'yes');

                            select.hasMod('related', value) ?
                                expect(hasMod).to.be.equal(false) :
                                expect(hasMod).to.be.equal(true)
                        });
                    });

                    it('блок предупреждений (если они есть) показан, если есть предупреждения', function() {
                        var hasMod = warningsBlock.hasMod('hidden', 'yes');

                        warningsBlock.hasWarnings() ?
                            expect(hasMod).to.be.equal(false) :
                            expect(hasMod).to.be.equal(true)
                    });

                    it('кнопка button_type_submit' + (value !== 'new' ? ' ' : ' НЕ ') + '_disabled_yes', function() {
                        var hasMod = submitButton.hasMod('disabled', 'yes');

                        value !== 'new' && warningsBlock.hasWarnings() ?
                            expect(hasMod).to.be.equal(true) :
                            expect(hasMod).to.be.equal(false);
                    });

                    it('hidden поле cmd меняет свое значение на ' + (cmd[value] || cmd._default), function() {
                        expect(block.getFormProperty('cmd')).to.be.equal(cmd[radioGroup.val()] || cmd._default);
                    });
                });
            });

            describe('при наличии предупреждений', function() {
                var warnings;

                beforeEach(function() {
                    warnings = []
                });

                it('блоки предупреждений НЕ _hidden_yes', function() {
                    radioGroup.val('other');

                    Object.keys(uploads.exampleCampaign.parse_warnings_for_exists_camp).forEach(function(key) {
                        var warning = warningsBlock.getWarning(key.replace(/_/g, '-'));
                        expect(warning.hasMod('hidden', 'yes')).to.be.equal(false);
                    });
                });

                it('предупреждение __has-oversized-banners НЕ _hidden_yes', function() {
                    var warning = warningsBlock.getWarning('has-oversized-banners');

                    expect(warning.hasMod('hidden', 'yes')).to.be.equal(false);
                });

                it('кнопка button_type_submit _disabled_yes пока все предупреждения НЕ разрезолвены', function() {
                    Object.keys(uploads.exampleCampaign.parse_warnings_for_exists_camp).forEach(function(key) {
                        warnings.push(warningsBlock.getWarning(key.replace(/_/g, '-')));
                    });

                    var hasMod = submitButton.hasMod('disabled', 'yes');

                    !warnings.every(function(warning) { return warning.isResolved() }) ?
                        expect(hasMod).to.be.equal(true) :
                        expect(hasMod).to.be.equal(false);
                });

                it('кнопка button_type_submit НЕ _disabled_yes, если все предупреждения разрезолвены', function() {
                    var isDisabled;

                    Object.keys(uploads.exampleCampaign.parse_warnings_for_exists_camp).forEach(function(key) {
                        warnings.push(warningsBlock.getWarning(key.replace(/_/g, '-')));
                    });

                    warnings.forEach(function(warning) {
                        var agreebox = warning.findBlockInside('checkbox');
                        agreebox && agreebox.setMod('checked', 'yes');
                    });

                    isDisabled = submitButton.hasMod('disabled', 'yes');

                    warnings.every(function(warning) { return warning.isResolved() }) ?
                        expect(isDisabled).to.be.equal(false) :
                        expect(isDisabled).to.be.equal(true);
                });
            });
        });

        describe('после загрузки файла для уже существующей кампании', function() {
            beforeEach(function() {
                filePreloader.trigger('uploaded', uploads.existingCampaign);
            });

            it('radioGroup.val() === "old"', function() {
                expect(radioGroup.val()).to.be.equal('old');
            });

            it('блок с радио на изменение кампании НЕ _hidden_yes', function() {
                expect(changeCampaignRow.hasMod('hidden', 'yes')).to.be.equal(false);
            });

            it('hidden поле для изменения кампании НЕ disabled и его значение есть ' + uploads.existingCampaign.cid, function() {
                expect(changeCampaignHidden.get(0).disabled).to.be.equal(false);
                expect(changeCampaignHidden.val()).to.be.equal(uploads.existingCampaign.cid);
            });
        });

        describe('при _checked_yes на чекбокс выбора региона', function() {
            it('с блока выбора региона снимается _hidden_yes', function(done) {
                regionCheckbox
                    .setMod('checked', 'yes')
                    .afterCurrentEvent(function() {
                        expect(regionBox.hasMod('hidden', 'yes')).to.be.equal(false);

                        done();
                    });
            });

            it('у выбора региона вызывается toggleVisibility(false)', function() {
                expect(regionBox.toggleVisibility.calledWith(false)).to.be.equal(true);
            });
        });

        [null, uploads.errorCampaign].forEach(function(upload) {
            describe(upload ? 'после загрузки файла с ошибкой' : 'при сбросе загруженного файла', function() {
                it('чекбокс выбора региона _disabled_yes', function(done) {
                    upload ?
                        filePreloader.trigger('uploaded', upload) :
                        filePreloader.trigger('reset');

                    expect(regionCheckbox.hasMod('disabled', 'yes')).to.be.equal(true);

                    regionCheckbox.afterCurrentEvent(done);
                });

                it('чекбокс выбора региона НЕ _checked_yes', function() {
                    expect(regionCheckbox.hasMod('checked', 'yes')).to.be.equal(false);
                });

                it('блок выбора региона _hidden_yes', function() {
                    expect(regionBox.hasMod('hidden', 'yes')).to.be.equal(true);
                });

                it('у выбора региона вызывается toggleVisibility(true)', function() {
                    expect(regionBox.toggleVisibility.calledWith(true)).to.be.equal(true);
                });

                it('блок действий _disabled_yes', function() {
                    expect(actions.hasMod('disabled', 'yes')).to.be.equal(true);
                });

                it('кнопка button_type_submit _disabled_yes', function() {
                    expect(submitButton.hasMod('disabled', 'yes')).to.be.equal(true);
                });

                it('hidden поле svars_name меняет свое значение пустое', function() {
                    expect(block.getFormProperty('svars_name')).to.be.equal('');
                });
            });
        });
    });

    describe('Проверка формы без кампаний:', function() {
        var block,
            radioGroup;

        describe('можно создавать', function() {
            beforeEach(function() {
                block = u.getInitedBlock(getJSON({
                    action: action,
                    agencies: [],
                    campaigns: [],
                    allow: {
                        createBySubclient: false,
                        createCampaign: true,
                        loadToMediaPlan: false
                    },
                    service: {
                        bySelf: false,
                        byManager: false
                    },
                    notAgencyControl: true,
                    isAnyClient: false,
                    hasMediaControl: false,
                    hasSupportControl: false,
                    formProperties: formProperties,
                    uploadProperties: uploadProperties,
                    campLimitError: ''
                }));

                radioGroup = block.findBlockInside('radiobox');
            });

            afterEach(function() {
                block.destruct();
            });

            it('у радиогруппы только 1 вариант и он new', function() {
                expect(radioGroup.elemInstances('radio').length).to.be.equal(1);
                expect(radioGroup.val()).to.be.equal('new');
            });
        });

        describe('нельзя создавать', function() {
            var block;

            beforeEach(function() {
                block = u.getInitedBlock(getJSON({
                    action: action,
                    agencies: [],
                    campaigns: [],
                    allow: {
                        createBySubclient: false,
                        createCampaign: false,
                        loadToMediaPlan: false
                    },
                    service: {
                        bySelf: false,
                        byManager: false
                    },
                    notAgencyControl: true,
                    isAnyClient: false,
                    hasMediaControl: false,
                    hasSupportControl: false,
                    formProperties: formProperties,
                    uploadProperties: uploadProperties,
                    campLimitError: ''
                }));
            });

            afterEach(function() {
                block.destruct();
            });

            it('форма отсутствует', function() {
                expect(block.findBlockInside('b-layout-form')).to.be.null;
            });

            it('текст сообщения вместо формы присутствует', function() {
                expect(block.domElem.text()).not.to.be.empty;
            });
        });
    });

    describe('Проверка формы с кампаниями, но нельзя создавать и загружать в медиаплан', function() {
        var block,
            radioGroup;

        beforeEach(function() {
            block = u.getInitedBlock(getJSON({
                action: action,
                agencies: [],
                campaigns: campaigns,
                allow: {
                    createBySubclient: false,
                    createCampaign: false,
                    loadToMediaPlan: false
                },
                service: {
                    bySelf: false,
                    byManager: false
                },
                notAgencyControl: true,
                isAnyClient: false,
                hasMediaControl: false,
                hasSupportControl: false,
                formProperties: formProperties,
                uploadProperties: uploadProperties,
                campLimitError: ''
            }));
            radioGroup = block.findBlockInside('radiobox');
        });

        afterEach(function() {
            block.destruct();
        });

        it('у радиогруппы только 2 варианта', function() {
            expect(radioGroup.elemInstances('radio').length).to.be.equal(2);
        });

        it('у радиогруппы 2 варианта: old и media', function() {
            expect(radioGroup.elemInstances('control').map(function(radio) { return radio.domElem.val() })
                .sort()).to.be.eql(['old', 'other']);
        });
    });

    describe('Проверка формы с обслуживанием, агентствами и без при not(agency_control)', function() {
        describe('с 1 агентством', function() {
            var block;

            beforeEach(function() {
                block = u.getInitedBlock(getJSON({
                    action: action,
                    agencies: agencies.slice(0, 1),
                    campaigns: [],
                    allow: {
                        createBySubclient: false,
                        createCampaign: true,
                        loadToMediaPlan: false
                    },
                    service: {
                        bySelf: false,
                        byManager: false
                    },
                    notAgencyControl: true,
                    isAnyClient: true,
                    hasMediaControl: false,
                    hasSupportControl: false,
                    formProperties: formProperties,
                    uploadProperties: uploadProperties,
                    campLimitError: ''
                }));
            });

            afterEach(function() {
                block.destruct();
            });

            it('при is_any_client & not(allow_create_scamp_by_subclient) должен быть __element hidden', function() {
                expect(block.elem('hidden', 'related', 'new').val()).to.be.equal(agencies[0].login);
            });
        });

        describe('с агентствами при allow_create_scamp_by_subclient', function() {
            var block,
                actions,
                select;

            beforeEach(function() {
                block = u.getInitedBlock(getJSON({
                    action: action,
                    agencies: agencies,
                    campaigns: [],
                    allow: {
                        createBySubclient: true,
                        createCampaign: true,
                        loadToMediaPlan: false
                    },
                    service: {
                        bySelf: false,
                        byManager: false
                    },
                    notAgencyControl: true,
                    isAnyClient: true,
                    hasMediaControl: false,
                    hasSupportControl: false,
                    formProperties: formProperties,
                    uploadProperties: uploadProperties,
                    campLimitError: ''
                }));

                actions = block.elemInstance('actions');
                select = actions.findBlockInside({ block: 'select', modName: 'related', modVal: 'new' });
            });

            afterEach(function() {
                block.destruct();
            });

            it('не должно быть __element hidden', function() {
                expect(block.elem('hidden', 'related', 'new').length).to.be.equal(0);
            });

            it('блок селекта в секции действия должен быть _disabled_yes', function() {
                expect(select.hasMod('disabled', 'yes')).to.be.equal(true);
            });

            it('после загрузки файла блок селекта в секции действия НЕ должен быть _disabled_yes', function() {
                block.findBlockInside('b-file-preloader')
                    .trigger('uploaded', uploads.exampleCampaign);

                expect(select.hasMod('disabled', 'yes')).to.be.equal(false);
            });

            describe('без выбора типа обслуживания', function() {
                var block,
                    actions,
                    select;

                beforeEach(function() {
                    block = u.getInitedBlock(getJSON({
                        action: action,
                        agencies: agencies,
                        campaigns: [],
                        allow: {
                            createBySubclient: true,
                            createCampaign: true,
                            loadToMediaPlan: false
                        },
                        service: {
                            bySelf: false,
                            byManager: false
                        },
                        notAgencyControl: true,
                        isAnyClient: false,
                        hasMediaControl: false,
                        hasSupportControl: false,
                        formProperties: formProperties,
                        uploadProperties: uploadProperties,
                        campLimitError: ''
                    }));

                    actions = block.elemInstance('actions');
                    select = actions.findBlockInside({ block: 'select', modName: 'related', modVal: 'new' });
                });

                afterEach(function() {
                    block.destruct();
                });

                it('блок селекта в секции действия должен иметь ' + agencies.length + ' option', function() {
                    block.findBlockInside('b-file-preloader')
                        .trigger('uploaded', uploads.exampleCampaign);

                    expect(select.elem('control').get(0).options.length).to.be.equal(agencies.length);
                });
            });

            describe('с выбором типа обслуживания', function() {
                describe('при is_any_client & super_control & support_control (самостоятельное)', function() {
                    var block,
                        actions,
                        select;

                    beforeEach(function() {
                        block = u.getInitedBlock(getJSON({
                            action: action,
                            agencies: agencies,
                            campaigns: [],
                            allow: {
                                createBySubclient: true,
                                createCampaign: true,
                                loadToMediaPlan: false
                            },
                            service: {
                                bySelf: true,
                                byManager: false
                            },
                            notAgencyControl: true,
                            isAnyClient: true,
                            hasMediaControl: false,
                            hasSupportControl: false,
                            formProperties: formProperties,
                            uploadProperties: uploadProperties,
                            campLimitError: ''
                        }));

                        actions = block.elemInstance('actions');
                        select = actions.findBlockInside({ block: 'select', modName: 'related', modVal: 'new' });
                    });

                    afterEach(function() {
                        block.destruct();
                    });

                    it('блок селекта в секции действия должен иметь ' + (agencies.length + 1) + ' option', function() {
                        block.findBlockInside('b-file-preloader')
                            .trigger('uploaded', uploads.oneCampaign);

                        expect(select.elem('control').get(0).options.length).to.be.equal(agencies.length + 1);
                    });
                });

                describe('при manager_control (менеджером)', function() {
                    var block,
                        actions,
                        select;

                    beforeEach(function() {
                        block = u.getInitedBlock(getJSON({
                            action: action,
                            agencies: agencies,
                            campaigns: [],
                            allow: {
                                createBySubclient: true,
                                createCampaign: true,
                                loadToMediaPlan: false
                            },
                            service: {
                                bySelf: false,
                                byManager: true
                            },
                            notAgencyControl: true,
                            isAnyClient: true,
                            hasMediaControl: false,
                            hasSupportControl: false,
                            formProperties: formProperties,
                            uploadProperties: uploadProperties,
                            campLimitError: ''
                        }));

                        actions = block.elemInstance('actions');
                        select = actions.findBlockInside({ block: 'select', modName: 'related', modVal: 'new' });
                    });

                    afterEach(function() {
                        block.destruct();
                    });

                    it('блок селекта в секции действия должен иметь ' + (agencies.length + 1) + ' option', function() {
                        block.findBlockInside('b-file-preloader')
                            .trigger('uploaded', uploads.oneCampaign);

                        expect(select.elem('control').get(0).options.length).to.be.equal(agencies.length + 1);
                    });
                });
            });
        });
    });

    function getJSON(ctx) {
        return {
            block: 'b-import-xls',
            js: true,
            content: !ctx.allow.createCampaign && !ctx.campaigns.length ?
                'Невозможно загрузить XLS/XLSX-файл. У клиента должна быть хотя бы одна кампания.' :
                {
                    block: 'b-layout-form',
                    mods: { layout: '27-71' },
                    mixes: {
                        label: [{ block: 'b-import-xls', elem: 'form-label' }],
                        control: [{ block: 'b-import-xls', elem: 'form-control' }],
                        submit: [{ block: 'b-import-xls', elem: 'form-submit' }]
                    },
                    submit: {
                        block: 'button',
                        mods: { size: 's', disabled: 'yes', type: 'submit' },
                        type: 'submit',
                        content: 'Продолжить'
                    },
                    method: 'GET',
                    action: ctx.action,
                    attrs: { name: 'accept' },
                    hiddenInputs: ['cmd', 'ulogin', 'retpath', 'svars_name'].map(function(prop) {
                        return {
                            name: prop,
                            value: ctx.formProperties[prop] || '',
                            mix: [{
                                block: 'b-import-xls',
                                elem: 'hidden-' + prop.replace(/_/g, '-')
                            }]
                        };
                    }, this),
                    rows: [
                        {
                            label: 'Файл',
                            control: {
                                block: 'b-file-preloader',
                                name: 'xls',
                                url: ctx.action,
                                data: ctx.uploadProperties,
                                timeout: 5000
                            }
                        },
                        {
                            label: 'Регион',
                            control: {
                                block: 'checkbox',
                                mods: { size: 's', disabled: 'yes' },
                                mix: [{ block: 'b-import-xls', elem: 'region-checkbox' }],
                                text: {
                                    content: ['&nbsp;', 'установить регион для всех загружаемых объявлений']
                                }
                            }
                        },
                        { block: 'b-import-xls', elem: 'region-box', elemMods: { hidden: 'yes' } },
                        {
                            label: 'Действие',
                            control: {
                                block: 'b-import-xls',
                                elem: 'actions',
                                agencies: ctx.agencies,
                                campaigns: ctx.campaigns,
                                campLimitError: ctx.campLimitError,
                                allow: ctx.allow,
                                service: ctx.service,
                                notAgencyControl: ctx.notAgencyControl,
                                isAnyClient: ctx.isAnyClient,
                                hasMediaControl: ctx.hasMediaControl,
                                hasSupportControl: ctx.hasSupportControl,
                                disabled: 'yes'
                            }
                        }
                    ]
                }
            };
    }
});
