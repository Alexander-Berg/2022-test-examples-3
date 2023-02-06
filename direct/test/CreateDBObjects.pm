# -*- encoding: utf-8; tab-width: 8 -*-
package Test::CreateDBObjects;

=head1 NAME

Test::CreateDBObjects - создание в БД объектов бизнес-логики (со всеми зависимостями); assertion'ы для работы с БД

=head1 SYNOPSIS

    use Test::More;

    use Yandex::DBTools; # Should be imported first, there is some interfence.

    use Test::CreateDBObjects;

    create_tables;

    # Create some objects
    my $group = create('group', ....);
    my $another_campaign = create('campaign', ...);

    # ... some tests using created objects

    # Assert that value fetched from database is equal to expected one.
    is_one_field PPC(shard => 1), ["select count(*) from banners", where => { pid => ... }], 10;

    done_testing;

=cut

use Direct::Modern;
use List::Util qw/shuffle/;
use Storable qw/dclone/;
use JSON;
use Direct::Test::DBObjects;

use base 'Test::Builder::Module';
our @EXPORT = qw/create create_tables

                 is_one_field

                 eq_hierarchical_multipliers make_hierarchical_multipliers_deep_comparable
                 prepare_mobile_multiplier_test
                 prepare_mobile_multiplier_test_for_camp
                 prepare_demography_multiplier_test
                 prepare_retargeting_multiplier_test
                 prepare_geo_multiplier_test
                 prepare_ab_segment_multiplier_test

                 cmp_camp_strategy_fields
                /;

use Test::Deep::NoTest qw/cmp_details deep_diag re code any/;
use Test::MockObject;

use Settings;
use ShardingTools;
use Yandex::DBTools;
use Yandex::DBShards;
use Yandex::HashUtils qw/hash_cut hash_merge/;
use User qw//;
use Campaign qw//;
use MailNotification;
use Models::AdGroup qw//;
use Yandex::DBUnitTest qw/:all/;
use Retargeting;
use HierarchicalMultipliers;
use PrimitivesIds;

use Direct::Feeds;

my %CONSTRUCTORS = (
    campaign => \&create_campaign,
    group => \&create_group,
    user => \&create_user,
    retargeting_condition => \&create_retargeting_condition,
    perf_feed => \&create_perf_feed,
);

=head2 create

Создаёт тестовый объект указанного типа. Если родительские объекты явно не указаны, то также создаёт и их, со
значениями по умолчанию.

    my $some_representation_of_created_object = create('object_type', some_create => 'options specific for this type');

=cut

sub create {
    my $type = shift;
    my %opts = @_;
    my $created_object = $CONSTRUCTORS{$type}->(\%opts);
    return $created_object;
}

=head2 create_user

Создаёт пользователя - запись в таблице users, и записи в шардинговой метабазе.

    my $user_info = create_user({shard => ...});

где

    $user_info = { uid => ..., ClientID => ... }

Если шард не задан, то выбирает случайный шард из набора доступных.

=cut

