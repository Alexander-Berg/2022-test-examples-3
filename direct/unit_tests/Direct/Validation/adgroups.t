use Direct::Modern;

use Test::More;
use Test::Exception;

use Direct::Model::Campaign;
use Direct::Model::Campaign::Role::AdGroupsCount;
use Direct::Model::Role::Update;
use Direct::Validation::MinusWords qw//;
use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::ValidationResult;
use Yandex::Queryrec;
use geo_regions;
use Direct::Model::Banner::Constants;

BEGIN {
    use_ok('Direct::Model::Banner');
    use_ok('Direct::Model::SitelinksSet');
    use_ok('Direct::Model::Sitelink');
    use_ok('Direct::Model::AdGroup');
    use_ok('Direct::Validation::AdGroups', qw/
        validate_add_adgroups
        validate_update_adgroups
        validate_delete_adgroups
    /);
}

package Test::Direct::Model::Campaign {
    use Mouse;
    extends 'Direct::Model::Campaign';
    with 'Direct::Model::Campaign::Role::AdGroupsCount';
    1;
}

package Test::Direct::Model::AdGroup {
    use Mouse;
    extends 'Direct::Model::AdGroup';
    with 'Direct::Model::Role::Update';
    1;
}


no warnings 'redefine';
local *Lang::Guess::call_external_queryrec = sub {
    my $text = shift;
    return Yandex::Queryrec::queryrec($text);
};

