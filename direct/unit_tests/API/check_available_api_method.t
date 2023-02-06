#!/usr/bin/perl

use Direct::Modern;

use Yandex::Test::UTF8Builder;
use Test::More tests => 3;
use Test::Deep;
use Test::MockTime qw/:all/;
use Time::Local;


use API;

use constant DEPRECATED => 'deprecated';
use constant AVAILABLE => 'available';
use constant NOT_AVAILABLE => 'not_available';

my %properties = (
    api_deprecated_error_show_rounds_count => 24,
    api_deprecated_error_show_interval => 60,
);

{
    no warnings 'redefine';
    *API::_get_property_value = sub { $properties{shift()} };
}

my @METHODS = qw/
   AccountManagement
   AdImage
   AdImageAssociation
   CheckPayment
   CreateInvoice
   CreateNewForecast
   CreateNewReport
   CreateNewSubclient
   CreateNewWordstatReport
   DeleteForecastReport
   DeleteReport
   DeleteSubscription
   DeleteWordstatReport
   EnableSharedAccount
   GetAvailableVersions
   GetBalance
   GetBannersStat
   GetBannersTags
   GetCampaignsTags
   GetClientInfo
   GetClientsList
   GetClientsUnits
   GetCreditLimits
   GetEventsLog
   GetForecast
   GetForecastList
   GetForecastSync
   GetKeywordsIntersection
   GetKeywordsSuggestion
   GetMetroStations
   GetNormalizedKeywords
   GetNormalizedKeywordsData
   GetRegions
   GetReportList
   GetRetargetingGoals
   GetRubrics
   GetStatGoals
   GetSubClients
   GetSubscription
   GetSummaryStat
   GetTimeZones
   GetVersion
   GetWordstatReport
   GetWordstatReportList
   GetWordstatSync
   ModerateBanners
   PayCampaigns
   PayCampaignsByCard
   PingAPI
   PingAPI_X
   Retargeting
   RetargetingCondition
   SaveSubscription
   SearchClients
   TransferMoney
   UpdateBannersTags
   UpdateCampaignsTags
   UpdateClientInfo
/;

my %DEPRECATED_API4_METHODS = map { $_ => 1 } qw/
/;

my %NOT_AVAILABLE_API4_METHODS = map { $_ => 1 } qw/
    AccountManagement
    AdImage
    AdImageAssociation
    CheckPayment
    CreateInvoice
    CreateNewForecast
    CreateNewReport
    CreateOfflineReport
    DeleteOfflineReport
    DeleteSubscription
    EnableSharedAccount
    GetBalance
    GetBannersStat
    GetBannersTags
    GetCampaignsTags
    GetClientInfo
    GetClientsList
    GetEventsLog
    GetForecast
    GetForecastSync
    GetKeywordsIntersection
    GetMetroStations
    GetNormalizedKeywords
    GetNormalizedKeywordsData
    GetOfflineReportList
    GetRetargetingGoals
    GetSubscription
    GetSummaryStat
    GetWordstatSync
    PayCampaigns
    PayCampaignsByCard
    Retargeting
    RetargetingCondition
    SaveSubscription
    SearchClients
    TransferMoney
    UpdateBannersTags
    UpdateCampaignsTags

    ModerateBanners
    UpdateClientInfo
    GetSubClients
    CreateNewSubclient
    GetReportList
    DeleteReport
/;

my %DEPRECATED_API4_LIVE_METHODS = map { $_ => 1} qw/
/;

my %NOT_AVAILABLE_API4_LIVE_METHODS = map { $_ => 1} qw/
    GetBalance
    GetClientInfo
    GetClientsList
    GetMetroStations
    ModerateBanners
    UpdateClientInfo
    CreateNewSubclient
    RetargetingCondition
    Retargeting
    CreateNewReport
    GetSummaryStat
    GetBannersStat
    DeleteReport
    GetReportList
    GetSubClients
    Mediaplan
    MediaplanAd
    MediaplanAdGroup
    MediaplanCategory
    MediaplanKeyword
