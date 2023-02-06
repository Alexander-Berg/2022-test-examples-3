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
use Yandex::HashUtils;
use Sitelinks qw//;
use Settings;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN { use_ok('Models::DesktopBanner', 'update_desktop_banners'); }

local $Yandex::DBShards::IDS_LOG_FILE = undef;

{
    no warnings 'redefine';
    *Models::Banner::_on_base_banners_updated = sub {};
}

my $uid = 12519399;
init_test_dataset(db_data());

my $banners_1 = [{
    bid => 1002,
}, {
    bid => 1003,
}];
throws_ok {
    update_desktop_banners($banners_1, $uid);
} qr/can't change banner type/;

my $banners_2 = [{
    bid => 1710350, pid => 900, cid => 11, 
    body           => "Ударопрочный, трудногорючий, теплостойкий, экструзионный. Подбор цвета.",
    title          => "АБС-пластик, ABS POLIMAXX гранул.",
    domain         => "www.kompamid.ru",
    href           => "http://www.kompamid.ru/material_type.php?binn_rubrik_pl_catelems1=205",
    sitelinks      => [
        {
            href => "http://www.kompamid.ru/material_type.php?binn_rubrik_pl_catelems1=267",
            title => "САН-пластик(SAN)",
            description => undef,
        },
        {
            href => "http://www.kompamid.ru/material_type.php?binn_rubrik_pl_catelems1=474",
            title => "Полиметилметакрилат ПММА",
            description => undef,
        },
        {
            href => "http://www.kompamid.ru/material_type.php?binn_rubrik_pl_catelems1=430",
            title => "Поликарбонат CARBOTEX",
            description => undef,
        }
    ],
    image => "eFdA3MAxkWAmSyjJJLGlJQ",
    statusEmpty => 'No', statusModerate => 'Yes',
    
    # vcard
    address_id => 2495564,
    apart => undef,
    build => undef,
    city  => "Москва",
    contact_email => 'plastic@kompamid.ru',
    contactperson => undef,
    country => "Россия",
    extra_message => "Полиацеталь (POM), полиамиды (PA-6, PA66), АБС-пластик (ABS), САН-пластик (SAN), полистирол, поликарбонат (PC), полиметилметакрилат (PMMA), полибутилентерефталат (PBT), полифениленсульфид (PPS).",
    geo_id => 213,
    house => 4,
    im_client => "icq",
    im_login => 460951503,
    metro => 20417,
    name  => "Компамид Инженерные Пластики",
    org_details_id => undef,
    country_code => "+7",
    city_code => "495",
    phone => "789-65-59",
    ext => "",                
    street => "Мастеркова",
    worktime => "0#4#9#00#18#00"
}];
$banners_2 = update_desktop_banners($banners_2, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_2],
    [{
        bid => 1710350, pid => 900, cid => 11, type => 'desktop',
        body           => "Ударопрочный, трудногорючий, теплостойкий, экструзионный. Подбор цвета.",
        title          => "АБС-пластик, ABS POLIMAXX гранул.",
        domain         => "www.kompamid.ru", reverse_domain => "ur.dimapmok.www",
        href           => "http://www.kompamid.ru/material_type.php?binn_rubrik_pl_catelems1=205",
        flags => 'distance_sales',
        statusModerate => 'Ready', statusPostModerate => 'No',
        camp_statusModerate => 'Yes',
        sitelinks      => [
            {
                href => "http://www.kompamid.ru/material_type.php?binn_rubrik_pl_catelems1=267",
                title => "САН-пластик(SAN)",
                description => undef,
            },
            {
                href => "http://www.kompamid.ru/material_type.php?binn_rubrik_pl_catelems1=474",
                title => "Полиметилметакрилат ПММА",
                description => undef,
            },
            {
                href => "http://www.kompamid.ru/material_type.php?binn_rubrik_pl_catelems1=430",
                title => "Поликарбонат CARBOTEX",
                description => undef,
            }
        ],
        statusSitelinksModerate => 'Ready',
        image => "eFdA3MAxkWAmSyjJJLGlJQ",
        image_statusModerate => 'Ready',
        
        # визитка не изменилась, но поменялся баннер - отправляем визитку на модерацию
        phoneflag => 'Ready',
        uid => $uid,
        address_id => 2495564,
        apart => undef,
        build => undef,
        city  => "Москва",
        contact_email => 'plastic@kompamid.ru',
        contactperson => undef,
        country => "Россия",
        extra_message => "Полиацеталь (POM), полиамиды (PA-6, PA66), АБС-пластик (ABS), САН-пластик (SAN), полистирол, поликарбонат (PC), полиметилметакрилат (PMMA), полибутилентерефталат (PBT), полифениленсульфид (PPS).",
        geo_id => 213,
        house => 4,
        im_client => "icq",
        im_login => 460951503,
        metro => 20417,
        name  => "Компамид Инженерные Пластики",
        org_details_id => undef,
        phone => "+7#495#789-65-59#",
        street => "Мастеркова",
        worktime => "0#4#9#00#18#00"
    }]
);
# новых визиток не создали
ok(1 == get_one_field_sql(PPC(cid => 11), "SELECT COUNT(*) FROM vcards WHERE cid = ? AND uid = ?", 11, $uid));