sub create_user {
    my ($opts) = @_;
    $opts //= {};

    my $shard = $opts->{shard} || (shuffle ppc_shards())[0];
    my $ClientID = 2 + (get_one_field_sql(PPCDICT(), "select max(ClientID) from shard_client_id") // 0);
    save_shard(ClientID => $ClientID, shard => $shard);
    my $uid = 1 + (get_one_field_sql(PPCDICT(), "select max(uid) from shard_uid") // 0);

    no warnings qw/redefine once/;
    local *User::get_info_by_uid_passport = sub ($) {
        my ( $uid ) = @_;
        return {
            login => "user_$uid",
            email => "user_$uid\@domain.com",
            fio => "User$uid User${uid}ovich",
            karma => 55,
            reg_date => '2015-01-01',
        };
    };
    User::create_update_user($uid, {ClientID => $ClientID, rep_type => 'chief'});
    return { uid => $uid, ClientID => $ClientID };
}


=head2 ensure_products

Заполним табличку `products` минимально необходимыми нам для работы данными.

=cut

sub ensure_products {
    do_sql(PPCDICT(), <<EOF);
INSERT IGNORE INTO `products` VALUES (1475,'Директ',0,'text',NULL,1.000000,'YND_FIXED',1,'Bucks',7,1,NULL,NULL,'','','clicks',1,'cpc');
EOF
    do_sql(PPCDICT(), <<EOF);
INSERT IGNORE INTO `products` VALUES (503162,'Рублевый Директ, Яндекс',0,'text',NULL,1.000000,'RUB',1,'Bucks',7,1,NULL,NULL,'','','clicks',1,'cpc');
EOF
    do_sql(PPCDICT(), <<EOF);
INSERT IGNORE INTO `products` VALUES (509692,'Внутренняя реклама. Дистрибуционные заказы',0,'internal_distrib',NULL,1.000000,'RUB',0,'Bucks',67,1,NULL,NULL,'','','clicks',1,'cpc');
EOF
}

my %CREATE_CAMPAIGN_OPTIONS = (
    bssynced_long_time_ago => sub {
        my ($cid, $this_option_value, $all_create_options) = @_;
        do_update_table(PPC(cid => $cid), 'campaigns',
                        {statusBsSynced => 'Yes', LastChange__dont_quote => 'date_sub(now(), interval 100 day)'},
                        where => {cid => $cid});
    },
    hierarchical_multipliers => sub {
        my ($cid, $this_option_value, $all_create_options) = @_;
        HierarchicalMultipliers::save_hierarchical_multipliers($cid, undef, $this_option_value, dont_forward_to_java => 1);
    },
);

=head2 create_campaign

    my $cid = create_campaign({uid => ..., # опционально, если уже есть готовый пользователь, которому надо добавить кампанию.
                               hierarchical_multipliers => ..., # Сохранить указанные корректировки на созданную кампанию
    });

=cut

sub create_campaign {
    my ($opts) = @_;
    $opts //= {};
    no warnings qw/redefine/;
    local *Client::ClientFeatures::has_get_strategy_id_from_shard_inc_strategy_id_enabled = sub { return 0 };

    $opts->{uid} = create_user(hash_cut $opts, qw/shard/)->{uid} unless $opts->{uid};
    my $user = User::get_user_data($opts->{uid}, [qw/ClientID fio email/]);
    my $cid = Campaign::create_empty_camp(
        client_chief_uid => $opts->{uid}, currency => $opts->{currency} // 'YND_FIXED', ClientID => $user->{ClientID},
        client_fio => $user->{fio}, client_email => $user->{email}, type => $opts->{type},
        type => $opts->{type} // 'text', product_type => $opts->{product_type} // 'text', domain => 'yandex.ru'
    );

    exec_sql(PPC(cid => $cid), ["update campaigns set statusEmpty = 'No'", where => {cid => $cid}]);

    apply_options($cid, $opts, \%CREATE_CAMPAIGN_OPTIONS);
    return $cid;
}


=head2 save_new_camp

    То же, что и create_campaign, но вызывает также Campaign::save_camp с моделькой из
    get_model_for_save_camp()

=cut
sub save_new_camp {
    my ($opts) = @_;
    $opts //= {};
    no warnings qw/redefine/;
    local *Client::ClientFeatures::has_get_strategy_id_from_shard_inc_strategy_id_enabled = sub { return 0 };

    $opts->{uid} = create_user(hash_cut $opts, qw/shard/)->{uid} unless $opts->{uid};
    my $user = User::get_user_data($opts->{uid}, [qw/ClientID fio email/]);
    my $cid = Campaign::create_empty_camp(
        client_chief_uid => $opts->{uid}, currency => $opts->{currency} // 'YND_FIXED', ClientID => $user->{ClientID},
        client_fio => $user->{fio}, client_email => $user->{email}, type => $opts->{type},
        type => $opts->{type} // 'text', product_type => $opts->{product_type} // 'text', domain => 'yandex.ru'
    );

    my $model = get_model_for_save_camp(new_camp => 1,
        fio => $user->{fio},
        email => $user->{email},
        cid => $cid
    );

    if ($opts->{model_patch}) {
        hash_merge($model, $opts->{model_patch});
    }

    my $c = DirectContext->new({
        login_rights => {
            is_any_client => 1,
        },
        uid => $opts->{uid},
        UID => $opts->{uid},
    });
    # MailNotification достаёт ID оператора из глобальной переменной.
    MailNotification::save_UID_host($opts->{uid}, 'direct.yandex.ru');
    Campaign::save_camp($c, $model, $opts->{uid});

    exec_sql(PPC(cid => $cid), ["update campaigns set statusEmpty = 'No'", where => {cid => $cid}]);

    apply_options($cid, $opts, \%CREATE_CAMPAIGN_OPTIONS);
    return $cid;
}

=head2 get_model_for_save_camp

    Возвращает модель кампании для передачи в save_camp
    Все параметры именованные.
    Обязательные:
        сid,
        email,
        fio

    Необязательные:
        new_camp  => если правдивый, то возвращается модель новой кампании (взятая из cmd_saveNewCamp)
                     иначе возвращается модель старой кампании (из cmd_saveCamp)
        wallet_cid => ID общего счёта. Игнорируется, если new_camp == 1

=cut
sub get_model_for_save_camp {
    my (%p) = @_;

    my $campaign = from_json('
        {
            "ContextLimit": "100",
            "banners_per_page": "20",
            "broad_match_flag": "1",
            "broad_match_limit": "40",
            "campaign_minus_words": [],
            "day_budget": 0,
            "day_budget_show_mode": "default",
            "disabled_video_placements": [],
            "email_notifications": {
              "paused_by_day_budget": 1
            },
            "hierarchical_multipliers": {},
            "meaningful_goals": [],
            "mediaType": "text",
            "money_warning_value": "20",
            "name": "Новая ut",
            "offlineStatNotice": "1",
            "opts": {
              "enable_cpc_hold": "1",
              "has_turbo_smarts": null,
              "no_extended_geotargeting": "",
              "no_title_substitute": null
            },
            "sendAccNews": "1",
            "sms_time_hour_from": "09",
            "sms_time_hour_to": "21",
            "sms_time_min_from": "00",
            "sms_time_min_to": "00",
            "start_time": "20190819000000",
            "statusOpenStat": "No",
            "status_click_track": "1",
            "strategy": {
              "is_autobudget": 0,
              "is_net_stop": 0,
              "is_search_stop": 0,
              "name": "different_places",
              "net": {
                "name": "maximum_coverage"
              },
              "search": {
                "name": "default"
              }
            },
            "timeTarget": "1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX",
            "time_target_holiday": "0",
            "time_target_holiday_dont_show": "0",
            "time_target_holiday_from": "8",
            "time_target_holiday_to": "20",
            "time_target_working_holiday": "1",
            "timezone_id": "130",
            "warnPlaceInterval": "60"
          }
    ');
    $campaign->{cid} = $p{cid};
    $campaign->{email} = $p{email};
    $campaign->{fio} = $p{fio};

    if ($p{new_camp}) {
        my $new_camp_fields = {
            autoOptimization => 'No',
            camp_with_common_ci => '0',
            cmd => 'saveNewCamp',
            email_notify_paused_by_day_budget => '1',
            enable_cpc_hold => '1',
            extended_geotargeting => '1',
            geo_multipliers_enabled => '0',
            json_day_budget => {
                set => '',
                show_mode => 'default',
                sum => '',
            },
            json_fallback_selected_goal => {},
            json_geo_changes => {
                '225' => {
                    is_negative => 0
                }
            },
            json_meaningful_goals => [
                {
                    goal_id => "12"
                }
            ],
            json_strategy => {
                is_net_stop => 0,
                name => "different_places",
                net => {
                    name => "maximum_coverage"
                },
                search => {
                    name => "default"
                }
            },
            product_type => 'text',
            show_permalink_info => '1',
            timeTargetMode => 'simple',
            timezone_text => 'Москва',
            uid_url => '',
            use_favorite_camp => '1',
            worktime => '0#4#10#00#18#00',
        };
        delete $campaign->{$_} for qw/
            sms_time_hour_from
            sms_time_hour_to
            sms_time_min_from
            sms_time_min_to
            strategy
            wallet_cid
        /;
        $campaign = hash_merge($campaign, $new_camp_fields);
    } else {
        $campaign->{wallet_cid} = $p{wallet_cid}; # if not new
    }

    return $campaign;
}

=head2 get_model_for_save_camp_existing($cid)

    Возвращает по $cid модель кампании, пригодную к отправке в Campaign::save_camp

=cut
sub get_model_for_save_camp_existing {
    my ($cid) = @_;

    my $params = get_one_line_sql(PPC(cid => $cid), ["
        SELECT c.cid, co.email, co.FIO AS fio, c.wallet_cid
        FROM campaigns c
            JOIN camp_options co ON co.cid = c.cid",
        WHERE => ['c.cid' => $cid]
    ]);
    return get_model_for_save_camp(%$params);
}

my %CREATE_GROUP_OPTIONS = (
    bssynced_long_time_ago => sub {
        my ($group, $this_option_value, $all_create_options) = @_;
        do_update_table(PPC(pid => $group->{pid}), 'phrases',
                        {statusBsSynced => 'Yes', LastChange__dont_quote => 'date_sub(now(), interval 100 day)'},
                        where => {pid => $group->{pid}});
    },
);


=head2 create_group

Создает группу объявлений. Если не указано явно к какой кампании привязана группа, то создаёт также кампанию и
всю остальную её обвязку. Возвращает хэш.

    my $group_hash = create_group({cid => ..., # опционально, если уже есть готовая кампания, куда надо сохранить группу
                                   hierarchical_multipliers => ..., # Сохранить указанные коэффициенты в создаваемой группе
                                   bssynced_long_time_ago => 1, # Проставить группе statusBsSynced=Yes и LastChange далеко в прошлом.
    });

=cut

sub create_group {
    my ($opts) = @_;

    $opts //= {};
    unless ($opts->{cid}) {
        $opts->{cid} = create_campaign(hash_cut($opts, qw/shard bssynced_long_time_ago uid/));
    }
    my $campaign = Campaign::get_camp_info($opts->{cid}, undef, short => 1);
    my $phrases = $opts->{phrases} // [{ phrase => 'some-text', price => '0.03', }];
    my $group_data = {
        currency => $campaign->{currency},
        bid => undef,
        geo => 225,
        banners => [{
            bid => undef,
            title => 'Ha',
            body => 'Ho',
            banner_type => 'desktop',
            href => 'https://ya.ru',
        }],
        phrases => $phrases,
    };
    $campaign->{strategy} = Campaign::campaign_strategy($campaign);
    no warnings 'redefine';
    local *MailNotification::rbac_is_scampaign = sub { 1 };
    MailNotification::save_UID_host($campaign->{uid});

    # при создании баннера заполняется aggregator_domain в зависимости от включенности фичи (java intapi)
    local *Client::ClientFeatures::_get_features_allowed_for_client_ids = sub { {} };
    local *CommonMaps::check_address_map = sub {};       

    my $group = Models::AdGroup::save_group($campaign, $group_data, UID => $campaign->{uid});

    if ($opts->{hierarchical_multipliers}) {
        HierarchicalMultipliers::save_hierarchical_multipliers($opts->{cid}, $group->{pid}, $opts->{hierarchical_multipliers}, dont_forward_to_java => 1);
    }
    apply_options($group, $opts, \%CREATE_GROUP_OPTIONS);
    return Models::AdGroup::get_groups({pid => $group->{pid}})->[0];
}

=head2 create_retargeting_condition

=cut

sub create_retargeting_condition {
    my ($opts) = @_;
    state $goal_id = 1_000_000;
    $opts //= {};

    $opts->{uid} = create_user(hash_cut $opts, qw/shard/)->{uid} unless $opts->{uid};

    state $cond_num = 0;

    my $ret_cond_id = Retargeting::save_retargeting_condition(
        uid => $opts->{uid}, {
            condition_name => 'test cond '.(++$cond_num),
            condition_desc => 'some dummy desc',
            condition => [
                {
                    "type" => "or",
                    "goals" => [
                        { "goal_id" => $goal_id++, "time" => "1" },
                    ]
                }
            ],
        }
    );
    return $ret_cond_id;
}

=head2 create_perf_feed

=cut

sub create_perf_feed {
    my ($opts) = @_;
    state $feed_name_seq = 1_000_000;

    $opts //= {};

    $opts->{uid} = create_user(hash_cut $opts, qw/shard/)->{uid} unless $opts->{uid};
    my $ClientID = get_clientid(uid => $opts->{uid});

    my $feed = Direct::Model::Feed->new(
        client_id => $ClientID,
        name => $opts->{name} // 'Test Feed ' . (++$feed_name_seq),
        refresh_interval => 86400,
        update_status => 'New',
        source => 'url',
        %{hash_cut $opts, qw/content refresh_interval source/}
    );

    no warnings 'redefine';
    local *Direct::Storage::save = sub {
        return Test::MockObject->new()
            ->set_always(url => 'http://somewhere.in.mds')
            ->set_always(filename => $feed->{name});
    };
    use warnings 'redefine';

    my $feed_logic = Direct::Feeds->new(items => [$feed]);

    if ($opts->{with_history}) {
        $feed->add_history(Direct::Model::FeedHistoryItem->new()) for 1..$opts->{with_history};
    }

    $feed_logic->save;

    if ($opts->{update_status}) {
        # We need to force this status separately, buisness logic doesn't allow any status except 'New' when
        # new file is uploaded.
        $feed->update_status($opts->{update_status});
        $feed_logic->save;
    }

    if ($opts->{last_change_long_ago}) {
        exec_sql(PPC(ClientID => $feed->client_id), [
            "update feeds set LastChange = date_sub(now(), interval 1 year)",
            where => {feed_id => $feed->id},
        ]);
    }

    return $feed_logic->get_by($feed->client_id, id => $feed->id)->items->[0];
}

=head2 create_perf_creative_video

=cut

sub create_perf_creative_video
{
    my ($user, $group) = @_;
    state $inc_creative_id = 0;
    my $creative_id = ++$inc_creative_id;
    my $creative = {
          'ClientID' => $user->{ClientID},
          'alt_text' => undef,
          'business_type' => 'retail',
          'creative_group_id' => undef,
          'creative_id' => $creative_id,
          'creative_type' => 'video_addition',
          'group_create_time' => undef,
          'group_name' => undef,
          'height' => undef,
          'href' => undef,
          'layout_id' => 4,
          'moderate_info' => undef,
          'moderate_send_time' => '2017-04-04 15:33:21',
          'moderate_try_count' => '0',
          'name' => 'testVideoAdditionName',
          'preview_url' => 'https://cdn-austrian.economicblogs.org/wp-content/uploads/2016/09/AdobeStock_58349892-300x300.jpeg',
          'statusModerate' => 'New',
          'stock_creative_id' => $creative_id,
          'sum_geo' => undef,
          'template_id' => undef,
          'theme_id' => undef,
          'width' => undef,
          'duration' => 15,
    };
    do_insert_into_table(PPC(uid => $user->{uid}), perf_creatives => $creative);
    my $id = get_new_id('banner_creative_id');
    do_insert_into_table(PPC(uid => $user->{uid}), 'banners_performance', {
        banner_creative_id => $id,
        cid => $group->{cid},
        pid => $group->{pid},
        bid => $group->{banners}->[0]->{bid},
        creative_id => $creative_id,
    });
    return $creative_id;
}


=head2 create_tables

Создает тестовые версии для все таблиц, необходимых для работы все create_XXX() этого модуля.

=cut

sub create_tables {
    $Settings::SHARDS_NUM = $Yandex::DBUnitTest::SHARDS_NUM;
    my @ppcdict_tables = qw/lock_object metrika_goals geo_timezones shard_inc_bid inc_phid
                            ppc_properties shard_uid shard_login shard_client_id shard_inc_cid
                            shard_inc_pid products targeting_categories
                            inc_hierarchical_multiplier_id shard_inc_ret_cond_id inc_feed_id
                            inc_mds_id trusted_redirects inc_banner_creative_id media_resources
                            inc_resource_id forecast_ctr features shard_inc_strategy_id
                            /;

    my @ppc_tables = qw/
                additions_item_callouts additions_item_disclaimers additions_item_experiments addresses adgroups_content_promotion 
                adgroups_dynamic adgroups_mobile_content adgroups_performance adgroup_page_targets adgroups_minus_words adgroup_bs_tags
                aggr_statuses_campaigns autobudget_forecast auto_moderate auto_price_camp_queue balance_info_queue banner_display_hrefs banner_images
                banner_images_formats banner_images_pool banner_logos banner_buttons banner_resources banners
                banners_additions banners_minus_geo banners_mobile_content banners_performance banners_performance_main banner_prices banner_permalinks aggregator_domains
                bids bids_base bids_arc bids_dynamic bids_href_params bids_manual_prices bids_performance
                bids_phraseid_history bids_retargeting bs_dead_domains bs_export_candidates
                bs_export_queue campaigns subcampaigns camp_additional_data campaigns_experiments
                campaigns_mobile_content campaigns_performance campaigns_cpm_yndx_frontpage campaign_promoactions
                camp_domains_count camp_dialogs client_dialogs camp_metrika_counters camp_metrika_goals
                camp_operations_queue camp_options camp_day_budget_stop_history catalogia_banners_rubrics client_limits
                client_phones clients clients_options deleted_banners demography_multiplier_values domains
                dynamic_conditions eventlog events experiments feeds filter_domain
                geo_multiplier_values ab_segment_multiplier_values mobile_multiplier_values group_params hierarchical_multipliers
                images maps mds_metadata media_groups mediaplan_banners mediaplan_bids_retargeting
                mediaplan_stats metrika_counters minus_words mod_edit moderation_cmd_queue
                mod_object_version mod_reasons org_details perf_creatives perf_feed_categories
                perf_feed_history perf_feed_vendors phrases post_moderate
                retargeting_conditions retargeting_goals retargeting_multiplier_values expression_multiplier_values
                sitelinks_sets tag_campaign_list tag_group user_campaigns_favorite users
                users_options vcards wallet_campaigns mobile_content banner_turbolandings turbolandings
                currency_convert_money_correspondence camp_turbolanding_metrika_counters banner_turbolanding_params
                campaigns_internal adgroups_internal banners_content_promotion content_promotion
                moderate_banner_pages banners_content_promotion_video content_promotion_video organizations
                camp_measurers banner_measurers banners_tns banners_to_fill_language_queue clients_features adgroup_priority
                campaign_permalinks campaign_phones adgroups_hypergeo_retargetings adgroup_additional_targetings
                agency_lim_rep_clients banner_multicards banner_multicard_sets reverse_clients_relations strategies strategy_metrika_counters
                strategy_inventori_data
    /;

    if (@{get_all_sql(PPC(shard => 1), "show tables like 'perf_feed_history'")} > 0) {
        # Tables were created somewhere outside
        return;
    }

    ;

    init_test_dataset({
        (map { $_, { original_db => PPCDICT, like => $_ } } @ppcdict_tables),
        (map { $_, { original_db => PPC(shard => 'all'), 
                     like => $_, 
                     ($Direct::Test::DBObjects::TABLE_ENGINE{$_} 
                             ? (engine => $Direct::Test::DBObjects::TABLE_ENGINE{$_}) 
                             : ()) } } @ppc_tables),
    });

    # NB: Too slow, wait for mysql move to tmpfs and runtests.pl alt-database integration
    # # As we've started using foreign keys:
    # # - We no longer want MyISAM tables
    # # - Order of table creation matters
    # # - InnoDB creation takes longer, so we should do parallel creation
    # my $log = Test::MockObject->new();
    # $log->set_true('msg_prefix')->mock('out', sub { warn $_[0]});
    # foreach_shard_parallel_verbose(
    #     $log, sub {
    #         my ($shard) = @_;
    #         for my $tbl (@ppc_tables) {
    #             my $sql = Yandex::DBSchema::get_create_table_sql(db => 'ppc', table => $tbl);
    #             do_sql(PPC(shard => $shard), $sql);
    #         }
    #     }
    # );

    ensure_products();
}

=head2 is_one_field

    is_one_field $shard_info, $sql_query, $expected_result;

=cut

sub is_one_field($$$;$) {
    my ($shard, $sql, $expected, $test_name) = @_;
    Test::CreateDBObjects->builder->is_eq(get_one_field_sql($shard, $sql), $expected, $test_name);
}

=head2 eq_hierarchical_multipliers

    eq_hierarchical_multipliers $cid, $pid_or_undef_for_camp_options, { mobile_multiplier => {multiplier_pct => 123} , ... };

=cut

sub eq_hierarchical_multipliers($$$;$) {
    my ($cid, $pid, $expected, $test_name) = @_;

    my ($ok, $stack) = cmp_details(HierarchicalMultipliers::get_hierarchical_multipliers($cid, $pid), make_hierarchical_multipliers_deep_comparable($expected));
    my $test = Test::CreateDBObjects->builder;

    unless ($test->ok($ok, $test_name)) {
        $test->diag(deep_diag($stack));
    }
}

=head2 apply_options

Хелпер для create_XXX() функций, применяет хуки.

=cut

sub apply_options {
    my ($created_object, $opts, $creation_hooks)= @_;
    for my $option_name (grep { exists $creation_hooks->{$_} } keys %$opts) {
        $creation_hooks->{$option_name}->($created_object, $opts->{$option_name}, $opts);
    }
}

=head2 make_hierarchical_multipliers_deep_comparable

В HierarchicalMultipliers описано полный набор данных, возвращаемых из БД. Эта функция на вход принимает данные
такой же структуры, но для отсутсвующих полей проставляется походящая по смыслу сравнивалка из Test::Deep. Так
мы может просто проверять только те части набора, которые нам интересны.


=cut

sub make_hierarchical_multipliers_deep_comparable {
    my ($multiplier_set) = @_;

    $multiplier_set = dclone($multiplier_set);

    my $last_change_re = re(qr/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/);
    my $positive_number = code(sub { $_[0] > 0 });

    while (my($type, $value) = each %$multiplier_set) {
        $value->{last_change} = $last_change_re unless exists $value->{last_change};
        $value->{hierarchical_multiplier_id} = $positive_number unless exists $value->{hierarchical_multiplier_id};
        if ($type eq 'demography_multiplier') {
            for my $condition (@{$value->{conditions}}) {
                $condition->{last_change} = $last_change_re unless exists $condition->{last_change};
                $condition->{demography_multiplier_value_id} = $positive_number unless exists $condition->{demography_multiplier_value_id};
            }
        } elsif ($type eq 'retargeting_multiplier') {
            for my $condition (values %{$value->{conditions}}) {
                $condition->{last_change} = $last_change_re unless exists $condition->{last_change};
                $condition->{retargeting_multiplier_value_id} = $positive_number unless exists $condition->{retargeting_multiplier_value_id};
            }
        }
    }
    return $multiplier_set;
}

=head2 prepare_mobile_multiplier_test

Возвращает ($group_in_first_shard, $hierarchical_multipliers_of_this_group_that_contain_only_mobile_multiplier, $hierarchical_multiplier_id, $identifier_of_corrections_set).

=cut

sub prepare_mobile_multiplier_test {
    my %opts = @_;
    my $mult = {
        mobile_multiplier => {
            multiplier_pct => 177,
        }
    };
    my $group = create('group', shard => 1, hierarchical_multipliers => $mult, %{hash_cut \%opts, qw/bssynced_long_time_ago cid/});
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), [
        "select hierarchical_multiplier_id from hierarchical_multipliers",
        where => { cid => $group->{cid}, pid => $group->{pid}, type => 'mobile_multiplier' }
    ]);
    return ($group, $mult, $hierarchical_multiplier_id, $hierarchical_multiplier_id);
}

=head2 prepare_mobile_multiplier_test_for_camp

Возвращает ($cid_in_first_shard, $hierarchical_multipliers_of_this_campaign_that_contain_only_mobile_multiplier, $hierarchical_multiplier_id, $identifier_of_corrections_set).

=cut

sub prepare_mobile_multiplier_test_for_camp {
    my %opts = @_;
    my $mult = {
        mobile_multiplier => {
            multiplier_pct => 177,
        }
    };
    my $cid = create('campaign', shard => 1, hierarchical_multipliers => $mult, %{hash_cut \%opts, qw/bssynced_long_time_ago/});
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $cid), [
        "select hierarchical_multiplier_id from hierarchical_multipliers",
        where => { cid => $cid, pid__is_null => 1, type => 'mobile_multiplier' }
    ]);
    return ($cid, $mult, $hierarchical_multiplier_id, $hierarchical_multiplier_id);
}

