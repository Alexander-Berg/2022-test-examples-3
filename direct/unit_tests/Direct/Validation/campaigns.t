#!/usr/bin/env perl

use Direct::Modern;

use base qw/Test::Class/;

use Direct::Model::AdGroup;
use Direct::Model::Campaign;
use Direct::Model::SitelinksSet;
use Direct::Model::Sitelink;
use Direct::Validation::MinusWords;

use Settings;
use TimeTarget qw//; # for mocking
use Test::More;
use Yandex::Test::ValidationResult;
use Yandex::Test::UTF8Builder;
use Yandex::TimeCommon qw/ ts_round_day ts_to_str /;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';

use Direct::Model::Banner::Constants;

use Yandex::DBUnitTest qw/:all/;

my %db = (
    ssp_platforms => {
        original_db => PPCDICT,
        rows => [
            { title => 'Valid SSP' },
        ],
    },
    ppc_properties => {
        original_db => PPCDICT
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
        ],
    },
    client_limits => {
        original_db => PPC(shard => 1),
        like => 'client_limits',
    }
);

init_test_dataset(\%db);

{
    no strict qw/ refs /;
    no warnings qw/ once redefine /;

    # my $orig = \&TimeTarget::get_timezone;
    # *TimeTarget::get_timezone = sub { my $res = $orig->( @) ); use Data::Dumper; print Dumper($res); return $res };

    my %timezones = (
        130 =>         {
            'country_id' => '225',
            'gmt_offset' => '+03:00',
            'group_nick' => 'russia',
            'id' => '130',
            'msk_offset' => '+00:00',
            'name' => 'Москва',
            'offset' => 10800,
            'offset_str' => '',
            'timezone' => 'Europe/Moscow',
            'timezone_id' => '130'
        },
        0 => { # особенность реализации TimeTarget::get_timezone
            'country_id' => '225',
            'gmt_offset' => '+03:00',
            'group_nick' => 'russia',
            'id' => '130',
            'msk_offset' => '+00:00',
            'name' => 'Москва',
            'offset' => 10800,
            'offset_str' => '',
            'timezone' => 'Europe/Moscow',
            'timezone_id' => '130'
        },
    );

    *TimeTarget::get_timezone = sub { return defined( $_[0] ) ? $timezones{ $_[0] } : undef; };

    *Client::ClientFeatures::has_cpc_device_modifiers_allowed_feature = sub { return 1 };
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
}

sub use_module : Tests( startup => 1 ) {
    use_ok('Direct::Validation::Campaigns', qw/ validate_campaigns /);
}

sub campaign_name : Tests( 6 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects({ without => 'campaign_name' })
        ),
        [
            { campaign_name => vr_errors(qr/^Не указано значение в поле #field#$/) }, # vr_errors('ReqField'),
        ],
        'error when campaign\'s name not specified'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ campaign_name => '' })
        ),
        [
            { campaign_name => vr_errors(qr/^В поле #field# указано пустое значение$/) }, # vr_errors('EmptyField'),
        ],
        'error when campaign\'s name is empty'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ campaign_name => '    ' })
        ),
        [
            { campaign_name => vr_errors(qr/^В поле #field# указано пустое значение$/) }, # vr_errors('EmptyField'),
        ],
        'error when campaign\'s name includes only spaces'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ campaign_name => 'Campaign <1>' })
        ),
        [
            { campaign_name => vr_errors(qr/^Поле #field# содержит спецсимволы$/) }, # vr_errors('InvalidChars'),
        ],
        'error when campaign\'s name includes incorrect symbols'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ campaign_name => 'a' x 256 })
        ),
        [
            { campaign_name => vr_errors(qr/^Значение в поле #field# не должно превышать #length# символов$/) }, # vr_errors('MaxLength'),
        ],
        'error when campaign\'s name too long'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ campaign_name => 'Тестовая кампания' })
        ),
        {},
        'no error when campaign\'s name is correct'
    );
}