my $banners_3 = [
    # поменяли только сайтлинки
    {
        bid => 91236, cid => 14, pid => 919,
        body           => "Анализ дымовых газов и поиск утечек Официальный сайт TESTO в России.",
        title          => "Газоанализаторы TESTO (Германия)",
        domain         => "www.testo.ru",
        href           => "http://www.testo.ru/ru/home/products/gasanalyzers/obzor/modeli_i_tseny-2.jsp",
        statusEmpty => 'No', statusModerate => 'Yes',
        sitelinks => [
            {
                href => "www.testo.ru/ru/home/products/productdetails.jsp?productNo=0563+2065",
                title => "testo 206-рН1",
                description => undef,
            },
            {
                href => "www.testo.ru/ru/home/products/productdetails.jsp?productNo=0563+2066",
                title => "testo 206-рН2",
                description => undef,
            },
            {
                href => "www.testo.ru/ru/home/products/productdetails.jsp?productNo=0563+2051",
                title => "testo 205",
                description => undef,
            }
        ],
        image => 'AhvkleWanO9xpcwS_vZhaw'
    },
    # добавили картинки и сайтлинки
    {
        bid            => 710205, cid => 11, pid => 900,
        body           => "Фирменный центр. Антикор обработка, установка подкрылок, «антигравий».",
        title          => "АНТИКОР Центр",
        domain         => "www.антикор.рф",
        href           => "https://www.антикор.рф",
        statusEmpty    => 'No', statusModerate => 'No',
        image          => "epBswyWMfu4xF7A2ZoRNKA",
        sitelinks      => [
            {
                href => "http://www.антикор.рф/auto-centers/",
                title => "Жидкие подкрылки",
                description => undef,
            },
            {
                href => "http://www.антикор.рф/products-and-services/8/",
                title => "Мойка днища",
                description => undef,
            },
            {
                href => "http://www.антикор.рф/products-and-services/9/",
                title => "Шумоизоляция авто",
                description => undef,
            }
        ],
    }
];
$banners_3 = update_desktop_banners($banners_3, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_3],
    [
        {
            bid => 91236, cid => 14, pid => 919, type => 'desktop',
            body           => "Анализ дымовых газов и поиск утечек Официальный сайт TESTO в России.",
            title          => "Газоанализаторы TESTO (Германия)",
            domain         => "www.testo.ru", reverse_domain => "ur.otset.www", 
            href           => "http://www.testo.ru/ru/home/products/gasanalyzers/obzor/modeli_i_tseny-2.jsp",
            flags => '',
            statusModerate => 'Yes', statusPostModerate => 'Yes',
            camp_statusModerate => 'Ready',
            phoneflag => 'New',
            sitelinks => [
                {
                    href => "http://www.testo.ru/ru/home/products/productdetails.jsp?productNo=0563+2065",
                    title => "testo 206-рН1",
                    description => undef,
                },
                {
                    href => "http://www.testo.ru/ru/home/products/productdetails.jsp?productNo=0563+2066",
                    title => "testo 206-рН2",
                    description => undef,
                },
                {
                    href => "http://www.testo.ru/ru/home/products/productdetails.jsp?productNo=0563+2051",
                    title => "testo 205",
                    description => undef,
                }
            ],
            statusSitelinksModerate => 'Ready',
            image => 'AhvkleWanO9xpcwS_vZhaw',
            image_statusModerate => 'Yes',
        }, 
        {
            bid            => 710205, cid => 11, pid => 900, type => 'desktop',
            body           => "Фирменный центр. Антикор обработка, установка подкрылок, «антигравий».",
            title          => "АНТИКОР Центр",
            domain         => "www.антикор.рф", reverse_domain => "фр.рокитна.www",
            href           => "https://www.антикор.рф",
            statusModerate => 'Ready', statusPostModerate => 'Rejected',
            camp_statusModerate => 'Yes',
            image          => "epBswyWMfu4xF7A2ZoRNKA",
            image_statusModerate => 'Ready',
            sitelinks      => [
                {
                    href => "http://www.антикор.рф/auto-centers/",
                    title => "Жидкие подкрылки",
                    description => undef,
                },
                {
                    href => "http://www.антикор.рф/products-and-services/8/",
                    title => "Мойка днища",
                    description => undef,
                },
                {
                    href => "http://www.антикор.рф/products-and-services/9/",
                    title => "Шумоизоляция авто",
                    description => undef,
                }
            ],
            statusSitelinksModerate => 'Ready',
            phoneflag => 'New',
            flags => ''
        }
    ]
);

