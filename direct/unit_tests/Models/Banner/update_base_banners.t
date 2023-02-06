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

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN { use_ok('Models::Banner'); }

local $Yandex::DBShards::IDS_LOG_FILE = undef;

{
    no warnings 'redefine';
    *Models::Banner::_on_base_banners_updated = sub {};
}

*update_base_banners = \&Models::Banner::update_base_banners;

my $uid = 12519399;
init_test_dataset(db_data());

my $banners_1 = [{
    bid => 1001,
    banner_type => 'mobile'
}, {
    bid => 1002,
    banner_type => 'desktop'
}];
throws_ok {
    update_base_banners($banners_1, $uid);
} qr/can't change banner type/;

my $banners_2 = [{
    bid => 1001,
}];
throws_ok {
    update_base_banners($banners_2, $uid);
} qr/can't change banner type/;

my $banners_3 = [{
    bid => 1002,
    banner_type => 'some strange type'
}];
throws_ok {
    update_base_banners($banners_3, $uid);
} qr/can't change banner type/;

# создание визитки вместе баннером
my $banners_4 = [
    {
        bid => 1002, cid => 11, banner_type => 'mobile',
        pid => 900, href => 'http://ya.ru', domain => 'ya.ru',
        title => 'title1', body => 'body1',
        statusEmpty => 'No', statusModerate => 'New',
        yaca_rough_list => '6,7,80,375',
    },
    {
        bid => 1001, cid => 11, banner_type => 'desktop',
        # domain считаем из href
        pid => 900, href => 'www.arimpex.ru/cat/chairs/',
        title => 'title2', body => 'body2',
        statusEmpty => 'No', statusModerate => 'New',
        # vcard (определение geo_id по городу)
        address_id => 2229364,
        apart => "",
        build => "",
        city  => "Москва",
        contact_email => 'mail@immun.ru',
        contactperson => "",
        country => "Россия",
        extra_message => "Многопрофильный медицинский центр по системным и хроническим заболеваниям.",
        # geo_id => 213,
        house => 6,
        im_client => "skype",
        im_login => "immunh",
        metro => 98561,
        name  => "МЦИК им. Ходановой Р.Н. ООО ВИТЕЛА",
        org_details_id => 78057,
        country_code => "+7",
        city_code => "499",
        phone => "445-40-83",
        ext => "",                
        street => "Давыдковская",
        worktime => "0#4#8#00#19#30;5#5#9#00#13#00"
    },
    {
        bid => 1003, cid => 11, banner_type => 'desktop',
        pid => 901, href => 'www.specialist.ru/section/hr-management?utm_source=yandex&utm_medium=cpc&utm_campaign=8095&utm_content=129809574',
        # изменили видимый домен
        domain => 'www.specialist.bmstu.ru',
        title => "Курсы по управлению персоналом!",
        body => "1С управление персоналом. «Специалист» при МГТУ Баумана. Свид. гос.образца",
        statusEmpty => 'No', statusModerate => 'New',
        # vcard (определение address_id по ->{map}->{aid})
        map => {aid => 177777},
        apart      => undef,
        build      => undef,
        city       => "Москва",
        contact_email => 'info@specialist.ru',
        contactperson => undef,
        country    => "Россия",
        extra_message => 'Лучший учебный компьютерный центр России 2002-11 гг. по результатам авторитетных рейтингов. Лучший учебный центр Microsoft. №1 в рейтинге IT-компаний РБК в сфере "Обучение IT". Компания года.',
        geo_id     => 213,
        house      => "4/6",
        im_client  => undef,
        im_login   => undef,
        metro      => 20478,
        name       => 'Центр компьютерного обучения "Специалист" при МГТУ им. Н.Э.Баумана',
        org_details_id => undef,
        country_code => "+7",
        city_code => "495",
        phone => "232-32-16",
        ext => "",                
        street     => "Госпитальный переулок",
        worktime   => "0#4#9#00#19#00;5#5#10#00#18#00",
    }
];
update_base_banners($banners_4, $uid);
cmp_banner_rows(
    [1001, 1002, 1003],
    [{
        bid => 1001, cid => 11, banner_type => 'desktop',
        pid => 900, href => 'http://www.arimpex.ru/cat/chairs/', domain => 'www.arimpex.ru',
        reverse_domain => 'ur.xepmira.www', flags => undef,
        title => 'title2', body => 'body2',
        statusPostModerate => 'No', statusModerate => 'New',
        phoneflag => 'New',
        camp_statusModerate => 'No', statusBsSynced => 'No',
        address_id => 2229364,
        apart => undef,
        build => undef,
        city  => "Москва",
        contact_email => 'mail@immun.ru',
        contactperson => undef,
        country => "Россия",
        extra_message => "Многопрофильный медицинский центр по системным и хроническим заболеваниям.",
        geo_id => 213,
        uid => $uid,
        house => 6,
        im_client => "skype",
        im_login => "immunh",
        metro => 98561,
        name  => "МЦИК им. Ходановой Р.Н. ООО ВИТЕЛА",
        org_details_id => 78057,
        phone => "+7#499#445-40-83#",
        street => "Давыдковская",
        worktime => "0#4#8#00#19#30;5#5#9#00#13#00",
        redirect_check => 0                
    }, {
        bid => 1002, cid => 11, banner_type => 'mobile',
        pid => 900, href => 'http://ya.ru', domain => 'ya.ru',
        reverse_domain => 'ur.ay', flags => undef,
        title => 'title1', body => 'body1',
        statusPostModerate => 'No', statusModerate => 'New',
        phoneflag => 'New',
        camp_statusModerate => 'No',
        statusBsSynced => 'No',
        redirect_check => 0
    }, {
        bid => 1003, cid => 11, banner_type => 'desktop',
        pid => 901, href => 'http://www.specialist.ru/section/hr-management?utm_source=yandex&utm_medium=cpc&utm_campaign=8095&utm_content=129809574',
        flags => "age:18,plus18,software",
        domain => 'www.specialist.bmstu.ru', reverse_domain => "ur.utsmb.tsilaiceps.www",
        title => "Курсы по управлению персоналом!",
        body => "1С управление персоналом. «Специалист» при МГТУ Баумана. Свид. гос.образца",
        statusPostModerate => 'No', statusModerate => 'New',
        phoneflag => 'New', camp_statusModerate => 'No',
        statusBsSynced => 'No',
        # vcard (определение address_id по ->{map}->{aid})
        address_id => 177777,
        apart      => undef,
        build      => undef,
        city       => "Москва",
        contact_email => 'info@specialist.ru',
        contactperson => undef,
        country    => "Россия",
        extra_message => 'Лучший учебный компьютерный центр России 2002-11 гг. по результатам авторитетных рейтингов. Лучший учебный центр Microsoft. №1 в рейтинге IT-компаний РБК в сфере "Обучение IT". Компания года.',
        geo_id     => 213,
        uid        => $uid,
        house      => "4/6",
        im_client  => undef,
        im_login   => undef,
        metro      => 20478,
        name       => 'Центр компьютерного обучения "Специалист" при МГТУ им. Н.Э.Баумана',
        org_details_id => undef,
        phone      => "+7#495#232-32-16#",
        street     => "Госпитальный переулок",
        worktime   => "0#4#9#00#19#00;5#5#10#00#18#00",
        redirect_check => 0
    }],
    "banners 4 check",
);
# создали 2 новые визитки
ok(2 == get_one_field_sql(PPC(cid => 11), "SELECT COUNT(*) FROM vcards WHERE cid = ? AND uid = ?", 11, $uid));

