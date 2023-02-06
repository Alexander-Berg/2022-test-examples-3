#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Exception;
use Test::MockTime ();

use Data::Dumper;

use Yandex::Test::UTF8Builder;

use Currency::Format qw/conv_unit_explanation/;

Test::MockTime::set_fixed_time('2015-04-15');

my @tests = (
    # [[$work_currency, $pay_currency, %O], $result, $should_die]

    # курсы для псевдовалют
    [['YND_FIXED', 'rub'], '1 у.е. = 30.00 руб.', 0],
    [['YND_FIXED', 'tenge'], '1 у.е. = 84.00 тенге', 0],

    # фиксированные курсы для валют к у.е. (если не указано, показываем с НДС)
    [['YND_FIXED', 'RUB'], '1 у.е. = 30.00 руб.', 0],
    [['YND_FIXED', 'USD'], '1 у.е. = 0.41 долл.', 0],
    [['YND_FIXED', 'UAH'], '1 у.е. = 12.00 грн', 0],

    # умеем работать только с у.е.
    [['RUB', 'UAH'], undef, 1],

    # курс с учётом НДС и без
    [['YND_FIXED', 'RUB', {nds => 1}], '1 у.е. = 30.00 руб. включая НДС', 0],
    [['YND_FIXED', 'RUB', {nds => 0}], '1 у.е. = 25.42 руб. без учёта НДС', 0],

    # разделитель для настоящих и псевдовалют
    [['YND_FIXED', 'rub', {delim => '___'}], '1___у.е.___=___30.00___руб.', 0],
    [['YND_FIXED', 'RUB', {delim => '___'}], '1___у.е.___=___30.00___руб.', 0],

    # курсы с историей
    [['YND_FIXED', 'UAH', {with_history => 1}], '1 у.е. = 12.00 грн (для оплат до 01.12.2012 г. 1 у.е. = 5.00 грн, до 01.09.2014 г. 1 у.е. = 6.75 грн)', 0],
    [['YND_FIXED', 'USD', {with_history => 1}], '1 у.е. = 0.41 долл. (для оплат до 01.12.2014 г. 1 у.е. = 0.85 долл., до 01.04.2015 г. 1 у.е. = 0.56 долл.)', 0],
    [['YND_FIXED', 'RUB', {with_history => 1}], '1 у.е. = 30.00 руб.', 0],

    # курсы с историей и с/без НДС
    [['YND_FIXED', 'UAH', {with_history => 1, nds => 0}], '1 у.е. = 10.00 грн без учёта НДС (для оплат до 01.12.2012 г. 1 у.е. = 5.00 грн, до 01.09.2014 г. 1 у.е. = 6.75 грн)', 0],
    [['YND_FIXED', 'UAH', {with_history => 1, nds => 1}], '1 у.е. = 12.00 грн включая НДС (для оплат до 01.12.2012 г. 1 у.е. = 6.00 грн, до 01.09.2014 г. 1 у.е. = 8.10 грн)', 0],
    [['YND_FIXED', 'RUB', {with_history => 1, nds => 1}], '1 у.е. = 30.00 руб. включая НДС', 0],

    # курсы с историей и с/без НДС и скидки
    [['YND_FIXED', 'UAH', {with_history => 1, discount => 0}], '1 у.е. = 12.00 грн (для оплат до 01.12.2012 г. 1 у.е. = 5.00 грн, до 01.09.2014 г. 1 у.е. = 6.75 грн)', 0],
    [['YND_FIXED', 'UAH', {with_history => 1, discount => 3}], '1 у.е. = 12.00 грн (для оплат до 01.12.2012 г. 1 у.е. = 5.00 грн, до 01.09.2014 г. 1 у.е. = 6.75 грн)', 0],
    [['YND_FIXED', 'UAH', {with_history => 1, nds => 0, discount => 3}], '1 у.е. = 10.00 грн без учёта НДС и скидки (для оплат до 01.12.2012 г. 1 у.е. = 5.00 грн, до 01.09.2014 г. 1 у.е. = 6.75 грн)', 0],
    [['YND_FIXED', 'UAH', {with_history => 1, nds => 0, discount => 0}], '1 у.е. = 10.00 грн без учёта НДС (для оплат до 01.12.2012 г. 1 у.е. = 5.00 грн, до 01.09.2014 г. 1 у.е. = 6.75 грн)', 0],
    [['YND_FIXED', 'UAH', {with_history => 1, nds => 1, discount => 0}], '1 у.е. = 12.00 грн включая НДС (для оплат до 01.12.2012 г. 1 у.е. = 6.00 грн, до 01.09.2014 г. 1 у.е. = 8.10 грн)', 0],
    [['YND_FIXED', 'UAH', {with_history => 1, nds => 1, discount => 3}], '1 у.е. = 12.00 грн включая НДС и без учёта скидки (для оплат до 01.12.2012 г. 1 у.е. = 6.00 грн, до 01.09.2014 г. 1 у.е. = 8.10 грн)', 0],
);

Test::More::plan(tests => scalar(@tests));

for my $test(@tests) {
    my ($args, $result, $should_die) = @$test;
    my $args_str = join ', ', Data::Dumper->new($args)->Indent(0)->Terse(1)->Quotekeys(0)->Dump();
    my $test_name = "conv_unit_explanation($args_str)";
    if ( !$should_die ) {
        is(conv_unit_explanation(@$args), $result, $test_name);
    } else {
        dies_ok { conv_unit_explanation(@$args) } $test_name;
    }
}
