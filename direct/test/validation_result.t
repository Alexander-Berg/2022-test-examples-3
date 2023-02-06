use strict;
use warnings;
use utf8;

package Test::Errors;

use Direct::Errors;

error ReqField => (code => 101, text => '');
error BadLang => (code => 102, text => '');
error InvalidChars => (code => 892, text => '');
error 'BannersLimit' => (code => 103, text => '');
error SitelinksLimit => (code => 203, text => '');
warning UnreachableHref => (code => 307, text => '');
error EmptyParam => (code => 304, text => '');
warning BadLang => (code => 102, text => '');
error MinusWords => (code => 105, text => '');

package main;

use Test::More;
use Test::Deep;
use Test::Exception;
use Direct::ValidationResult;

Test::Errors->import();

BEGIN { use_ok('Yandex::Test::ValidationResult'); }

*vr_errors = \&Yandex::Test::ValidationResult::vr_errors;
*eq_validation_result = \&Yandex::Test::ValidationResult::eq_validation_result;

cmp_deeply(Direct::ValidationResult->new->convert_to_hash, {});

my $vr_1 = Direct::ValidationResult->new;
$vr_1->add_generic(error_BannersLimit());
my $o1_vr_1 = $vr_1->next;
$o1_vr_1->add(title => warning_BadLang());
$o1_vr_1->add(title => error_InvalidChars('Не допустимые символы в заголовке'));
$o1_vr_1->add(body => error_InvalidChars());
$o1_vr_1->add_generic(error_EmptyParam('Отсутсвует param1|param2'));
$vr_1->next->add(title => undef);
my $o3_vr_1 = $vr_1->next;
$o3_vr_1->add(href => error_InvalidChars());
$o3_vr_1->add(body => undef);
$vr_1->next;

cmp_deeply(
    $vr_1->convert_to_hash,
    {
        generic_errors => [superhashof({name => 'BannersLimit'})],
        objects_results => [
            {
                generic_errors => [superhashof({name => 'EmptyParam', description => 'Отсутсвует param1|param2'})],
                title => bag(
                    superhashof({name => 'BadLang'}), 
                    superhashof({name => 'InvalidChars', description => 'Не допустимые символы в заголовке'}),
                ),
                body => [superhashof({name => 'InvalidChars'})],
            },
            {},
            {href => [superhashof({name => 'InvalidChars'})]},
            {}
        ]
    }
);

my $are_eq = eq_validation_result($vr_1, {});
ok(!$are_eq);

$are_eq = eq_validation_result(
    $vr_1,
    {
        generic_errors => [],
        objects_results => [
            {body => vr_errors('InvalidChars')},
            {},
            {href => vr_errors('InvalidChars')},
            {}
        ]
    }
);
ok(!$are_eq);

$are_eq = eq_validation_result(
    $vr_1,
    {
        generic_errors => vr_errors('BannersLimit'),
        objects_results => [
            {
                generic_errors => vr_errors('EmptyParam'),
                # qr// - проверка текстового описания ошибки на соответсвие шаблону
                title => vr_errors('BadLang', qr/Не допустимые символы в заголовке/),
                body => vr_errors('InvalidChars'), 
            },
            {},
            {href => vr_errors('InvalidChars')},
            {}
        ]
    }
);
ok($are_eq);

# проверка аналогично проверке выше (только сравниваем названия ошибок)
$are_eq = eq_validation_result(
    $vr_1,
    {
        generic_errors => vr_errors('BannersLimit'),
        objects_results => [
            {
                generic_errors => vr_errors('EmptyParam'),
                title => vr_errors('InvalidChars', 'BadLang'),
                body => vr_errors('InvalidChars'), 
            },
            {},
            {href => vr_errors('InvalidChars')},
            {}
        ]
    }
);
ok($are_eq);



# adgroup
my $vr_2 = Direct::ValidationResult->new;
$vr_2->next->add(minus_words => error_ReqField('Минус слова необходимо задать'));;
$vr_2->next->add(keywords => Direct::ValidationResult->new);
$vr_2->next->add(keywords => Direct::ValidationResult->new);
my $o3_vr_2 = $vr_2->next;
$o3_vr_2->add(banners => $vr_1);
$o3_vr_2->add(keywords => error_ReqField('Отсутствую ключевые фразы у группы объявлений'));

cmp_deeply(
    $vr_2->convert_to_hash,
    {
        objects_results => [
            {
                minus_words => [superhashof({description => 'Минус слова необходимо задать'})]
            },
            {},
            {},
            {
                banners => {
                    generic_errors => [superhashof({name => 'BannersLimit'})],
                    objects_results => [
                        {
                            generic_errors => [superhashof({name => 'EmptyParam', description => 'Отсутсвует param1|param2'})],
                            title => bag(
                                superhashof({name => 'BadLang'}), 
                                superhashof({name => 'InvalidChars', description => 'Не допустимые символы в заголовке'}),
                            ),
                            body => [superhashof({name => 'InvalidChars'})],
                        },
                        {},
                        {href => [superhashof({name => 'InvalidChars'})]},
                        {}
                    ]
                },
                keywords => [superhashof({description => 'Отсутствую ключевые фразы у группы объявлений'})]
            }
        ]
    }
);

$are_eq = eq_validation_result(
    $vr_2,
    [
        {
            minus_words => vr_errors(qr/Минус слова необходимо задать/)
        },
        {},
        {},
        {
            banners => {
                generic_errors => vr_errors('BannersLimit'),
                objects_results => [
                    {
                        generic_errors => vr_errors('EmptyParam'),
                        title => vr_errors('BadLang', qr/Не допустимые символы в заголовке/),
                        body => vr_errors('InvalidChars'), 
                    },
                    {},
                    {href => vr_errors('InvalidChars')},
                    {}
                ]
            },
            keywords => vr_errors(qr/Отсутствую ключевые фразы/)
        }
    ]
);
ok($are_eq);

throws_ok {
    eq_validation_result(
        Direct::ValidationResult->new,
        [bless {}, 'SomeClass']
    )
} qr/unhadle ref/;

# 3 уровня вложенности ValidationResult 
# adgroup
my $vr_3 = Direct::ValidationResult->new;

$vr_3->next;
my $o2_vr_3 = $vr_3->next;
$o2_vr_3->add(group_name => error_InvalidChars());

#banners
my $vr_3_1 = Direct::ValidationResult->new;
my $o1_vr_3_1 = $vr_3_1->next;
$o1_vr_3_1->add(title => error_BadLang('Язык не соответсвует гео'));
$o1_vr_3_1->add(title => error_InvalidChars('Не допустимый символ'));

#sitelinks
my $vr_3_1_1 = Direct::ValidationResult->new;
$vr_3_1_1->add_generic(error_SitelinksLimit());
$vr_3_1_1->next;
$vr_3_1_1->next->add(sitelink => warning_UnreachableHref());

$o1_vr_3_1->add(sitelinks => $vr_3_1_1);
$o2_vr_3->add(banners => $vr_3_1);

$are_eq = eq_validation_result(
    $vr_3,
    [
        {},
        {
            group_name => vr_errors('InvalidChars'),
            banners => [
                {
                    title => vr_errors(qr/Язык не соответсвует гео/, qr/Не допустимый символ/),
                    sitelinks => {
                        generic_errors => vr_errors('SitelinksLimit'),
                        objects_results => [
                            {},
                            {sitelink => vr_errors('UnreachableHref')}
                        ]
                    }
                }
            ]
        }
    ]
);
ok($are_eq);

done_testing;
