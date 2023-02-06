#!/usr/bin/perl

use strict;
use warnings;
use utf8;
use open ':std' => ':utf8';

use Test::More;
use Test::Deep;
use Test::Exception;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use Settings;

BEGIN { use_ok('Models::MobileBanner', 'update_mobile_banners'); }
{
    no warnings 'redefine';
    *Models::Banner::_on_base_banners_updated = sub {};
}

my $uid = 12519399;
init_test_dataset(db_data());

my $banners_1 = [{
    bid => 1003,
}, {
    bid => 1002,
}];
throws_ok {
    update_mobile_banners($banners_1, $uid);
} qr/can't change banner type/;

my $banners_2 = [
    {
        bid            => 147318749, cid => 2003, pid => 587,
        body           => "Цифровое оборудование для печати по рулонным тканям и готовым изделиям.",
        title          => "Принтеры для ткани",
        domain         => "текстильный-принтер.рф",
        href           => "текстильный-принтер.рф/",
        statusEmpty => 'No', statusModerate => "Yes", 

        address_id => 2075353,
        apart => undef,
        build => undef,
        city  => "Москва",
        contact_email => 'sale@dtg-printer.ru',
        contactperson => undef,
        country => "Россия",
        extra_message => undef,
        geo_id => 213,
        house => undef,
        im_client => undef,
        im_login => undef,
        metro => 20476,
        name  => "ЭнЭксДжет",
        org_details_id => 216946,
        phone => "+7#495#225-99-00#",
        country_code => "+7",
        city_code => "495",
        phone => "225-99-00",
        ext => "",
        street => "Танковый проезд дом 4 строение 42",
        worktime => "0#4#10#00#20#00"
    }
];
$banners_2 = update_mobile_banners($banners_2, $uid);
cmp_banner_rows(
    [map { $_->{bid} } @$banners_2],
    [
        {
            bid            => 147318749, cid => 2003, pid => 587, type => 'mobile',
            body           => "Цифровое оборудование для печати по рулонным тканям и готовым изделиям.",
            title          => "Принтеры для ткани",
            domain         => "текстильный-принтер.рф", reverse_domain => "фр.ретнирп-йыньлитскет",
            href           => "http://текстильный-принтер.рф/",
            flags          => "",
            statusModerate => 'Ready', statusPostModerate => 'No',
            camp_statusModerate => 'Sent', statusSitelinksModerate => 'New',
            
            phoneflag => 'Ready',
            address_id => 2075353,
            apart => undef,
            build => undef,
            city  => "Москва",
            contact_email => 'sale@dtg-printer.ru',
            contactperson => undef,
            country => "Россия",
            extra_message => undef,
            geo_id => 213,
            house => undef,
            im_client => undef,
            im_login => undef,
            metro => 20476,
            name  => "ЭнЭксДжет",
            org_details_id => 216946,
            phone => "+7#495#225-99-00#",
            street => "Танковый проезд дом 4 строение 42",
            worktime => "0#4#10#00#20#00",
            uid => $uid,
        }
    ]
);

# новых визиток не создали
ok(1 == get_one_field_sql(PPC(cid => 2003), "SELECT COUNT(*) FROM vcards WHERE cid = ? AND uid = ?", 2003, $uid));
    
done_testing;

sub cmp_banner_rows {
    
    my ($bids, $expected, $name) = @_;
    
    my @vc_fields = grep {$_ ne 'cid'} @$VCards::VCARD_FIELDS_DB;
    my $vcard_fields = join ',', map {"vc.$_"} @vc_fields;
    my $got_banners = get_all_sql(PPC(shard => 'all'), [
        "SELECT
                b.bid, b.type, b.flags, b.title, b.body,
                b.href, b.domain, b.reverse_domain,
                b.pid, b.cid, b.statusPostModerate, b.statusModerate,
                b.phoneflag,
                c.statusModerate AS camp_statusModerate, b.vcard_id,
                $vcard_fields,
                b.statusSitelinksModerate
            FROM banners b
                JOIN campaigns c ON b.cid = c.cid
                LEFT JOIN vcards vc ON b.vcard_id = vc.vcard_id",
            WHERE => {'b.bid' => $bids},
        "ORDER BY b.bid"
    ]);
    
    foreach my $banner (@$got_banners) {
        unless (delete $banner->{vcard_id}) {
            delete @{$banner}{@vc_fields};
        }
    }
    
    cmp_deeply($got_banners, $expected, $name);
}