sub client_fio : Tests( 8 ) {

    cmp_validation_result(
        validate_campaigns(
            get_objects({ without => 'client_fio' })
        ),
        [
            { client_fio => vr_errors(qr/^Не указано значение в поле #field#$/) }, # vr_errors('ReqField'),
        ],
        'error when client\'s fio not specified'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ client_fio => 'Client <1>' })
        ),
        [
            { client_fio => vr_errors(qr/^Поле #field# содержит спецсимволы$/) }, # vr_errors('InvalidChars'),
        ],
        'error when client\'s fio include incorrect symbols'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ client_fio => "Client\n" })
        ),
        [
            { client_fio => vr_errors(qr/^Поле #field# содержит спецсимволы$/) }, # vr_errors('InvalidChars'),
        ],
        'error when client\'s fio includes incorrect symbols (\n)'
    );

    my $camp1 = get_objects({ client_fio => 'Client <1>' })->[0];
    $camp1->old(
        get_objects({ client_fio => 'Client' })->[0]
    );

    cmp_validation_result(
        validate_campaigns([ $camp1 ]),
        [
            { client_fio => vr_errors(qr/^Поле #field# содержит спецсимволы$/) }, # vr_errors('InvalidChars'),
        ],
       	'error when client\'s fio include incorrect symbols and updated'
    );

    my $camp2 = get_objects({ client_fio => 'Client <1>' })->[0];
    $camp2->old(
        get_objects({ client_fio => 'Client <1>' })->[0]
    );

    cmp_validation_result(
        validate_campaigns([ $camp2 ]),
        {},
        'no error when invalid fio is not updated'
    );
    cmp_validation_result(
        validate_campaigns(
            get_objects({ client_fio => 'a' x 256 })
        ),
        [
            { client_fio => vr_errors(qr/^Значение в поле #field# не должно превышать #length# символов$/) }, # vr_errors('MaxLenght'),
        ],
        'error when client\'s fio too long'
    );

    my $camp3 = get_objects({ client_fio => 'a' x 256 })->[0];
    $camp3->old(
        get_objects({ client_fio => 'a' x 256 })->[0]
    );

    cmp_validation_result(
        validate_campaigns([ $camp3 ]),
        {},
        'no error when too long fio is not updated'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ client_fio => 'Сидоров Иван Петрович' })
        ),
        {},
        'no error when client\'s fio is correct'
    );
}

sub email : Tests( 5 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects({ without => 'email' })
        ),
        [
            { email => vr_errors(qr/^Не указано значение в поле #field#$/) }, # vr_errors('ReqField'),
        ],
        'error when email not specified'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ email => '' })
        ),
        [
            { email => vr_errors(qr/^В поле #field# указано пустое значение$/) }, # vr_errors('EmptyField'),
        ],
        'error when email is empty'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ email => 'Вася@yandex.ru' })
        ),
        [
            { email => vr_errors(qr/^Значение в поле #field# указано в неправильном формате$/) }, # vr_errors('InvalidFormat'),
        ],
        'error when email is incorrect'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ email => sprintf('%s@yandex.ru', 'a' x 255) })
        ),
        [
            { email => vr_errors(qr/^Значение в поле #field# не должно превышать #length# символов$/) }, # vr_errors('MaxLength'),
        ],
        'error when email not specified'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ email => 'direct@yandex.ru' })
        ),
        {},
        'no error when email is correct'
    );
}

sub start_date : Tests( 7 ) {

    my $day_before = ts_to_str( ts_round_day( time() ) - 2 * 24 * 60 * 60 );
    my $yesterday  = ts_to_str( ts_round_day( time() ) - 24 * 60 * 60 );
    my $today      = ts_to_str( ts_round_day( time() ) );
    my $tomorrow   = ts_to_str( ts_round_day( time() ) + 24 * 60 * 60 );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ without => 'start_date' })
        ),
        [
            { start_date => vr_errors(qr/^Не указано значение в поле #field#$/) }, # vr_errors('ReqField'),
        ],
        'error when start date not specified'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ start_date => '' })
        ),
        [
            { start_date => vr_errors(qr/^В поле #field# указано пустое значение$/) }, # vr_errors('EmptyField'),
        ],
        'error when start date is empty'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ start_date => 'abcd-ef-gh' })
        ),
        [
            { start_date => vr_errors(qr/^Значение даты в поле #field# указано в неправильном формате$/) }, # vr_errors('InvalidField_IncorrectDate'),
        ],
        'error when start date is incorrect'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ start_date => $yesterday })
        ),
        [
            { start_date => vr_errors(qr/^Значение даты в поле #field# не может быть меньше текущей даты$/) }, # vr_errors('InconsistentState'),
        ],
        'error on add when start date less than current date'
    );

    my $camp1 = get_objects({ start_date => $day_before })->[0];
    $camp1->old(
        get_objects({ start_date => $yesterday })->[0]
    );

    cmp_validation_result(
        validate_campaigns([ $camp1 ]),
        [
            { start_date => vr_errors(qr/^Значение даты в поле #field# не может быть меньше текущей даты$/) }, # vr_errors('InconsistentState'),
        ],
        'error on update when start date less than current date'
    );

    my $camp2 = get_objects({ start_date => $yesterday })->[0];
    $camp2->old(
        get_objects({ start_date => $yesterday })->[0]
    );

    cmp_validation_result(
        validate_campaigns([ $camp2 ]),
        {},
        'no error on update when start date less than current date, but not changed'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ start_date => $tomorrow })
        ),
        {},
        'no error when start date is correct'
    );
}

