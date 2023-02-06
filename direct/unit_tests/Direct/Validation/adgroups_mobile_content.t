use Direct::Modern;

use Test::More;
use Yandex::Test::ValidationResult;
use Settings;
use Direct::Model::Campaign;
use Direct::Model::Campaign::Role::AdGroupsCount;
use Direct::Model::Role::Update;
use Settings;
use Yandex::DBUnitTest qw/:all/;
use geo_regions;

# здесь не хватает проверок на общие атрибуты группы (имя, geo и  т.п.)

# FIXME ужасный хак, чтобы прекратить ворнинги 'Use of uninitialized value in lc at /usr/share/perl5/Yandex/CheckMobileRedirect.pm line 402.'
$SIG{__WARN__} = sub {};

my %db = (
    clients => {
        original_db => PPC(shard => 1),
        like => 'clients',
        rows => {1 => [
            {ClientID => 1812, country_region_id =>  $geo_regions::RUS},
            {ClientID => 9827, country_region_id =>  $geo_regions::UKR},
        ]},
    }, 
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1812, shard => 1 },
            { ClientID => 9827, shard => 1 },
        ],
    },
);
init_test_dataset(\%db);


BEGIN {
    use_ok('Direct::Model::BannerMobileContent');
    use_ok('Direct::Model::AdGroupMobileContent');
    use_ok('Direct::Validation::AdGroupsMobileContent', qw/
        validate_add_mobile_adgroups
        validate_update_mobile_adgroups
    /);
}

package Test::Direct::Model::Campaign {
    use Mouse;
    extends 'Direct::Model::Campaign';
    with 'Direct::Model::Campaign::Role::AdGroupsCount';
    1;
}

package Test::Direct::Model::AdGroupMobileContent {
    use Mouse;
    extends 'Direct::Model::AdGroupMobileContent';
    with 'Direct::Model::Role::Update';
    1;
}

cmp_validation_result(
    validate_update_mobile_adgroups(
        get_adgroups({
            adgroup_name => 'AdGroup#1', geo => '225',
            store_content_href => '', device_type_targeting => [],
            client_id => 1812,
        })
    ),
    [{
        device_type_targeting => vr_errors('ReqField'), store_content_href => vr_errors('ReqField'),
        network_targeting => vr_errors('ReqField')
    }]
);

ok_validation_result(
    vr_add([{
        adgroup_name => 'AdGroup#1', geo => '225',
        store_content_href => 'https://play.google.com/store/apps/details?id=com.kabam.marvelbattle',
        device_type_targeting => ['phone'],
        network_targeting => ['cell', 'wifi'], client_id => 1812,
        min_os_version => '4.2'
    }, {
        adgroup_name => 'AdGroup#13', geo => '1',
        store_content_href => 'https://itunes.apple.com/app/alco-dog/id383869154?ign-mpt=uo%3D5',
        device_type_targeting => ['tablet'],
        network_targeting => ['wifi'], client_id => 1812,
        min_os_version => '8.0'
    }])
);

cmp_validation_result(
    vr_add([{
        adgroup_name => 'AdGroup#1', geo => '225',
        store_content_href => " \t \t    ",
        device_type_targeting => ['phone'],
        network_targeting => ['cell', 'wifi'], client_id => 1812,
        min_os_version => 'Android L'
    }, {
        adgroup_name => 'AdGroup#1', geo => '225',
        store_content_href => "lasdlaksdhttp:/asdd33sd.ru",
        device_type_targeting => ['phone'],
        network_targeting => ['cell', 'wifi'], client_id => 1812,
        min_os_version => "  \t   ",
    }, {
        adgroup_name => 'AdGroup#1', geo => '225',
        store_content_href => "http://www.windowsphone.com/ru-ru/store/app/%D1%81%D0%B1%D0%B5%D1%80%D0%B1%D0%B0%D0%BD%D0%BA/eda346d9-467a-4f01-952f-bc96b04a8f95",
        device_type_targeting => [],
        network_targeting => ['cell', 'wifi'], client_id => 1812,
        min_os_version => 'iOS8',
    }, {
        adgroup_name => 'AdGroup#1', geo => '225',
        store_content_href => "https://play.google.com/store/apps/details?id=",
        device_type_targeting => ['tablet'],
        network_targeting => [], client_id => 1812,
        min_os_version => '4.22'
    }, {
        adgroup_name => 'AdGroup#1', geo => '225',
        store_content_href => "https://itunes.apple.com/alco-dog/",
        device_type_targeting => [],
        network_targeting => [], client_id => 1812,
        min_os_version => '8.0'
    }]),
    [
        {store_content_href => vr_errors('ReqField')},
        {store_content_href => vr_errors(qr/Неправильный формат ссылки/)},
        {store_content_href => vr_errors('UnsupportedStore'), device_type_targeting => vr_errors('ReqField')},
        {store_content_href => vr_errors(qr/получить информацию о приложении/), network_targeting => vr_errors('ReqField')},
        {
            store_content_href => vr_errors(qr/получить информацию о приложении/),
            network_targeting => vr_errors('ReqField'), device_type_targeting => vr_errors('ReqField')
        }
    ]
);