# баннер не поменяли (баннер уже был принят на модерации)
my $banners_4 = [{
    bid            => 707967, cid => 14, pid => 919,
    body           => "ГОСТ13940/41/42/43 Все размеры! Отличные цены! Отгрузка по РФ каждый день.!",
    domain         => "www.oao-tochnost.ru",
    href           => "http://www.oao-tochnost.ru",
    image          => "HBOcxs29Uhn_57sH2NcW7w",
    sitelinks      => [
        {
            href => "http://oao-tochnost.ru/products/okompanii.php",
            title => "О компании",
            description => undef,
        },
        {
            href => "http://oao-tochnost.ru/menu/dostavka.php",
            title => "Доставка",
            description => undef,
        },
        {
            href => "http://oao-tochnost.ru/menu/spetspredlozheniyause.php",
            title => "Спецпредложения",
            description => undef,
        }
    ],
    statusEmpty => 'No', statusModerate => 'Yes',
    title          => "Стопорные Кольца Лист! Проволока!",
    
    # vcard
    address_id => 381,
    apart => undef,
    build => undef,
    cid   => 14,
    city  => "Москва",
    contact_email => 'oao-tochnost@mail.ru',
    contactperson => undef,
    country => "Россия",
    extra_message => 'ОоО "Точность" - производитель пружин тарельчатых/витьевых любого размера и сложности. Колец стопорных ГОСТ 13940/41/42/43-86. Шайб ГОСТ 11872,11648,13463/65 Выгодные цены. Отгрузка во все регионы',
    geo_id => 213,
    house => undef,
    im_client => undef,
    im_login => undef,
    metro => 20419,
    name  => 'ООО "Точность"',
    org_details_id => undef,
    country_code => "+7",
    city_code => "495",
    phone => "223-44-49",
    ext => "",                
    street => undef,
    worktime => "0#4#8#00#18#30"
}];
$banners_4 = update_desktop_banners($banners_4, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_4],
    [
        {
            bid            => 707967, cid => 14, pid => 919, type => 'desktop',
            body           => "ГОСТ13940/41/42/43 Все размеры! Отличные цены! Отгрузка по РФ каждый день.!",
            title          => "Стопорные Кольца Лист! Проволока!",
            domain         => "www.oao-tochnost.ru", reverse_domain => "ur.tsonhcot-oao.www",
            href           => "http://www.oao-tochnost.ru",
            flags          => "payment",
            statusModerate => "Yes",
            statusPostModerate => "Yes",
            camp_statusModerate => 'Ready',
            image          => "HBOcxs29Uhn_57sH2NcW7w",
            image_statusModerate => 'Yes',
            sitelinks      => [
                {
                    href => "http://oao-tochnost.ru/products/okompanii.php",
                    title => "О компании",
                    description => undef,
                },
                {
                    href => "http://oao-tochnost.ru/menu/dostavka.php",
                    title => "Доставка",
                    description => undef,
                },
                {
                    href => "http://oao-tochnost.ru/menu/spetspredlozheniyause.php",
                    title => "Спецпредложения",
                    description => undef,
                }
            ],
            statusSitelinksModerate => 'Yes',
            
            # vcard
            phoneflag => 'Yes',
            address_id => 381,
            apart => undef,
            build => undef,
            uid => $uid,
            city  => "Москва",
            contact_email => 'oao-tochnost@mail.ru',
            contactperson => undef,
            country => "Россия",
            extra_message => 'ОоО "Точность" - производитель пружин тарельчатых/витьевых любого размера и сложности. Колец стопорных ГОСТ 13940/41/42/43-86. Шайб ГОСТ 11872,11648,13463/65 Выгодные цены. Отгрузка во все регионы',
            geo_id => 213,
            house => undef,
            im_client => undef,
            im_login => undef,
            metro => 20419,
            name  => 'ООО "Точность"',
            org_details_id => undef,
            phone => "+7#495#223-44-49#",
            street => undef,
            worktime => "0#4#8#00#18#30"
        }
    ]
);

