#!/usr/bin/perl

use strict;
use warnings;
use utf8;
use open ':std' => ':utf8';

use Test::More;
use Test::Deep;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use Yandex::HashUtils;
use Sitelinks qw//;
use Settings;
use BannerFlags qw//;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN { use_ok('Models::DesktopBanner', 'create_desktop_banners'); }

local $Yandex::DBShards::IDS_LOG_FILE = undef;

{
    no warnings 'redefine';
    *Models::Banner::_on_base_banners_created = sub {};
}

*flags2hash = sub {
     BannerFlags::get_banner_flags_as_hash(shift, all_flags => 1);
};


my $uid = 12519399;
init_test_dataset(db_data());

# баннер с визиткой, картинкой, сайтлинками
my $banners_1 = [{
    cid => 14, pid => 983,
    body       => "Доставка. Монтаж. Ремонт.",
    title      => "Турникет в проходную - от 29.000",
    domain     => "www.shelni.ru",
    flags      => flags2hash("distance_sales"),
    href       => "www.shelni.ru/price/nedorogie_turnikety_tver_s_dostavkoi_do_habarovska/",
    sitelinks  => [
        {
            href => "http://www.shelni.ru/price/turnikety_oma_spb/?_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7",
            title => "ОМА"
        },
        {
            href => "http://www.shelni.ru/price/turnikety_perco_spb/?_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7",
            title => "PERCo"
        },
        {
            href => "http://www.shelni.ru/price/turnikety_sibirskii_arsenal/?_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7",
            title => "Сибирский Арсенал"
        },
        {
            href => "http://www.shelni.ru/price/turnikety_tripody_s_dostavkoi_po_rossii/?_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7",
            title => "Трипод"
        }
    ],
    geo => '225',
    statusModerate => "Yes",
    statusEmpty => 'No',
    image => 'PKDj82-BV1na3aqcWGQa3w',
    
    # vcard
    address_id => 0,
    apart => undef,
    build => undef,
    city  => "Москва",
    contact_email => 'zt@z-tec.ru',
    contactperson => undef,
    country => "Россия",
    extra_message => undef,
    house => undef,
    im_client => undef,
    im_login => undef,
    metro => undef,
    name  => "Зет-Техно",
    org_details_id => 112272,
    country_code => "+7",
    city_code => "495",
    phone => "734-99-57",
    ext => "",        
    street => undef,
    worktime => "0#4#8#30#18#00"
}];
$banners_1 = create_desktop_banners($banners_1, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_1],
    [{
        cid => 14, pid => 983, type => 'desktop',
        body       => "Доставка. Монтаж. Ремонт.",
        title      => "Турникет в проходную - от 29.000",
        domain     => "www.shelni.ru", reverse_domain => "ur.inlehs.www",
        flags      => flags2hash("distance_sales"),
        opts => '',
        href       => "http://www.shelni.ru/price/nedorogie_turnikety_tver_s_dostavkoi_do_habarovska/",
        sitelinks  => [
            {
                href => "http://www.shelni.ru/price/turnikety_oma_spb/?_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7",
                title => "ОМА"
            },
            {
                href => "http://www.shelni.ru/price/turnikety_perco_spb/?_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7",
                title => "PERCo"
            },
            {
                href => "http://www.shelni.ru/price/turnikety_sibirskii_arsenal/?_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7",
                title => "Сибирский Арсенал"
            },
            {
                href => "http://www.shelni.ru/price/turnikety_tripody_s_dostavkoi_po_rossii/?_openstat=dGVzdDsxOzE7&_openstat=dGVzdDsxOzE7",
                title => "Трипод"
            }
        ],
        statusSitelinksModerate => 'Ready',
        statusModerate => "Ready", statusPostModerate => 'No',
        camp_statusModerate => 'Ready',
        image_hash => 'PKDj82-BV1na3aqcWGQa3w',
        bi_statusModerate => 'Ready',

        # vcard
        phoneflag => 'Ready',
        address_id => undef,
        apart => undef,
        build => undef,
        city  => "Москва",
        contact_email => 'zt@z-tec.ru',
        contactperson => undef,
        country => "Россия",
        extra_message => undef,
        geo_id => 213,
        house => undef,
        im_client => undef,
        im_login => undef,
        metro => undef,
        name  => "Зет-Техно",
        org_details_id => 112272,
        phone => "+7#495#734-99-57#",
        street => undef,
        uid   => $uid,
        worktime => "0#4#8#30#18#00"
    }],
    "banner 1 check",
);