my %db = (
    ppc_properties => {
        original_db => PPCDICT
    },
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


# TODO: перенести проверку Direct::Validation::AdGroups в campaigns.t

*validate_adgroups = \&Direct::Validation::AdGroups::validate_adgroups;

# обращаемся к не заданным полям
my $groups_1 = get_adgroups([{adgroup_name => 'AdGroup#1'}]);
# throws_ok { #1
#     validate_adgroups($groups_1)
# } qr/Can't locate object method "_build_geo"/;

my $groups_2 = get_adgroups([{adgroup_name => 'AdGroup#1', geo => '225'}]);
# throws_ok { #2
#     validate_adgroups($groups_2)
# } qr/Can't locate object method "_build_client_id"/;

my $groups_4 = get_adgroups([{adgroup_name => 'AdGroup#1', geo => '225', client_id => 1812}]);
lives_ok { #3
    validate_adgroups($groups_4)
};

my $vr_1 = validate_adgroups( #4
    get_adgroups([{adgroup_name => '', geo => undef, minus_words => []}])
);
cmp_validation_result($vr_1, [ #5
    {geo => vr_errors(qr/задать геотаргетинг группы/), adgroup_name => vr_errors('InvalidFormat')},
]);

my $ok_camp = get_camp({
    status_empty => 'No',
    status_archived => 'No'
});

my $vr_2 = validate_update_adgroups(
    get_adgroups([
        {adgroup_name => undef, geo => undef, minus_words => [], banners => [], campaign => $ok_camp},
        {
            adgroup_name => "\t\t\t  ", geo => undef, minus_words => [],
            banners => [{id => 1, title => 'title#1', body => 'body#1'}],
            campaign => $ok_camp
        },
        {
            adgroup_name => '', geo => undef, minus_words => [],
            banners => [
                {id => 1, title => 'title#1', body => 'body#1', client_id => 1812,},
                {id => 2, title => 'title#2', body => 'body#2', client_id => 1812,},
            ],
            campaign => $ok_camp
        },
        {
            # допустимое имя группы
            adgroup_name => "0", geo => 0, minus_words => [],
            client_id => 1812,
            banners => [
                {id => 1, title => 'заголовок#1', body => 'текст#1', client_id => 1812,},
                {id => 2, title => 'заголовок#2', body => 'текст#2', client_id => 1812,},
            ],
            campaign => $ok_camp
        },
        {
            # допустимое имя группы
            adgroup_name => "00000", geo => 0, minus_words => [],
            client_id => 1812,
            banners => [
                {id => 1, title => 'заголовок#1', body => 'текст#1', client_id => 1812,},
                {id => 2, title => 'заголовок#2', body => 'текст#2', client_id => 1812,},
            ],
            campaign => $ok_camp
        },
        {
            adgroup_name => " \t   \t   ", geo => undef, minus_words => [],
            banners => [
                {id => 1, title => 'title#1', body => 'body#1', client_id => 1812,},
                {id => 2, title => 'title#2', body => 'body#2', client_id => 1812,},
                {id => 3, title => 'title#3', body => 'body#3', client_id => 1812,},
            ],
            campaign => $ok_camp
        }
    ])
);

cmp_validation_result($vr_2, [ #6
    {geo => vr_errors(qr/задать геотаргетинг группы/), adgroup_name => vr_errors('ReqField')},
    {geo => vr_errors('ReqField'), adgroup_name => vr_errors('InvalidFormat')},
    {
        adgroup_name => vr_errors('InvalidFormat'),
        geo => vr_errors(qr/задать геотаргетинг группы/)
    },
    {},
    {},
    {
        adgroup_name => vr_errors('InvalidFormat'),
        geo => vr_errors(qr/задать геотаргетинг группы/)
    }
], 'adgroup name');

my $vr_2_2 = validate_adgroups(
    get_adgroups([
        {
            adgroup_name => 'Группа 234', geo => "0", client_id => 1812,
            banners => [{id => 1, title => 'title#1', body => 'body#1', client_id => 1812}]
        }
    ])
);
ok_validation_result($vr_2_2); #7

my $vr_2_1 = validate_update_adgroups( #8
    get_adgroups([
        {adgroup_name => 'pid№941', geo => '1,10693,10832', minus_words => [], banners => [], client_id => 1812, campaign => $ok_camp},
        {adgroup_name => "AdGroup#1", geo => '1,10693,10832', minus_words => [], banners => [], client_id => 1812, campaign => $ok_camp},
        {
            adgroup_name => 'Выв ески ', geo => '1,10693,10832', minus_words => [], campaign => $ok_camp,
            banners => [
                {id => 1, title => 'Производство вывесок', body => 'Производство вывесок, наружной рекламы. Монтаж, регистрация', client_id => 1812},
            ],
            client_id => 1812
        },
        {
            adgroup_name => "AdGroup#2", geo => '11156', minus_words => [], campaign => $ok_camp,
            banners => [
                {id => 1, title => 'Курьерская служба Метеор', body => 'За 400 руб в течении 2-х часов мы доставим Вашу почту по Санкт-Петербургу', client_id => 1812},
                {id => 2, title => 'Интернет-магазин запонок', body => 'Классические и оригинальные запонки с широким выбором от ведущих марок.', client_id => 1812}
            ],
            client_id => 1812
        }
    ])
);
ok_validation_result($vr_2_1);

# проверка geo
my $vr_3 = validate_adgroups(
    get_adgroups([
        {geo => '', client_id => 1812, adgroup_name => 'AdGroup#4534', minus_words => []},
        {geo => "\t  \t ", client_id => 1812, adgroup_name => 'AdGroup#4534', minus_words => []},
    ])
);
cmp_validation_result($vr_3, [
    {geo => vr_errors('ReqField')},
    {geo => vr_errors('ReqField')}
]);

my $vr_4 = validate_adgroups(
    get_adgroups([
        {geo => '0', client_id => 1812, adgroup_name => 'AdGroup#4534', minus_words => []},
        {geo => ' 0  ', client_id => 1812, adgroup_name => 'AdGroup#4534', minus_words => []},
    ])
);
ok_validation_result($vr_4);

my $vr_5 = validate_adgroups(
    get_adgroups([
        {geo => '0,0', client_id => 1812, adgroup_name => 'AdGroup#4534', minus_words => []},
        {geo => '219,-1', client_id => 1812, adgroup_name => 'AdGroup#4534', minus_words => []},
        {geo => '10832,-0', client_id => 9827, adgroup_name => 'AdGroup#4534', minus_words => []},
        {geo => '10693,10832,-10693', client_id => 9827, adgroup_name => 'AdGroup#4534', minus_words => []},
    ])
);

cmp_validation_result($vr_5, [
    {geo => vr_errors(qr/повторяется несколько раз/)},
    {geo => vr_errors(qr/не содержится ни в одном из регионов показа/)},
    {geo => vr_errors('BadGeo')},
    {geo => vr_errors(qr/полностью исключает регион/)},
], 'check adgroup geo');

# проверка соответсвия geo языку баннера
# русский язык на любой регион
my $vr_6 = validate_update_adgroups( #12
    get_adgroups([
        {
            adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 1, title => "Все для производства скотча!", body => "Станки и оборудование для производства упаковочного скотча.", client_id => 1812}
            ], campaign => $ok_camp
        },
        {
            adgroup_name => 'AdGroup#3', geo => '0,-213',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 1, title => "Архитектурное бюро", body => "Архитектурное бюро. Архитектурное проектирование и дизайн интерьеров.", client_id => 1812},
                {id => 2, title => "МАКСКОМ: строительство заводов", body => "Строительство заводов, производственных и складских комплексов.", client_id => 1812}
            ], campaign => $ok_camp
        },
    ])
);
ok_validation_result($vr_6);