sub finish_date : Tests( 6 ) {

    my $day_before = ts_to_str( ts_round_day( time() ) - 2 * 24 * 60 * 60 );
    my $yesterday  = ts_to_str( ts_round_day( time() ) - 24 * 60 * 60 );
    my $today      = ts_to_str( ts_round_day( time() ) );
    my $tomorrow   = ts_to_str( ts_round_day( time() ) + 24 * 60 * 60 );
    my $day_after  = ts_to_str( ts_round_day( time() ) + 2 * 24 * 60 * 60 );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ finish_date => '' })
        ),
        [
            { finish_date => vr_errors(qr/^В поле #field# указано пустое значение$/) }, # vr_errors('EmptyField'),
        ],
        'error when finish date is empty'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ finish_date => 'abcd-ef-jh' })
        ),
        [
            { finish_date => vr_errors(qr/^Значение даты в поле #field# указано в неправильном формате$/) }, # vr_errors('InvalidField_IncorrectDate'),
        ],
        'error when finish date is incorrect'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ start_date => $today, finish_date => $yesterday })
        ),
        [
            { finish_date => vr_errors(qr/^Значение даты в поле #from# не может быть больше значения даты в поле #to#$/) }, # vr_errors('InconsistentState'),
        ],
        'error when finish date less than start date'
    );

    my $camp1 = get_objects({ start_date => $day_before, finish_date => $day_before })->[0];
    $camp1->old(
        get_objects({ start_date => $day_before, finish_date => $yesterday })->[0]
    );

    cmp_validation_result(
        validate_campaigns([ $camp1 ]),
        [
            { finish_date => vr_errors(qr/^Значение даты в поле #field# не может быть меньше текущей даты$/) }, # vr_errors('InconsistentState'),
        ],
        'error on update when finish date changed and less than current date'
    );

    my $camp2 = get_objects({ start_date => $yesterday, finish_date => $today })->[0];
    $camp2->old(
        get_objects({ start_date => $yesterday, finish_date => $yesterday })->[0]
    );

    cmp_validation_result(
        validate_campaigns([ $camp2 ]),
        {},
        'no error on update when finish date changed and not less than current date'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({
                campaign_type => 'text',
                start_date    => $today,
                finish_date   => $tomorrow,
            })
        ),
        {},
        'no error when finish date is correct'
    );
}

sub disabled_domains : Tests( 3 ) {
    # for more tests for disabled domains see
    # unit_tests/Direct/Validation/domains.t

    cmp_validation_result(
        validate_campaigns(
            get_objects({ disabled_domains => [ join('' => 'www.', ('пыщь' x 64), '.рф') ], client_id => "123" })
        ),
        [
            { disabled_domains => vr_errors(qr/^Элемент [^ ]+ списка #field# - неправильный формат домена или идентификатора мобильного приложения$/) }, # vr_errors('InvalidFormat'),
        ],
        'error when disabled domains contain incorrect value'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ disabled_domains => [ map { "domain$_.ru" } 0 .. $Settings::DEFAULT_GENERAL_BLACKLIST_SIZE_LIMIT ], client_id => "123" })
        ),
        [
            { disabled_domains => vr_errors(qr/^Размер списка #field# превышает максимально допустимый размер /) }, # vr_errors('ReachLimit'),
        ],
        'max domains qty, error if list of domains changed'
    );

    # проверим как коструктор десериализует значения из строки
    cmp_validation_result(
        validate_campaigns(
            get_objects({ _disabled_domains => 'www.google.com раз, www.rambler.ru два', client_id => "123" })
        ),
        {},
        'no error when disabled domains are correct'
    );
}


sub disabled_ips : Tests( 2 ) {
    # for more tests for disabled ips see
    # unit_tests/Direct/Validation/ips.t

    cmp_validation_result(
        validate_campaigns(
            get_objects({ disabled_ips  => [ 'a.b.c.d' ] })
        ),
        [
            { disabled_ips => vr_errors(qr/^Элемент [^ ]+ списка #field# - неправильный формат IP-адреса$/) }, # vr_errors('InvalidFormat'),
        ],
        'error when disabled ips contain incorrect value'
    );

    # проверим как коструктор десериализует значения из строки
    cmp_validation_result(
        validate_campaigns(
            get_objects({ _disabled_ips  => '   7.7.7.7,    8.8.8.8 , 9.9.9.9,    ' })
        ),
        {},
        'no error when disabled ips are correct'
    );
}

sub timezone_id : Tests( 2 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects({ timezone_id => 1000 }) # seems that max timezone_id = 767
        ),
        [
            { timezone_id => vr_errors(qr/^В поле #field# указано некорректное значение$/) }, # vr_errors('InvalidField'),
        ],
        'error when timezone id is incorrect'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ timezone_id => 130 }) # Europe/Moscow
        ),
        {},
        'no error when timezone id is correct'
    );
}

sub time_target : Tests( 5 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects({ without => 'time_target' }),
        ),
        [
            { time_target => vr_errors(qr/^Не указано значение в поле #field#$/) }, # vr_errors('ReqField'),
        ],
        'error when time target is not set'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ time_target => '1JbKbLbMbNbObPbQbRbSb2JbKbLbMbNbObPbQbRbSb3JbKbLbMbNbObPbQbRbSb4JbKbLbMbNbObPbQbRbSb5JbKbLbMbNbObPbQbRbSb;', _autobudget => 'No' })
        ),
        {},
        'no error when extended time target is allowed and correct'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ time_target => '1JKLMNOPQRS2JKLMNOPQRS3JKLMNOPQRS4JKLMNOPQRS5JKLMNOPQRS8JKLMNOPQRS;p:a' })
        ),
        [
            { time_target => vr_errors(qr/^Временной таргетинг задан неверно$/) }, # vr_errors('InvalidFormat'),
        ],
        'error when time target preset is "all" and holidays are on'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ time_target => '1JKLMNOPQRS2JKLMNOPQRS3JKLMNOPQRS4JKLMNOPQRS5JKLMNOPQRS9;p:a' })
        ),
        [
            { time_target => vr_errors(qr/^Временной таргетинг задан неверно$/) }, # vr_errors('InvalidFormat'),
        ],
        'error when time target preset is "all" and working holidays are on'
    );

    # NB: из-за того, что validate_campaigns использует TimeTarget::parse_timetarget, часть
    # ошибок валидации TimeTarget::validate_timetarget недостижима, т.к. parse_timetarget
    # (или, точнее, hours_hash) в некоторых ситуациях отбрасывает не валидные данные (например,
    # более 7 дней или больше 25 часов в день)

    cmp_validation_result(
        validate_campaigns(
            get_objects({ time_target => '1JKLMNOPQRS3JKLMNOPQRS5JKLMNOPQRS;' })
        ),
        [
            { time_target => vr_errors(qr/^Объявления должны быть включены не менее 40 часов в неделю в рабочие дни. Измените расписание показа объявлений.$/) }, # vr_errors('InvalidFormat'),
        ],
        'error when time targe has invalid format - too less working hours'
    );
}