=head2 prepare_demography_multiplier_test

Создаёт группу с сохранённой валидной корректировкой по соцдему, возвращает:

    ($group_as_returned_by_get_groups_gr, $hierarchical_multipliers_data, $identifier_of_the_only_correction, $identifier_of_corrections_set)

=cut

sub prepare_demography_multiplier_test {
    my %opts = @_;
    my $mult = {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => undef,
                    gender => 'male',
                    multiplier_pct => 177,
                }
            ],
        },
    };
    my $group = create('group', shard => 1, hierarchical_multipliers => $mult, %{hash_cut \%opts, qw/bssynced_long_time_ago/});
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), [
        "select hierarchical_multiplier_id from hierarchical_multipliers",
        where => { cid => $group->{cid}, pid => $group->{pid}, type => 'demography_multiplier' }
    ]);

    my $demography_multiplier_value_id = get_one_field_sql(PPC(cid => $group->{cid}), [
        "select demography_multiplier_value_id from demography_multiplier_values",
        where => {hierarchical_multiplier_id => $hierarchical_multiplier_id}
    ]);
    return ($group, $mult, $demography_multiplier_value_id, $hierarchical_multiplier_id);
}


=head2 prepare_retargeting_multiplier_test

Создаёт группу с сохранённой валидной корректировкой по ретаргетингу, возвращает:

    ($group_as_returned_by_get_groups_gr, $hierarchical_multipliers_data, $identifier_of_the_only_correction, $identifier_of_corrections_set)

