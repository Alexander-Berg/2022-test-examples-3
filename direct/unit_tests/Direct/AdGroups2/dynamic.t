#!/usr/bin/env perl

use my_inc "../../..";
use Direct::Modern;

use Test::More;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBShards;
use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::AdGroupDynamic');
    use_ok('Direct::AdGroups2::Dynamic');
    use_ok('Direct::AdGroups2::Smart');
}

init_test_dataset(&get_test_dataset);

subtest 'Prepare to create dynamic adgroups' => sub {
    my $uid = 1;

    no warnings 'redefine';
    local *Direct::AdGroups2::get_new_id_multi = sub {
        my ($key, $cnt, $chain_key, $chain_val) = @_;
        state $next_id = 1;
        is_deeply({$key => $cnt, $chain_key => $chain_val}, {pid => $cnt, uid => $uid});
        return [map { $next_id++ } 1..$cnt];
    };
    use warnings 'redefine';

    my $adgroup = Direct::Model::AdGroupDynamic->new(adgroup_name => 'adgroup1', main_domain => 'ya.ru');
    Direct::AdGroups2::Dynamic->new([$adgroup])->prepare_create($uid);

    is_deeply($adgroup->to_db_hash, {
        pid => 1,
        adgroup_type => 'dynamic',
        group_name => 'adgroup1',
        main_domain => 'ya.ru',
        statusModerate => 'New',
        statusPostModerate => 'No',
    });
    is_deeply($adgroup->get_state_hash->{flags}, {});
    
    #группа с feed_id
    $adgroup = Direct::Model::AdGroupDynamic->new(adgroup_name => 'adgroup2', feed_id => 111);
    Direct::AdGroups2::Dynamic->new([$adgroup])->prepare_create($uid);
    is_deeply($adgroup->to_db_hash, {
        pid => 2,
        adgroup_type => 'dynamic',
        group_name => 'adgroup2',
        dynamic_feed_id => 111,
        statusModerate => 'New',
        statusPostModerate => 'No',
    });
};