sub geo : Tests( 13 ) {
    # for more tests for minus words see
    # unit_tests/GeoTools/*

    # TODO:
    #   * test for traslocal_ops specified as $camp->client_id

    my $russia_geo = 3;
    my $turkey_geo = 983;
    my $ukraine_geo = 187;

    my $ru_banner = Direct::Model::Banner->new( id => 1, title => 'Здравствуй, мир!', body => 'Как прекрасен белый свет!', title_extension => undef, client_id => 1, language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE, );
    my $uk_banner = Direct::Model::Banner->new( id => 2, title => 'Здрастуй, свїт!',  body => 'Як прекрасний білий свїт!', title_extension => undef, client_id => 1, language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE, );
    my $kk_banner = Direct::Model::Banner->new( id => 3, title => 'Сәлем, әлем!',     body => 'Қандай ғажап ақ жарық!', title_extension => undef, client_id => 1, language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE, );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => 'abc' }),
            translocal_tree => 'api'
        ),
        [
            { geo => vr_errors(qr/^Неверный или несуществующий регион abc$/) }, # vr_errors('BadGeo'),
        ],
        'error when geo is incorrect'
    );
    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $turkey_geo, adgroups => [ Direct::Model::AdGroup->new( banners => [ $uk_banner ] ) ] }),
            translocal_tree => 'api',
        ),
        [
            { geo => vr_errors(
                qr/^Геотаргетинг не может быть изменён, так как текст объявления 2 на украинском языке\. Возможен таргетинг только на Украину\.$/, # vr_errors('InvalidGeoTargeting')
            ) },
        ],
        'error when geo is not consistent with banner\'s texts, uk'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $turkey_geo, adgroups => [ Direct::Model::AdGroup->new( banners => [ $kk_banner ] ) ] }),
            translocal_tree => 'api',
        ),
        [
            { geo => vr_errors(
                qr/^Геотаргетинг не может быть изменён, так как текст объявления 3 на казахском языке\. Возможен таргетинг только на Казахстан\.$/, # vr_errors('InvalidGeoTargeting')
            ) },
        ],
        'error when geo is not consistent with banner\'s texts, kk'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $turkey_geo, adgroups => [ Direct::Model::AdGroup->new( banners => [ $uk_banner, $kk_banner ] ) ] }), # Belarus
            translocal_tree => 'api',
        ),
        [
            { geo => vr_errors(
                qr/^Геотаргетинг не может быть изменён, так как текст объявления 2 на украинском языке\. Возможен таргетинг только на Украину\.$/,    # vr_errors('InvalidGeoTargeting')
            ) },
        ],
        'error when geo is not consistent with banner\'s texts, uk & kk'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $russia_geo, content_lang => 'kk', adgroups => [ Direct::Model::AdGroup->new( banners => [ $ru_banner ] ) ] }),
            translocal_tree => 'api',
        ),
        [
            { geo => vr_errors(
                qr/^Геотаргетинг не может быть изменён, так как текст объявления 1 на казахском языке\. Возможен таргетинг только на Казахстан\.$/,    # vr_errors('InvalidGeoTargeting')
            ) },
        ],
        'error when geo is not consistent with campaign lang banner'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $russia_geo, adgroups => [ Direct::Model::AdGroup->new( banners => [ $ru_banner ] ) ] }),
            translocal_tree => 'api',
        ),
        {},
        'no error when geo is consistent with banner\'s texts'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $turkey_geo, content_lang=>'tr', adgroups => [ Direct::Model::AdGroup->new( banners => [ $uk_banner ] ) ] }),
            translocal_tree => 'api',
        ),
        {},
        'no error when geo is consistent with banner\'s texts'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $ukraine_geo, content_lang=>'uk', adgroups => [ Direct::Model::AdGroup->new( banners => [ $ru_banner ] ) ] }),
            translocal_tree => 'api',
        ),
        {},
        'no error when geo is consistent with banner\'s texts'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $ukraine_geo, content_lang=>'uk', adgroups => [ Direct::Model::AdGroup->new( banners => [ $kk_banner ] ) ] }),
            translocal_tree => 'api',
        ),
        {},
        'no error when geo is consistent with banner\'s texts'
    );

    my $ru_sitelink = Direct::Model::Sitelink->new( title => 'Новый год', description => 'Новогодние традиции' );
    my $uk_sitelink = Direct::Model::Sitelink->new( title => 'Новий рік', description => 'Новорічні традиції' );
    my $kk_sitelink = Direct::Model::Sitelink->new( title => 'Жаңа жыл',  description => 'Жаңа жылдық дәстүрлер' );

    $ru_banner->sitelinks_set( Direct::Model::SitelinksSet->new( links => [ $uk_sitelink,  ] ) );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $turkey_geo, adgroups => [ Direct::Model::AdGroup->new( banners => [ $ru_banner ] ) ] }),
            translocal_tree => 'api',
        ),
        {},
        'no error when geo is not consistent with banner\'s sitelink, uk'
    );

    $ru_banner->sitelinks_set( Direct::Model::SitelinksSet->new( links => [ $kk_sitelink ] ) );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $turkey_geo, adgroups => [ Direct::Model::AdGroup->new( banners => [ $ru_banner ] ) ] }),
            translocal_tree => 'api',
        ),
        {},
        'no error when geo is not consistent with banner\'s sitelink, kk'
    );

    $ru_banner->sitelinks_set( Direct::Model::SitelinksSet->new( links => [ $uk_sitelink ] ) );

    my $ru_banner_new = Direct::Model::Banner->new( id => 4, title => 'Здравствуй, мир!', body => 'Как прекрасен белый свет!', title_extension => undef, client_id => 1, language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE, );
    $ru_banner_new->sitelinks_set( Direct::Model::SitelinksSet->new( links => [ $kk_sitelink ] ) );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $turkey_geo, adgroups => [ Direct::Model::AdGroup->new( banners => [ $ru_banner, $ru_banner_new ] ) ] }),
            translocal_tree => 'api',
        ),
        {},
        'no error when geo is not consistent with banner\'s sitelinks, uk & kk'
    );

    $ru_banner->sitelinks_set( Direct::Model::SitelinksSet->new( links => [ $ru_sitelink ] ) );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ geo => $russia_geo, adgroups => [ Direct::Model::AdGroup->new( banners => [ $ru_banner ] ) ] }),
            translocal_tree => 'api',
        ),
        {},
        'no error when geo is consistent with banner\'s sitelink'
    );
}