my $banners_5 = [ {
    bid => 1003, cid => 11, banner_type => 'desktop',
    pid => 901, href => 'www.specialist.ru/SAPR/?src=yd',
    domain => 'www.specialist.ru',
    title => "Лучший бухгалтер России преподает",
    body => 'в "Специалисте". Отличные отзывы. Престижное св-во. Новогодняя скидка 20%',
    statusEmpty => 'No', statusModerate => 'Yes',
    # vcard
    address_id => 177777,
    apart      => undef,
    build      => undef,
    city       => "Москва",
    contact_email => 'info@specialist.ru',
    contactperson => undef,
    country    => "Россия",
    extra_message => 'Лучший учебный компьютерный центр России 2002-11 гг. по результатам авторитетных рейтингов. Лучший учебный центр Microsoft. №1 в рейтинге IT-компаний РБК в сфере "Обучение IT". Компания года.',
    geo_id     => 213,
    house      => "4/6",
    im_client  => undef,
    im_login   => undef,
    metro      => 20478,
    name       => 'Центр компьютерного обучения "Специалист" при МГТУ им. Н.Э.Баумана',
    org_details_id => undef,
    country_code => "+7",
    city_code => "495",
    phone => "232-32-16",
    ext => "",                
    street     => "Госпитальный переулок",
    worktime   => "0#4#9#00#19#00;5#5#10#00#18#00",
}];

