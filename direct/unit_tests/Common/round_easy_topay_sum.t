#!/usr/bin/perl

# $Id$

use strict;
use warnings;

use Test::More;

use List::MoreUtils qw(any);

use Settings;
use Currencies;
use Currency::Pseudo;
use Common;

# очень круглые суммы
my @round_sums = qw/100 150 200/;
# совсем некруглые суммы
my @non_round_sums = qw/10.1 16.67 153 875.13/; 
# просто разные значения
my @arbitrary_sums = qw/12 14 16 245 220 330/;

my $test_count_for_one_currency = 1 + @round_sums + @non_round_sums + @arbitrary_sums; 
my @non_testible_currencies = qw(grivna tenge tr_lira);

Test::More::plan(tests => $test_count_for_one_currency * ((keys %$Currency::Pseudo::PSEUDO_CURRENCIES) - scalar(@non_testible_currencies) )); 

*rts = sub { Common::round_easy_topay_sum(topay => $_[0], conv_unit_rate => $_[1]); };
my $DEFAULT_PAY = get_currency_constant('YND_FIXED', 'DIRECT_DEFAULT_PAY');

# Выполняем тесты по порядку для всех валют из $PSEUDO_CURRENCIES
for my $id (keys %$Currency::Pseudo::PSEUDO_CURRENCIES){ 
    next if any { $id eq $_ } @non_testible_currencies;
    my $pseudo_currency = get_pseudo_currency(id => $id);

    # Для дефолтного платежа должен получиться (примерно) он же, пересчитанный в нужные ден. единицы
    is(rts($DEFAULT_PAY, $pseudo_currency->{rate}), $DEFAULT_PAY*$pseudo_currency->{rate}, "default pay for '$id'");
    
    # Совсем круглые суммы в у.е. должны так и остаться
    for my $sum (@round_sums){
        my $rsum = rts($sum, $pseudo_currency->{rate});
        my $rsum_conv = $rsum / $pseudo_currency->{rate};
        ok( $rsum_conv == $sum, "topay sum should be multiple of 10 in conv. units (for $sum cu got $rsum $id = $rsum_conv cu)" );
    }

    # для любой суммы результат должен быть кратен 10 у.е. 
    for my $sum (@non_round_sums, @arbitrary_sums){
        my $rsum = rts($sum, $pseudo_currency->{rate});
        my $rsum_conv = $rsum / $pseudo_currency->{rate};
        if (int($pseudo_currency->{rate}) == $pseudo_currency->{rate}) {
            ok($rsum_conv == int($rsum_conv) && $rsum_conv % 10 == 0,
                "topay sum should be multiple of 10 in conv. units (for $sum cu got $rsum $id = $rsum_conv cu)");
        } else {
            ok($rsum_conv <= $rsum,
                "round topay sum should be greate or equal than source pay sum")
        }
    }

}
