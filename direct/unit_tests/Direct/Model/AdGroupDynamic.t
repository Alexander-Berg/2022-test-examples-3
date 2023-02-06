#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;
use Yandex::DBShards;

use Direct::AdGroups2::Dynamic;

BEGIN {
    use_ok('Direct::Model::AdGroupDynamic');
    use_ok('Direct::Model::AdGroupDynamic::Manager');
    use_ok('Direct::Model::Banner');
    use_ok('Direct::Model::BannerDynamic');
    use_ok('Direct::Model::BannerDynamic::Manager');
    use_ok('Direct::Model::Tag');
}

subtest 'AdGroupDynamic Model' => sub {
    my $adgroup;
    lives_ok { $adgroup = Direct::Model::AdGroupDynamic->new() };
    ok($adgroup->adgroup_type eq 'dynamic');

    dies_ok { $adgroup->adgroup_type('base') };
    dies_ok { Direct::Model::AdGroupDynamic->new(adgroup_type => 'base') };

    ok($adgroup->adgroup_type eq 'dynamic', 'adgroup type after dies_ok');
    lives_ok { $adgroup->adgroup_type('dynamic') };

    lives_ok { $adgroup->banners([Direct::Model::BannerDynamic->new()]) } 'only dynamic banners#1';
    dies_ok { $adgroup->banners([Direct::Model::Banner->new()]) } 'only dynamic banners#2';

    dies_ok { $adgroup->has_phraseid_href(1) } 'no has_phraseid_href';
};