# баннер приняли на модерации
do_update_table(PPC(shard => 'all'), 'banners', {
    statusModerate => 'Yes',
    statusPostModerate => 'Yes',
}, where => {bid => 1003});
update_base_banners($banners_5, $uid);

cmp_banner_rows(
    [1003],
    [{
        bid => 1003, cid => 11, banner_type => 'desktop',
        pid => 901, href => 'http://www.specialist.ru/SAPR/?src=yd',
        domain => 'www.specialist.ru', reverse_domain => "ur.tsilaiceps.www",
        title => "Лучший бухгалтер России преподает",
        body => 'в "Специалисте". Отличные отзывы. Престижное св-во. Новогодняя скидка 20%',
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'Ready', camp_statusModerate => 'Ready',
        flags => "age:18,plus18,software",
        statusBsSynced => 'No',
        # vcard
        address_id => 177777,
        apart      => undef,
        build      => undef,
        city       => "Москва",
        contact_email => 'info@specialist.ru',
        contactperson => undef,
        country    => "Россия",
        extra_message => 'Лучший учебный компьютерный центр России 2002-11 гг. по результатам авторитетных рейтингов. Лучший учебный центр Microsoft. №1 в рейтинге IT-компаний РБК в сфере "Обучение IT". Компания года.',
        geo_id     => 213,
        house      => "4/6",
        uid        => $uid,
        im_client  => undef,
        im_login   => undef,
        metro      => 20478,
        name       => 'Центр компьютерного обучения "Специалист" при МГТУ им. Н.Э.Баумана',
        org_details_id => undef,
        phone      => "+7#495#232-32-16#",
        street     => "Госпитальный переулок",
        worktime   => "0#4#9#00#19#00;5#5#10#00#18#00",
        redirect_check => 0
    }],
    "banners 5 check",
);
# использовали существующую визитку (не создали новых)
ok(2 == get_one_field_sql(PPC(cid => 11), "SELECT COUNT(*) FROM vcards WHERE cid = ? AND uid = ?", 11, $uid));