sub db_data {
    
    {
        banners_to_fill_language_queue => {
            original_db => PPC(shard => 'all'),
            rows => {}
        },
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
                    { cid => 11, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'Yes' },
                    { cid => 12, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'No' },
                    { cid => 13, uid => 12519399, type => 'text', statusEmpty => 'Yes', statusModerate => 'No' },
                    { cid => 14, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'Ready' },
                    { cid => 8991, uid => 12519399, type => 'text', statusEmpty => 'Yes', statusModerate => 'New' },
                    { cid => 2003, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'Sent' }
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
                    
                    { pid => 600, cid => 8991, statusModerate => 'New' },
                    { pid => 587, cid => 2003, statusModerate => 'Yes' },
                ],
            },
        },
        banners => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { bid => 1002, type => 'mobile', pid => 900, cid => 11, statusModerate => 'New' },
                    { bid => 1003, type => 'desktop', pid => 901, cid => 11, statusModerate => 'New', flags => "age:18,plus18,software" },
                    
                    {
                        bid            => 147318749, cid => 2003, pid => 587, type => 'mobile',
                        body           => "Принтеры для прямой печати по текстилю, рулонам и готовым изделиям.",
                        domain         => "текстильный-принтер.рф",
                        flags          => "",
                        href           => "текстильный-принтер.рф/",
                        phoneflag      => "Yes",
                        vcard_id => 6346030,
                        reverse_domain => "фр.ретнирп-йыньлитскет",
                        sitelinks_set_id => undef,
                        statusModerate => "Yes",
                        statusPostModerate => "Yes",
                        statusSitelinksModerate => "New",
                        title          => "Все для печати по текстилю.",
                    }
                ],
            },
        },
        banner_images => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
            },
        },
        banner_display_hrefs => {
            original_db => PPC(shard => 'all'),
            rows => {},
        },
        vcards => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                {
                    vcard_id => 6346030,
                    address_id => 2075353,
                    apart => undef,
                    build => undef,
                    cid   => 2003,
                    city  => "Москва",
                    contact_email => 'sale@dtg-printer.ru',
                    contactperson => undef,
                    country => "Россия",
                    extra_message => undef,
                    geo_id => 213,
                    house => undef,
                    im_client => undef,
                    im_login => undef,
                    metro => 20476,
                    name  => "ЭнЭксДжет",
                    org_details_id => 216946,
                    phone => "+7#495#225-99-00#",
                    street => "Танковый проезд дом 4 строение 42",
                    uid   => $uid,
                    worktime => "0#4#10#00#20#00"
                }
                ]
            },
        },
        (map {
            $_ => {original_db => PPC(shard => 'all'), rows => []}
        } qw/org_details post_moderate auto_moderate moderation_cmd_queue mod_edit filter_domain aggregator_domains addresses maps redirect_check_queue
             banners_additions additions_item_callouts
            /),
        moderation_cmd_queue => {original_db => PPC(shard => 'all'), rows => []},
        filter_domain => {original_db => PPC(shard => 'all'), rows => []},
        geo_regions => {
            original_db => PPCDICT,
            rows => [
                {region_id => 213, name => 'Москва'}
            ],
        },
        
        (map {
            $_ => {original_db => PPCDICT, rows => []}
        } qw/shard_inc_pid shard_inc_vcard_id inc_sl_id shard_inc_banner_images_pool_id trusted_redirects mirrors mirrors_correction/),
        shard_inc_bid => {
            original_db => PPCDICT,
            rows => [
                { bid => 1002, ClientID => 338556 },
                { bid => 1003, ClientID => 338556 },
                { bid => 147318749, ClientID => 338556 },
            ],
        },
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
                { cid => 8991, ClientID => 338556 },
                { cid => 2003, ClientID => 338556 },
            ],
        },
        shard_uid => {
            original_db => PPCDICT,
            rows => [
                { uid => $uid, ClientID => 338556 },
            ],
        },
    }
}    
