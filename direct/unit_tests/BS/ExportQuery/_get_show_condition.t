#!/usr/bin/perl

# $Id$

use Direct::Modern;

use Test::More;
use Test::Deep;
use Yandex::Test::UTF8Builder;

use Yandex::DBUnitTest qw/init_test_dataset/;

use BS::ExportQuery ();
use TimeTarget ();
use Settings;

{
    no warnings 'redefine';
    *TimeTarget::get_timezone = sub {
        my ($timezone_id) = @_;

        if (($timezone_id // 0) == 130) {
            return {
              'gmt_offset' => '+03:00',
              'offset_str' => '',
              'country_id' => '225',
              'name' => 'Москва',
              'group_nick' => 'russia',
              'offset' => 10800,
              'timezone' => 'Europe/Moscow',
              'msk_offset' => '+00:00',
              'timezone_id' => '130',
              'id' => '130'
          };
      } else {
          die 'unknown timezone_id ' . ($timezone_id // 0);
      }
    };
}

my %db = (
    ppc_properties => {
        original_db => PPCDICT,
        rows => [],
    },
    bad_domains_titles => {
        original_db => PPCDICT,
        rows => [],
    },
    products => {
        original_db => PPCDICT,
        rows => [],
    },
    crypta_goals => {
        original_db => PPCDICT,
        rows => [],
    },
);

init_test_dataset(\%db);

my %common_args = (
    c_type            => 'text',
    DontShowDomains   => '',
    statusYandexAdv   => 'No',
    platform          => 'both',
    showOnYandexOnly  => 'No',
    campaign_pId      => 1,
    ClientID          => 1,
    broad_match_rate => 'optimal',
    broad_match_limit => '40',
    is_related_keywords_enabled => 0,
    allowed_frontpage_types => undef,
    strategy_data     => '{"name":"default"}',
);
$common_args{$_} = undef for qw/
    disabled_ssp allowed_page_ids disabledIps broad_match_flag
    timeTarget minus_words finish_date device_targeting disabled_video_placements
/;
my %common_results = (
    DontShowDomains => [],
    TargetType      => [],
    BroadMatchLimit => 0,
    StopTime        => '',
    DeviceType      => [],
    MinusPhrases    => [],
);

my $strategy_data = '{"auto_prolongation":1, "name":"period_fix_bid"}';
# [\@args, \%expected_result, $test_name]
my @tests = (
    [
        [
            {
                %common_args,
            },
        ],
        {
            %common_results,
        },
        'обычная кампания',
    ],
    [
        [
            {
                %common_args,
                DontShowDomains => 'президент.рф',
            },
        ],
        {
            %common_results,
            DontShowDomains => ['xn--d1abbgf6aiiy.xn--p1ai'],
        },
        'запрет показов на кириллическом домене',
    ],
    [
        [
            {
                %common_args,
                DontShowDomains => 'президент.рф,www.ru',
            },
        ],
        {
            %common_results,
            DontShowDomains => [ 'xn--d1abbgf6aiiy.xn--p1ai', 'www.ru' ],
        },
        'запрет показов на нескольких доменах + домен www.ru',
    ],
    [
        [
            {
                %common_args,
                finish_date => '2011-06-23',
            },
        ],
        {
            %common_results,
            StopTime => '2011-06-23',
        },
        'кампания с датой окончания',
    ],
    [
        [
            {
                %common_args,
                device_targeting => 'other_devices,iphone,android_phone,ipad,android_tablet',
            },
        ],
        {
            %common_results,
            DeviceType => bag('other_devices', 'ipad', 'iphone', 'android_phone', 'android_tablet'),
        },
        'таргетинг на устройства',
    ],
    [
        [
            {
                %common_args,
                timeTarget => '1AfBfCfDfEFGHIJKLMNOPQRSTUVWXf2AfBfCfDfEFGHIJKLMNOPQRSTUVWXf3AfBfCfDfEFGHIJKLMNOPQRSTUVWXf4AfBfCfDfEFGHIJKLMNOPQRSTUVWXf5AfBfCfDfEFGHIJKLMNOPQRSTUVWXf6AfBfCfDfEFGHIJKLMNOPQRSTUVWXf7AfBfCfDfEFGHIJKLMNOPQRSTUVWXf9',
                timezone_id => 130,
            },
            1,
        ],
        {
            %common_results,
            TimeZone => 'Europe/Moscow',
        },
        'при круглосуточном временном таргетинге с почасовыми коэффициентами есть часовой пояс',
    ],
    [
        [
            {
                %common_args,
                timeTarget => '123ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEGHIJKLMNOPQRSTUVWX5ABDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPRSTUVWX7ABCDFGHIJLMNOPQRSTUVW8IJKLMNOPQRST',
                timezone_id => 130,
            },
        ],
        {
            %common_results,
            TargetTimeLike => bag('3ABCDEFGHIJKLMNOPQRSTUVWX', '4ABCDEGHIJKLMNOPQRSTUVWX', '5ABDEFGHIJKLMNOPQRSTUVWX', '6ABCDEFGHIJKLMNOPRSTUVWX', '7ABCDFGHIJLMNOPQRSTUVW', '8IJKLMNOPQRST'),
            TargetTimeWorking => 0,
            TimeZone => 'Europe/Moscow',
        },
        'ограниченном временной таргетинг',
    ],
    [
        [
            {
                %common_args,
                c_type                    => 'cpm_banner',
                disabled_video_placements => Encode::encode('UTF-8','["ya.ru","адвокат-27.рф","xn--e1aarjeur.xn--p1ai"]')
            },
        ],
        {
            %common_results,
            TargetType           => [0, 1, 2],
            DontShowVideoDomains => ["ya.ru", "xn---27-5cdaln3c9a5b.xn--p1ai", "xn--e1aarjeur.xn--p1ai"],
        },
        'punycode disabled video placemnts',
    ],
    [
        [
            {
                %common_args,
                strategy_data => undef,
            },
        ],
        {
            %common_results,
        },
        'strategy_data is null',
    ],
    [
        [
            {
                %common_args,
                c_type        => 'cpm_price',
                finish_date   => '2021-12-23',
                strategy_data => $strategy_data
            },
        ],
        {
            %common_results,
            StopTime  => '20211225',
            DontShowVideoDomains => [],
            TargetType => [0, 1, 2],
        },
        'stop time is longer for 2 days',
    ],
);

Test::More::plan(tests => scalar(@tests));

for my $test (@tests) {
    my ($args, $expected_result, $test_name) = @$test;

    my %camps;
    for my $arg (@$args) {
        next if ref $arg ne 'HASH';

        $camps{1} = $arg;
        $arg->{campaign_pId} = 1;
    }
    BS::ExportQuery::init(is_preprod => 0, error_logger => sub { });
    BS::ExportQuery::set_global_variables({campaign_minus_objects => {}, campaigns_descriptions => \%camps});
    my $result = BS::ExportQuery::_get_show_condition(@$args);
    cmp_deeply($result, $expected_result, $test_name);
}