ok(0 < do_update_table(PPC(bid => 707967), 'banner_images', {statusShow => 'No'}, where => {bid => 707967}));
my $banners_5 = [
# замена удаленной картинки
{
    bid            => 707967, cid => 14, pid => 919,
    body           => "ГОСТ13940/41/42/43 Все размеры! Отличные цены! Отгрузка по РФ каждый день.!",
    domain         => "www.oao-tochnost.ru",
    href           => "http://www.oao-tochnost.ru",
    image          => "WualyazBt4G2Um112mykkw",
    sitelinks      => [
        {
            href => "http://oao-tochnost.ru/products/okompanii.php",
            title => "О компании",
            description => undef,
        },
        {
            href => "http://oao-tochnost.ru/menu/dostavka.php",
            title => "Доставка",
            description => undef,
        },
        {
            href => "http://oao-tochnost.ru/menu/spetspredlozheniyause.php",
            title => "Спецпредложения",
            description => undef,
        }
    ],
    statusEmpty => 'No', statusModerate => 'Yes',
    title          => "Стопорные Кольца Лист! Проволока!",
    
    # vcard
    address_id => 381,
    apart => undef,
    build => undef,
    cid   => 14,
    city  => "Москва",
    contact_email => 'oao-tochnost@mail.ru',
    contactperson => undef,
    country => "Россия",
    extra_message => 'ОоО "Точность" - производитель пружин тарельчатых/витьевых любого размера и сложности. Колец стопорных ГОСТ 13940/41/42/43-86. Шайб ГОСТ 11872,11648,13463/65 Выгодные цены. Отгрузка во все регионы',
    geo_id => 213,
    house => undef,
    im_client => undef,
    im_login => undef,
    metro => 20419,
    name  => 'ООО "Точность"',
    org_details_id => undef,
    country_code => "+7",
    city_code => "495",
    phone => "223-44-49",
    ext => "",                
    street => undef,
    worktime => "0#4#8#00#18#30"
},
# добавление картинки и сайтлинков к промодерированному баннеру
{
    bid            => 1696191, cid => 2003, pid => 587,
    body           => "Расчет и оплата online. Москва, Россия, мир. Доставка корреспонденции. СДЭК",
    domain         => "www.cdek.ru",
    href           => "https://www.cdek.ru/service/",
    title          => "Доставка корреспонденции курьером",
    statusEmpty    => 'No', statusModerate => 'Yes',
    image          => "1fqFu_AMRU-fHv3XAIX2pQ",
    sitelinks      => [
        {
            href => "http://www.cdek.ru/happyten/",
            title => "Акции",
            description => undef,
        },
        {
            href => "http://www.cdek.ru/tarif/",
            title => "Калькулятор",
            description => undef,
        },
        {
            href => "http://www.cdek.ru/before_order/",
            title => "Вызов курьера",
            description => undef,
        }
    ]
}];
$banners_5 = update_desktop_banners($banners_5, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_5],
    [
        {
            bid            => 707967, cid => 14, pid => 919, type => 'desktop',
            body           => "ГОСТ13940/41/42/43 Все размеры! Отличные цены! Отгрузка по РФ каждый день.!",
            title          => "Стопорные Кольца Лист! Проволока!",
            domain         => "www.oao-tochnost.ru", reverse_domain => "ur.tsonhcot-oao.www",
            href           => "http://www.oao-tochnost.ru",
            flags          => "payment",
            statusModerate => "Yes",
            statusPostModerate => "Yes",
            camp_statusModerate => 'Ready',
            image          => "WualyazBt4G2Um112mykkw",
            image_statusModerate => 'Ready',
            sitelinks      => [
                {
                    href => "http://oao-tochnost.ru/products/okompanii.php",
                    title => "О компании",
                    description => undef,
                },
                {
                    href => "http://oao-tochnost.ru/menu/dostavka.php",
                    title => "Доставка",
                    description => undef,
                },
                {
                    href => "http://oao-tochnost.ru/menu/spetspredlozheniyause.php",
                    title => "Спецпредложения",
                    description => undef,
                }
            ],
            statusSitelinksModerate => 'Yes',
            
            # vcard
            phoneflag => 'Yes',
            address_id => 381,
            apart => undef,
            build => undef,
            uid => $uid,
            city  => "Москва",
            contact_email => 'oao-tochnost@mail.ru',
            contactperson => undef,
            country => "Россия",
            extra_message => 'ОоО "Точность" - производитель пружин тарельчатых/витьевых любого размера и сложности. Колец стопорных ГОСТ 13940/41/42/43-86. Шайб ГОСТ 11872,11648,13463/65 Выгодные цены. Отгрузка во все регионы',
            geo_id => 213,
            house => undef,
            im_client => undef,
            im_login => undef,
            metro => 20419,
            name  => 'ООО "Точность"',
            org_details_id => undef,
            phone => "+7#495#223-44-49#",
            street => undef,
            worktime => "0#4#8#00#18#30"
        }, 
        {
            bid            => 1696191, cid => 2003, pid => 587, type => 'desktop',
            body           => "Расчет и оплата online. Москва, Россия, мир. Доставка корреспонденции. СДЭК",
            domain         => "www.cdek.ru", reverse_domain => "ur.kedc.www",
            href           => "https://www.cdek.ru/service/",
            title          => "Доставка корреспонденции курьером",
            flags          => "distance_sales",
            statusModerate => "Yes",
            statusPostModerate => "Yes",
            camp_statusModerate => 'Sent',
            image          => "1fqFu_AMRU-fHv3XAIX2pQ",
            image_statusModerate => 'Ready',
            sitelinks      => [
                {
                    href => "http://www.cdek.ru/happyten/",
                    title => "Акции",
                    description => undef,
                },
                {
                    href => "http://www.cdek.ru/tarif/",
                    title => "Калькулятор",
                    description => undef,
                },
                {
                    href => "http://www.cdek.ru/before_order/",
                    title => "Вызов курьера",
                    description => undef,
                }
            ],
            statusSitelinksModerate => 'Ready',
            phoneflag => 'New',
        }
    ]
);