sub minus_words : Tests( 3 ) {
    # for more tests for minus words see
    # unit_tests/Direct/Validation/minus_word.t

    cmp_validation_result(
        validate_campaigns(
            get_objects({ minus_words => [map {'абв' . (rand 1)} 1..$Settings::CAMPAIGN_MINUS_WORDS_LIMIT] })
        ),
        [
            { minus_words => vr_errors(qr/^Длина минус-фраз превышает \d+ символов.$/) }, # vr_errors('MaxLength'),
        ],
        'error when total length of all minus-words is too long'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ minus_words => ['а' x ($Direct::Validation::MinusWords::MAX_MINUS_WORD_LENGTH + 1)]})
        ),
        [
            { minus_words => vr_errors(qr/^Превышена допустимая длина слова в/) }, # vr_errors('MinusWords'),
        ],
        'error when any of minus-words is too long'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ minus_words => ['реферат'] })
        ),
        {},
        'no error when minus words are correct'
    );
}

sub competitors_domains : Tests( 4 ) {
    # проверим как конструктор десериализует значения из строки
    cmp_validation_result(
        validate_campaigns(
            get_objects({ _competitors_domains => join( '' => 'www.', ('пыщь' x 64), '.рф', ' ', 'www.', ('тыщь' x 64), '.рф' ) })
        ),
        [
            { competitors_domains => vr_errors( map { qr/^Элемент [^ ]+ списка #field# - неправильный формат домена$/} 0 .. 1 ) }, # vr_errors('InvalidFormat'),
        ],
        'error when competitors domains contain incorrect value'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ competitors_domains => [ qw/ www.google.com www.mail.ru www.rambler.ru/ ] })
        ),
        {},
        'no error when competitors domains are correct'
    );
}