my $banners_6 = [
    # баннер не поменялся (но т.к. был отклонен на модерации переотправим его ещё раз)
    {
        bid => 1010, banner_type => 'mobile', pid => 983, cid => 14,
        body  => "Ремонт сейфов. Перекодировка. Ключ по замку. Более двух сейфов - скидка.",
        title => "Вскрытие сейфов без повреждений!",
        domain => "www.mazter-x.ru",
        href  => "www.mazter-x.ru/category/7",
        statusEmpty => 'No', statusModerate => 'No',
    },
    # баннер не поменялся и ранее был принят на модерации
    {
        bid => 62450611, banner_type => 'desktop', cid => 14, pid => 919,
        body => "У нас оборудование последнего поколения. Быстро и красиво вышьем логотип.",
        title => "Нужно вышить логотип?",
        domain => "www.flag.ru", href => "http://www.flag.ru",
        statusEmpty => 'No', statusModerate => 'Yes',
    },
    {
        bid => 1011, banner_type => 'desktop', pid => 983, cid => 14,
        body       => "Вскрытие сейфов. Перекодировка. Замена замков. Более двух сейфов - скидка.",
        title      => "Ремонт сейфов и замков.",
        domain     => "www.mazter-x.ru",
        href       => "https://www.mazter-x.ru/category/7",
        statusEmpty => 'No', statusModerate => 'No',
        # будут проигнорированы
        yaca_rough_list => "7,333,103",
    }
];
update_base_banners($banners_6, $uid);
cmp_banner_rows(
    [1010, 1011, 62450611],
    [{
        bid => 1010, banner_type => 'mobile', pid => 983, cid => 14,
        body  => "Ремонт сейфов. Перекодировка. Ключ по замку. Более двух сейфов - скидка.",
        title => "Вскрытие сейфов без повреждений!",
        domain => "www.mazter-x.ru", reverse_domain => 'ur.x-retzam.www',
        href  => "http://www.mazter-x.ru/category/7",
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'New', camp_statusModerate => 'Ready',
        flags => undef,
        statusBsSynced => 'No',
        redirect_check => 0
    },
    {
        bid => 1011, banner_type => 'desktop', pid => 983, cid => 14,
        body       => "Вскрытие сейфов. Перекодировка. Замена замков. Более двух сейфов - скидка.",
        title      => "Ремонт сейфов и замков.",
        domain     => "www.mazter-x.ru",
        href       => "https://www.mazter-x.ru/category/7",
        reverse_domain => "ur.x-retzam.www",
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'New', camp_statusModerate => 'Ready',
        flags => undef,
        statusBsSynced => 'No',
        redirect_check => 0
    },
    {
        bid => 62450611, banner_type => 'desktop', cid => 14, pid => 919,
        body => "У нас оборудование последнего поколения. Быстро и красиво вышьем логотип.",
        title => "Нужно вышить логотип?",
        domain => "www.flag.ru", href => "http://www.flag.ru",
        reverse_domain => "ur.galf.www",
        statusModerate => "Yes",
        statusPostModerate => "Yes",
        statusBsSynced => 'Yes',
        camp_statusModerate => 'Ready', flags => undef, phoneflag => 'New',
        redirect_check => 0
    }],
    "banners 6 check",
);

