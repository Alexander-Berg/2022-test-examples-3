#!/usr/bin/env perl

use Direct::Modern;

use open ':std' => 'utf8';

use JSON;

use Test::More;
use Test::Exception;

use my_inc '../../../';

use Direct::Campaigns;

use Primitives;
use RBACDirect qw/rbac_delete_campaign/;

use Settings;

use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;
use Yandex::DBShards;
use Yandex::TimeCommon qw/ mysql_round_day ts_round_day ts_to_str /;

use Yandex::DBUnitTest qw/:all/;

my %db = (
    ssp_platforms => {
        original_db => PPCDICT,
        rows => [
            { title => 'Valid SSP' },
            { title => 'Another SSP' },
        ],
    },
    products => {
        original_db => PPCDICT,
        rows => [
            { type => 'text', currency => 'RUB', ProductID => 12345, UnitName => 'Bucks', }
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
        ],
    },
    ppc_properties => {
        original_db => PPCDICT()
    },
    client_limits => {
        original_db => PPC(shard => 1),
        like => 'client_limits',
    }
);

init_test_dataset(\%db);

{
    no warnings 'redefine';
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
}


BEGIN {
    use_ok 'Direct::Model::Campaign';
}

subtest 'Campaign Model' => sub {

    subtest "to_hash() should force integer format for 'id' field" => sub {
        my $camp = Direct::Model::Campaign->new(id => "123");
        is(
            to_json($camp->to_hash, {canonical => 1}),
            '{"id":123}',
        );
    };

    subtest 'campaign type' => sub {
        lives_ok { Direct::Model::Campaign->new(campaign_type => 'text') } 'campaign can be created with correct type';

        my $campaign = Direct::Model::Campaign->new();
        lives_ok { $campaign->campaign_type('text') } 'campaign\'s type can be set to correct value';

        dies_ok { Direct::Model::Campaign->new(campaign_type => 'wallet') } 'campaign can\'t be created with incorrect type';
        dies_ok { $campaign->campaign_type('wallet') } 'campaign\'s type can\'t be set to incorrect value';
    };

    subtest 'deserialization/serialization for disabled platforms' => sub {
        my $campaign = Direct::Model::Campaign->new(
            client_id => 1,
            _disabled_domains => ' google.ru ,google.ua, google.com evil\'s empire,   ',
            _disabled_ssp => '["Valid SSP"]',
            client_id => "456",
        );
        is_deeply( $campaign->disabled_domains, ['google.ru', 'google.ua', 'google.com evil\'s empire', 'Valid SSP'], 'deserialization' );

        $campaign->disabled_domains( [qw/ mail.ru mail.ua mail.com /,  'Another SSP'] );
        is( $campaign->_disabled_domains, 'mail.com,mail.ru,mail.ua', 'domains serialization' ); # mail.ru запрещать нельзя DIRECT-24204
        is( $campaign->_disabled_ssp, '["Another SSP"]', 'ssp serialization' );
    };

    subtest 'deserialization/serialization for disabled_ips' => sub {
        my $campaign = Direct::Model::Campaign->new(_disabled_ips => ' 1.1.1.1  2.2.2.2 , ,  3.3.3.3,   ');
        is_deeply( $campaign->disabled_ips, [qw/ 1.1.1.1 2.2.2.2 3.3.3.3 /], 'deserialization' );

        $campaign->disabled_ips( [qw/ 4.4.4.4 5.5.5.5 6.6.6.6 /] );
        is( $campaign->_disabled_ips, '4.4.4.4,5.5.5.5,6.6.6.6', 'serialization' );
    };

    subtest 'deserialization/serialization for competitors_domains' => sub {
        my $campaign = Direct::Model::Campaign->new(_competitors_domains => ' google.ru, ,,  google.ua   google.com,   ');
        is_deeply( $campaign->competitors_domains, ['google.ru', 'google.ua', 'google.com'], 'deserialization' );

        $campaign->competitors_domains( [qw/ mail.ru mail.ua mail.com /] );
        is( $campaign->_competitors_domains, 'mail.ru,mail.ua,mail.com', 'serialization' );
    };

    subtest 'deserialization/serialization for metrika_counters' => sub {
        my $campaign = Direct::Model::Campaign->new(_metrika_counters => '1, ,  2   3 ');
        is_deeply( $campaign->metrika_counters, [qw/ 1 2 3 /], 'deserialization' );

        $campaign->metrika_counters( [qw/ 4 5 6 /] );
        is( $campaign->_metrika_counters, '4,5,6', 'serialization' );
    };

    subtest 'deserialization/serialization for sms_time' => sub {
        dies_ok { Direct::Model::Campaign->new(_sms_time => '09:00:21:00:12') } 'can\'t deserialize incorrect sms time';

        my $campaign = Direct::Model::Campaign->new(_sms_time => '09:00:21:00');
        is( $campaign->sms_time_from_hours, '09', 'deserialization, correct from_hours' );
        is( $campaign->sms_time_from_minutes, '00', 'deserialization, correct from_minutes' );
        is( $campaign->sms_time_to_hours, '21', 'deserialization, correct to_hours' );
        is( $campaign->sms_time_to_minutes, '00', 'deserialization, correct to_minutes' );


        $campaign->sms_time_from_hours('10');
        $campaign->sms_time_from_minutes('00');
        $campaign->sms_time_to_hours('22');
        $campaign->sms_time_to_minutes('00');
        is( $campaign->_sms_time, '10:00:22:00', 'serialization' );
    };

};

