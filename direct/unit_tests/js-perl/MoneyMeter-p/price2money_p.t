#!/usr/bin/perl

# $Id$

=pod

    Тест для Баблометра. Проверяет функцию "из ограничения на среднюю цену клика рассчитать бюджет". 

=cut

use strict;
use warnings;

use Test::More;

use Settings;
use Yandex::ExecuteJS;


# набор цен, для которых прогоняем тест
my @prices = (0.1, 0.2, 3, 0.5, 0.8, 0.74, 1, 2, 2.1, 5, 8, 11, 115, 10000);

Test::More::plan(tests => 2 * (scalar @prices - 1) );

# TODO 
# Как бы отделить такие фейковые данные от тестов? 
# Данных может быть много, они могут меняться, и одни и те же данные нужны для нескольких тестов...
my $fake_forecast_data = [
    {
      exps_high => 2995.88389864593,
      exps_low => 54.1665102560665,
      exps_middle => 36.1110068373777,
      left_lim_exps => 0,
      max_exps => 2995.88389864593,
      md5 => 'dbb8367dd5dbc2ca690cc8b2a66f193c',
      phrase => '',
      rec_budget => 39.7221075211154,
      right_lim_exps => 535.646601421102,
      shows => 1740,
      transitions => [
                         {
                           add_clicks => 11.0570137509049,
                           cost => 0.9,
                           md5 => 'dbb8367dd5dbc2ca690cc8b2a66f193c'
                         },
                         {
                           add_clicks => 9.00465671430487,
                           cost => 2.90512956701678,
                           md5 => 'dbb8367dd5dbc2ca690cc8b2a66f193c'
                         },
                         {
                           add_clicks => 92.1437189971846,
                           cost => 32.1212658227848,
                           md5 => 'dbb8367dd5dbc2ca690cc8b2a66f193c'
                         }
                       ]
    },
    {
      exps_high => 32282.1277976723,
      exps_low => 0,
      exps_middle => 1224.68084153984,
      left_lim_exps => 0,
      max_exps => 32282.1277976723,
      md5 => 'ab2080967543083949ec0653b304fc54',
      phrase => '',
      rec_budget => 1347.14892569382,
      right_lim_exps => 2222.56893464637,
      shows => 20850,
      transitions => [
                         {
                           add_clicks => 69.473694778872,
                           cost => 3.6,
                           md5 => 'ab2080967543083949ec0653b304fc54'
                         },
                         {
                           add_clicks => 81.7214708433303,
                           cost => 11.92557513073,
                           md5 => 'ab2080967543083949ec0653b304fc54'
                         },
                         {
                           add_clicks => 2044.86794986571,
                           cost => 15.1879963486992,
                           md5 => 'ab2080967543083949ec0653b304fc54'
                         }
                       ]
    },

];


# готовим массив переходов. Хорошо бы для этого тоже вызывать js-ный код...
my @transitions = sort { $a->{cost} <=> $b->{cost} } map { @{$_->{transitions}} } @$fake_forecast_data;

@prices = sort {$a <=> $b} @prices;
# вычисляем соответствие: ограничение на цену --> бюджет
my %budget = map { $_ => call_js($Settings::JS_PERL_DIR.'/MoneyMeter-p.js', "price2money_p", [$_, \@transitions]) } @prices;

# проверяем, что получившиеся числа разумны
for my $i (0 .. scalar @prices - 2){
    my ($p1, $p2) = @prices[$i, $i+1];

    # проверяем, что цены в массиве действительно упорядочены (на всякий случай, чтобы при следующих модификациях теста не поломать)
    cmp_ok($p1, '<=', $p2, "\@prices should be sorted (\@prices[$i, $i+1] == ($p1, $p2))");

    my ($b1, $b2) = @budget{$p1, $p2};
    my $test_name = "budget for price $p1 should be less then budget for price $p2";
    # и проверяем, что большая допустимая цена дает больший бюджет
    cmp_ok($b1, '<=', $b2, $test_name);
}

