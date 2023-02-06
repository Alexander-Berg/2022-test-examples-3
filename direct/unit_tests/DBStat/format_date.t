#!/usr/bin/perl

use warnings;
use strict;
use Yandex::Test::UTF8Builder;
use Test::More;

use Tie::IxHash;
use Stat::Tools;
use Yandex::I18n;

use utf8;
use open ':std' => ':utf8';

*f = sub { return Stat::Tools::format_date(@_)};
*f_pdf = sub { return Stat::Tools::format_date_pdf(@_)};

tie my %tests, 'Tie::IxHash', (
    day => [
        ['20140210', '10.02.2014'],
        [20140210, '10.02.2014'],
        ['2014-02-10', '10.02.2014'],
        ['2014-2-4', '04.02.2014'],
    ],
    week => [
        ['2014-09-01', "01.09.14 \x{2013} 07.09.14"],
        ['2014-09-04', "01.09.14 \x{2013} 07.09.14"],
        ['2014-09-30', "29.09.14 \x{2013} 05.10.14"],
        ['2014-10-05', "29.09.14 \x{2013} 05.10.14"],
        ['2014-01-01', "30.12.13 \x{2013} 05.01.14"],
        ['2014-12-31', "29.12.14 \x{2013} 04.01.15"],
    ],
    month => [
        ['2014-01-01', iget('янв').' 2014'],
        ['2014-12-31', iget('дек').' 2014'],
        ['2014-09-10', iget('сен').' 2014'],
    ],
    quarter => [
        ['2014-01-01', '1 '.iget('квартал').' 2014'],
        ['2014-12-31', '4 '.iget('квартал').' 2014'],
        ['2014-09-10', '3 '.iget('квартал').' 2014'],
    ],
    year => [
        ['2014-01-01', '2014 '.iget('г.')],
        ['2014-12-31', '2014 '.iget('г.')],
    ],
    none => [
        ['2014-02-10', '10.02.2014'],
    ]            
);

foreach my $dateagg (keys %tests) {
    foreach my $test ( @{$tests{$dateagg}} ) {
        my ($date, $fmt_date) = @$test;
        is(f($date, $dateagg), $fmt_date, "$date, $dateagg => $fmt_date");
    }    
}

tie my %tests_pdf, 'Tie::IxHash', (day => $tests{day}, 
                                   week => [], 
                                   month => [], 
                                   quarter => $tests{quarter}, 
                                   year => $tests{year}, 
                                   none => $tests{none});
foreach my $t (@{$tests{week}}) {
    my ($date, $fmt_date) = @$t;
    $fmt_date =~ s/\x{2013}/--/;
    push @{$tests_pdf{week}}, [$date, $fmt_date];
}
@{$tests_pdf{month}} = (
    ['2014-01-01', iget('Январь').' 2014'],
    ['2014-12-31', iget('Декабрь').' 2014'],
    ['2014-09-10', iget('Сентябрь').' 2014'],
);
foreach my $dateagg (keys %tests_pdf) {
    foreach my $test ( @{$tests_pdf{$dateagg}} ) {
        my ($date, $fmt_date) = @$test;
        is(f_pdf($date, $dateagg), $fmt_date, "pdf $date, $dateagg => $fmt_date");
    }    
}

done_testing();