# TODO: траслокальность
# украинский только на украину
my $vr_7 = validate_update_adgroups( #13
    get_adgroups([
        {
            adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 1, title => "Стелажі в Україні - Каталог", body => "Професійне стелажне обладнання. Проектування, доставка та сервіс по Україні", client_id => 1812},
                {id => 2, title => "Двері ціна (Львів)", body => "Протиударні вхідні сертифіковані двері. Виробник MUL-T-LOCK.", client_id => 1812},
            ], campaign => $ok_camp
        },
        {
            adgroup_name => 'AdGroup#2', geo => '1,10693,10832,187',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 1, title => "Уяви ще більше простору!", body => "Монітори Samsung 2032/2232BW - WIDE формат, динамічний контраст 3000:1", client_id => 1812},
            ], campaign => $ok_camp
        },
        # такой группы быть не должно (т.к. баннеры на разных языка)
        # ошибка будет про не правильный таргетинг
        {
            adgroup_name => 'AdGroup#2', geo => '1,20544',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 1, title => 'Стиральные машины всех типов.', body => 'Стиральные машины достойных брендов. Более 500 моделей на Холодильник.Ру', client_id => 1812},
                {id => 2, title => 'Holodilnik.Ru - магазин', body => 'Посудомоечные машины. Большие и не очень. Merlony Bosch Electrolux Zanussi.', client_id => 1812},
                {id => 3, title => "Бесплатный семинар 22 Декабря!", body => 'Монітори Samsung 2032/2232BW - WIDE формат, динамїчний контраст 3000:1', client_id => 1812},
            ], campaign => $ok_camp
        },
    ])
);
cmp_validation_result($vr_7, [
    {geo => vr_errors('InvalidGeoTargeting')},
    {geo => vr_errors('InvalidGeoTargeting')},
    {geo => vr_errors('InvalidGeoTargeting')},
], 'check geo restrictions');

my $vr_8 = validate_update_adgroups( #14
    get_adgroups([
        {
            adgroup_name => 'AdGroup#2', geo => '187,-20529',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 1, title => 'Нерухомість від АКБ "Укрсоцбанк"', body => 'Продаж нерухомості від АКБ "Укрсоцбанк", який входить до UniCreditGroup', client_id => 1812},
                {id => 2, title => 'Центр Професій. Перепідготовки', body => 'Київ. Міжрегіональний Центр Перепідготовки Військовослужбовців.', client_id => 1812},
            ], campaign => $ok_camp
        },
        {
            adgroup_name => 'AdGroup#2', geo => '187,-20545,-20537,-20536,-20539,-20540,-20529,-20543,-20541,-20549,-20552,-20538,-20542,-20535,-20551',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 2, title => 'Екскаваторні і бульдозерні роботи', body => 'роботи по підняттю та вирівненю площ, осушенню ділянок, будуєм штучні озера', client_id => 1812},
            ], campaign => $ok_camp
        },
        {
            adgroup_name => 'AdGroup#2', geo => '20544,-10369',
            client_id => 1812, minus_words => [],
            banners => [
                {
                    id => 1,
                    title => 'Стиральные машины всех типов.',
                    body => 'Стиральные машины достойных брендов. Более 500 моделей на Холодильник.Ру',
                    sitelinks => [
                        {title => 'Інші країни', href => 'www.eurobuslines.com.ua/rejsy.htm', description => undef},
                        {title => 'Контакти', href => 'www.eurobuslines.com.ua/contact.htm', description => undef},
                    ],
                    client_id => 1812
                },
            ], campaign => $ok_camp
        },
        {
            adgroup_name => 'AdGroup#2', geo => '1,20544',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 1, title => 'Стиральные машины всех типов.', body => 'Стиральные машины достойных брендов. Более 500 моделей на Холодильник.Ру', client_id => 1812},
                {
                    id => 2,
                    title => 'Holodilnik.Ru - магазин',
                    body => 'Посудомоечные машины. Большие и не очень. Merlony Bosch Electrolux Zanussi.',
                    sitelinks => [
                        {title => 'Замовлення', href => 'www.krisha.org/kontaktnaa-forma', description => undef},
                        {title => 'Офіс на карті', href => 'www.krisha.org/my-na-karte-kieva', description => undef},
                    ],
                    client_id => 1812
                },
            ], campaign => $ok_camp
        },
    ])
);
ok_validation_result($vr_8);