my $banners_7 = [
    # поменялась только визитка
    {
        bid => 3483262, banner_type => 'mobile', cid => 15, pid => 8112,
        body => 'ИТП "ПРОМБИОФИТ" представляет упаковочное оборудование для малого бизнеса.',
        title => "25-26 сентября, Уфа",
        domain => "www.prombiofit.com",
        href => "https://www.prombiofit.com/news/2008.09.ufa.html",
        statusEmpty => 'No', statusModerate => 'Yes',
        address_id => 2229480,
        apart => 220,
        build => undef,
        city  => "Иркутск",
        contact_email => 'info@virtech.ru',
        contactperson => "Сизых Елена",
        country => "Россия",
        extra_message => "Полный комплекс услуг по разработке",
        geo_id => 63,
        house => 18, 
        im_client => undef,
        im_login => undef,
        metro => undef,
        name  => "ПРОМБИОФИТ",
        org_details_id => undef,
        country_code => "+7",
        city_code => "3952",
        phone => "21-11-01",
        ext => "",                
        street => "Сухэ-Батора",
        worktime => "0#4#9#00#18#00"
    },
    # поменялся только сам баннер
    {
        bid        => 166606, banner_type => 'desktop', cid => 15, pid => 8112,
        body       => "25 октября Локомотив-Атлетико. Кубок УЕФА. Заказ и доставка билетов",
        title      => "Локомотив-Атлетико. Заказ Билетов",
        domain     => "www.ebilet.ru",
        href       => "www.ebilet.ru/order/77549.html",
        statusEmpty => 'No', statusModerate => 'Yes',
                
        address_id => undef,
        apart      => undef,
        build      => undef,
        city       => "Москва",
        contact_email => undef,
        contactperson => undef,
        country    => "Россия",
        extra_message => undef,
        house      => undef,
        im_client  => undef,
        im_login   => undef,
        metro      => undef,
        name       => "eBILET",
        org_details_id => undef,
        country_code => "+7",
        city_code => "496",
        phone => "225-58-00",
        ext => "",                
        street     => undef,
        worktime   => "0#6#9#30#20#00",
    }
];
update_base_banners($banners_7, $uid);
cmp_banner_rows(
    [166606, 3483262],
    [{
        bid        => 166606, banner_type => 'desktop', cid => 15, pid => 8112,
        body       => "25 октября Локомотив-Атлетико. Кубок УЕФА. Заказ и доставка билетов",
        title      => "Локомотив-Атлетико. Заказ Билетов",
        domain     => "www.ebilet.ru", reverse_domain => "ur.telibe.www",
        href       => "http://www.ebilet.ru/order/77549.html",
        statusModerate => "Ready",
        statusPostModerate => "No",
        statusBsSynced => 'No',
        camp_statusModerate => 'Ready', flags => undef, phoneflag => 'Ready',
                
        address_id => undef,
        apart      => undef,
        build      => undef,
        city       => "Москва",
        contact_email => undef,
        contactperson => undef,
        country    => "Россия",
        geo_id     => 213,
        extra_message => undef,
        house      => undef,
        im_client  => undef,
        im_login   => undef,
        metro      => undef,
        name       => "eBILET",
        org_details_id => undef,
        uid        => $uid,
        phone      => "+7#496#225-58-00#",
        street     => undef,
        worktime   => "0#6#9#30#20#00",
        redirect_check => 0
    }, {
        bid => 3483262, banner_type => 'mobile', cid => 15, pid => 8112,
        body => 'ИТП "ПРОМБИОФИТ" представляет упаковочное оборудование для малого бизнеса.',
        title => "25-26 сентября, Уфа",
        domain => "www.prombiofit.com", reverse_domain => "moc.tifoibmorp.www",
        href => "https://www.prombiofit.com/news/2008.09.ufa.html",
        statusModerate => "Yes",
        statusPostModerate => "Yes",
        statusBsSynced => 'No',
        camp_statusModerate => 'Ready', flags => undef, phoneflag => 'Ready',
        address_id => 2229480,
        apart => 220,
        build => undef,
        city  => "Иркутск",
        contact_email => 'info@virtech.ru',
        contactperson => "Сизых Елена",
        country => "Россия",
        extra_message => "Полный комплекс услуг по разработке",
        geo_id => 63,
        house => 18,
        im_client => undef,
        im_login => undef,
        metro => undef,
        name  => "ПРОМБИОФИТ",
        org_details_id => undef,
        phone => "+7#3952#21-11-01#",
        street => "Сухэ-Батора",
        uid   => $uid,
        worktime => "0#4#9#00#18#00",
        redirect_check => 0
    }],
    "banners 7 check",
);

# Создали одну новую визитку (т.к. теперь они иммутабельные, то старые остались нетронутыми)
is(get_one_field_sql(PPC(cid => 15), "SELECT COUNT(*) FROM vcards WHERE cid = ? AND uid = ?", 15, $uid), 3);

# при отсутствии href в переданном баннере должны затереть banners.href, banners.domain
my $banners_8 = [
    # удалили href
    {
        bid => 3483262, banner_type => 'mobile', cid => 15, pid => 8112,
        body => 'ИТП "ПРОМБИОФИТ" представляет упаковочное оборудование для малого бизнеса.',
        title => "25-26 сентября, Уфа",
        statusEmpty => 'No', statusModerate => 'Yes',
        address_id => 2229480,
        apart => 220,
        build => undef,
        city  => "Иркутск",
        contact_email => 'info@virtech.ru',
        contactperson => "Сизых Елена",
        country => "Россия",
        extra_message => "Полный комплекс услуг по разработке",
        geo_id => 63,
        house => 18, 
        im_client => undef,
        im_login => undef,
        metro => undef,
        name  => "ПРОМБИОФИТ",
        org_details_id => undef,
        country_code => "+7",
        city_code => "3952",
        phone => "21-11-01",
        ext => "",                
        street => "Сухэ-Батора",
        worktime => "0#4#9#00#18#00"
    }
];