my $banners_2 = [{
    cid => 13, pid => 903,
    body       => "Hoffer Flow Controls. Типоразмеры от 3/4'' до 12'', расходы до 60000 л/мин",
    title      => "Турбинные расходомеры",
    domain     => "www.massflow.ru",
    href       => "https://www.massflow.ru/catalog/massflow/hoffer/liquid/",
    yaca_rough_list => '7',
    geo => '187',
    statusModerate => "New",
    statusEmpty => 'No',
    image => 'VU54gPxDWHRfUaazpRiA-w'
}, {
    cid => 13, pid => 903,
    body       => "Срочные языковые переводы любой сложности. Большие объемы. Редкие языки.",
    title      => "Бюро переводов в Москве",
    domain     => "www.doriangrey.ru",
    href       => "http://www.doriangrey.ru",
    sitelinks  => [
        {
            href => "http://doriangrey.ru/about.htm",
            title => 'Агентство "Dorian Grey".'
        },
        {
            href => "http://doriangrey.ru/service.htm",
            title => "Услуги."
        },
        {
            href => "http://doriangrey.ru/price.htm",
            title => "Цены и сроки."
        }
    ],
    statusModerate => "New",
    statusEmpty => 'No',
    image => 'eFdA3MAxkWAmSyjJJLGlJQ'
}, {
    cid => 13, pid => 903,
    body       => "Перевод больших объемов технической литературы и финансовой документации.",
    title      => "Агентство «Dorian Grey»",
    domain     => "www.doriangrey.ru",
    href       => "http://www.doriangrey.ru",
    sitelinks  => [
        {
            href => "http://doriangrey.ru/about.htm",
            title => "О компании."
        },
        {
            href => "http://doriangrey.ru/price.htm",
            title => "Цены и сроки."
        },
        {
            href => "http://doriangrey.ru/service.htm",
            title => "Письменный перевод."
        },
        {
            href => "http://doriangrey.ru/service_note.htm",
            title => "Документы."
        }
    ],
    statusModerate => "Yes",
    statusEmpty => 'No',
    yaca_rough_list => '37,27',
}];
$banners_2 = create_desktop_banners($banners_2, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_2],
    [{
        cid => 13, pid => 903, type => 'desktop',
        body       => "Hoffer Flow Controls. Типоразмеры от 3/4'' до 12'', расходы до 60000 л/мин",
        title      => "Турбинные расходомеры",
        domain     => "www.massflow.ru", reverse_domain => 'ur.wolfssam.www',
        flags      => flags2hash(undef),
        href       => "https://www.massflow.ru/catalog/massflow/hoffer/liquid/",
        opts => '',
        statusModerate => "New", statusPostModerate => 'No',
        camp_statusModerate => 'Ready',
        image_hash => 'VU54gPxDWHRfUaazpRiA-w',
        bi_statusModerate => 'New',
        phoneflag => 'New',
        statusSitelinksModerate => 'New',
        sitelinks_set_id => undef, 
    }, {
        cid => 13, pid => 903, type => 'desktop', 
        body       => "Срочные языковые переводы любой сложности. Большие объемы. Редкие языки.",
        title      => "Бюро переводов в Москве",
        href       => "http://www.doriangrey.ru",
        domain     => "www.doriangrey.ru", reverse_domain => "ur.yergnairod.www",
        opts => '',
        flags      => flags2hash(undef),
        statusModerate => 'New', statusPostModerate => 'No', 
        phoneflag  => "New",
        camp_statusModerate => "Ready",
        sitelinks  => [
            {
                href => "http://doriangrey.ru/about.htm",
                title => 'Агентство "Dorian Grey".'
            },
            {
                href => "http://doriangrey.ru/service.htm",
                title => "Услуги."
            },
            {
                href => "http://doriangrey.ru/price.htm",
                title => "Цены и сроки."
            }
        ],
        statusSitelinksModerate => 'New',
        image_hash => 'eFdA3MAxkWAmSyjJJLGlJQ',
        bi_statusModerate => 'New'
    }, {
        cid => 13, pid => 903, type => 'desktop',
        body       => "Перевод больших объемов технической литературы и финансовой документации.",
        title      => "Агентство «Dorian Grey»",
        domain     => "www.doriangrey.ru", reverse_domain => "ur.yergnairod.www",
        href       => "http://www.doriangrey.ru",
        opts => '', flags => flags2hash(undef),
        sitelinks  => [
            {
                href => "http://doriangrey.ru/about.htm",
                title => "О компании."
            },
            {
                href => "http://doriangrey.ru/price.htm",
                title => "Цены и сроки."
            },
            {
                href => "http://doriangrey.ru/service.htm",
                title => "Письменный перевод."
            },
            {
                href => "http://doriangrey.ru/service_note.htm",
                title => "Документы."
            }
        ],
        statusSitelinksModerate => 'Ready',
        statusModerate => "Ready", statusPostModerate => 'No',
        camp_statusModerate => "Ready",
        phoneflag  => "New",
        image_hash => undef, bi_statusModerate => undef,
    }],
    "banner 2 check",
);

