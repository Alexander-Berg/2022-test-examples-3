#!/usr/bin/perl

# $Id$

use Direct::Modern;

use Test::More;
use Test::Deep;
use Data::Dumper;
use Yandex::Test::UTF8Builder;
use Yandex::DBUnitTest qw/copy_table/;

use Settings;

use TimeTarget;

sub bt {
    my $tt = shift;
    print STDERR Dumper(tt => $tt) if $ENV{DEBUG};
    my $res = TimeTarget::bs_timetarget($tt);
    print STDERR Dumper(res => $res) if $ENV{DEBUG};
    return $res;
}

sub bt_coef {
    my $tt = shift;
    print STDERR Dumper(tt => $tt) if $ENV{DEBUG};
    my $res = TimeTarget::bs_timetarget_coef($tt);
    print STDERR Dumper(res => $res) if $ENV{DEBUG};
    return $res;
}

copy_table(PPCDICT, 'geo_timezones');

cmp_deeply(bt(''), undef);

cmp_deeply(bt(undef), undef);

cmp_deeply(bt('1A'), {TargetTime => ["234567BCDEFGHIJKLMNOPQRSTUVWX"], TargetTimeWorking => 0});

cmp_deeply(bt('2BbCbD3BCD6A7BC9'), {
    TargetTime => bag('14567AEFGHIJKLMNOPQRSTUVWX',
        '123457BCDEFGHIJKLMNOPQRSTUVWX',
        '123456ADEFGHIJKLMNOPQRSTUVWX'),
    TargetTimeWorking => 1,
});

cmp_deeply(bt('123----AB----------------------'), {TargetTime => ["123AB"], TargetTimeWorking => 0});
cmp_deeply(bt('--3-5---B-D--------------------'), {TargetTime => ["35BD"], TargetTimeWorking => 0});

cmp_deeply(bt('2BCD3BCD4BCD'), {TargetTime => ["1567AEFGHIJKLMNOPQRSTUVWX"], TargetTimeWorking => 0});

cmp_deeply(bt('2BCD3BD4BCD'), {TargetTime => bag("13567AEFGHIJKLMNOPQRSTUVWX", "124567ACEFGHIJKLMNOPQRSTUVWX"), TargetTimeWorking => 0});

cmp_deeply(bt('2BCD3BCD4BCD8'), {TargetTimeLike => bag("234BCD"), TargetTimeWorking => 0});

cmp_deeply(bt('2BCD3BCD4BCD8BCD'), {TargetTimeLike => ["2348BCD"], TargetTimeWorking => 0});

cmp_deeply(bt('2BCD3BCD4BCD8ABCD'), {TargetTimeLike => bag("234BCD", "8ABCD"), TargetTimeWorking => 0});

cmp_deeply(bt('1BCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX'), {TargetTime => ["167", "234567A"], TargetTimeWorking => 0});
cmp_deeply(bt('1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX'), {TargetTime => ["67"], TargetTimeWorking => 0});

# with coef
cmp_deeply(bt('1Ab'), {TargetTime => ["234567BCDEFGHIJKLMNOPQRSTUVWX"]
                     , TargetTimeWorking => 0
                     });

cmp_deeply(bt('2BbCbDb3BcCcDc4BdCdDd'), {TargetTime => ["1567AEFGHIJKLMNOPQRSTUVWX"]
                                       , TargetTimeWorking => 0
                                       });
# коэффициент 10% только на один день
cmp_deeply(bt('2BbCD3BCD4BCD8BCD'), {TargetTimeLike => ["2348BCD"]
                                    , TargetTimeWorking => 0
                                    });

# коэффициенты только на праздники
cmp_deeply(bt('2BCD3BCD4BCD8BjCjDj'), {TargetTimeLike => ["2348BCD"]
                                    , TargetTimeWorking => 0
                                    });

# параметр TimeTargetCoef
my $bs_day_0pr = 'a' x 24; # весь день отключен (0%)
my $bs_day_100pr = 'k' x 24; # весь день включен (100%)
my $bs_day_no_set_pr = 'z' x 24; # без учета праздников

is(bt_coef(undef), '');
is(bt_coef(''), '');
is(bt_coef('1A'), '');
is(bt_coef('2BCD3BCD4BCD'), '');

# 00 часов пн - 10%, в остальное время 0%);
is(bt_coef('1Ab'),
   'b' . ('a' x 23)
   . ($bs_day_0pr x 6)
   . $bs_day_no_set_pr
);

is(bt_coef('2BbCbDb3BcCcDc4BdCdDd'),
    $bs_day_0pr
    . 'abbbaaaaaaaaaaaaaaaaaaaa'
    . 'acccaaaaaaaaaaaaaaaaaaaa'
    . 'adddaaaaaaaaaaaaaaaaaaaa'
    . $bs_day_0pr
    . $bs_day_0pr
    . $bs_day_0pr
    . $bs_day_no_set_pr
);

# коэффициент 10% только на один день
is(bt_coef('2BbCD3BCD4BCD8BCD'), 
    $bs_day_0pr
    . 'abkkaaaaaaaaaaaaaaaaaaaa'
    . 'akkkaaaaaaaaaaaaaaaaaaaa'
    . 'akkkaaaaaaaaaaaaaaaaaaaa'
    . $bs_day_0pr
    . $bs_day_0pr
    . $bs_day_0pr
    . 'akkkaaaaaaaaaaaaaaaaaaaa'
);

# коэффициент 110%, 200% только на 2 дня
is(bt_coef('2BlCuD3BCD4BCD8BCD'), 
    $bs_day_0pr
    . 'alukaaaaaaaaaaaaaaaaaaaa'
    . 'akkkaaaaaaaaaaaaaaaaaaaa'
    . 'akkkaaaaaaaaaaaaaaaaaaaa'
    . $bs_day_0pr
    . $bs_day_0pr
    . $bs_day_0pr
    . 'akkkaaaaaaaaaaaaaaaaaaaa'
);

# коэффициенты только на праздники
is(bt_coef('2BCD3BCD4BCD8BjCjDj'),
    $bs_day_0pr
    . 'akkkaaaaaaaaaaaaaaaaaaaa'
    . 'akkkaaaaaaaaaaaaaaaaaaaa'
    . 'akkkaaaaaaaaaaaaaaaaaaaa'
    . $bs_day_0pr
    . $bs_day_0pr
    . $bs_day_0pr
    . 'ajjjaaaaaaaaaaaaaaaaaaaa'
);

# Поведение со строкой настроек
cmp_deeply(bt(';'), undef);
cmp_deeply(bt(';p:'), undef);
cmp_deeply(bt('2BbCD3BCD4BCD8BCD;p:a'), {TargetTimeLike => ["2348BCD"]
                                    , TargetTimeWorking => 0
                                    });

is(bt_coef(';'), '');
is(bt_coef(';p:'), '');
is(bt_coef('1A;p:w'), '');
is(bt_coef('2BCD3BCD4BCD;p:a'), '');

done_testing();