subtest 'AdGroupDynamic Manager' => sub {
    init_test_dataset(&get_test_dataset);

    local *get_dynamic_adgroup = sub { Direct::AdGroups2::Dynamic->get([$_[0]], with_tags => 1, with_multipliers => 1)->items->[0] };
    my $adgroup_std_id;

    local *get_banner_data = sub {
        get_one_line_sql(PPC(bid => $_[0]), ['SELECT statusBsSynced, LastChange, opts, geoflag FROM banners', where => {bid => SHARD_IDS}])
    };

    # Создание группы (простое)
    subtest 'Create adgroup (simple)' => sub {
        my $adgroup = Direct::Model::AdGroupDynamic->new(
            id => get_new_id('pid', ClientID => 1), campaign_id => 1, adgroup_name => "dyn_group1", main_domain => "ya.ru"
        );
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->create();
        compare_models($adgroup, get_dynamic_adgroup($adgroup->id), qw/id adgroup_type campaign_id adgroup_name _main_domain_id main_domain/);
    };

    # Создание группы (сложное)
    subtest 'Create adgroup' => sub {
        my $adgroup = Direct::Model::AdGroupDynamic->new(
            id => get_new_id('pid', ClientID => 1), client_id => 1, campaign_id => 1, adgroup_name => "dyn_group2",
            main_domain => "auto.ru", geo => "225", status_moderate => "Ready", minus_words => ["-минус", "-слова"],
            tags => [Direct::Model::Tag->new(tag_name => "тег1"), Direct::Model::Tag->new(tag_name => "Тег2")],
            hierarchical_multipliers => {mobile_multiplier => {multiplier_pct => 237}},
            href_params => "campaign_id={campaign_id}&banner_id={banner_id}",
        );
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->create();

        my $adgroup2 = get_dynamic_adgroup($adgroup->id);
        compare_models($adgroup, $adgroup2, qw/
            id adgroup_type campaign_id adgroup_name _main_domain_id main_domain geo status_moderate _mw_id minus_words href_params
        /);
        is($adgroup2->hierarchical_multipliers->{mobile_multiplier}->{multiplier_pct}, 237);
        is_deeply(
            [map { +{id => $_->id, tag_name => $_->tag_name} } @{$adgroup->tags}],
            [map { +{id => $_->id, tag_name => $_->tag_name} } @{$adgroup2->tags}],
        );

        $adgroup_std_id = $adgroup->id;
    };

    # Обновление группы (наиболее полное)
    subtest 'Update adgroup' => sub {
        my $adgroup = get_dynamic_adgroup($adgroup_std_id);
        my $orig_mw_id = $adgroup->_mw_id;
        my $orig_tag1_id = (grep { $_->tag_name eq "тег1" } @{$adgroup->tags})[0]->id;

        $adgroup->client_id(1);
        $adgroup->adgroup_name("dyn_group2_updated");
        $adgroup->main_domain("mini.auto.ru");
        $adgroup->geo("213,159");
        $adgroup->status_moderate("Yes");
        $adgroup->minus_words(["-бесплатно", "-скачать"]);
        $adgroup->href_params("x_campaign_id={campaign_id}&x_banner_id={banner_id}&ok=1");
        $adgroup->hierarchical_multipliers({mobile_multiplier => {multiplier_pct => 117}});
        $adgroup->tags([Direct::Model::Tag->new(tag_name => "тег1"), Direct::Model::Tag->new(tag_name => "тег3")]);

        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->update();

        my $adgroup2 = get_dynamic_adgroup($adgroup_std_id);
        compare_models($adgroup, $adgroup2, qw/
            id adgroup_type campaign_id adgroup_name _main_domain_id main_domain geo status_moderate _mw_id minus_words href_params
        /);
        is($adgroup2->hierarchical_multipliers->{mobile_multiplier}->{multiplier_pct}, 117);
        is_deeply(
            [map { +{id => $_->id, tag_name => $_->tag_name} } @{$adgroup->tags}],
            [map { +{id => $_->id, tag_name => $_->tag_name} } @{$adgroup2->tags}],
        );
        is($orig_tag1_id, (grep { $_->tag_name eq "тег1" } @{$adgroup2->tags})[0]->id, 'Идентификатор "тег1" не изменился');
        isnt($orig_mw_id, $adgroup2->_mw_id, 'Идентификатор минус-слов изменился');

        # Clear minus_words
        $adgroup = get_dynamic_adgroup($adgroup_std_id);
        $adgroup->minus_words([]);
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->update();
        ok(!$adgroup->_mw_id);
    };

    # Флаг: update_last_change
    subtest 'Flag: update_last_change' => sub {
        my $adgroup = get_dynamic_adgroup($adgroup_std_id);
        $adgroup->do_update_last_change(0);
        sleep(1);
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->update();
        my $adgroup2 = get_dynamic_adgroup($adgroup->id);
        is($adgroup->last_change, $adgroup2->last_change, 'force keep last_change');
        $adgroup->do_update_last_change(1);
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->update();
        isnt($adgroup2->last_change, get_dynamic_adgroup($adgroup->id)->last_change, 'force update last_change');
    };

    # Флаг: schedule_forecast
    subtest 'Flag: schedule_forecast' => sub {
        my $adgroup = get_dynamic_adgroup($adgroup_std_id);
        $adgroup->do_schedule_forecast(1);
        isnt(get_one_field_sql(PPC(pid => $adgroup->id), ["select autobudgetForecastDate from campaigns", where => {cid => $adgroup->campaign_id}]), undef);
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->update();
        is(get_one_field_sql(PPC(pid => $adgroup->id), ["select autobudgetForecastDate from campaigns", where => {cid => $adgroup->campaign_id}]), undef);
    };

    # Добавим несколько баннеров в группу (для тестов ниже)
    my ($banner1, $banner2);
    do {
        my $adgroup = get_dynamic_adgroup($adgroup_std_id);

        my $banner_tmpl = sub {(
            id => get_new_id('bid', ClientID => $adgroup->client_id), adgroup_id => $adgroup->id, campaign_id => $adgroup->campaign_id,
            geoflag => 1, status_bs_synced => 'Yes', last_change => $adgroup->last_change,
        )};
        $banner1 = Direct::Model::BannerDynamic->new(&$banner_tmpl, body => 'banner (1) text');
        $banner2 = Direct::Model::BannerDynamic->new(&$banner_tmpl, body => 'banner (2) text');
        Direct::Model::BannerDynamic::Manager->new(items => [$banner1, $banner2])->create();
    };

    # Флаг: update_banners_geoflag
    subtest 'Flag: update_banners_geoflag' => sub {
        my $adgroup = get_dynamic_adgroup($adgroup_std_id);

        $adgroup->do_update_banners_geoflag(undef);
        is(join(':', @{get_banner_data($_->id)}{qw/opts geoflag statusBsSynced/}), 'geoflag:1:Yes') for ($banner1, $banner2);
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->update();
        is(join(':', @{get_banner_data($_->id)}{qw/opts geoflag statusBsSynced/}), 'geoflag:1:Yes') for ($banner1, $banner2);

        sleep(1); # Чтобы перещелкнул LastChange

        $adgroup->do_update_banners_geoflag(0);
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->update();
        is(join(':', @{get_banner_data($_->id)}{qw/opts geoflag statusBsSynced/}), ':0:No') for ($banner1, $banner2);
        is(get_banner_data($_->id)->{LastChange}, $_->last_change) for ($banner1, $banner2);

        $adgroup->do_update_banners_geoflag(1);
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->update();
        is(join(':', @{get_banner_data($_->id)}{qw/opts geoflag statusBsSynced/}), 'geoflag:1:No') for ($banner1, $banner2);
        is(get_banner_data($_->id)->{LastChange}, $_->last_change) for ($banner1, $banner2);
    };

    # Флаг: bs_sync_banners
    subtest 'Flag: bs_sync_banners' => sub {
        my $adgroup = get_dynamic_adgroup($adgroup_std_id);

        sleep(1); # Чтобы перещелкнул LastChange
        do_update_table(PPC(pid => $adgroup->id), 'banners', {statusBsSynced => 'Yes', LastChange__dont_quote => 'LastChange'}, where => {pid => $adgroup->id});

        $adgroup->do_bs_sync_banners(1);
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->update();
        is_deeply([@{get_banner_data($_->id)}{qw/statusBsSynced LastChange/}], ['No', $_->last_change]) for ($banner1, $banner2);
    };
};

sub compare_models {
    my ($model1, $model2, @fields) = @_;
    is_deeply({map { $_ => $model1->$_ } @fields}, {map { $_ => $model2->$_ } @fields});
}

sub get_test_dataset { +{
    shard_client_id => {original_db => PPCDICT, rows => [{ClientID => 1, shard => 1}]},
    shard_inc_cid   => {original_db => PPCDICT, rows => [{cid => 1, ClientID => 1}]},
    (map { $_ => {original_db => PPCDICT} } qw/
        shard_inc_pid shard_inc_bid shard_inc_tag_id domains_dict inc_mw_id inc_hierarchical_multiplier_id
    /),

    (map { $_ => {original_db => PPC(shard => 'all')} } qw/
        phrases group_params adgroups_dynamic domains tag_campaign_list tag_group minus_words banners users hierarchical_multipliers
        mobile_multiplier_values banners_minus_geo banners_to_fill_language_queue
    /),
    campaigns       => {original_db => PPC(shard => 'all'), rows => {
        1 => [{cid => 1, uid => 1, ClientID => 1, type => 'dynamic', autobudgetForecastDate => '2015-01-01 10:00:00'}],
    }},
    users           => {original_db => PPC(shard => 'all'), rows => {
        1 => [{uid => 1, ClientID => 1, login => 'unit-test'}],
    }},
} }

done_testing;