sub broad_match : Tests( 5 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects(
                { broad_match_flag => 'Yes', broad_match_limit => 10, broad_match_goal_id => 13 },
                { broad_match_flag => 'Yes', broad_match_limit => 115, broad_match_goal_id => 0 },
            )
        ),
        [
            { broad_match_goal_id => vr_errors('NotFound') },
            {
                broad_match_limit => vr_errors(qr/^В поле #field# указано некорректное значение$/), # vr_errors('InvalidField'),
            },
        ],
        'error when broad match\'s settings contain incorrect values'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ broad_match_flag => 'Yes', broad_match_limit => -1, })
        ),
        [
            {
                broad_match_limit => vr_errors(qr/^В поле #field# указано некорректное значение$/), # vr_errors('InvalidField'),
            },
        ],
        'error when broad match\'s settings contain incorrect values'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ broad_match_flag => 'No', broad_match_limit => 200, })
        ),
        {},
        'no error when broad match off and invalid settings are given'
    );
    
    cmp_validation_result(
        validate_campaigns(
            get_objects({ without => 'broad_match_flag', broad_match_limit => 50, })
        ),
        {},
        'no error when broad match off and correct settings are given'
    );
    
        cmp_validation_result(
        validate_campaigns(
            get_objects({ broad_match_flag => 'Yes', broad_match_limit => 61, })
        ),
        {},
        'no error when broad match match\'s limit not multiple of ten'
    );
}

