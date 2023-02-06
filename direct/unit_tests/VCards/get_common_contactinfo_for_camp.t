#!/usr/bin/perl

=pod

    Проверяем, правильно ли определяется "единая" визитка на кампании

=cut

#  $Id$

use strict;
use warnings;

use Test::More;


use Settings;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;
use VCards;

use utf8;

$Yandex::DBTools::DONT_SEND_LETTERS = 1;

my $PH = "+7#111#222-22-22";

# есть ли у кампании единая КИ?
sub has_common_ci_default
{
    my $cid = shift;
    my $vcard = get_common_contactinfo_for_camp($cid);
    return $vcard && $vcard->{phone} ? 1 : 0;
}

# имеет ли кампания ровно одну непустую визитку?
sub has_common_ci_skip_empty
{
    my $cid = shift;
    my $vcard = get_common_contactinfo_for_camp($cid, {skip_empty => 1});
    return $vcard && $vcard->{phone} ? 1 : 0;
}

# готовим данные для таблиц

# кампании
# $cid div 10 -- количество объявлений в кампании
my $campaigns_rows = [
    # кампании без объявлений
    {cid => 1, }, # без ЕКИ
    {cid => 2, }, # с ЕКИ в camp_options
    # кампании с 1 объявлением
    {cid => 10, }, # без КИ
    {cid => 11, }, # с КИ
    # кампании с 2 объявлениями 
    {cid => 20, }, # без КИ
    {cid => 21, }, # 1 без, 1 с
    {cid => 22, }, # 2 с КИ_1
    {cid => 23, }, # 1 с КИ_1, 1 с КИ_2
    # кампании с 3 объявлениями 
    {cid => 30, }, # 3 без
    {cid => 31, }, # 2 без, 1 с КИ_1
    {cid => 32, }, # 1 без, 2 с КИ_1
    {cid => 33, }, # 1 без, 1 с КИ_1, 1 с КИ_2
    {cid => 34, }, # 3 с КИ_1
    {cid => 35, }, # 2 с КИ_1, 1 с КИ_2
    {cid => 36, }, # 1 с КИ_1, 1 с КИ_2, 1 с КИ_3
];

my $camp_options_rows = {
    without_camp_options_contactinfo => [
        {cid => 2, contactinfo => VCards::serialize({phone => '123-45-67'}) },
    ],
    with_camp_options_contactinfo => [
        {cid => 2, contactinfo => VCards::serialize({phone => '123-45-67'}) },
        map { {cid => $_->{cid}, contactinfo => VCards::serialize({phone => '123-45-67'})} } 
        grep { $_->{cid} >= 10 }  
        @$campaigns_rows,
    ],
};

# объявления
my $banners_rows = [
    # по одному объявлению
    {cid => 10, bid => 100, },
    {cid => 11, bid => 110, vcard_id => 110},
    
    # по 2 объявления
    {cid => 20, bid => 200,},
    {cid => 20, bid => 201,},

    {cid => 21, bid => 210,},
    {cid => 21, bid => 211, vcard_id => 210},

    {cid => 22, bid => 220, vcard_id => 220},
    {cid => 22, bid => 221, vcard_id => 220},

    {cid => 23, bid => 230, vcard_id => 230},
    {cid => 23, bid => 231, vcard_id => 231},

    # по 3 объявления
    {cid => 30, bid => 300,},
    {cid => 30, bid => 301,},
    {cid => 30, bid => 302,},

    {cid => 31, bid => 310,},
    {cid => 31, bid => 311,},
    {cid => 31, bid => 312, vcard_id => 310},

    {cid => 32, bid => 320,},
    {cid => 32, bid => 321, vcard_id => 320},
    {cid => 32, bid => 322, vcard_id => 320},

    {cid => 33, bid => 330,},
    {cid => 33, bid => 331, vcard_id => 330},
    {cid => 33, bid => 332, vcard_id => 331},

    {cid => 34, bid => 340, vcard_id => 340},
    {cid => 34, bid => 341, vcard_id => 340},
    {cid => 34, bid => 342, vcard_id => 340},

    {cid => 35, bid => 350, vcard_id => 350},
    {cid => 35, bid => 351, vcard_id => 350},
    {cid => 35, bid => 352, vcard_id => 351},

    {cid => 36, bid => 360, vcard_id => 360},
    {cid => 36, bid => 361, vcard_id => 361},
    {cid => 36, bid => 362, vcard_id => 362},
];