# добавление|изменение картинки и сайтлинков к баннеру черновику
my $banners_6 = [
{
    bid            => 217636, cid => 8991, pid => 600,
    body           => "Профессиональный перевод больших объемов документации с/на Эстонский язык.",
    domain         => "www.doriangrey.ru",
    href           => "www.doriangrey.ru",
    image          => "atsb5hyCV8KUbQE8ncgLfA",
    sitelinks      => [
        {
            href => "http://doriangrey.ru/price.htm",
            title => "Цены и сроки.",
            description => undef,
        },
        {
            href => "http://doriangrey.ru/service_type.htm",
            title => "Письменный перевод.",
            description => undef,
        },
        {
            href => "http://doriangrey.ru/contacts.php",
            title => "Контакты.",
            description => undef,
        }
    ],
    title          => 'Агентство переводов "Дориан Грей"',
    statusEmpty => 'Yes', statusModerate => "New"
}
];
$banners_6 = update_desktop_banners($banners_6, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_6],
    [
        {
            bid            => 217636, cid => 8991, pid => 600, type => 'desktop',
            body           => "Профессиональный перевод больших объемов документации с/на Эстонский язык.",
            domain         => "www.doriangrey.ru", reverse_domain => "ur.yergnairod.www",
            href           => "http://www.doriangrey.ru",
            flags          => "",
            statusModerate => 'New', statusPostModerate => 'No',
            camp_statusModerate => 'New',
            image          => "atsb5hyCV8KUbQE8ncgLfA",
            image_statusModerate => 'New',
            sitelinks      => [
                {
                    href => "http://doriangrey.ru/price.htm",
                    title => "Цены и сроки.",
                    description => undef,
                },
                {
                    href => "http://doriangrey.ru/service_type.htm",
                    title => "Письменный перевод.",
                    description => undef,
                },
                {
                    href => "http://doriangrey.ru/contacts.php",
                    title => "Контакты.",
                    description => undef,
                }
            ],
            statusSitelinksModerate => 'New',
            title          => 'Агентство переводов "Дориан Грей"',
            phoneflag => 'New',
        }
    ]
);

