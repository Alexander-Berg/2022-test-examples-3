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
use BannerFlags qw//;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN { use_ok('Models::Banner'); }

local $Yandex::DBShards::IDS_LOG_FILE = undef;

*create_base_banners = \&Models::Banner::create_base_banners;
{
    no warnings 'redefine';
    *Models::Banner::_on_base_banners_created = sub {};
}

*flags2hash = sub {
     BannerFlags::get_banner_flags_as_hash(shift, all_flags => 1);
};

init_test_dataset(db_data());

my $uid = 12519399;

throws_ok {
    create_base_banners([{title => 'title'}], $uid);
} qr/unknown banner type/;

my $banners_1 = [
    {
        cid => 11, pid => 900, banner_type => 'desktop',
        title => 'title1', body => 'body1', href => 'http://ya.ru',
        domain => 'ya.ru', statusEmpty => 'No', statusModerate => 'Yes',
        flags => flags2hash('age:18,distance_sales,plus18'), geo => '225',
        yaca_rough_list => '27,101000027',
        apart => undef,
        build => undef,
        city => "Москва",
        map => {aid => 300},
        contact_email => 'info@zakazlinz.ru',
        contactperson => undef,
        country => "Россия",
        extra_message => undef,
        house => undef,
        im_client => undef,
        im_login => undef,
        metro => undef,
        name => "Заказлинз.ру",
        org_details_id => undef,
        country_code => "+7",
        city_code => "495",
        phone => "649-60-57",
        ext => "",
        street => undef,
        worktime => '0#4#10#30#20#30;5#6#11#00#19#00',
    }
];
# активный баннер с визиткой
$banners_1 = create_base_banners($banners_1, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_1],
    [{
        title => 'title1', body => 'body1', href => 'http://ya.ru',
        domain => 'ya.ru', reverse_domain => 'ur.ay',
        banner_type => 'desktop', pid => 900, cid => 11,
        flags => flags2hash('age:18,distance_sales,plus18'),
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'Ready', opts => '',
        camp_statusModerate => 'Ready',
        address_id => 300,
        apart => undef,
        build => undef,
        city => "Москва",
        geo_id => 213,
        contact_email => 'info@zakazlinz.ru',
        contactperson => undef,
        country => "Россия",
        extra_message => undef,
        house => undef,
        uid => $uid,
        im_client => undef,
        im_login => undef,
        metro => undef,
        name => "Заказлинз.ру",
        org_details_id => undef,
        phone => '+7#495#649-60-57#',
        street => undef,
        worktime => '0#4#10#30#20#30;5#6#11#00#19#00',
        # домен задали при вызове, проверки домена не будет
        redirect_check => 0
    }],
    "banner 1 check",
);

# баннер черновик
my $banners_2 = [
    {
        cid => 12, pid => 901, banner_type => 'desktop',
        title => 'title2', body => 'body2', href => 'http://remont-okon-ekb.ru',
        domain => 'remont-okon-ekb.ru', statusEmpty => 'No', statusModerate => 'New',
        geo => '54'
    },
    {
        cid => 12, pid => 901, banner_type => 'mobile',
        title => 'title3', body => 'body3', href => 'http://m.remont-okon-ekb.ru',
        domain => 'remont-okon-ekb.ru', statusEmpty => 'No', statusModerate => 'New',
        geo => '54'
    },
    {
        cid => 12, pid => 901, banner_type => 'mobile',
        title => 'title4', body => 'body4', href => 'http://m.remont-okon-ekb.ru',
        statusEmpty => 'No', statusModerate => 'New',
        geo => '54'
    }
    
];
$banners_2 = create_base_banners($banners_2, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_2],
    [{
        title => 'title2', body => 'body2', href => 'http://remont-okon-ekb.ru',
        domain => 'remont-okon-ekb.ru', reverse_domain => 'ur.bke-noko-tnomer',
        banner_type => 'desktop', pid => 901, cid => 12,
        flags => flags2hash(undef),
        statusPostModerate => 'No', statusModerate => 'New',
        phoneflag => 'New', opts => '',
        camp_statusModerate => 'No',
        redirect_check => 0
    }, {
        title => 'title3', body => 'body3', href => 'http://m.remont-okon-ekb.ru',
        domain => 'remont-okon-ekb.ru', reverse_domain => 'ur.bke-noko-tnomer',
        banner_type => 'mobile', pid => 901, cid => 12,
        flags => flags2hash(undef),
        statusPostModerate => 'No', statusModerate => 'New',
        phoneflag => 'New', opts => '',
        camp_statusModerate => 'No',
        redirect_check => 0
    }, {
        # домен вычислиться из href
        # href на простукивание не поставим (это не домен счетчик)
        title => 'title4', body => 'body4', href => 'http://m.remont-okon-ekb.ru',
        domain => 'm.remont-okon-ekb.ru', reverse_domain => 'ur.bke-noko-tnomer.m',
        banner_type => 'mobile', pid => 901, cid => 12,
        flags => flags2hash(undef),
        statusPostModerate => 'No', statusModerate => 'New',
        phoneflag => 'New', opts => '',
        camp_statusModerate => 'No',
        redirect_check => 0
    }],
    "banners 2 check",
);