=cut

sub prepare_retargeting_multiplier_test {
    my %opts = @_;
    my $group = create('group', shard => 1, %{hash_cut \%opts, qw/bssynced_long_time_ago/});
    my $ret_cond_id = create('retargeting_condition', uid => get_uid(cid => $group->{cid}));
    my $mult = {
        retargeting_multiplier => {
            is_enabled => 1,
            conditions => {
                $ret_cond_id => {
                    multiplier_pct => 177,
                },
            },
        },
    };
    HierarchicalMultipliers::save_hierarchical_multipliers($group->{cid}, $group->{pid}, $mult, dont_forward_to_java => 1);

    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), [
        "select hierarchical_multiplier_id from hierarchical_multipliers",
        where => { cid => $group->{cid}, pid => $group->{pid}, type => 'retargeting_multiplier' }
    ]);

    my $retargeting_multiplier_value_id = get_one_field_sql(PPC(cid => $group->{cid}), [
        "select retargeting_multiplier_value_id from retargeting_multiplier_values",
        where => {hierarchical_multiplier_id => $hierarchical_multiplier_id}
    ]);
    return ($group, $mult, $retargeting_multiplier_value_id, $hierarchical_multiplier_id);
}

=head2 prepare_geo_multiplier_test

Создаёт группу с сохранённой валидной геокорректировкой, возвращает:

    ($group_as_returned_by_get_groups_gr, $hierarchical_multipliers_data, $identifier_of_the_only_correction, $identifier_of_corrections_set)