sub metrika_counters : Tests( 5 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects({ metrika_counters => [ 0 ] }, { metrika_counters => [ 4294967296 ] })
        ),
        [
            { metrika_counters => vr_errors(qr/^Элемент [^ ]+ списка #field# - неверное значение, корректное значение должно быть целым положительным$/) }, # vr_errors('InvalidField'),
            { metrika_counters => vr_errors(qr/^Элемент [^ ]+ списка #field# - неверное значение, корректное значение должно быть целым положительным$/) }, # vr_errors('InvalidField'),
        ],
        'error when metrika counters contain incorrect value'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ metrika_counters => [ 1 .. 101 ] })
        ),
        [
            { metrika_counters => vr_errors(qr/^Размер списка #field# превышает максимально допустимый размер 100$/) }, # vr_errors('ReachLimit'),
        ],
        'error when metrika counters contain too much counter ids'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ metrika_counters => [ 1 .. 100 ] })
        ),
        {},
        'no error when metrika counters are correct'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ _metrika_counters => '1 2  3   4    5     ' })
        ),
        {},
        'no error when metrika counters are correct, deserialize from space separated string'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ _metrika_counters => '1,2,,3,,,4,,,,5,,,,,' })
        ),
        {},
        'no error when metrika counters are correct, deserialize from comma separated string'
    );
}

sub device_target : Tests( 2 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects({ device_target => 'Ubuntu' })
        ),
        [
            { device_target => vr_errors(qr/^В поле #field# указано некорректное значение$/) }, # vr_errors('InvalidField'),
        ],
        'error when device target is empty'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ device_target => 'android_phone,android_tablet,ipad,iphone,other_devices' })
        ),
        {},
        'no error when device target is correct'
    );
}

sub hierarchical_multipliers : Tests( 3 ) {
    # for more tests for hierarchical multipliers see
    # unit_tests/Direct/Validation/hierarchical_multipliers.t

    cmp_validation_result(
        validate_campaigns(
            get_objects({ client_id => 1, hierarchical_multipliers => { mobile_multiplier => { multiplier_pct => 15000 } } })
        ),
        [
            {
                hierarchical_multipliers => {
                    mobile_multiplier => vr_errors(qr/^Значение коэффициента не может быть больше 1300$/), # vr_errors('InvalidField'),
                }
            }
        ],
        'error when hierarchical multipliers are incorrect'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ client_id => 1, hierarchical_multipliers => { mobile_multiplier => { multiplier_pct => 1000 } } })
        ),
        {},
        'no error when hierarchical multipliers are correct'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ client_id => 1, hierarchical_multipliers => {
                    mobile_multiplier => { multiplier_pct => 0 },
                    desktop_multiplier => { multiplier_pct => 0 }
                } })
        ),
        [
            {
                hierarchical_multipliers => {
                    mobile_multiplier  => vr_errors(qr/^Нельзя устанавливать корректировку -100% на все устройства$/),
                    desktop_multiplier => vr_errors(qr/^Нельзя устанавливать корректировку -100% на все устройства$/),
                }
            }
        ],
        'error when hierarchical multipliers are incorrect'
    );
}

sub sms_time_from : Tests( 7 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_from_hours => '-1', sms_time_from_minutes => '0', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #from# указано в неправильном формате$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_from is incorrect - hours are less than 0'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_from_hours => '25', sms_time_from_minutes => '0', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #from# указано в неправильном формате$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_from is incorrect - hours are greater than 24'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_from_hours => '0', sms_time_from_minutes => '-1', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #from# указано в неправильном формате$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_from is incorrect - minutes are less than 0'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_from_hours => '0', sms_time_from_minutes => '60', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #from# указано в неправильном формате$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_from is incorrect - minutes are greater than 59'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_from_hours => '24', sms_time_from_minutes => '5', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #from# указано в неправильном формате$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_from is incorrect - hours are 24 and minutes not equal 0'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_from_hours => '12', sms_time_from_minutes => '10', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #from# должно быть кратно 15$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_from is incorrect - minutes multiplicity not equal 15'
    );


    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_from_hours => '12', sms_time_from_minutes => '30', })
        ),
        {},
        'no error when sms_time_from is correct'
    );
}