update_base_banners($banners_8, $uid);
cmp_banner_rows(
    [3483262],
    [{
        bid => 3483262, banner_type => 'mobile', cid => 15, pid => 8112,
        body => 'ИТП "ПРОМБИОФИТ" представляет упаковочное оборудование для малого бизнеса.',
        title => "25-26 сентября, Уфа",
        domain => undef, reverse_domain => undef,
        href => undef,
        statusModerate => "Yes",
        statusPostModerate => "Yes",
        statusBsSynced => 'No',
        camp_statusModerate => 'Ready', flags => undef, phoneflag => 'Ready',
        address_id => 2229480,
        apart => 220,
        build => undef,
        city  => "Иркутск",
        contact_email => 'info@virtech.ru',
        contactperson => "Сизых Елена",
        country => "Россия",
        extra_message => "Полный комплекс услуг по разработке",
        geo_id => 63,
        house => 18,
        im_client => undef,
        im_login => undef,
        metro => undef,
        name  => "ПРОМБИОФИТ",
        org_details_id => undef,
        phone => "+7#3952#21-11-01#",
        street => "Сухэ-Батора",
        uid   => $uid,
        worktime => "0#4#9#00#18#00",
        redirect_check => 0
    }],
    'banner_8 check'    
);

my $banners_9 = [
    {
        bid => 62450611, banner_type => 'desktop', cid => 14, pid => 919,
        body => "У нас оборудование последнего поколения. Вышьем логотип.",
        title => "Нужно вышить логотип?",
        href => "https://ad.atdmt.com/s/go?adv=11087200871573",
        statusEmpty => 'No', statusModerate => 'Yes',
    },
    {
        bid => 1011, banner_type => 'desktop', pid => 983, cid => 14,
        body       => "Вскрытие сейфов. Перекодировка. Замена замков. Более двух сейфов - скидка.",
        title      => "Ремонт сейфов и замков.",
        domain     => "events.microsoft.ru",
        href       => "https://ad.atdmt.com/s/go?adv=11087200871573",                
        statusEmpty => 'No', statusModerate => 'No',
        # будут проигнорированы
        yaca_rough_list => "7,333,103",
    }
];
update_base_banners($banners_9, $uid);
cmp_banner_rows(
    [1011, 62450611],
    [
    {
        bid => 1011, banner_type => 'desktop', pid => 983, cid => 14,
        body       => "Вскрытие сейфов. Перекодировка. Замена замков. Более двух сейфов - скидка.",
        title      => "Ремонт сейфов и замков.",
        domain     => "events.microsoft.ru",
        href       => "https://ad.atdmt.com/s/go?adv=11087200871573",
        reverse_domain => "ur.tfosorcim.stneve",
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'New', camp_statusModerate => 'Ready',
        flags => undef,
        statusBsSynced => 'No',
        redirect_check => 0
    },
    {
        bid => 62450611, banner_type => 'desktop', cid => 14, pid => 919,
        body => "У нас оборудование последнего поколения. Вышьем логотип.",
        title => "Нужно вышить логотип?",
        domain     => "ad.atdmt.com",
        href       => "https://ad.atdmt.com/s/go?adv=11087200871573",
        reverse_domain => "moc.tmdta.da",
        statusModerate => "Ready",
        statusPostModerate => "No",
        statusBsSynced => 'No',
        camp_statusModerate => 'Ready', flags => undef, phoneflag => 'New',
        redirect_check => 1
    }],
    "banners 9 check",
);

# href не поменялся, видимый домен не меняли (или нет прав на его замену)
# оставляемый предыдущий видимый домен, ссылку на простукивание не ставим
my $banners_10 = [
    {
        bid => 1011, banner_type => 'desktop', pid => 983, cid => 14,
        body       => "body_1011_45",
        title      => "title_1011_46",
        href       => "https://ad.atdmt.com/s/go?adv=11087200871573",                
        statusEmpty => 'No', statusModerate => 'No',
        # будут проигнорированы
        yaca_rough_list => "7,333,103",
    }
];

