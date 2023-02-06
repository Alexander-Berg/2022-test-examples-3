#!/usr/bin/perl

use Direct::Modern;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Deep;

use utf8;

use BS::Export ();

# [$test_name, $direct_modifier, $expected_result]
my @tests = (
    [
        '1. Конъюнкция  op1 & op2 & op3 & op4',
        {
            condition_json => [
                [ { parameter => "temp", operation => "ge", value => 10 } ],
                [ { parameter => "temp", operation => "le", value => 15 } ],
                [ { parameter => "prec_type", operation => "eq", value => 2 } ],
                [ { parameter => "cloudness", operation => "ge", value => 50 } ]
            ],
            multiplier_pct => 100
        },
        {
            Expression => [
                [ [ "temp", "greater or equal", "10" ] ],
                [ [ "temp", "less or equal", "15" ] ],
                [ ["prec_type", "equal", "2"] ],
                [ ["cloudness", "greater or equal", "50" ] ]
            ],
            Coef => 100
        },

    ],
    [
        '2. Условие со скобками (op1 || op2) & op3 & op4',
        {
            condition_json => [
                [ { parameter => "temp", operation => "le", value => 10 }, { parameter => "temp", operation => "ge", value => 15 } ],
                [ { parameter => "prec_type", operation => "eq", value => 2 } ],
                [ { parameter => "cloudness", operation => "ge", value => 50 } ]
            ],
            multiplier_pct => 100
        },
        {
            Expression => [
                [ [ "temp", "less or equal", "10" ], [ "temp", "greater or equal", "15" ] ],
                [ ["prec_type", "equal", "2"] ],
                [ ["cloudness", "greater or equal", "50" ] ]
            ],
            Coef => 100
        },

    ],
    [
        '3. Условие состоит из одной операции',
        {
            condition_json => [
                [ { parameter => "cloudness", operation => "le", value => 25 }],
            ],
            multiplier_pct => 1000
        },
        {
            Expression => [
                [ ["cloudness", "less or equal", "25" ] ]
            ],
            Coef => 1000
        },

    ],
    [
        '4. Ошибка. В операции отсутствует поле parameter',
        {
            condition_json => [
                [ { param_name => "temp", operation => "le", value => "10" } ],
                [ { parameter => "prec_type", operation => "eq", value => "2" } ],
            ],
            multiplier_pct => 100
        },
        undef,

    ],
    [
        '5. Ошибка. Неизветный тип операции сравнения',
        {
            condition_json => [
                [ { parameter => "temp", operation => "unknown operation", value => "10" } ],
                [ { parameter => "prec_type", operation => "eq", value => "2" } ],
            ],
            multiplier_pct => 100
        },
        undef,
    ],
);

Test::More::plan(tests => scalar(@tests));

foreach my $test (@tests) {
    my ($test_name, $direct_modifier, $expected_result) = @$test;
    my $result = eval {
        BS::Export::get_bs_expression_for_modifier(condition => $direct_modifier->{condition_json}, multiplier_pct => $direct_modifier->{multiplier_pct} );
    };
    cmp_deeply($result, $expected_result, $test_name);
}