my $campaign_8a = get_camp({content_lang=>'kk'});
my $vr_8a = validate_update_adgroups( #15
    get_adgroups([
        {
            adgroup_name => 'AdGroup#2', geo => '187',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 1, title => 'Нерухомість від АКБ "Укрсоцбанк"', body => 'Продаж нерухомості від АКБ "Укрсоцбанк", який входить до UniCreditGroup', client_id => 1812},
                {id => 2, title => 'Центр Професій. Перепідготовки', body => 'Київ. Міжрегіональний Центр Перепідготовки Військовослужбовців.', client_id => 1812},
            ], campaign => $campaign_8a
        },
    ])
);
cmp_validation_result($vr_8a, [
    {geo => vr_errors('InvalidGeoTargeting')},
]);

# турецкий язык только на турцию
my $vr_9 = validate_update_adgroups(
    get_adgroups([
         {
            adgroup_name => 'AdGroup#2', geo => '111,-10083',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 1, title => 'Bulaşık makinesi', body => "Bulaşık makineleri, fiyat avantajı ve taksit seçenekleriyle kliksa'da!", client_id => 1812},
                {id => 2, title => 'Whirlpool 4 pr ankastre bulaşık', body => "Makinesi 3 yıl garantili ücretsiz montaj indirimi değerlendirin 630 TL", client_id => 1812},
            ], campaign => $ok_camp
        },
        {
            adgroup_name => 'AdGroup#2', geo => '225',
            client_id => 1812, minus_words => [],
            banners => [
                {
                    id => 1,
                    title => 'China Wooden Doors For Sale‎ Ürünlerâ',
                    body => "Find Audited China Manufacturers Of Wooden Door. Order Now!",
                    sitelinks => [
                        {title => 'Ürünler - Fakir', href => 'http://www.fakir.com.tr/u/urunler', description => undef},
                        {title => 'En Yeni Ürünler', href => 'http://www.fakir.com.tr/u/urunler', description => undef}
                    ],
                    client_id => 1812
                },
            ], campaign => $ok_camp
        },
    ])
);

cmp_validation_result($vr_9, [
    {geo => vr_errors('InvalidGeoTargeting')},
    {geo => vr_errors('InvalidGeoTargeting')},
]);

my $vr_10 = validate_update_adgroups(
    get_adgroups([
         {
            adgroup_name => 'AdGroup#2', geo => '983,-103669,-103670,-103678,-103672,-103673,-103675,-103676,-103677,-103671,-103668,-103680,-103681,-103682',
            client_id => 1812, minus_words => [],
            banners => [
                {id => 1, title => 'Bulaşık makinesi', body => "Bulaşık makineleri, fiyat avantajı ve taksit seçenekleriyle kliksa'da!", client_id => 1812},
                {id => 2, title => 'Whirlpool 4 pr ankastre bulaşık', body => "Makinesi 3 yıl garantili ücretsiz montaj indirimi değerlendirin 630 TL", client_id => 1812},
            ], campaign => $ok_camp
        },
        {
            adgroup_name => 'AdGroup#2', geo => '103688,103743,103728,103697,103706',
            client_id => 1812, minus_words => [],
            banners => [
                {
                    id => 1,
                    title => 'China Wooden Doors For Sale‎ Ürünler',
                    body => "Find Audited China Manufacturers Of Wooden Door. Order Now!",
                    sitelinks => [
                        {title => 'Ürünler - Fakir', href => 'http://www.fakir.com.tr/u/urunler', description => undef},
                        {title => 'En Yeni Ürünler', href => 'http://www.fakir.com.tr/u/urunler', description => undef}
                    ],
                    client_id => 1812
                },
            ], campaign => $ok_camp
        },
    ])
);
ok_validation_result($vr_10);


# проверка минус-слов

my $vr_11 = validate_adgroups(
    get_adgroups([
        {
            adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
            client_id => 1812, minus_words => ['[финский', 'форум', 'фото]'],
            banners => []
        },
        {
            adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
            client_id => 1812, minus_words => [('x') x ($Settings::GROUP_MINUS_WORDS_LIMIT + 1)],
            banners => []
        },
        {
            adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
            client_id => 1812, minus_words => ['x' x ($Direct::Validation::MinusWords::MAX_MINUS_WORD_LENGTH + 1)],
            banners => []
        }
    ])
);