cmp_validation_result(
    vr_add([{
            adgroup_name => 'AdGroup#1', geo => '225',
            store_content_href => "https://itunes.apple.com/us/app/garageband/id794957821",
            device_type_targeting => ['phone'],
            network_targeting => ['wifi'], client_id => 1812,
            min_os_version => '2.2'
        }, {
            adgroup_name => 'AdGroup#1', geo => '225',
            store_content_href => "http://www.amazon.com/gp/product/B009C7ZQM4/ref=mas_billboard",
            device_type_targeting => [],
            network_targeting => [], client_id => 1812,
            min_os_version => 'V10.23'            
        }],
        {adgroups_count => 10, adgroups_limit => 11}
    ),
    {
        generic_errors => vr_errors('LimitExceeded'),
        objects_results => [
            {min_os_version => vr_errors('InvalidFormat')},
            {store_content_href => vr_errors('UnsupportedStore'), device_type_targeting => vr_errors('ReqField'),
                network_targeting => vr_errors('ReqField')}
        ]
    }
);

cmp_validation_result(
    validate_update_mobile_adgroups(
        get_adgroups([{
            old => Direct::Model::AdGroupMobileContent->new(
                store_content_href => 'https://play.google.com/store/apps/details?id=com.aita' 
            ),
            adgroup_name => 'AdGroup#1', geo => '225',
            store_content_href => "https://play.google.com/store/apps/details?id=com.dama.paperartistgp",
            device_type_targeting => ['phone'],
            network_targeting => ['cell', 'wifi'], client_id => 1812,
            min_os_version => '5.0'
        }])
    ),
    [{store_content_href => vr_errors('CantChangeMobileContent')}]
);

ok_validation_result(
    validate_update_mobile_adgroups(
        get_adgroups([{
            # store_content_href разные, но ведут на одно и тоже приложение
            old => Direct::Model::AdGroupMobileContent->new(
                store_content_href => 'https://play.google.com/store/apps/details?id=com.squareenix.dxm&hl=ru&utm_campaign=direct_124'
            ),
            adgroup_name => 'AdGroup#1', geo => '225',
            store_content_href => "https://play.google.com/store/apps/details?id=com.squareenix.dxm&gl=ru",
            device_type_targeting => ['phone', 'tablet'],
            network_targeting => ['cell'], client_id => 1812,
            min_os_version => '5.0'
        }, {
            old => Direct::Model::AdGroupMobileContent->new(
                store_content_href => 'https://itunes.apple.com/app/garageband/id794957821'
            ),
            adgroup_name => 'AdGroup#1812', geo => '1',
            store_content_href => "https://itunes.apple.com/app/garageband/id794957821?fuz=baz&bar=foo",
            device_type_targeting => ['tablet'],
            network_targeting => ['cell', 'wifi'], client_id => 1812,
            min_os_version => '3.1'
        }])
    )
);

# страна по умолчанию для itunes - ru
cmp_validation_result(
    validate_update_mobile_adgroups(
        get_adgroups([{
            old => Direct::Model::AdGroupMobileContent->new(
                store_content_href => 'https://itunes.apple.com/us/app/roof-jumping-stunt-driving/id926243887?mt=8' 
            ),
            adgroup_name => 'AdGroup#1', geo => '225',
            store_content_href => "https://itunes.apple.com/app/roof-jumping-stunt-driving/id926243887?mt=900",
            device_type_targeting => ['phone'],
            network_targeting => ['cell'], client_id => 1812,
            min_os_version => '5.0'
        }])
    ),
    [{store_content_href => vr_errors('CantChangeMobileContent')}]
);



done_testing;

sub vr_add {
    my ($groups, $camp) = @_;    
    return validate_add_mobile_adgroups(get_adgroups($groups), get_camp($camp));
}

sub get_adgroups {
    my $groups = shift;
    
    my @group_objects;
    my $default_camp = get_camp();
    for my $group (ref $groups eq 'ARRAY' ? @$groups : $groups) {
        my @banners = map {
            Direct::Model::BannerMobileContent->new(%$_)
        } @{$group->{banners} || []};
        delete $group->{banners};
        my $adgroup = Test::Direct::Model::AdGroupMobileContent->new(
            %$group,
            banners => \@banners,
        );
        $adgroup->campaign($default_camp) if !$adgroup->has_campaign;
        push @group_objects, $adgroup;
    }
    return \@group_objects;
}

sub get_camp {
    my $camp = shift;    
    return Test::Direct::Model::Campaign->new(
        adgroups_count => $camp->{adgroups_count} || 0,
        adgroups_limit => $camp->{adgroups_limit} || $Settings::DEFAULT_BANNER_COUNT_LIMIT
    );      
}
