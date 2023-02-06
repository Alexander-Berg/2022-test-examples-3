#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::Deep;
use Test::More;

use Yandex::Test::UTF8Builder;

use Stat::ReportMaster;

use Settings;

my @tests = (
    {
        options => {
            group_by => [ qw/adgroup/ ],
            filters  => { },
            columns  => [ qw// ]
        },
        result  => 'Не заданы колонки',
        message => 'Не показано сообщение о пустых колонках',
    },
    {
        options => {
            group_by => [ qw/campaign adgroup/ ],
            filters  => { },
            columns  => [ qw/clicks/ ]
        },
        result  => 'Слишком много срезов',
        message => 'Слишком много срезов',
    },
    {
        options => {
            group_by => [ qw/campaign/ ],
            filters  => { },
            columns  => [ qw/clicks shows/ ]
        },
        result  => 'Если задан срез, нельзя запросить данные больше чем по одному столбцу',
        message => 'Если задан срез, нельзя запросить данные больше чем по одному столбцу',
    },
    {
        options => {
            group_by => [ qw// ],
            filters  => { },
            columns  => [ qw/clicks shows ctr sum/ ]
        },
        result  => 'Нельзя запрашивать данные о столбцах больше чем двух размерностей',
        message => 'Нельзя запрашивать данные о столбцах больше чем двух размерностей',
    },
    {
        options => {
            group_by => [ qw/campaign/ ],
            filters  => { },
            columns  => [ qw/clicks/ ]
        },
        result  => undef,
        message => 'Не показано сообщение о пустых колонках',
    },
);

for my $t (@tests) {
    my ($report_options, $expected_result) = ($t->{options}, $t->{result});
    my $result = Stat::ReportMaster::validate_report_options_for_plot($report_options);
    if (defined $expected_result) {
        is($result, $expected_result, $t->{message});
    } else {
        ok(!defined $result, $t->{message});
    }
}

done_testing;