sub sms_time_to : Tests( 7 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_to_hours => '-1', sms_time_to_minutes => '0', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #to# указано в неправильном формате$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_to is incorrect - hours are less than 0'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_to_hours => '25', sms_time_to_minutes => '0', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #to# указано в неправильном формате$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_to is incorrect - hours are greater than 24'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_to_hours => '0', sms_time_to_minutes => '-1', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #to# указано в неправильном формате$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_to is incorrect - minutes are less than 0'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_to_hours => '0', sms_time_to_minutes => '60', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #to# указано в неправильном формате$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_to is incorrect - minutes are greater than 59'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_to_hours => '24', sms_time_to_minutes => '5', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #to# указано в неправильном формате$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_to is incorrect - hours are 24 and minutes not equal 0'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_to_hours => '12', sms_time_to_minutes => '10', })
        ),
        [
            { sms_time => vr_errors(qr/^Значение времени в поле #to# должно быть кратно 15$/) }, # vr_errors('InvalidField'),
        ],
        'error when sms_time_to is incorrect - minutes multiplicity not equal 15'
    );


    cmp_validation_result(
        validate_campaigns(
            get_objects({ sms_time_to_hours => '12', sms_time_to_minutes => '30', })
        ),
        {},
        'no error when sms_time_to is correct'
    );
}

sub money_warning_threshold : Tests( 3 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects({ without => 'money_warning_threshold' })
        ),
        [
            { money_warning_threshold => vr_errors(qr/^Не указано значение в поле #field#$/) }, # vr_errors('ReqField'),
        ],
        'error when money warning threshold is empty'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ money_warning_threshold => 0 }, { money_warning_threshold => 55 })
        ),
        [
            { money_warning_threshold => vr_errors(qr/^Значение в поле #field# должно быть от 1 до 50 \%$/) }, # vr_errors('InvalidField'),
            { money_warning_threshold => vr_errors(qr/^Значение в поле #field# должно быть от 1 до 50 \%$/) }, # vr_errors('InvalidField'),
        ],
        'error when money warning threshold is incorrect'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ money_warning_threshold => 25 })
        ),
        {},
        'no error when money warning threshold are correct'
    );
}

sub position_check_interval : Tests( 3 ) {
    cmp_validation_result(
        validate_campaigns(
            get_objects({ without => 'position_check_interval' })
        ),
        [
            { position_check_interval => vr_errors(qr/^Не указано значение в поле #field#$/) }, # vr_errors('ReqField'),
        ],
        'error when position check interval is empty'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ position_check_interval => 10 })
        ),
        [
            { position_check_interval => vr_errors(qr/^Значение в поле #field# должно совпадать с одним из значений 15, 30, 60$/) }, # vr_errors('InvalidField'),
        ],
        'error when position check interval is incorrect'
    );

    cmp_validation_result(
        validate_campaigns(
            get_objects({ position_check_interval => 30 })
        ),
        {},
        'no error when position check interval are correct'
    );
}

sub get_objects {

    my $today = ts_to_str( ts_round_day( time() ) + 24 * 60 * 60 );

    my $default = {
        campaign_type           => 'text',
        campaign_name           => 'Тестовая кампания #1',
        client_fio              => 'Иван Петрович Сидоров',
        email                   => 'mail@yandex.ru',
        start_date              => $today,
        time_target             => '1JKLMNOPQRS2JKLMNOPQRS3JKLMNOPQRS4JKLMNOPQRS5JKLMNOPQRS;',
        timezone_id             => 0,
        _autobudget             => 'No',
        broad_match_flag        => 'Yes',
        broad_match_limit       => 50,
        money_warning_threshold => 20,
        position_check_interval => 15,
        content_lang            => '',
        client_id               => 123,
    };

    my @res;
    for my $pref ( @_ ) {
        my $state = { %$default };

        my $without = delete $pref->{without};
        if ( $without ) {
            if ( ! ref $without ) {
                delete $state->{ $without };
            } elsif ( ref $without eq 'ARRAY' ) {
                delete @$state{ @$without };
            }
        }

        while ( my ( $n, $v ) = each %$pref ) {
            $state->{ $n } = $v;
        }

        push @res, Direct::Model::Campaign->new( %$state );
    }

    return \@res;
}

__PACKAGE__->runtests();