update_base_banners($banners_10, $uid);
cmp_banner_rows(
    [1011],
    [{
        bid => 1011, banner_type => 'desktop', pid => 983, cid => 14,
        body       => "body_1011_45",
        title      => "title_1011_46",
        domain     => "events.microsoft.ru",
        href       => "https://ad.atdmt.com/s/go?adv=11087200871573",
        reverse_domain => "ur.tfosorcim.stneve",
        statusPostModerate => 'No', statusModerate => 'Ready',
        phoneflag => 'New', camp_statusModerate => 'Ready',
        flags => undef,
        statusBsSynced => 'No',
        redirect_check => 0
    }]);

done_testing;


# в сравнении полагаемся что записи добавляются в БД последовательно (нет сортировки $expected)
sub cmp_banner_rows {
    
    my ($bids, $expected, $name) = @_;
    
    my @vc_fields = grep {$_ ne 'cid'} @$VCards::VCARD_FIELDS_DB;
    my $vcard_fields = join ',', map {"vc.$_"} @vc_fields;
    my $got_banners = get_all_sql(PPC(shard => 'all'), [
        "SELECT
                b.bid, b.type AS banner_type, b.flags, b.title, b.body,
                b.href, b.domain, b.reverse_domain,
                b.pid, b.cid, b.statusPostModerate, b.statusModerate,
                b.phoneflag, b.statusBsSynced,
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
        unless (delete $banner->{vcard_id}) {
            delete @{$banner}{@vc_fields};
            next;
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
                    { cid => 11, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'No' },
                    { cid => 12, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'No' },
                    { cid => 13, uid => 12519399, type => 'text', statusEmpty => 'Yes', statusModerate => 'No' },
                    { cid => 14, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'Ready' },
                    { cid => 15, uid => 12519399, type => 'text', statusEmpty => 'No', statusModerate => 'Ready' },
                ],
            },
        },
        phrases => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { pid => 900, cid => 11, statusModerate => 'Yes' },
                    { pid => 901, cid => 11, statusModerate => 'New' },
                    { pid => 903, cid => 13, statusModerate => 'New' },
                    
                    { pid => 919, cid => 14, statusModerate => 'Yes' },
                    { pid => 983, cid => 14, statusModerate => 'Yes' },
                    
                    { pid => 8112, cid => 15, statusModerate => 'Yes' },
                ],
            },
        },
        banners => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    { bid => 1001, type => 'desktop', pid => 900, cid => 11, statusModerate => 'New' },
                    { bid => 1002, type => 'mobile', pid => 900, cid => 11, statusModerate => 'New' },
                    
                    { bid => 1003, type => 'desktop', pid => 901, cid => 11, statusModerate => 'New', flags => "age:18,plus18,software" },
                    
                    
                    { 
                        bid => 1010, type => 'mobile', pid => 983, cid => 14, statusModerate => 'No', statusPostModerate => 'No',
                        body  => "Ремонт сейфов. Перекодировка. Ключ по замку. Более двух сейфов - скидка.",
                        title => "Вскрытие сейфов без повреждений!",
                        domain => "www.mazter-x.ru", href => "www.mazter-x.ru/category/7",
                        statusBsSynced => 'Yes',
                        reverse_domain => "ur.x-retzam.www",
                    },
                    { 
                        bid => 1011, type => 'desktop', pid => 983, cid => 14, statusModerate => 'No', statusPostModerate => 'No',
                        body  => "Замена замков, личинок, ручек, задвижек, обивки, МДФ. Сварка. Нам 19 лет.",
                        title => "Ремонт стальных дверей.",
                        domain => "www.mul-t-key.ru",
                        href  => "www.mul-t-key.ru",
                        reverse_domain => "ur.yek-t-lum.www",
                        statusBsSynced => 'Yes', 
                    },
                    {
                        bid => 62450611, type => 'desktop', cid => 14, pid => 919,
                        body => "У нас оборудование последнего поколения. Быстро и красиво вышьем логотип.",
                        title => "Нужно вышить логотип?",
                        domain => "www.flag.ru", href => "http://www.flag.ru",
                        reverse_domain => "ur.galf.www",
                        statusModerate => "Yes",
                        statusBsSynced => 'Yes',
                        statusPostModerate => "Yes",
                    },
                    {
                        bid => 3483262, type => 'mobile', cid => 15, pid => 8112,
                        body => 'ИТП "ПРОМБИОФИТ" представляет упаковочное оборудование для малого бизнеса.',
                        title => "25-26 сентября, Уфа",
                        domain => "www.prombiofit.com", reverse_domain => "moc.tifoibmorp.www",
                        href => "https://www.prombiofit.com/news/2008.09.ufa.html",
                        statusModerate => "Yes", statusPostModerate => "Yes",
                        vcard_id => 972, phoneflag => 'Yes'
                    },
                    {
                        bid        => 166606, type => 'desktop', cid => 15, pid => 8112,
                        body       => "30 ноября Ходынка. Бои без правил Россия-США. Заказ и доставка билетов",
                        title      => "Бои без правил Россия-США. Билеты",
                        domain     => "www.ebilet.ru", reverse_domain => "ur.telibe.www",
                        href       => "www.ebilet.ru/order/75871.html",
                        phoneflag  => "Yes",
                        statusModerate => "Yes", statusPostModerate => "Yes",
                        vcard_id   => 2881,
                    }
                ],
            },
        },
        banner_images => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => []
            }
        },
        banner_display_hrefs => {
            original_db => PPC(shard => 'all'),
            rows => {},
        },
        vcards => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [{
                    vcard_id => 972,
                    address_id => 12980,
                    apart => undef,
                    build => undef,
                    cid   => 15,
                    city  => "Москва",
                    contact_email => 'itp@prombiofit.com',
                    contactperson => undef,
                    country => "Россия",
                    extra_message => undef,
                    geo_id => 213,
                    house => 4,
                    im_client => undef,
                    im_login => undef,
                    metro => 20371,
                    name  => "ПРОМБИОФИТ",
                    org_details_id => undef,
                    phone => "+7#499#150-27-64#",
                    street => "ул.Клары Цеткин",
                    uid => $uid,
                    worktime => "0#4#10#00#18#30"
                }, {
                    vcard_id => 2881,
                    address_id => undef,
                    apart => undef,
                    build => undef,
                    cid   => 15,
                    city  => "Москва",
                    contact_email => undef,
                    contactperson => undef,
                    country => "Россия",
                    extra_message => undef,
                    geo_id => 213,
                    house => undef,
                    im_client => undef,
                    im_login => undef,
                    metro => undef,
                    name  => "eBILET",
                    org_details_id => undef,
                    phone => "+7#496#225-58-00#",
                    street => undef,
                    uid   => $uid,
                    worktime => "0#6#9#30#20#00"
                }],
            },
        },
        org_details => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                ],
            },
        },
        (map {
            $_ => {original_db => PPC(shard => 'all'), rows => []}
        } qw/post_moderate auto_moderate mod_edit filter_domain aggregator_domains addresses maps redirect_check_queue mod_object_version
             banners_additions additions_item_callouts
            /),
        
        geo_regions => {
            original_db => PPCDICT,
            rows => [
                {region_id => 213, name => 'Москва'}
            ],
        }, 
        (map {
            $_ => {original_db => PPCDICT, rows => []}
        } qw/shard_inc_pid shard_inc_bid shard_inc_vcard_id mirrors mirrors_correction/),
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
                { cid => 15, ClientID => 338556 },
            ],
        },
        shard_inc_bid => {
            original_db => PPCDICT,
            rows => [
                { bid => 1001, ClientID => 338556 },
                { bid => 1002, ClientID => 338556 },
                { bid => 1003, ClientID => 338556 },
                { bid => 1010, ClientID => 338556 },
                { bid => 1011, ClientID => 338556 },
                { bid => 62450611, ClientID => 338556 },
                { bid => 3483262, ClientID => 338556 },
                { bid => 166606, ClientID => 338556 },
            ],
        },
        shard_inc_org_details_id  => {
            original_db => PPCDICT,
            rows => [
                { org_details_id => 78057, ClientID => 338556 },
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
