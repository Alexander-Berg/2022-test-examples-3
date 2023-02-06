#!/usr/bin/perl

use strict;
use warnings;
use utf8;
use open ':std' => ':utf8';

use Test::More;
use Test::Deep;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use Settings;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN { use_ok('Models::MobileBanner', 'create_mobile_banners'); }
{
    no warnings 'redefine';
    *Models::Banner::_on_base_banners_created = sub {};
}

init_test_dataset(db_data());

my $uid = 12519399;

my $banners_1 = [
    {
        pid => 901, cid => 11,
        body           => "Широкий выбор! Наличие! Гарантия! Звоните, о цене договоримся! Дилер",
        title          => "Бортовой МАЗ в Наличии! Звоните",
        geo => '3',
        yaca_rough_list => "7,66",
        statusEmpty => 'No',
        statusModerate => 'New', 
        
        # vcard
        address_id => 2228809,
        apart => undef,
        build => 1,
        city  => "Москва",
        contact_email => 'info@avtodin.ru',
        contactperson => undef,
        country => "Россия",
        extra_message => "Продажа грузовых автомобилей, специальной техники и тракторов со стоянки в Москве.",
        house => 41,
        im_client => undef,
        im_login => undef,
        metro => 0,
        name  => "Автодин",
        org_details_id => undef,
        country_code => "+7",
        city_code => "495",
        phone => "589-15-45",
        ext => "",        
        street => "Полярная",
        worktime => "0#4#9#00#18#00"
    },
    {
        pid => 903, cid => 13,
        body           => "Скидки! Наличие! Гарантия! Звоните, о цене договоримся! Официальный дилер!",
        title          => "Самосвалы МАЗ в Наличии! Звоните",
        domain         => "www.avtodin.ru",
        geo            => 3,
        href           => "www.avtodin.ru/auto/brands_auto/auto1/350/",
        statusEmpty => 'No',
        statusModerate => "Ready",
    }
    
     
    #{}
];
$banners_1 = create_mobile_banners($banners_1, $uid);
cmp_banner_rows(
    [map { $_->{bid} } @$banners_1],
    [
        {
            pid => 901, cid => 11, type => 'mobile',
            body           => "Широкий выбор! Наличие! Гарантия! Звоните, о цене договоримся! Дилер",
            title          => "Бортовой МАЗ в Наличии! Звоните",
            domain => undef, reverse_domain => undef, href => undef,
            flags          => undef,
            opts => '',
            statusModerate => 'New', statusPostModerate => 'No',
            camp_statusModerate => 'New',
            
            phoneflag => 'New',
            address_id => 2228809,
            apart => undef,
            build => 1,
            city  => "Москва",
            contact_email => 'info@avtodin.ru',
            contactperson => undef,
            country => "Россия",
            extra_message => "Продажа грузовых автомобилей, специальной техники и тракторов со стоянки в Москве.",
            geo_id => 213,
            house => 41,
            im_client => undef,
            im_login => undef,
            metro => 0,
            name  => "Автодин",
            org_details_id => undef,
            phone => "+7#495#589-15-45#",
            street => "Полярная",
            uid => $uid,
            worktime => "0#4#9#00#18#00"
        }, 
        {
            pid => 903, cid => 13, type => 'mobile',
            body           => "Скидки! Наличие! Гарантия! Звоните, о цене договоримся! Официальный дилер!",
            title          => "Самосвалы МАЗ в Наличии! Звоните",
            domain         => "www.avtodin.ru", reverse_domain => "ur.nidotva.www",
            href           => "http://www.avtodin.ru/auto/brands_auto/auto1/350/",
            flags          => undef,
            opts => '',
            statusModerate => 'Ready', statusPostModerate => 'No',
            phoneflag => 'New',
            camp_statusModerate => 'Yes', 
        }
    ]
);


done_testing;

sub cmp_banner_rows {
    
    my ($bids, $expected, $name) = @_;
    
    my @vc_fields = grep {$_ ne 'cid'} @$VCards::VCARD_FIELDS_DB;
    my $vcard_fields = join ',', map {"vc.$_"} @vc_fields;
    my $got_banners = get_all_sql(PPC(shard => 'all'), [
        "SELECT
                b.type, b.flags, b.title, b.body,
                b.href, b.domain, b.reverse_domain,
                b.pid, b.cid, b.statusPostModerate, b.statusModerate,
                b.phoneflag, b.opts,
                c.statusModerate AS camp_statusModerate, b.vcard_id,
                $vcard_fields
            FROM banners b
                JOIN campaigns c ON b.cid = c.cid
                LEFT JOIN vcards vc ON b.vcard_id = vc.vcard_id",
            WHERE => {'b.bid' => $bids},
        "ORDER BY bid"
    ]);
    
    foreach my $banner (@$got_banners) {
        unless (delete $banner->{vcard_id}) {
            delete @{$banner}{@vc_fields};
            next;
        }
    }
    
    cmp_deeply($got_banners, $expected, $name);
}


sub db_data {
    
    {
        users => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {uid => 12519399, ClientID => 338556}
                ],
            }
        },
        clients => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {ClientID => 338556, country_region_id => 225}
                ],
            }
        },
        campaigns => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { cid => 11, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'New' },
                    { cid => 12, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'No' },
                    { cid => 13, uid => 12519399, type => 'text', statusEmpty => 'Yes', statusModerate => 'Yes' },
                    { cid => 14, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'Ready' },
                ],
            },
        },
        phrases => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { pid => 900, cid => 11, statusModerate => 'Yes' },
                    { pid => 901, cid => 12, statusModerate => 'New' },
                    { pid => 903, cid => 13, statusModerate => 'New' },
                    
                    { pid => 919, cid => 14, statusModerate => 'Yes' },
                    { pid => 983, cid => 14, statusModerate => 'Yes' },
                ],
            },
        },
        banners => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
            },
        },
        vcards => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
            },
        },
        filter_domain => {original_db => PPC(shard => 'all'), rows => []},
        aggregator_domains => {original_db => PPC(shard => 'all'), rows => []},
        redirect_check_queue => {original_db => PPC(shard => 'all'), rows => []},
        geo_regions => {
            original_db => PPCDICT,
            rows => [
                {region_id => 213, name => 'Москва'}
            ],
        },
        
        (map {
            $_ => {original_db => PPCDICT, rows => []}
        } qw/shard_inc_pid shard_inc_bid shard_inc_vcard_id trusted_redirects mirrors mirrors_correction/),
        shard_client_id => {
            original_db => PPCDICT,
            rows => [
                { ClientID => 338556, shard => 1 },
            ],
        },
        shard_inc_cid => {
            original_db => PPCDICT,
            rows => [
                { cid => 11, ClientID => 338556 },
                { cid => 12, ClientID => 338556 },
                { cid => 13, ClientID => 338556 },
                { cid => 14, ClientID => 338556 },
            ],
        },
        shard_uid => {
            original_db => PPCDICT,
            rows => [
                { uid => 12519399, ClientID => 338556 },
            ],
        },
    }
}