my $banners_3 = [{
    cid => 11, pid => 900,
    body       => "Цифровой металлографический микроскоп. Отличное соотношение цена/качество.",
    title      => "Цифровая металлография!",
    domain     => "altami.ru",
    href       => "altami.ru/microscopes/metallurgical/digi/",
    statusModerate => 'Ready',
    statusEmpty => 'No',
    yaca_rough_list => '18,96',
}, {
    
    cid => 12, pid => 901,
    body       => "Полет в стратосферу на 21 км и высший пилотаж! VIP - Подарок! Оператор!",
    title      => "Полеты на истребителе МИГ-29!",
    domain     => "www.kupi-polet.ru",
    href       => "http://www.kupi-polet.ru",
    statusModerate => 'New',
    statusEmpty => 'No',
}];
$banners_3 = create_desktop_banners($banners_3, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_3],
    [{
        cid => 11, pid => 900, type => 'desktop',
        body       => "Цифровой металлографический микроскоп. Отличное соотношение цена/качество.",
        title      => "Цифровая металлография!",
        domain     => "altami.ru", reverse_domain => "ur.imatla",
        href       => "http://altami.ru/microscopes/metallurgical/digi/",
        flags      => flags2hash(undef), opts => '',
        statusModerate => 'Ready', statusPostModerate => 'No',
        camp_statusModerate => "Yes",
        phoneflag  => "New",
        sitelinks_set_id => undef, statusSitelinksModerate => 'New',
        image_hash => undef, bi_statusModerate => undef,
    }, {
        cid => 12, pid => 901, type => 'desktop',
        body       => "Полет в стратосферу на 21 км и высший пилотаж! VIP - Подарок! Оператор!",
        title      => "Полеты на истребителе МИГ-29!",
        domain     => "www.kupi-polet.ru", reverse_domain => "ur.telop-ipuk.www",
        href       => "http://www.kupi-polet.ru",
        flags      => flags2hash(undef), opts => '',
        statusModerate => 'New', statusPostModerate => 'No',
        camp_statusModerate => "No",
        phoneflag  => "New",
        sitelinks_set_id => undef, statusSitelinksModerate => 'New',
        image_hash => undef, bi_statusModerate => undef,
    }],
    "banner 3 check",
);

done_testing;

# в сравнении полагаемся что записи добавляются в БД последовательно (нет сортировки $expected)
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
                $vcard_fields, bi.image_hash, bi.statusModerate AS bi_statusModerate,
                b.sitelinks_set_id, b.statusSitelinksModerate
            FROM banners b
                LEFT JOIN banner_images bi ON b.bid = bi.bid
                JOIN campaigns c ON b.cid = c.cid
                LEFT JOIN vcards vc ON b.vcard_id = vc.vcard_id",
            WHERE => {'b.bid' => $bids},
        "ORDER BY b.bid"
    ]);
    
    foreach my $banner (@$got_banners) {
        $banner->{flags} = flags2hash($banner->{flags});
        unless (delete $banner->{vcard_id}) {
            delete @{$banner}{@vc_fields};
        }
        if ($banner->{sitelinks_set_id}) {
            my $sitelinks = Sitelinks::get_sitelinks_by_set_id($banner->{sitelinks_set_id});
            $banner->{sitelinks} = [map {hash_cut $_, qw/title href/} @$sitelinks];
            delete $banner->{sitelinks_set_id}; 
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
                    { cid => 11, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'Yes' },
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
        banner_images => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
            },
        },
        banner_turbolandings => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
            },
        },
        turbolandings => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
            },
        },
        sitelinks_set_to_link => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
            },
        },
        sitelinks_sets => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
            },
        },
        sitelinks_links => {
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
        banner_images_pool => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {ClientID => 338556, name => 'бусы товар исходник копияcr.jpg', image_hash => 'eFdA3MAxkWAmSyjJJLGlJQ'}
                ],
            },
        },
        banner_images_formats => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {image_hash => 'eFdA3MAxkWAmSyjJJLGlJQ', image_type => 'small'}
                ],
            },
        },
        banner_images_uploads => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { hash_final => 'VU54gPxDWHRfUaazpRiA-w', cid => 14, name => 'медвед.jpg' }
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
        } qw/shard_inc_pid shard_inc_bid shard_inc_vcard_id shard_inc_sitelinks_set_id inc_sl_id shard_inc_banner_images_pool_id trusted_redirects mirrors mirrors_correction/),
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
