#!/usr/bin/perl

# $Id$

=head1 DESCRIPTION

    тестирует расчет количества стандартных, тяжелых и buggy потоков в экспорте в БК.
    Можно проверять количество стандартных (std), тяжелых (heavy), их суммарное количество (sum) и количество buggy потоков.

    Для добавления проверок надо пополнять массив @test_cases,
    для каждого кейса должна быть приведена статистика + проверки.
    + для каждого тесткейса выполняются "общие проверки" (@common_checks).

    Доступные проверки:
    __eq -- равно
    __gt -- строго больше
    __ge -- больше или равно
    __lt -- строго меньше
    __le -- меньше или равно

=cut

use warnings;
use strict;
use Test::More;

use Data::Dumper;
use List::Util qw/sum/;

use BS::Export;
use BS::ExportMaster;

use utf8;
use open ':std' => ':utf8';


my $std_max_num = $BS::ExportMaster::WORKERS_STD_MAX;
my $std_min_num = $BS::ExportMaster::WORKERS_STD_MIN;
my $heavy_max_num = $BS::ExportMaster::WORKERS_HEAVY_MAX;
my $heavy_min_num = $BS::ExportMaster::WORKERS_HEAVY_MIN;
my $buggy_max_num = $BS::ExportMaster::WORKERS_BUGGY_MAX;
my $buggy_min_num = $BS::ExportMaster::WORKERS_BUGGY_MIN;
my $std_heavy_max_num  = $BS::ExportMaster::WORKERS_STD_HEAVY_MAX;

# общие проверки
my @common_checks = (
    std__gt => $std_min_num,
    std__le => $std_max_num,
    heavy__ge => $heavy_min_num,
    heavy__le => $heavy_max_num,
    buggy__ge => $buggy_min_num,
    buggy__le => $buggy_max_num,
    sum__le => $std_heavy_max_num,
);

# тестовые сценарии с их специфичными проверками
my @test_cases = (
    {
        name => 'simple case',
        stat => {
            std => {
                integral_size => 100_000,
                max_age => 1000,
            },
            heavy => {
                integral_size => 350_000,
                max_age => 1500,
            },
            buggy=> {
                integral_size => 0,
                max_age => 0,
            },
        },
        check => [
        ],
    },
    {
        name => 'heavy heavy, small std',
        stat => {
            std => {
                integral_size => 50_000,
                max_age => 600,
            },
            heavy => {
                integral_size => 2_000_000,
                max_age => 15000,
            },
            buggy=> {
                integral_size => 0,
                max_age => 0,
            },
        },
        check => [
            heavy__eq => $heavy_max_num,
        ],
    },
    {
        name => 'huge not old std + no heavy',
        stat => {
            std => {
                integral_size => 10_000_000,
                max_age => 10*60,
            },
            heavy => {
                integral_size => 0,
                max_age => 0,
            },
            buggy=> {
                integral_size => 0,
                max_age => 0,
            },
        },
        check => [
            std__ge => $std_max_num/2,
            heavy__eq => $heavy_min_num,
        ],
    },
    {
        name => 'huge not old std + heavy heavy',
        stat => {
            std => {
                integral_size => 10_000_000,
                max_age => 10*60,
            },
            heavy => {
                integral_size => 10_000_000,
                max_age => 30*60,
            },
            buggy=> {
                integral_size => 0,
                max_age => 0,
            },
        },
        check => [
            std__ge => $std_max_num/2,
            heavy__ge => 2*$heavy_max_num/3,
        ],
    },
    {
        name => 'old small std',
        stat => {
            std => {
                integral_size => 10_000,
                max_age => 60*60,
            },
            heavy => {
                integral_size => 30_000,
                max_age => 30*60,
            },
            buggy=> {
                integral_size => 0,
                max_age => 0,
            },
        },
        check => [
            std__eq => $std_max_num,
            heavy__eq => $heavy_min_num,
        ],
    },
    {
        name => 'old small std + heavy heavy',
        stat => {
            std => {
                integral_size => 10_000,
                max_age => 60*60,
            },
            heavy => {
                integral_size => 50_000_000,
                max_age => 300*60,
            },
            buggy=> {
                integral_size => 0,
                max_age => 0,
            },
        },
        check => [
            std__eq => $std_max_num,
            heavy__eq => 8
        ],
    },
    {
        name => 'large old queue',
        stat => {
            std => {
                integral_size => 10_000_000,
                max_age => 10000,
            },
            heavy => {
                integral_size => 2_000_000,
                max_age => 15000,
            },
            buggy=> {
                integral_size => 0,
                max_age => 0,
            },
        },
        check => [
            std__eq => $std_max_num,
            heavy__eq => 8,
        ],
    },
    {
        name => 'small buggy queue',
        stat => {
            std => {
                integral_size => 100_000,
                max_age => 1000,
            },
            heavy => {
                integral_size => 0,
                max_age => 0,
            },
            buggy=> {
                integral_size => 10_000,
                max_age => 1200,
            },
        },
        check => [
            buggy__eq => $buggy_min_num,
        ],
    },
    {
        name => 'large buggy queue',
        stat => {
            std => {
                integral_size => 100_000,
                max_age => 1000,
            },
            heavy => {
                integral_size => 0,
                max_age => 0,
            },
            buggy=> {
                integral_size => 500_000,
                max_age => 1200,
            },
        },
        check => [
            buggy__eq => $buggy_max_num,
        ],
    },
    {
        name => 'old buggy queue',
        stat => {
            std => {
                integral_size => 100_000,
                max_age => 1000,
            },
            heavy => {
                integral_size => 0,
                max_age => 0,
            },
            buggy=> {
                integral_size => 100_000,
                max_age => 3600,
            },
        },
        check => [
            buggy__eq => $buggy_max_num,
        ],
    },

);