subtest 'Prepare to update dynamic adgroups' => sub {
    my %adgroup_db_hash = (
        pid => 1, group_name => 'adgroup1', main_domain => 'ya.ru', geo => undef,
        href_params => "campaign_id={campaign_id}&adgroup_id={adgroup_id}&keyword={keyword}&from_yandex=1",
        statusModerate => 'Yes', statusPostModerate => 'Yes', dyn_statusBlGenerated => 'No',
    );
    my $uid = 1;

    # Изменение имени группы
    subtest 'Change adgroup name' => sub {
        my $adgroup = Direct::Model::AdGroupDynamic->from_db_hash(\%adgroup_db_hash, \{});
        $adgroup->adgroup_name('adgroup2');

        Direct::AdGroups2::Dynamic->new([$adgroup])->prepare_update();

        is_deeply($adgroup->to_db_hash, {
            %adgroup_db_hash,
            adgroup_type => 'dynamic',
            group_name => 'adgroup2',
            statusModerate => 'Yes',
            statusPostModerate => 'Yes',
        });
        is_deeply($adgroup->get_state_hash, {
            flags => {},
            changes => {adgroup_name => 1},
        });
    };

    # Изменение минус-слов
    subtest 'Change minus words' => sub {
        my $adgroup = Direct::Model::AdGroupDynamic->from_db_hash({%adgroup_db_hash, minus_words => ['-минус', '-слова']}, \{});
        $adgroup->minus_words(['-просто', '-слова']);

        my $dyn_cond = Direct::Model::DynamicCondition->new(id => 1, adgroup_id => $adgroup->id);
        $dyn_cond->adgroup($adgroup);

        Direct::AdGroups2::Dynamic->new([$adgroup])->prepare_update();

        is_deeply($adgroup->to_db_hash, {
            %adgroup_db_hash,
            adgroup_type => 'dynamic',
            statusBsSynced => 'No',
            statusShowsForecast => 'New',
        });
        is_deeply($adgroup->get_state_hash, {
            flags => {schedule_forecast => 1, bs_sync_banners => 1},
            changes => {_minus_words_hash => 1, status_bs_synced => 1, status_shows_forecast => 1},
        });
    };

    # Изменение гео-таргетинга
    subtest 'Change geo targeting' => sub {
        my $adgroup = Direct::Model::AdGroupDynamic->from_db_hash({%adgroup_db_hash, client_id => 1}, \{});
        $adgroup->old($adgroup->clone);

        my $dyn_cond = Direct::Model::DynamicCondition->new(id => 1, adgroup_id => $adgroup->id);
        $dyn_cond->adgroup($adgroup);

        $adgroup->geo("2,213,159");
        $adgroup->banners([new Direct::Model::BannerDynamic(_flags=>'', id=>10, status_moderate=>'Yes')]);

        Direct::AdGroups2::Dynamic->new([$adgroup])->prepare_update();

        is_deeply($adgroup->to_db_hash, {
            %adgroup_db_hash,
            adgroup_type => 'dynamic',
            geo => "2,213,159",
            statusBsSynced => 'No',
            statusShowsForecast => 'New',
        });
        is_deeply($adgroup->get_state_hash, {
            flags => {schedule_forecast => 1, update_banners_geoflag => '0', bs_sync_banners => 1},
            changes => {geo => 1, status_bs_synced => 1, status_shows_forecast => 1},
        });
    };

    # Изменение геотаргетинга с перемодерацией группы
    subtest 'Change geo targeting and send to moderation' => sub {
        my $adgroup = Direct::Model::AdGroupDynamic->from_db_hash({%adgroup_db_hash, client_id => 1}, \{});
        $adgroup->banners([new Direct::Model::BannerDynamic(_flags=>'', id=>10, status_moderate=>'Yes')]);
        $adgroup->old($adgroup->clone);

        my $dyn_cond = Direct::Model::DynamicCondition->new(id => 1, adgroup_id => $adgroup->id);
        $dyn_cond->adgroup($adgroup);

        my $new_geo = $ModerateChecks::DANGER_REGIONS[0];
        $adgroup->geo($new_geo);

        Direct::AdGroups2::Dynamic->new([$adgroup])->prepare_update();

        is_deeply($adgroup->to_db_hash, {
            %adgroup_db_hash,
            adgroup_type => 'dynamic',
            geo => $new_geo,
            statusBsSynced => 'No',
            statusModerate => 'Ready',
            statusPostModerate => 'No',
            statusShowsForecast => 'New',
        });
        is_deeply($adgroup->get_state_hash, {
            flags => {
                schedule_forecast => 1,
                update_banners_geoflag => '0',
                bs_sync_banners => 1,
                update_status_post_moderate_unless_rejected => 1,
                clear_banners_moderation_flags => 1,
            },
            changes => {geo => 1, status_bs_synced => 1, status_shows_forecast => 1, status_moderate => 1, status_post_moderate => 1},
        });
    };

    # Изменение основного домена
    subtest 'Change main domain' => sub {
        my $adgroup = Direct::Model::AdGroupDynamic->from_db_hash(\%adgroup_db_hash, \{});
        $adgroup->main_domain("auto.ru");

        my $dyn_cond = Direct::Model::DynamicCondition->new(id => 1, adgroup_id => $adgroup->id);
        $dyn_cond->adgroup($adgroup);

        Direct::AdGroups2::Dynamic->new([$adgroup])->prepare_update();

        is_deeply($adgroup->to_db_hash, {
            %adgroup_db_hash,
            adgroup_type => 'dynamic',
            main_domain => 'auto.ru',
            statusBsSynced => 'No',
            dyn_statusBlGenerated => 'Processing',
        });
        is_deeply($adgroup->get_state_hash, {
            flags => {update_last_change => 1, bs_sync_banners => 1},
            changes => {main_domain => 1, status_bs_synced => 1, status_bl_generated => 1},
        });
    };

    # Изменение параметров в URL ссылки
    subtest 'Change href params' => sub {
        my $adgroup = Direct::Model::AdGroupDynamic->from_db_hash(\%adgroup_db_hash, \{});
        $adgroup->href_params("campaign_id={campaign_id}&ok=1");

        my $dyn_cond = Direct::Model::DynamicCondition->new(id => 1, adgroup_id => $adgroup->id);
        $dyn_cond->adgroup($adgroup);

        Direct::AdGroups2::Dynamic->new([$adgroup])->prepare_update();

        is_deeply($adgroup->to_db_hash, {
            %adgroup_db_hash,
            adgroup_type => 'dynamic',
            href_params => 'campaign_id={campaign_id}&ok=1',
            statusBsSynced => 'No',
        });
        is_deeply($adgroup->get_state_hash, {
            flags => {update_last_change => 1, bs_sync_banners => 1},
            changes => {status_bs_synced => 1, href_params => 1},
        });
    };
};