subtest 'Campaign Manager' => sub {

    init_test_dataset(&get_test_dataset);

    my $campaign_id;

    subtest 'create campaign' => sub {
        my $campaign = get_campaign();
        $campaign_id = $campaign->id;
        get_manager($campaign)->create();
        compare_models($campaign, get_db_campaign($campaign_id), qw/id user_id campaign_type campaign_name client_fio email start_date/);
    };

    subtest 'update campaign' => sub {
        my $campaign = get_db_campaign($campaign_id);
        #$campaign->campaign_type('mobile_content'); # WTF??
        $campaign->campaign_name('Test campaign №1');
        $campaign->client_fio('Тестер Тестерович Тестеров');
        $campaign->email('noboby@nowhere.org');
        $campaign->start_date('2016-01-01');
        get_manager($campaign)->update();
        compare_models($campaign, get_db_campaign($campaign_id), qw/id user_id campaign_type campaign_name client_fio email start_date/);
    };

    subtest 'create metrika counters' => sub {
        my $campaign = get_campaign(_metrika_counters => '1, 2,  3,4,    5 ');
        $campaign_id = $campaign->id;
        get_manager($campaign)->create();
        is_deeply([sort @{ $campaign->metrika_counters }], [sort @{ get_metrika_counters($campaign_id) }] );
    };

    subtest 'update metrika counters' => sub {
        my $campaign = get_db_campaign($campaign_id);
        $campaign->metrika_counters([qw/6 7 8 9 10/]);
        get_manager($campaign)->update();
        is_deeply([sort @{ $campaign->metrika_counters }], [sort @{ get_metrika_counters($campaign->id) }] );
    };

    # TODO: add ut for hierarchical multipliers

    subtest 'delete campaigns' => sub {
        no strict qw/ refs /;
        no warnings qw/ once redefine /;
        *Direct::Campaigns::rbac_delete_campaign = sub {  };
        Direct::Campaigns->get($campaign_id)->delete();
        is_deeply(
            get_all_sql(PPC(cid => $campaign_id), ['SELECT cid, operation FROM camp_operations_queue', where => {cid => SHARD_IDS}]),
            [{cid => $campaign_id, operation => 'del'}]
        );
    };

};

sub get_test_dataset { +{
    shard_client_id => {original_db => PPCDICT, rows => [{ClientID => 1, shard => 1}]},
    shard_uid => {original_db => PPCDICT, rows => [{ClientID => 1, uid => 1}]},
    (map { $_ => {original_db => PPCDICT} } qw/ shard_inc_cid inc_hierarchical_multiplier_id inc_mw_id /),
    (map { $_ => {original_db => PPC(shard => 'all')} } qw/
        campaigns camp_options campaigns_performance campaigns_cpm_yndx_frontpage 
        minus_words metrika_counters hierarchical_multipliers camp_operations_queue
    /),
    users => {original_db => PPC(shard => 'all'), rows => {
        1 => [{uid => 1, ClientID => 1, login => 'unit-test'}],
    }},
} }

sub get_manager { Direct::Model::Campaign::Manager->new(items => [@_]) }

sub get_campaign {
    my %pref = @_;

    my $tomorrow = mysql_round_day( ts_to_str( ts_round_day() + 24 * 60 * 60 ), delim => '-');

    my $currency = 'RUB';
    my $product_id = product_info(type => 'text', currency => $currency)->{ProductID};

    my $default = {
        id                      => get_new_id('cid', ClientID => 1),
        client_id               => 1,
        user_id                 => 1,
        campaign_type           => 'text',
        campaign_name           => 'Тестовая кампания #1',
        client_fio              => 'Unit Tester',
        email                   => 'ut@yandex-team.ru',
        start_date              => $tomorrow,
        time_target             => '1JKLMNOPQRS2JKLMNOPQRS3JKLMNOPQRS4JKLMNOPQRS5JKLMNOPQRS;',
        timezone_id             => 0,
        _autobudget             => 'No',
        _autobudget_date        => '0000-00-00',
        broad_match_flag        => 'Yes',
        broad_match_limit       => 50,
        money_warning_threshold => 20,
        position_check_interval => 15,
        currency                => $currency,
        minus_words             => [''],
        last_change             => '2016-10-17 12:40:26',
        context_price_coef      => 100,
        opts                    => '',
        product_id              => $product_id,
        wallet_id               => 0,
        strategy_id             => 0,
    };

    my $state = { %$default };

    my $without = delete $pref{without};
    if ( $without ) {
        if ( ! ref $without ) {
            delete $state->{ $without };
        } elsif ( ref $without eq 'ARRAY' ) {
            delete @$state{ @$without };
        }
    }

    while ( my ( $n, $v ) = each %pref ) {
        $state->{ $n } = $v;
    }

    return  Direct::Model::Campaign->new( %$state );
}

sub get_db_campaign { Direct::Campaigns->get( $_[0] )->items->[0] }

sub get_metrika_counters {
    get_one_column_sql(PPC(cid => $_[0]), ['SELECT metrika_counter FROM metrika_counters', where => {cid => SHARD_IDS}])
}

sub compare_models {
    my ($model1, $model2, @fields) = @_;
    is_deeply({map { $_ => $model1->$_ } @fields}, {map { $_ => $model2->$_ } @fields});
}

done_testing;