# в "пустую" кампанию добавляем "активный" баннер
my $banners_3 = [
    {
        cid => 13, pid => 903, banner_type => 'desktop',
        title => 'title3_1', body => 'body3_1', href => 'https://www.zakazlinz.ru',
        domain => 'www.zakazlinz.ru', statusEmpty => 'Yes', statusModerate => 'Ready',
        geo => '1', flags => flags2hash('distance_sales,medicine')
    },
    {
        cid => 13, pid => 903, banner_type => 'desktop',
        title => 'title3_2', body => 'body3_2',
        statusEmpty => 'Yes', statusModerate => 'No',
        geo => '1',
        # должны использовать существующую визитку
        apart => undef,
        build => undef,
        city => "Москва",
        contact_email => 'info@zakazlinz.ru',
        contactperson => undef,
        country => "Россия",
        extra_message => undef,
        house => undef,
        im_client => undef,
        im_login => undef,
        metro => undef,
        name => "Заказлинз.ру",
        org_details_id => undef,
        country_code => "+7",
        city_code => "495",
        phone => "649-60-57",
        ext => "",        
        street => undef,
        worktime => '0#4#10#30#20#30;5#6#11#00#19#00',
        geo_id => 213,
        address_id => 300 
    }
];
$banners_3 = create_base_banners($banners_3, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_3],
    [{
        title => 'title3_1', body => 'body3_1', href => 'https://www.zakazlinz.ru',
        domain => 'www.zakazlinz.ru', reverse_domain => 'ur.znilzakaz.www',
        banner_type => 'desktop', pid => 903, cid => 13,
        flags => flags2hash('distance_sales,medicine'),
        statusPostModerate => 'No', statusModerate => 'New',
        phoneflag => 'New', opts => '',
        camp_statusModerate => 'No',
        redirect_check => 0
    }, {
        title => 'title3_2', body => 'body3_2',
        href => undef, domain => undef, reverse_domain => undef,
        banner_type => 'desktop', pid => 903, cid => 13,
        flags => flags2hash(undef),
        statusPostModerate => 'No', statusModerate => 'New',
        phoneflag => 'New', opts => '',
        camp_statusModerate => 'No',
        # vcard
        geo_id => 213,
        address_id => 300,
        uid => $uid,
        apart => undef,
        build => undef,
        city => "Москва",
        contact_email => 'info@zakazlinz.ru',
        contactperson => undef,
        country => "Россия",
        extra_message => undef,
        house => undef,
        im_client => undef,
        im_login => undef,
        metro => undef,
        name => "Заказлинз.ру",
        org_details_id => undef,
        phone => '+7#495#649-60-57#',
        street => undef,
        worktime => '0#4#10#30#20#30;5#6#11#00#19#00',
        redirect_check => 0
    }],
    "banners 3 check",
);
ok(1 == get_one_field_sql(PPC(cid => 13), "SELECT COUNT(*) FROM vcards WHERE cid = ? AND uid = ?", 13, $uid));