subtest 'Tests with database access' => sub {

    my ($uid, $cid) = (1, 1);
    local *get_dynamic_adgroups = sub { Direct::AdGroups2::Dynamic->get_by(campaign_id => $_[0], with_tags => 1) };

    subtest 'Create adgroups' => sub {
        my $adgroup = Direct::Model::AdGroupDynamic->new(
            campaign_id => $cid, adgroup_name => "dyn_adgroup1", main_domain => "ya.ru", geo => "225",
        );
        my $adgroup2 = Direct::Model::AdGroupDynamic->new(
            campaign_id => $cid, adgroup_name => "dyn_adgroup2", main_domain => "auto.ru", geo => "225",
        );

        my $obj = Direct::AdGroups2::Dynamic->new(items => [$adgroup, $adgroup2]); $obj->create($uid);

        compare_models($adgroup, get_dynamic_adgroups($cid)->items_by->{$adgroup->id}, qw/
            id campaign_id adgroup_name adgroup_type geo status_moderate status_post_moderate
        /);
        compare_models($adgroup2, get_dynamic_adgroups($cid)->items_by->{$adgroup2->id}, qw/
            id campaign_id adgroup_name adgroup_type geo status_moderate status_post_moderate
        /);
    };

    #subtest 'Select dynamic adgroups' => sub {
    #    my $adgroup = get_dynamic_adgroups($cid)->items->[0];
    #
    #    # Добавим в группу баннеры и условия нацеливания
    #    Direct::AdGroups2::Smart->from_user_data($cid, [{
    #        adgroup_id => $adgroup->id,
    #        banners => [{body => "Test dynamic"}],
    #        dynamic_conditions => [{conditon_name => "Все страницы", condition => [{type => "any"}], price => '1.17'}],
    #    }])->apply;
    #
    #    ok(1);
    #};
};

sub compare_models {
    my ($model1, $model2, @fields) = @_;
    is_deeply({map { $_ => $model1->$_ } @fields}, {map { $_ => $model2->$_ } @fields});
}

sub get_test_dataset { +{
    shard_client_id => {original_db => PPCDICT, rows => [{ClientID => 1, shard => 1}]},
    shard_uid       => {original_db => PPCDICT, rows => [{uid => 1, ClientID => 1}]},
    shard_inc_cid   => {original_db => PPCDICT, rows => [{cid => 1, ClientID => 1}]},
    (map { $_ => {original_db => PPCDICT} } qw/
        shard_inc_pid shard_inc_bid shard_inc_tag_id domains_dict inc_mw_id inc_hierarchical_multiplier_id
    /),

    (map { $_ => {original_db => PPC(shard => 'all')} } qw/
        phrases group_params adgroups_dynamic domains tag_campaign_list tag_group minus_words banners users hierarchical_multipliers
        banners_minus_geo
    /),
    campaigns       => {original_db => PPC(shard => 'all'), rows => {
        1 => [{cid => 1, uid => 1, type => 'dynamic', autobudgetForecastDate => '2015-01-01 10:00:00'}],
    }},
    users           => {original_db => PPC(shard => 'all'), rows => {
        1 => [{uid => 1, ClientID => 1, login => 'unit-test'}],
    }},
} }


done_testing;