cmp_validation_result($vr_11, [
    {minus_words => vr_errors('InvalidChars')},
    {},
    {minus_words => vr_errors('MaxMinusWordLength')},
]);

my $vr_12 = validate_adgroups(
    get_adgroups([
        {
            adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
            client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
            banners => []
        },
    ])
);
ok_validation_result($vr_12);

# проверка добавления групп через кампанию

my $campaign = get_camp({id => 111, adgroups_count => 4, adgroups_limit => 5, status_empty => 'No', status_archived => 'No'});
my $groups = get_adgroups([
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
]);

my $vr_13 = validate_add_adgroups($groups, $campaign);
ok_validation_result($vr_13);

my $groups_14 = get_adgroups([
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
]);

my $vr_14 = validate_add_adgroups($groups_14, $campaign);
cmp_validation_result($vr_14, vr_errors('ReachLimit'));

my $campaign_15 = get_camp({id => 111, adgroups_count => 0, adgroups_limit => 5, status_empty => 'No', status_archived => 'No'});
my $groups_15 = get_adgroups([
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
]);
cmp_validation_result(validate_add_adgroups($groups_15, $campaign_15), vr_errors('ReachLimit'));

my $campaign_16 = get_camp({id => 222, adgroups_count => 3, adgroups_limit => 5, status_empty => 'No', status_archived => 'No'});
my $groups_16 = get_adgroups([
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 222,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 222,
        banners => []
    },
]);
ok_validation_result(validate_add_adgroups($groups_16, $campaign_16));

my $campaign_17 = get_camp({id => 111, adgroups_count => 0, adgroups_limit => 5, status_empty => 'No', status_archived => 'No'});
my $groups_17 = get_adgroups([
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 111,
        banners => []
    },
]);
ok_validation_result(validate_add_adgroups($groups_17, $campaign_17));

my $campaign_18 = get_camp({id => 777, adgroups_count => 10, adgroups_limit => 5, status_empty => 'No', status_archived => 'No'}); # в кампании уже больше групп чем разрешено
my $groups_18 = get_adgroups([
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 777,
        banners => []
    },
    {
        adgroup_name => 'AdGroup#2', geo => '225,169,166,111,-983,977',
        client_id => 1812, minus_words => ['бесплатно', '!обучение', 'реферат', '+сам'],
        campaign_id => 777,
        banners => []
    },
]);

cmp_validation_result(validate_add_adgroups($groups_18, $campaign_18), vr_errors('ReachLimit'));

# ошибка в группе

my $campaign_19 = get_camp({id => 444, adgroups_count => 5, adgroups_limit => 5, status_empty => 'No', status_archived => 'No'});
my $groups_19 = get_adgroups([
    {
        adgroup_name => '', geo => '871,-0',
        client_id => 1812, minus_words => [],
        campaign_id => 444,
        banners => [
            {id => 1, title => 'Заголовок1', body => 'Текст1'},
            {id => 2, title => 'Заголовок2', body => 'Текст2'}
        ]
    },
]);

cmp_validation_result(validate_add_adgroups($groups_19, $campaign_19),
    {
        generic_errors => vr_errors('ReachLimit'),
        objects_results => [
            {
                adgroup_name => vr_errors('InvalidFormat'),
                geo => vr_errors('BadGeo'),
            },
        ]
    },
);

done_testing;

sub get_adgroups {
    my $groups = shift;

    my @group_objects;
    my $default_camp = get_camp();
    for my $group (ref $groups eq 'ARRAY' ? @$groups : $groups) {
        my @banners = map {
            Direct::Model::Banner->new(
                $_->{sitelinks} ? (
                sitelinks_set => Direct::Model::SitelinksSet->new(
                    links => [map { Direct::Model::Sitelink->new(%$_) } @{delete($_->{sitelinks})}]
                )
                ) : (),
                title_extension => undef,
                language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
                %$_
            )
        } @{$group->{banners} || []};
        delete $group->{banners};
        my $adgroup = Test::Direct::Model::AdGroup->new(
            %$group,
            banners => \@banners
        );
        $adgroup->campaign($default_camp) if !$adgroup->has_campaign;
        push @group_objects, $adgroup;

    }
    return \@group_objects;
}

sub get_camp {
    my $camp = shift;
    return Test::Direct::Model::Campaign->new(
        adgroups_count => delete $camp->{adgroups_count} || 0,
        adgroups_limit => delete $camp->{adgroups_limit} || $Settings::DEFAULT_BANNER_COUNT_LIMIT,
        content_lang => undef,
        %$camp
    );
}
