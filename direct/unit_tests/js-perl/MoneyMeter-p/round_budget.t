#!/usr/bin/perl

# $Id$

use strict;
use warnings;

use Test::More;

use Settings;
use Yandex::ExecuteJS;



# человечески-круглая сумма -- круглая на взгляд человека
# алгоритмически-круглая -- та, которая не меняется после округления

# TODO окрасивить округление, чтобы 
# 110, 31000, 72000 стали алгоритмически-некруглыми

# человечески-круглые суммы
my @round_sums = qw/100 1000 1500 15000 20000 30000 3500 4500 70000 75000/;
# человечески-некруглые суммы
my @non_round_sums = qw/1010 1570 15300 20800/; 
# просто разные значения
my @arbitrary_sums = qw/120 140 1600 24550 22000 33000/;

Test::More::plan(tests => @round_sums + 2 * (@non_round_sums + @arbitrary_sums));

*rb = sub { call_js($Settings::JS_PERL_DIR.'/MoneyMeter-p.js', "round_budget", [@_]); };

# человечески-круглая сумма должна быть алгоритмически-круглой
for my $sum (@round_sums){
    is( rb($sum), $sum, "result for round sum ($sum) should be sum itselt");
}

# из любой суммы должна получиться алгоритмически-круглая
for my $sum (@non_round_sums, @arbitrary_sums){
    my $r_sum = rb($sum);
    my $r2_sum = rb($r_sum);
    ok( $r2_sum == $r_sum, "result for any sum should be round sum ( $sum --> $r_sum --> $r2_sum)");
}

# человечески-некруглая сумма не должна оказаться алгоритмически-круглой
for my $sum (@non_round_sums){
    isnt( rb($sum), $sum, "result for non-round sum ($sum) may not be sum itselt");
}

# округление не должно быть слишком грубым
for my $sum (@arbitrary_sums){ 
    my $r_sum = rb($sum);
    ok( abs($sum - $r_sum)/$sum < 0.1 , "difference between sum and sum rounded may not be too big (for $sum got $r_sum)" );
}