/;

my %data = (
    api_version => 4,
    application_id => 'f01b8f334438467dab8d89131a5a18f9',
);

subtest 'API4 methods availability' => sub {
    plan tests => 58;
    foreach my $method (@METHODS) {
        my ($status);
        if ($DEPRECATED_API4_METHODS{$method}) {
            $status = DEPRECATED;
        } elsif ($NOT_AVAILABLE_API4_METHODS{$method}) {
            $status = NOT_AVAILABLE;
        } else {
            $status = AVAILABLE;
        }

        cmp_deeply(
            API::check_available_api_method({ %data, method => $method }, 0),
            superhashof({ res => $status }),
            "$method $status in API4"
        );
    }
};

subtest 'API4 live methods availability' => sub {
    plan tests => 58;
    foreach my $method (@METHODS) {
        my ($status);
        if ($DEPRECATED_API4_LIVE_METHODS{$method}) {
            $status = DEPRECATED;
        } elsif ($NOT_AVAILABLE_API4_LIVE_METHODS{$method}) {
            $status = NOT_AVAILABLE;
        } else {
            $status = AVAILABLE;
        }

        cmp_deeply(
            API::check_available_api_method({ %data, latest => 1, method => $method }, 0),
            superhashof({ res => $status }),
            "$method $status in API4 live"
        );
    }
};

my @application_ids = qw/
    f01b8f334438467dab8d89131a5a1800
    f01b8f334438467dab8d89131a5a1801
    f01b8f334438467dab8d89131a5a1802
    f01b8f334438467dab8d89131a5a1803
    f01b8f334438467dab8d89131a5a1804
    f01b8f334438467dab8d89131a5a1805
/;

%data = (
    api_version => 4,
    latest => 1,
    method => (keys %DEPRECATED_API4_LIVE_METHODS)[0]//'',
);

my %method_deprecate_time_by_application = (
    f01b8f334438467dab8d89131a5a1800 => {
        10 => { map {$_ => 1} qw/0_0 4_0 8_0 12_0 16_0 20_0/ },
        60 => { map {$_ => 1} qw/
            0_0 0_10 0_20 0_30 0_40 0_50
            4_0 4_10 4_20 4_30 4_40 4_50
            8_0 8_10 8_20 8_30 8_40 8_50
            12_0 12_10 12_20 12_30 12_40 12_50
            16_0 16_10 16_20 16_30 16_40 16_50
            20_0 20_10 20_20 20_30 20_40 20_50
        / },
    },
    f01b8f334438467dab8d89131a5a1801 => {
        10 => { map {$_ => 1} qw/0_40 4_40 8_40 12_40 16_40 20_40/ },
        60 => { map {$_ => 1} qw/
            0_40 0_50 1_0 1_10 1_20 1_30
            4_40 4_50 5_0 5_10 5_20 5_30
            8_40 8_50 9_0 9_10 9_20 9_30
            12_40 12_50 13_0 13_10 13_20 13_30
            16_40 16_50 17_0 17_10 17_20 17_30
            20_40 20_50 21_0 21_10 21_20 21_30
        / },

    },
    f01b8f334438467dab8d89131a5a1802 => {
        10 => { map {$_ => 1} qw/1_20 5_20 9_20 13_20 17_20 21_20/ },
        60 => { map {$_ => 1} qw/
            1_20 1_30 1_40 1_50 2_0 2_10
            5_20 5_30 5_40 5_50 6_0 6_10
            9_20 9_30 9_40 9_50 10_0 10_10
            13_20 13_30 13_40 13_50 14_0 14_10
            17_20 17_30 17_40 17_50 18_0 18_10
            21_20 21_30 21_40 21_50 22_0 22_10
        / },
    },
    f01b8f334438467dab8d89131a5a1803 => {
        10 => { map {$_ => 1} qw/2_0 6_0 10_0 14_0 18_0 22_0/ },
        60 => { map {$_ => 1} qw/
            2_0 2_10 2_20 2_30 2_40 2_50
            6_0 6_10 6_20 6_30 6_40 6_50
            10_0 10_10 10_20 10_30 10_40 10_50
            14_0 14_10 14_20 14_30 14_40 14_50
            18_0 18_10 18_20 18_30 18_40 18_50
            22_0 22_10 22_20 22_30 22_40 22_50
        / },
    },
    f01b8f334438467dab8d89131a5a1804 => {
        10 => { map {$_ => 1} qw/2_40 6_40 10_40 14_40 18_40 22_40/ },
        60 => { map {$_ => 1} qw/
            2_40 2_50 3_0 3_10 3_20 3_30
            6_40 6_50 7_0 7_10 7_20 7_30
            10_40 10_50 11_0 11_10 11_20 11_30
            14_40 14_50 15_0 15_10 15_20 15_30
            18_40 18_50 19_0 19_10 19_20 19_30
            22_40 22_50 23_0 23_10 23_20 23_30
        / },
    },
    f01b8f334438467dab8d89131a5a1805 => {
        10 => { map {$_ => 1} qw/3_20 7_20 11_20 15_20 19_20 23_20/ },
        60 => { map {$_ => 1} qw/
            3_20 3_30 3_40 3_50 4_0 4_10
            7_20 7_30 7_40 7_50 8_0 8_10
            11_20 11_30 11_40 11_50 12_0 12_10
            15_20 15_30 15_40 15_50 16_0 16_10
            19_20 19_30 19_40 19_50 20_0 20_10
            23_20 23_30 23_40 23_50 0_0 0_10
        / },
    },
);

