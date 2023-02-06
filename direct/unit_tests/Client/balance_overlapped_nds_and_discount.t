#!/usr/bin/perl

use Direct::Modern;

# Yandex::Test::UTF8Builder не работает с subtest
use open ':std', ':encoding(utf8)';
use Test::Deep;
use Test::More;
use Test::Exception;
use Yandex::Test::UTF8Builder;

use Settings;

use Client::NDSDiscountSchedule;


# list of [[<args>], <expected errors count>, <test_name>]
my @tests = (
    [
        # args
        [
            # balance data
            [
                {ClientID => 1, date_from => '2016-01-03', date_to => '2016-01-04', nds => 3.14},
                {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-02', nds => 3.14},
            ],
            # db data
            [
                {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-05', nds => 3.14},
            ],
        ],
        # expected data
        [
            {},
            {},
            1,
        ],
        'данные расходятся с БД по датам',
    ],

    [
        # args
        [
            # balance data
            [
                {ClientID => 1, date_from => '2016-01-03', date_to => '2016-01-04', nds => 3.14},
                {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-02', nds => 3.14},
            ],
            # db data
            [
                {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-04', nds => 3.1427},
            ],
        ],
        # expected data
        [
            {},
            {},
            1,
        ],
        'данные расходятся с БД по значению',
    ],

    [
        # args
        [
            # balance data
            [
                {ClientID => 1, date_from => '2016-01-03', date_to => '2016-01-04', nds => 3.14},
                {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-02', nds => 3.14},
            ],
            # db data
            [
                {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-04', nds => 3.14},
            ],
        ],
        # expected data
        [
            {
                1 => [
                    {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-04', nds => 3.14}
                ],
            },
            {
                1 => [
                    {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-04', nds => 3.14}
                ],
            },
            0,
        ],
        'данные совпадают с БД',
    ],

    [
        # args
        [
            # balance data
            [
                {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-04', nds => 3.14},
            ],
            # db data
            [
                {ClientID => 1, date_from => '2016-01-03', date_to => '2016-01-04', nds => 3.14},
                {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-02', nds => 3.14},
            ],
        ],
        # expected data
        [
            {
                1 => [
                    {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-04', nds => 3.14},
                ],
            },
            {
                1 => [
                    {ClientID => 1, date_from => '2016-01-01', date_to => '2016-01-04', nds => 3.14},
                ],
            },
            0,
        ],
        'данные совпадают с БД',
    ],

    [
        # args
        [
            # balance data
            [
                {ClientID => 1, date_from => '2116-01-01', date_to => '2116-01-04', nds => 3.14},
            ],
            # db data
            [
                {ClientID => 1, date_from => '2116-01-01', date_to => '2116-01-24', nds => 3.1427},
            ],
        ],
        # expected data
        [
            {
                1 => [
                    {ClientID => 1, date_from => '2116-01-01', date_to => '2116-01-04', nds => 3.14},
                ],
            },
            {
                1 => [
                    {ClientID => 1, date_from => '2116-01-01', date_to => '2116-01-24', nds => 3.1427},
                ],
            },
            0,
        ],
        'данные расходятся в будущем',
    ],

    [
        # args
        [
            # balance data
            [
                {ClientID => 1, date_from => '2003-01-01', date_to => '2003-12-31', nds => 3.14},
                {ClientID => 1, date_from => '2004-01-01', date_to => '2012-01-29', nds => 3.14},
                {ClientID => 1, date_from => '2012-01-30', date_to => '2012-02-10', nds => 3.14},
                {ClientID => 1, date_from => '2012-02-11', date_to => '2012-02-15', nds => 3.14},
            ],
            # db data
            [
                {ClientID => 1, date_from => '2003-01-01', date_to => '2003-12-31', nds => 3.14},
                {ClientID => 1, date_from => '2004-01-01', date_to => '2012-01-29', nds => 3.14},
                {ClientID => 1, date_from => '2012-01-30', date_to => '2012-02-15', nds => 3.14},
            ],
        ],
        # expected data
        [
            {
                1 => [
                    {ClientID => 1, date_from => '2003-01-01', date_to => '2003-12-31', nds => 3.14},
                    {ClientID => 1, date_from => '2004-01-01', date_to => '2012-01-29', nds => 3.14},
                    {ClientID => 1, date_from => '2012-01-30', date_to => '2012-02-15', nds => 3.14},
                ],
            },
            {
                1 => [
                    {ClientID => 1, date_from => '2003-01-01', date_to => '2003-12-31', nds => 3.14},
                    {ClientID => 1, date_from => '2004-01-01', date_to => '2012-01-29', nds => 3.14},
                    {ClientID => 1, date_from => '2012-01-30', date_to => '2012-02-15', nds => 3.14},
                ],
            },
            0,
        ],
        'данные от баланса идут подряд кусочками, все кроме последнего - захардкоженные несклеиваемы исключения',
    ],
    [
        # args
        [
            # balance data
            [
                {ClientID => 1, date_from => '2003-01-01', date_to => '2003-12-31', nds => 3.14},
                {ClientID => 1, date_from => '2004-01-01', date_to => '2012-01-29', nds => 3.14},
                {ClientID => 1, date_from => '2012-01-30', date_to => '2012-02-15', nds => 3.14},
            ],
            # db data
            [
                {ClientID => 1, date_from => '2003-01-01', date_to => '2003-12-31', nds => 3.14},
                {ClientID => 1, date_from => '2004-01-01', date_to => '2012-01-29', nds => 3.14},
                {ClientID => 1, date_from => '2012-01-30', date_to => '2012-02-10', nds => 3.14},
                {ClientID => 1, date_from => '2012-02-11', date_to => '2012-02-15', nds => 3.14},
            ],
        ],
        # expected data
        [
            {
                1 => [
                    {ClientID => 1, date_from => '2003-01-01', date_to => '2003-12-31', nds => 3.14},
                    {ClientID => 1, date_from => '2004-01-01', date_to => '2012-01-29', nds => 3.14},
                    {ClientID => 1, date_from => '2012-01-30', date_to => '2012-02-15', nds => 3.14},
                ],
            },
            {
                1 => [
                    {ClientID => 1, date_from => '2003-01-01', date_to => '2003-12-31', nds => 3.14},
                    {ClientID => 1, date_from => '2004-01-01', date_to => '2012-01-29', nds => 3.14},
                    {ClientID => 1, date_from => '2012-01-30', date_to => '2012-02-15', nds => 3.14},
                ],
            },
            0,
        ],
        'данные от бд идут подряд кусочками, все кроме последнего - захардкоженные несклеиваемы исключения',
    ],
    [
        # args
        [
            # balance data
            [
                {ClientID => 1, date_from => '2003-01-01', date_to => '2003-12-31', nds => 3.14},
                {ClientID => 1, date_from => '2004-01-01', date_to => '2012-01-29', nds => 3.14},
                {ClientID => 1, date_from => '2012-01-30', date_to => '2012-02-15', nds => 3.14},
            ],
            # db data
            [
            ],
        ],
        # expected data
        [
            {
                1 => [
                    {ClientID => 1, date_from => '2003-01-01', date_to => '2003-12-31', nds => 3.14},
                    {ClientID => 1, date_from => '2004-01-01', date_to => '2012-01-29', nds => 3.14},
                    {ClientID => 1, date_from => '2012-01-30', date_to => '2012-02-15', nds => 3.14},
                ],
            },
            {
            },
            0,
        ],
        'в БД нет данных, должны принять данные от Баланса',
    ],
);

plan tests => 2;

for my $dont_die (undef, 'dont_die') {

    subtest "запуск ".($dont_die ? "с флажком" : "без флажка")." dont_die" => sub {

        plan tests => scalar(@tests);
        
        for my $test (@tests) {
            my ($args, $expected, $test_name) = @$test;
            my (undef, undef, $errors_expected) = @$expected;

            if ($errors_expected && !$dont_die) {
                dies_ok {
                        Client::NDSDiscountSchedule::_check_and_filter_nds_data(@$args)
                    } $test_name;
            } else {
                cmp_deeply(
                    [Client::NDSDiscountSchedule::_check_and_filter_nds_data(@$args, undef, $dont_die)],
                    $expected,
                    $test_name,
                );
            }
        }
    
    }
}