=cut

sub prepare_geo_multiplier_test {
    my %opts = @_;
    my $mult = {
        geo_multiplier => {
            is_enabled => 1,
            regions => [
                {
                    region_id => 244,
                    multiplier_pct => 177,
                }
            ],
        },
    };
    my $group = create('group', shard => 1, hierarchical_multipliers => $mult, %{hash_cut \%opts, qw/bssynced_long_time_ago/});
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $group->{cid}), [
        "select hierarchical_multiplier_id from hierarchical_multipliers",
        where => { cid => $group->{cid}, pid => $group->{pid}, type => 'geo_multiplier' }
    ]);

    my $geo_multiplier_value_id = get_one_field_sql(PPC(cid => $group->{cid}), [
        "select geo_multiplier_value_id from geo_multiplier_values",
        where => {hierarchical_multiplier_id => $hierarchical_multiplier_id}
    ]);
    return ($group, $mult, $geo_multiplier_value_id, $hierarchical_multiplier_id);
}

=head2 prepare_ab_segment_multiplier_test

Создаёт кампанию с сохранённой валидной корректировкой по аб-сегментам, возвращает:

    ($cid, $hierarchical_multipliers_data, $identifier_of_the_only_correction, $identifier_of_corrections_set)

=cut

sub prepare_ab_segment_multiplier_test {
    my %opts = @_;
    my $mult = {
        ab_segment_multiplier => {
            is_enabled => 1,
            ab_segments => [
                {
                    segment_id     => 2_500_000_005,
                    section_id     => 1,
                    multiplier_pct => 177,
                }
            ],
        },
    };
    my $cid = create('campaign', shard => 1, hierarchical_multipliers => $mult, %{hash_cut \%opts, qw/bssynced_long_time_ago/});
    my $hierarchical_multiplier_id = get_one_field_sql(PPC(cid => $cid), [
            "select hierarchical_multiplier_id from hierarchical_multipliers",
            where => { cid => $cid, pid__is_null => 1, type => 'ab_segment_multiplier' }
        ]);

    my $ab_segment_multiplier_value_id = get_one_field_sql(PPC(cid => $cid), [
            "select ab_segment_multiplier_value_id from ab_segment_multiplier_values",
            where => {hierarchical_multiplier_id => $hierarchical_multiplier_id}
        ]);
    return ($cid, $mult, $ab_segment_multiplier_value_id, $hierarchical_multiplier_id);
}


