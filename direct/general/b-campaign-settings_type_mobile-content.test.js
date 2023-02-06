describe('b-campaign-settings_type_mobile-content', function() {
    var modelData = {
        'cid': '7997364',
        'strategy2': {
            'name': 'autobudget_avg_cpi',
            'options': {
                'name': 'autobudget_avg_cpi',
                'avg_cpi': '50',
                'sum': '12000',
                'bid': null
            },
            'is_autobudget': true,
            'is_net_stopped': false
        },
        'mediaType': 'mobile_content',
        'name': '2016 - iOS - золотые',
        'strategy_name': '',
        'email_notifications': {
            'paused_by_day_budget': 1
        },
        'device_type_targeting': [
            'phone', 'tablet'
        ],
        'network_targeting': [
            'wifi', 'cell'
        ]
    },
    bemJson = {
        block: 'b-campaign-settings',
        mods: { 'type': 'mobile-content' },
        mix: [
            {
                block: 'b-campaign-settings',
                elem: 'form-container'
            }
        ],
        js: {
            'modelParams':
            {
                'name':'dm-mobile-content-campaign',
                'id':'7997364'
            },
            'newCamp':false
        },
        content: [
            {
                elem: 'form',
                elemMods: { camp: 'new' },
                content: {
                    block: 'b-layout-form2',
                    header: {
                       'cells' : [
                          {
                             'elem' : 'old-camp',
                             'content' : [
                                {
                                   'elem' : 'title',
                                   'block' : 'b-campaign-settings-header',
                                   'content' : [
                                      'Параметры кампании'
                                   ]
                                }
                             ]
                          }
                       ],
                       'mixes' : [
                          {
                             'elem' : 'header-group',
                             'block' : 'b-campaign-settings'
                          }
                       ],
                       'name' : 0
                    },
                    hiddenInputs: [],
                    submit: {
                        position: 'left',
                        control: {
                            block: 'b-campaign-settings',
                            elem: 'submit',
                            content: {
                                'mods': {
                                    'theme': 'action',
                                    'disabled': 'yes'
                                },
                                'block': 'button',
                                'content': 'Сохранить'
                            }
                        }
                    },
                    groups: []
                }
            },
            {
                elem: 'model-data',
                content: {
                    block: 'i-model',
                    modelParams: {
                        name: 'dm-mobile-content-campaign',
                        id: '7997364',
                        data: modelData
                    }
                }
            }
        ]
    },
    createBlock = function() {
        var ctx;
        ctx = u._.cloneDeep(bemJson);
        return  u.getDOMTree(ctx)
                    .appendTo(document.body)
                    .bem('b-campaign-settings');
    },
    block,
    sandbox;

    before(function() {
        block = createBlock();
    });

    after(function() {
        block.destruct();
    });

    beforeEach(function() {
        sandbox = sinon.sandbox.create({ useFakeTimers: true });
    });

    afterEach(function() {
        sandbox.restore();
    });

    //плавающий тест
    it.skip('При инициализации создается view модель b-campaign-settings_type_mobile-content', function() {
        expect(BEM.MODEL.getOne('b-campaign-settings_type_mobile-content')).to.not.be.undefined;
    });

    it('Должен содержать модель dm-mobile-content-campaign', function() {
        expect(BEM.MODEL.get('dm-mobile-content-campaign').length).to.equal(1);
    });

    it('Сущствующая DM правильная', function() {
        expect(BEM.MODEL.getOne('dm-mobile-content-campaign').id).to.equal('7997364');
    });
});