subtest 'getting error for deprecated method' => sub {
    #plan tests => 1260;
    plan( skip_all => 'no deprecated methods atm' );
    foreach my $hour (0..4) {
        for (my $minutes = 0; $minutes < 60; $minutes += 10) {
            my $time = timelocal(0, $minutes, $hour, 29, 5, 116);
            set_absolute_time($time);
            foreach my $application_id (@application_ids) {
                $properties{api_deprecated_error_show_rounds_count} = 0;
                $properties{api_deprecated_error_show_interval} = 10;
                cmp_deeply(
                    API::check_available_api_method({%data, application_id => $application_id }),
                    superhashof({ res => AVAILABLE }),
                    "get no error for application_id $application_id at ".localtime($time).' with rounds count of 0'
                );

                $properties{api_deprecated_error_show_rounds_count} = 6;
                $properties{api_deprecated_error_show_interval} = 0;
                cmp_deeply(
                    API::check_available_api_method({%data, application_id => $application_id }),
                    superhashof({ res => AVAILABLE }),
                    "get no error for application_id $application_id at ".localtime($time).' with interval of 0'
                );

                $properties{api_deprecated_error_show_rounds_count} = 6;

                foreach my $interval (10, 60) {
                    $properties{api_deprecated_error_show_interval} = $interval;
                    cmp_deeply(
                        API::check_available_api_method({%data, application_id => $application_id }),
                        superhashof({ res => $method_deprecate_time_by_application{$application_id}{$interval}{$hour.'_'.$minutes} ? DEPRECATED : AVAILABLE }),
                        "get error for application_id $application_id at ".localtime($time)." with interval of $interval"
                    );
                    cmp_deeply(
                        API::check_available_api_method({%data, application_id => $application_id, app_min_api_version_override => 104 }),
                        superhashof({ res => AVAILABLE }),
                        "get no error for application_id $application_id when override for 104 (or via persistent_token) version is on at ".localtime($time)." with interval of $interval"
                    );
                }

                cmp_deeply(
                    API::check_available_api_method({%data, latest => 0, application_id => $application_id }),
                    superhashof({ res => NOT_AVAILABLE }),
                    "get error for application_id $application_id at ".localtime($time).' in API4'
                );
            }
        }
    }
};

restore_time();