# визитки
my $vcards_rows = [
    {cid => 11, vcard_id => 110, phone => $PH,},
    
    {cid => 21, vcard_id => 210, phone => $PH,},
    {cid => 22, vcard_id => 220, phone => $PH,},
    {cid => 23, vcard_id => 230, phone => $PH,},
    {cid => 23, vcard_id => 231, phone => $PH,},

    {cid => 31, vcard_id => 310, phone => $PH,},
    {cid => 32, vcard_id => 320, phone => $PH,},
    {cid => 33, vcard_id => 330, phone => $PH,},
    {cid => 33, vcard_id => 331, phone => $PH,},
    {cid => 34, vcard_id => 340, phone => $PH,},
    {cid => 35, vcard_id => 350, phone => $PH,},
    {cid => 35, vcard_id => 351, phone => $PH,},
    {cid => 36, vcard_id => 360, phone => $PH,},
    {cid => 36, vcard_id => 361, phone => $PH,},
    {cid => 36, vcard_id => 362, phone => $PH,},
];

# вносим данные в базу
my %db = (
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [ grep {  $_->{cid} % 2 } @$campaigns_rows ],
            2 => [ grep {!($_->{cid} % 2)} @$campaigns_rows ],
        },
    },
    camp_options => {
        original_db => PPC(shard => 'all'),
        # заполняется непосредственно в тестах
    },
    banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [ grep {  $_->{cid} % 2 } @$banners_rows ],
            2 => [ grep {!($_->{cid} % 2)} @$banners_rows ],
        },
    },
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [ grep {  $_->{cid} % 2 } @$vcards_rows ],
            2 => [ grep {!($_->{cid} % 2)} @$vcards_rows ],
        },
    },
    addresses => {
        original_db => PPC(shard => 'all'),
    },
    maps => {
        original_db => PPC(shard => 'all'),
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
            {ClientID => 2, shard => 2},
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [ map { { cid => $_->{cid}, ClientID => (2 - $_->{cid} % 2) } } @$campaigns_rows ],
    },
    shard_inc_bid => {
        original_db => PPCDICT,
        rows => [ map { { bid => $_->{bid}, ClientID => (2 - $_->{cid} % 2) } } @$banners_rows ]
    },
    shard_inc_vcard_id => {
        original_db => PPCDICT,
        rows => [ map { { vcard_id => $_->{vcard_id}, ClientID => (2 - $_->{cid} % 2) } } @$vcards_rows ],
    },
);
init_test_dataset(\%db);

# эталонные результаты: есть или нет у кампании единая визитка 
my %standard_results = (
    1 => {
        default    => 0, 
        skip_empty => 0, 
    },
    2 => {
        default    => 1, 
        skip_empty => 1, 
    },

    10 => {
        default    => 0, 
        skip_empty => 0, 
    },
    11 => {
        default    => 1, 
        skip_empty => 1, 
    },

    20 => {
        default    => 0, 
        skip_empty => 0, 
    },
    21 => {
        default    => 0, 
        skip_empty => 1, 
    },
    22 => {
        default    => 1, 
        skip_empty => 1, 
    },
    23 => {
        default    => 0, 
        skip_empty => 0, 
    },

    30 => {
        default    => 0, 
        skip_empty => 0, 
    },
    31 => {
        default    => 0, 
        skip_empty => 1, 
    },
    32 => {
        default    => 0, 
        skip_empty => 1, 
    },
    33 => {
        default    => 0, 
        skip_empty => 0, 
    },
    34 => {
        default    => 1, 
        skip_empty => 1, 
    },
    35 => {
        default    => 0, 
        skip_empty => 0, 
    },
    36 => {
        default    => 0, 
        skip_empty => 0, 
    },
);


# был когда-то баг для кампании с объявлениями визитка из camp_options должна игнорироваться,
# но не игнорировалась: DIRECT-11093
# Как проверять: тесты должны нормально проходить с полным @types 
my @types = qw/without_camp_options_contactinfo with_camp_options_contactinfo/;
# my @types = qw/without_camp_options_contactinfo/;

Test::More::plan(tests => (2 * @types * @$campaigns_rows));

# для каждой кампании сравниваем результат с эталоном
for my $type (@types){
    my $sharded_camp_options_row = {
        1 => [ grep {$_->{cid} % 2} @{ $camp_options_rows->{$type} } ],
        2 => [ grep {!($_->{cid} % 2)} @{ $camp_options_rows->{$type} } ],
    };

    # shard => all - на самом деле нужно только для того, чтобы функция поняла, что нужно работать с шардами.
    # шард для замены данных выбирается из хеша с данными
    replace_test_data(PPC(shard => 'all'), 'camp_options', $sharded_camp_options_row);
    
    for my $cid ( map {$_->{cid}} @$campaigns_rows){
        die "there isn't standard result for cid $cid" unless exists $standard_results{$cid};
        my $tc = $standard_results{$cid};

        is(has_common_ci_default($cid), $tc->{default}, "($type) common ci for cid = $cid");
        is(has_common_ci_skip_empty($cid), $tc->{skip_empty}, "($type) common ci (skip empty) for cid = $cid" );
    }
}