# общее количество проверок
Test::More::plan(tests => 8 + (sum map {int((@common_checks + @{$_->{check}})/2)} @test_cases));

# нижняя оценка количества потоков
is($std_max_num, 18, "max num of std workers");
is($std_min_num, 5, "min num of std workers");
ok($std_min_num >= $std_max_num/4, "min num of std workers-2");
is($heavy_max_num, 12, "max num of heavy workers");
is($heavy_min_num, 3, "min num of heavy workers");
is($buggy_max_num, 2, "max num of buggy workers");
is($buggy_min_num, 1, "min num of buggy workers");
is($std_heavy_max_num, 26, "max num of std + heavy workers");

# выполнение тестовых сценариев и проверок
my $i = 0;
for my $t (@test_cases){
    $_->{campaigns_count} = 0 for values(%{ $t->{stat} });
    my $res = BS::ExportMaster::calc_suggested_workers_count($t->{stat});

    my @checks = (@common_checks, @{$t->{check}});
    while (@checks){
        my $key = shift @checks;
        my $value_etalon = shift @checks; # эталонное значение
        die unless defined $value_etalon;

        $key =~ /^(std|heavy|buggy|sum)__(gt|ge|lt|le|eq)$/;
        my ($f, $op) = ($1, $2);
        die "incorrect check $key" unless $f && $op;
        my $cmp_result;
        my $value_act = $f eq 'sum' ? $res->{std} + $res->{heavy} : $res->{$f}; # полученное значение
        if ($op eq 'eq'){
            $cmp_result = $value_act == $value_etalon;
        } elsif ($op eq 'ge') {
            $cmp_result = $value_act >= $value_etalon;
        } elsif ($op eq 'gt') {
            $cmp_result = $value_act >  $value_etalon;
        } elsif ($op eq 'le') {
            $cmp_result = $value_act <= $value_etalon;
        } elsif ($op eq 'lt') {
            $cmp_result = $value_act <  $value_etalon;
        }

        my $name = "test case $i".($t->{name} ? " ($t->{name})" : "").", check '$key => $value_etalon'\n*** Details ***\nstat: ".Dumper($t->{stat})."res: ".Dumper($res);
        ok($cmp_result, $name)
    }

    $i++;
}
