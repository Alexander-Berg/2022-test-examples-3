describe('dm-mobile-content-campaign', function() {
    var dataModel;

    describe('Проверка полей на наличие', function() {
        before(function(){
            dataModel = BEM.MODEL.create('dm-mobile-content-campaign');
        });

        after(function(){
            dataModel.destruct();
        });
        // Список полей для проверки получен выполнением для транковой версии модели
       // Object.keys(BEM.MODEL.create('dm-mobile-content-campaign').fields).map(function(name) { return name; });
        [
            'active_orders_money_out_sms', 'active_orders_money_warning_sms', 'camp_banners_domain', 'cid',
            'camp_finished_sms', 'ContextLimit', 'ContextPriceCoef', 'currency', 'day_budget', 'day_budget2', 'day_budget_show_mode',
            'enable_cpc_hold', 'disabledIps', 'DontShow', 'email',
            'email_notify_paused_by_day_budget', 'finish_date', 'fio', 'groupId', 'hasPlatformSelect',
            'is_autobudget', 'mediaType', 'moderate_result_sms', 'money_warning_value', 'name',
            'nobsdata', 'notify_metrica_control_sms', 'notify_order_money_in_sms', 'net_strategy', 'phrases_inited',
            'places_strategy', 'platform', 'platform_select', 'product_type', 'readonly', 'search_strategy', 'sendAccNews',
            'sendWarn', 'showPmax', 'sms_phone', 'sms_time_hour_from', 'sms_time_hour_to', 'sms_time_min_from',
            'sms_time_min_to', 'spent_today', 'start_date', 'statusContextStop',
            'strategy_name', 'sum_rest', 'tags', 'timetarget_coef', 'title', 'ulogin',
            'untagged_banners_num', 'warnPlaceInterval', 'campaignIsArchived', 'currentTab', 'countArchTab',
            'countOffTab', 'common_geo_set', 'common_geo', 'banners_count', 'isCampShows', 'selectedAdgroupIds',
            'allAdgroupIds', 'campaign_goals', 'offlineStatNotice', 'fairAuction', 'broad_match_flag', 'agency_uid',
            'ClientID', 'statusBsSynced', 'autobudget', 'sms_flags', 'statusBehavior', 'lastShowTime', 'money_type', 'statusNoPay',
            'is_camp_locked', 'total_units', 'sums_uni', 'strategy_min_price', 'sum_units', 'all_banners_num', 'autobudget_date',
            'warnplaces_str', 'images', 'pages_num',
            'minus_words', 'statusShow', 'paid_by_certificate', 'compaign_domains_count', 'statusEmpty', 'email_notifications',
            'sum_spent_units', 'sum_spent', 'sum_to_pay', 'broad_match_limit', 'stopTime', 'statusActive', 'has_groups',
            'statusAutobudgetForecast', 'statusPostModerate', 'sendNews', 'statusModerate', 'LastChange',
            'mediaplan_status', 'camp_description', 'currencyConverted', 'MIN_PHRASE_RANK_WARNING', 'day_budget_stop_time',
            'sum', 'timeTarget', 'dontShowCatalog', 'start_time', 'is_search_stop', 'warnplaces', 'MAX_PHRASE_RANK_WARNING',
            'MAX_BANNER_LIMIT', 'optimal_groups_on_page', 'groups_on_page', 'AgencyID', 'statusBsArchived', 'archived', 'type',
            'sum_last', 'statusYacobotDeleted', 'budget_strategy', 'finish_time', 'manager_uid', 'OrderID',
            'dd', 'mm', 'yyyy', 'banners_per_page', 'device_type_targeting', 'network_targeting', 'sms_time',
            'broad_match_rate', 'dontShowYacontext', 'currency_archived', 'is_archived', 'groups', 'maxKeywordLimit',
            'strategy2', 'rmpCounters', 'pictures'
        ].forEach(function(name) {
            it('Поле ' + name + ' должно содержаться в модели', function() {
                expect(dataModel.hasField(name)).to.be.true;
            });
        });
    });
});