# группа только из мобильных баннеров (добавление в разные группы)
my $banners_4 = [
    {
        cid => 14, pid => 919, banner_type => 'mobile',
        # домен считаем из href
        title => 'title4_1', body => 'body4_1', href => 'https://www.lex-s.ru',
        statusEmpty => 'No', statusModerate => 'Ready',
        geo => '159,225'
    },
    {
        cid => 14, pid => 983, banner_type => 'mobile',
        title => 'title4_2', body => 'body4_2', href => 'https://www.lex-s.ru',
        domain => 'lex-s.ru', statusEmpty => 'No', statusModerate => 'Ready',
        geo => '159,225',
        # создание новой визитки 
        'apart' => '611',
        'build' => undef,
        'city' => "Москва",
        'contact_email' => undef,
        'contactperson' => undef,
        'country' => "Россия",
        'extra_message' => undef,
        'house' => '1',
        'im_client' => undef,
        'im_login' => undef,
        'metro' => '20386',
        'name' => "Группа компаний \"Лекс\"",
        'org_details_id' => undef,
        country_code => "+7",
        city_code => "495",
        phone => "787-35-55",
        ext => "",        
        'street' => "проезд Дежнева",
        'worktime' => '0#4#9#00#18#00',
        address_id => 819,
        geo_id => 225 
    },
    {
        cid => 14, pid => 983, banner_type => 'mobile',
        title => 'title4_3', body => 'body4_3',
        statusEmpty => 'No', statusModerate => 'New',
        geo => '159,225',
        # использование существующей визитки
        'apart' => '611',
        'build' => undef,
        'city' => "Москва",
        'contact_email' => undef,
        'contactperson' => undef,
        'country' => "Россия",
        'extra_message' => undef,
        'house' => '1',
        'im_client' => undef,
        'im_login' => undef,
        'metro' => '20386',
        'name' => "Группа компаний \"Лекс\"",
        'org_details_id' => undef,
        country_code => "+7",
        city_code => "495",
        phone => "787-35-55",
        ext => "",        
        'street' => "проезд Дежнева",
        'worktime' => '0#4#9#00#18#00',
        address_id => 819,
        geo_id => 225 
    },
];
$banners_4 = create_base_banners($banners_4, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_4],
    [{
        title => 'title4_1', body => 'body4_1', href => 'https://www.lex-s.ru',
        domain => 'www.lex-s.ru', reverse_domain => 'ur.s-xel.www',
        banner_type => 'mobile', pid => 919, cid => 14,
        flags => flags2hash(undef),
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'New', opts => '',
        camp_statusModerate => 'Ready',
        redirect_check => 0
    }, {
        title => 'title4_2', body => 'body4_2', href => 'https://www.lex-s.ru',
        domain => 'lex-s.ru', reverse_domain => 'ur.s-xel',
        banner_type => 'mobile', pid => 983, cid => 14,
        flags => flags2hash(undef),
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'Ready', opts => '',
        camp_statusModerate => 'Ready',
        # vcard
        uid => $uid,
        address_id => 819,
        geo_id => 225,
        'apart' => '611',
        'build' => undef,
        'city' => "Москва",
        'contact_email' => undef,
        'contactperson' => undef,
        'country' => "Россия",
        'extra_message' => undef,
        'house' => '1',
        'im_client' => undef,
        'im_login' => undef,
        'metro' => '20386',
        'name' => "Группа компаний \"Лекс\"",
        'org_details_id' => undef,
        'phone' => '+7#495#787-35-55#',
        'street' => "проезд Дежнева",
        'worktime' => '0#4#9#00#18#00',
        redirect_check => 0
    }, {
        title => 'title4_3', body => 'body4_3', href => undef,
        domain => undef, reverse_domain => undef,
        banner_type => 'mobile', pid => 983, cid => 14,
        flags => flags2hash(undef),
        statusPostModerate => 'No', statusModerate => 'New',
        phoneflag => 'New', opts => '',
        camp_statusModerate => 'Ready',
        # vcard
        uid => $uid,
        address_id => 819,
        geo_id => 225, 
        'apart' => '611',
        'build' => undef,
        'city' => "Москва",
        'contact_email' => undef,
        'contactperson' => undef,
        'country' => "Россия",
        'extra_message' => undef,
        'house' => '1',
        'im_client' => undef,
        'im_login' => undef,
        'metro' => '20386',
        'name' => "Группа компаний \"Лекс\"",
        'org_details_id' => undef,
        'phone' => '+7#495#787-35-55#',
        'street' => "проезд Дежнева",
        'worktime' => '0#4#9#00#18#00',
        redirect_check => 0
    }],
    "banners 4 check",
);
# создали только одну визитку
ok(1 == get_one_field_sql(PPC(cid => 14), "SELECT COUNT(*) FROM vcards WHERE cid = ? AND uid = ?", 14, $uid));


my $banners_5 = [{
    banner_type => 'mobile', pid => 983, cid => 14,
    body       => "Вскрытие сейфов. Перекодировка. Замена замков. Более двух сейфов - скидка.",
    title      => "Ремонт сейфов и замков.",
    domain     => "zamki-look.com",
    href       => "https://m.mazter-x.ru/category/7",
    statusEmpty => 'No', statusModerate => 'No',
    yaca_rough_list => "7,333,103"
}];
$banners_5 = create_base_banners($banners_5, $uid);   
cmp_banner_rows(
    [map {$_->{bid}} @$banners_5],
    [{        
        banner_type => 'mobile', pid => 983, cid => 14,
        body       => "Вскрытие сейфов. Перекодировка. Замена замков. Более двух сейфов - скидка.",
        title      => "Ремонт сейфов и замков.",
        domain     => "zamki-look.com",
        href       => "https://m.mazter-x.ru/category/7",
        reverse_domain => "moc.kool-ikmaz",
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'New', camp_statusModerate => 'Ready',
        flags => flags2hash(undef),
        redirect_check => 0, opts => '',
    }],
    "banners 5 check"    
);