=head2 cmp_camp_strategy_fields

Проверяет, что у кампании установлены только указанные поля (из всего списка полей, относящихся к стратегии)

=cut

sub cmp_camp_strategy_fields {
    my ($cid, $expect, $test_name) = @_;
    $expect = dclone($expect);

    my $camp = get_one_line_sql(PPC(cid => $cid), [
        "select * from campaigns c ",
        "join camp_options co using (cid)",
        "left join campaigns_performance using (cid)",
        where => {cid => $cid}
    ]);

    # repack json for correct comparsion
    $camp->{strategy_data} = to_json from_json($camp->{strategy_data}), {canonical => 1};

    my %ignore_fields = (
        autobudget_avg_cpi => 1, # stored in 'autobudget_avg_cpa' field
        campaign_goals => 1, count_all_goals => 1, # Stored in (or calculated from) camp_metrika_goals
        context_strategy => 1, search_strategy => 1, # NB Remove after devtest is reloaded
    );
    my %defaults = (
        day_budget_show_mode => any('default', 'stretched'),
        day_budget => '0.00',
        autobudget_date => '0000-00-00',
        statusAutobudgetForecast => 'New',
    );

    my @interesting_fields = grep { !exists $ignore_fields{$_} } (
        @Campaign::STRATEGY_FILEDS,
        'platform',
    );

    my $camp_interesting = hash_cut $camp, \@interesting_fields;
    for my $field (@interesting_fields) {
        if (!exists $expect->{$field}) {
            $expect->{$field} = exists $defaults{$field} ? $defaults{$field} : any(undef, '');
        }
    }
    my ($ok, $stack) = cmp_details($camp_interesting, $expect);
    my $test = Test::CreateDBObjects->builder;

    unless ($test->ok($ok, $test_name)) {
        $test->diag(deep_diag($stack));
    }
}

1;