# отправка баннера-черновика с визиткой/сайтлинками/картинкой на модерацию
my $banners_7 = [{
    bid            => 707968, cid => 14, pid => 919,
    body           => "ГОСТ13940/41/42/43 Все размеры! Отличные цены! Отгрузка в РФ каждый день.!",
    domain         => "www.oao-tochnost.ru",
    href           => "http://www.oao-tochnost.ru",
    image          => "HBOcxs29Uhn_59sH2NcW7w",
    sitelinks      => [
        {
            href => "http://oao-tochnost.ru/products/okompanii.php",
            title => "О компании",
            description => undef,
        },
        {
            href => "http://oao-tochnost.ru/menu/dostavka.php",
            title => "Доставка",
            description => undef,
        },
        {
            href => "http://oao-tochnost.ru/menu/spetspredlozheniyause.php",
            title => "Спецпредложения",
            description => undef,
        }
    ],
    statusEmpty => 'No', statusModerate => 'Ready',
    title          => "Стопорные Кольца Лист! Проволока!",

    # vcard
    address_id => 381,
    apart => undef,
    build => undef,
    cid   => 14,
    city  => "Москва",
    contact_email => 'oao-tochnost@mail.ru',
    contactperson => undef,
    country => "Россия",
    extra_message => 'ОоО "Точность" - производитель пружин тарельчатых/витьевых любого размера и сложности. Колец стопорных ГОСТ 13940/41/42/43-86. Шайб ГОСТ 11872,11648,13463/65 Выгодные цены. Отгрузка во все регионы',
    geo_id => 213,
    house => undef,
    im_client => undef,
    im_login => undef,
    metro => 20419,
    name  => 'ООО "Точность"',
    org_details_id => undef,
    country_code => "+7",
    city_code => "495",
    phone => "223-44-49",
    phoneflag => 'New',
    image_statusModerate => 'New',
    ext => "",                
    street => undef,
    worktime => "0#4#8#00#18#30"
}];
$banners_7 = update_desktop_banners($banners_7, $uid);
cmp_banner_rows(
    [map {$_->{bid}} @$banners_7],
    [
        {
            bid            => 707968, cid => 14, pid => 919, type => 'desktop',
            body           => "ГОСТ13940/41/42/43 Все размеры! Отличные цены! Отгрузка в РФ каждый день.!",
            title          => "Стопорные Кольца Лист! Проволока!",
            domain         => "www.oao-tochnost.ru", reverse_domain => "ur.tsonhcot-oao.www",
            href           => "http://www.oao-tochnost.ru",
            flags          => "payment",
            statusModerate => "Ready",
            statusPostModerate => "No",
            camp_statusModerate => 'Ready',
            image          => "HBOcxs29Uhn_59sH2NcW7w",
            image_statusModerate => 'Ready',
            sitelinks      => [
                {
                    href => "http://oao-tochnost.ru/products/okompanii.php",
                    title => "О компании",
                    description => undef,
                },
                {
                    href => "http://oao-tochnost.ru/menu/dostavka.php",
                    title => "Доставка",
                    description => undef,
                },
                {
                    href => "http://oao-tochnost.ru/menu/spetspredlozheniyause.php",
                    title => "Спецпредложения",
                    description => undef,
                }
            ],
            statusSitelinksModerate => 'Ready',
            
            # vcard
            phoneflag => 'Ready',
            address_id => 381,
            apart => undef,
            build => undef,
            uid => $uid,
            city  => "Москва",
            contact_email => 'oao-tochnost@mail.ru',
            contactperson => undef,
            country => "Россия",
            extra_message => 'ОоО "Точность" - производитель пружин тарельчатых/витьевых любого размера и сложности. Колец стопорных ГОСТ 13940/41/42/43-86. Шайб ГОСТ 11872,11648,13463/65 Выгодные цены. Отгрузка во все регионы',
            geo_id => 213,
            house => undef,
            im_client => undef,
            im_login => undef,
            metro => 20419,
            name  => 'ООО "Точность"',
            org_details_id => undef,
            phone => "+7#495#223-44-49#",
            street => undef,
            worktime => "0#4#8#00#18#30"
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
                b.bid, b.type, b.flags, b.title, b.body,
                b.href, b.domain, b.reverse_domain,
                b.pid, b.cid, b.statusPostModerate, b.statusModerate,
                b.phoneflag,
                c.statusModerate AS camp_statusModerate, b.vcard_id,
                $vcard_fields, bim.image_hash AS image, bim.statusModerate AS image_statusModerate,
                b.sitelinks_set_id, b.statusSitelinksModerate
            FROM banners b
                LEFT JOIN banner_images bim ON b.bid = bim.bid
                JOIN campaigns c ON b.cid = c.cid
                LEFT JOIN vcards vc ON b.vcard_id = vc.vcard_id",
            WHERE => {'b.bid' => $bids},
        "ORDER BY b.bid"
    ]);
    
    foreach my $banner (@$got_banners) {
        unless (delete $banner->{vcard_id}) {
            delete @{$banner}{@vc_fields};
        }
        if ($banner->{sitelinks_set_id}) {
            my $sitelinks = Sitelinks::get_sitelinks_by_set_id($banner->{sitelinks_set_id});
            $banner->{sitelinks} = [map {hash_cut $_, qw/title description href/} @$sitelinks];
            delete $banner->{sitelinks_set_id}; 
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
                        bid => 1710350, type => 'desktop', pid => 900, cid => 11, statusModerate => 'Yes', statusPostModerate => 'Yes',
                        body        => "Полиформальдегид (ПФЛ) от 79 руб./кг. Склад в Московской обл. Доставка.",
                        title       => "Полиацеталь KOCETAL в гранулах.",
                        domain      => "www.kompamid.ru",
                        flags       => "distance_sales",
                        href        => "http://www.kompamid.ru/material_type.php?binn_rubrik_pl_catelems1=440",
                        vcard_id    => 1708,
                        phoneflag   => "Yes",
                        reverse_domain => "ur.dimapmok.www",
                        sitelinks_set_id => undef,
                        statusSitelinksModerate => "New",
                     },
                     {
                        bid => 91236, cid => 14, pid => 919, type => 'desktop',
                        body           => "Анализ дымовых газов и поиск утечек Официальный сайт TESTO в России.",
                        title          => "Газоанализаторы TESTO (Германия)",
                        domain         => "www.testo.ru",
                        flags          => "",
                        href           => "http://www.testo.ru/ru/home/products/gasanalyzers/obzor/modeli_i_tseny-2.jsp",
                        reverse_domain => "ur.otset.www",
                        sitelinks_set_id => 28182411,
                        statusSitelinksModerate => "Yes",
                        statusModerate => "Yes",
                        statusPostModerate => "Yes",
                        vcard_id => undef,
                        phoneflag => "New",
                     }, 
                     {
                        bid  => 710205, cid => 11, pid => 900, type => 'desktop',
                        body           => "Фирменный центр. Антикор обработка, установка подкрылок, «антигравий».",
                        domain         => "www.антикор.рф",
                        flags          => "",
                        href           => "https://www.антикор.рф",
                        reverse_domain => "фр.рокитна.www",
                        sitelinks_set_id => undef,
                        statusModerate => "No",
                        statusPostModerate => "Rejected",
                        statusSitelinksModerate => "New",
                        title          => "АНТИКОР Центр",
                        vcard_id => undef,
                        phoneflag => 'New',
                     }, 
                     {
                        bid            => 707967, cid => 14, pid => 919, type => 'desktop',
                        body           => "ГОСТ13940/41/42/43 Все размеры! Отличные цены! Отгрузка по РФ каждый день.!",
                        title          => "Стопорные Кольца Лист! Проволока!",
                        domain         => "www.oao-tochnost.ru",
                        flags          => "payment",
                        href           => "http://www.oao-tochnost.ru",
                        reverse_domain => "ur.tsonhcot-oao.www",
                        sitelinks_set_id => 14997994,
                        statusModerate => "Yes",
                        statusPostModerate => "Yes",
                        statusSitelinksModerate => "Yes",
                        vcard_id => 67219,
                        phoneflag      => "Yes",                        
                     },
                     {
                        bid            => 1696191, cid => 2003, pid => 587, type => 'desktop',
                        body           => "Расчет и оплата online. Москва, Россия, мир. Доставка корреспонденции. СДЭК",
                        domain         => "www.cdek.ru",
                        flags          => "distance_sales",
                        href           => "https://www.cdek.ru/service/",
                        statusModerate => "Yes",
                        statusPostModerate => "Yes",
                        reverse_domain => "ur.kedc.www",
                        title          => "Доставка корреспонденции курьером",
                     },
                     {
                        bid            => 217636, cid => 8991, pid => 600, type => 'desktop',
                        body           => "Профессиональный перевод больших объемов документации с/на Эстонский язык.",
                        domain         => "www.doriangrey.ru",
                        flags          => "",
                        href           => "www.doriangrey.ru",
                        reverse_domain => "ur.yergnairod.www",
                        statusModerate => "New",
                        statusPostModerate => "No",
                        title          => 'Агентство переводов "Дориан Грей"',
                     },
                     # Баннер-черновик с визиткой и сайтлинками
                     {
                        bid            => 707968, cid => 14, pid => 919, type => 'desktop',
                        body           => "ГОСТ13940/41/42/43 Все размеры! Отличные цены! Отгрузка по РФ каждый день.!",
                        title          => "Стопорные Кольца Лист! Проволока!",
                        domain         => "www.oao-tochnost.ru",
                        flags          => "payment",
                        href           => "http://www.oao-tochnost.ru",
                        reverse_domain => "ur.tsonhcot-oao.www",
                        sitelinks_set_id => 14997994,
                        statusModerate => "New",
                        statusPostModerate => "No",
                        statusSitelinksModerate => "New",
                        vcard_id => 67219,
                        phoneflag      => "New",
                     },
                ],
            },
        },
        banner_images => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { image_id => 8921, bid => 1710350, image_hash => '86YyvcBTTN6SgNjTrm5VJg', statusModerate => 'Yes' },
                    { image_id => 8922, bid => 91236, image_hash => 'AhvkleWanO9xpcwS_vZhaw', statusModerate => 'Yes' },
                    { image_id => 8923, bid => 707967, image_hash => 'HBOcxs29Uhn_57sH2NcW7w', statusModerate => 'Yes' },
                    { image_id => 8924, bid => 707968, image_hash => 'HBOcxs29Uhn_59sH2NcW7w', statusModerate => 'New' },
                ],
            },
        },
        banner_display_hrefs => {
            original_db => PPC(shard => 'all'),
            rows => {},
        },
        banner_turbolandings => {
            original_db => PPC(shard => 'all'),
            rows => {},
        },
        turbolandings => {
            original_db => PPC(shard => 'all'),
            rows => {},
        },
        sitelinks_set_to_link => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {
                        order_num => 0,
                        sitelinks_set_id => 28182411,
                        sl_id   => 73162020
                    },
                    {
                        order_num => 1,
                        sitelinks_set_id => 28182411,
                        sl_id   => 73162021
                    },
                    {
                        order_num => 2,
                        sitelinks_set_id => 28182411,
                        sl_id   => 73162022
                    },
                    {
                        order_num => 0,
                        sitelinks_set_id => 14997994,
                        sl_id   => 40767813
                    },
                    {
                        order_num => 1,
                        sitelinks_set_id => 14997994,
                        sl_id   => 40767814
                    },
                    {
                        order_num => 2,
                        sitelinks_set_id => 14997994,
                        sl_id   => 40767815
                    }
                ],
            },
        },
        sitelinks_sets => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {
                        ClientID => 338556,
                        links_hash => 459059506735374383,
                        sitelinks_set_id => 28182411
                    },
                    {
                        ClientID => 338556,
                        links_hash => 10492186650305712868,
                        sitelinks_set_id => 14997994
                    }
                ],
            },
        },
        sitelinks_links => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {
                        hash => 7912612632841983479,
                        href => "www.testo.ru/ru/home/products/productdetails.jsp?productNo=0632+3306+70",
                        sl_id => 73162020,
                        title => "testo 330",
                        description => undef,
                    },
                    {
                        hash => 17545193532008912361,
                        href => "www.testo.ru/ru/home/products/productdetails.jsp?productNo=0632+3220",
                        sl_id => 73162021,
                        title => "testo 320",
                        description => undef,
                    },
                    {
                        hash => 16422675082981056478,
                        href => "www.testo.ru/ru/home/products/productdetails.jsp?productNo=0563+3100",
                        sl_id => 73162022,
                        title => "testo 310",
                        description => undef,
                    },
                    {
                        hash => 17635767206326838054,
                        href => "http://oao-tochnost.ru/products/okompanii.php",
                        sl_id => 40767813,
                        title => "О компании",
                        description => undef,
                    },
                    {
                        hash => 8219262552755461527,
                        href => "http://oao-tochnost.ru/menu/dostavka.php",
                        sl_id => 40767814,
                        title => "Доставка",
                        description => undef,
                    },
                    {
                        hash => 18417435452489562311,
                        href => "http://oao-tochnost.ru/menu/spetspredlozheniyause.php",
                        sl_id => 40767815,
                        title => "Спецпредложения",
                        description => undef,
                    }
                ],
            },
        },
        vcards => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [{
                    vcard_id => 1708,
                    address_id => 2495564,
                    apart => undef,
                    build => undef,
                    cid   => 11,
                    city  => "Москва",
                    contact_email => 'plastic@kompamid.ru',
                    contactperson => undef,
                    country => "Россия",
                    extra_message => "Полиацеталь (POM), полиамиды (PA-6, PA66), АБС-пластик (ABS), САН-пластик (SAN), полистирол, поликарбонат (PC), полиметилметакрилат (PMMA), полибутилентерефталат (PBT), полифениленсульфид (PPS).",
                    geo_id => 213,
                    house => 4,
                    im_client => "icq",
                    im_login => 460951503,
                    metro => 20417,
                    name  => "Компамид Инженерные Пластики",
                    org_details_id => undef,
                    phone => "+7#495#789-65-59#",
                    street => "Мастеркова",
                    uid   => $uid,
                    worktime => "0#4#9#00#18#00"
                }, {
                    vcard_id => 67219,
                    address_id => 381,
                    apart => undef,
                    build => undef,
                    cid   => 14,
                    city  => "Москва",
                    contact_email => 'oao-tochnost@mail.ru',
                    contactperson => undef,
                    country => "Россия",
                    extra_message => 'ОоО "Точность" - производитель пружин тарельчатых/витьевых любого размера и сложности. Колец стопорных ГОСТ 13940/41/42/43-86. Шайб ГОСТ 11872,11648,13463/65 Выгодные цены. Отгрузка во все регионы',
                    geo_id => 213,
                    house => undef,
                    im_client => undef,
                    im_login => undef,
                    metro => 20419,
                    name  => 'ООО "Точность"',
                    org_details_id => undef,
                    phone => "+7#495#223-44-49#",
                    street => undef,
                    uid   => $uid,
                    worktime => "0#4#8#00#18#30"
                }]
            },
        },
        (map {
            $_ => {original_db => PPC(shard => 'all'), rows => []}
        } qw/org_details post_moderate auto_moderate moderation_cmd_queue mod_edit filter_domain aggregator_domains addresses maps mod_object_version
             banners_additions additions_item_callouts
            /),
        banner_images_pool => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {imp_id => 200, ClientID => 338556, name => 'бусы товар исходник копияcr.jpg', image_hash => 'eFdA3MAxkWAmSyjJJLGlJQ'},
                    {imp_id => 201, ClientID => 338556, name => 'img_123.jpg', image_hash => '86YyvcBTTN6SgNjTrm5VJg'}
                ],
            },
        },
        banner_images_formats => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {image_hash => 'eFdA3MAxkWAmSyjJJLGlJQ', image_type => 'small'},
                    {image_hash => '86YyvcBTTN6SgNjTrm5VJg', image_type => 'small'}
                ],
            },
        },
        banner_images_uploads => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { hash_final => 'VU54gPxDWHRfUaazpRiA-w', cid => 14, name => 'медвед.jpg' },
                    { hash_final => 'epBswyWMfu4xF7A2ZoRNKA', cid => 11, name => 'JF8JU1991.jpg' },
                    { hash_final => 'WualyazBt4G2Um112mykkw', cid => 14, name => '2(1).jpg' }
                ],
            },
        },
        moderation_cmd_queue => {original_db => PPC(shard => 'all'), rows => []},
        filter_domain => {original_db => PPC(shard => 'all'), rows => []},
        redirect_check_queue => {original_db => PPC(shard => 'all'), rows => []},
        geo_regions => {
            original_db => PPCDICT,
            rows => [
                {region_id => 213, name => 'Москва'}
            ],
        },
        
        (map {
            $_ => {original_db => PPCDICT, rows => []}
        } qw/shard_inc_pid shard_inc_vcard_id inc_sl_id shard_inc_banner_images_pool_id trusted_redirects mirrors mirrors_correction/),
        shard_inc_sitelinks_set_id => {
            original_db => PPCDICT,
            rows => [
                { sitelinks_set_id => 28182411, ClientID => 338556 },
                { sitelinks_set_id => 14997994, ClientID => 338556 }
            ]
        },
        shard_inc_bid => {
            original_db => PPCDICT,
            rows => [
                { bid => 1002, ClientID => 338556 },
                { bid => 1003, ClientID => 338556 },
                { bid => 1710350, ClientID => 338556 },
                { bid => 91236, ClientID => 338556 },
                { bid => 710205, ClientID => 338556 },
                { bid => 707967, ClientID => 338556 },
                { bid => 707968, ClientID => 338556 },
                { bid => 1696191, ClientID => 338556 },
                { bid => 217636, ClientID => 338556 },
                
                # images
                { bid => 8921, ClientID => 338556 },
                { bid => 8922, ClientID => 338556 },
                { bid => 8923, ClientID => 338556 }
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
                { uid => 12519399, ClientID => 338556 },
            ],
        },
    }
}