my $banners_6 = [
# баннеры с href счетчиками (будем ставить в очередь на редиректы)
{
    banner_type => 'desktop', pid => 983, cid => 14,
    body       => "Вскрытие сейфов. Перекодировка. Замена замков. Более двух сейфов - скидка.",
    title      => "Ремонт сейфов и замков.",
    href       => "https://ad.atdmt.com/s/go?adv=11087200871573",
    statusEmpty => 'No', statusModerate => 'No',
    yaca_rough_list => "7,333,103"
}, 
# баннеры с href счетчиками (не будем ставить в очередь на редиректы т.к. domain передали при вызове)
{
    banner_type => 'desktop', pid => 983, cid => 14,
    body       => "Вскрытие сейфов.",
    title      => "Ремонт сейфов и замков.",
    domain     => 'events.microsoft.ru',
    href       => "https://ad.atdmt.com/s/go?adv=11087200871573",
    statusEmpty => 'No', statusModerate => 'No',
    yaca_rough_list => "7,333,103"
}
];
$banners_6 = create_base_banners($banners_6, $uid);   
cmp_banner_rows(
    [map {$_->{bid}} @$banners_6],
    [{        
        banner_type => 'desktop', pid => 983, cid => 14,
        body       => "Вскрытие сейфов. Перекодировка. Замена замков. Более двух сейфов - скидка.",
        title      => "Ремонт сейфов и замков.",
        domain     => "ad.atdmt.com",
        href       => "https://ad.atdmt.com/s/go?adv=11087200871573",
        reverse_domain => "moc.tmdta.da",
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'New', camp_statusModerate => 'Ready',
        flags => flags2hash(undef),
        redirect_check => 1, opts => '',
    },{        
        banner_type => 'desktop', pid => 983, cid => 14,
        body       => "Вскрытие сейфов.",
        title      => "Ремонт сейфов и замков.",
        domain     => "events.microsoft.ru",
        href       => "https://ad.atdmt.com/s/go?adv=11087200871573",
        reverse_domain => "ur.tfosorcim.stneve",
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'New', camp_statusModerate => 'Ready',
        flags => flags2hash(undef),
        redirect_check => 0, opts => '',
    }],
    "banners 6 check"    
);


done_testing;

# в сравнении полагаемся что записи добавляются в БД последовательно (нет сортировки $expected)
sub cmp_banner_rows {
    
    my ($bids, $expected, $name) = @_;
    
    my @vc_fields = grep {$_ ne 'cid'} @$VCards::VCARD_FIELDS_DB;
    my $vcard_fields = join ',', map {"vc.$_"} @vc_fields;
    my $got_banners = get_all_sql(PPC(shard => 'all'), [
        "SELECT
                b.type AS banner_type, b.flags, b.title, b.body,
                b.href, b.domain, b.reverse_domain,
                b.pid, b.cid, b.statusPostModerate, b.statusModerate,
                b.phoneflag, b.opts,
                c.statusModerate AS camp_statusModerate, b.vcard_id,
                IF(rcq.object_id IS NOT NULL, 1, 0) AS redirect_check,
                $vcard_fields
            FROM banners b
                JOIN campaigns c ON b.cid = c.cid
                LEFT JOIN vcards vc ON b.vcard_id = vc.vcard_id
                LEFT JOIN redirect_check_queue rcq ON b.bid = rcq.object_id AND rcq.object_type = 'banner'",
            WHERE => {'b.bid' => $bids},
        "ORDER BY bid"
    ]);
    
    foreach my $banner (@$got_banners) {
        $banner->{flags} = flags2hash($banner->{flags});
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
                    { cid => 11, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'No' },
                    { cid => 12, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'No' },
                    { cid => 13, uid => 12519399, type => 'text', statusEmpty => 'Yes', statusModerate => 'No' },
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
        } qw/shard_inc_pid shard_inc_bid shard_inc_vcard_id  mirrors_correction mirrors/),
        
        trusted_redirects => {original_db => PPCDICT, rows => [
            {domain => 'atdmt.com', redirect_type => 'counter'}
        ]},
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
        ppc_properties => {
            original_db => PPCDICT,
            rows => [],
        },
    }
}
