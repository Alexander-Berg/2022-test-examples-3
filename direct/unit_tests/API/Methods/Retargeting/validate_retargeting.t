#!/usr/bin/perl

use strict;
use warnings;

use utf8;

use my_inc '../../../../';

use Settings;

use Test::More tests => 4;

use Yandex::Test::UTF8Builder;

# за счет DBUnitTest умрем если забудем замокать какие-то обращения к БД.

use_ok('API::Methods::Retargeting');

*validate_retargeting = \&API::Methods::Retargeting::validate_retargeting;

subtest 'API::Methods::Retargeting::validate_retargeting - Add method' => sub {
    plan tests => 7;

    # do not process retargeting with errors
    {
        no strict 'refs';
        no warnings 'redefine';

        *{"API::Methods::Retargeting::are_banners_archived"} = sub { +{ map { $_ => 1 } @{ shift() } } };
        *{"API::Methods::Retargeting::filter_arch_groups"} = sub { [] };

        my $self = {
            ret => [
                { Errors => [ 'fake' ], },
            ],
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, },
                ],
                pid2cid => { 1 => 1, },
                adgroup_types => { 1 => 'base' },
            },
        };
        my $params = {
            Action       => 'Add',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                { Errors => [ 'fake' ], },
            ],
            'do not process retargeting with errors'
        );
    }

    # ArchiveEdit error
    {
        no strict 'refs';
        no warnings 'redefine';

        *{"API::Methods::Retargeting::filter_arch_groups"} = sub { [] };

        my $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, ret_cond_id => 7},
                ],
                pid2cid => { 1 => 1, },
                camp_strategy => {
                    1 => { is_autobudget => 1, }
                },
                adgroup_types => { 1 => 'base' },
            },
        };
        my $params = {
            Action       => 'Add',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 156,
                            FaultString => 'Не позволяется изменение архивных кампаний или объявлений',
                            FaultDetail => 'Все объявления группы 1 перенесены в архив и поэтому группа недоступна для редактирования',
                        },
                    ],
                },
            ],
            'ArchiveEdit error'
        );
    }

    # autobudget camp strategy and price validation
    {
        no strict 'refs';
        no warnings 'redefine';

        *{"API::Methods::Retargeting::filter_arch_groups"} = sub { [ @{ $_[0] } ] };

        my $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, price_context => 'abc', ret_cond_id => 7, currency => 'RUB'},
                ],
                pid2cid => { 1 => 1, },
                adgroup_types => { 1 => 'mobile_content' },
                camps => {
                    1 => { currency => 'RUB', }
                },
            },
        };
        my $params = {
            Action       => 'Add',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 242,
                            FaultString => 'Неверно указана цена',
                            FaultDetail => "Некорректная цена: 'abc'",
                        },
                    ],
                },
            ],
            'invalid price and not autobudget camp strategy - BadPrice error'
        );

        $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, price_context => 'abc', ret_cond_id => 7},
                ],
                pid2cid => { 1 => 1, },
                camp_strategy => { 1 => { is_autobudget => 1, } },
                adgroup_types => { 1 => 'base' },
            },
        };
        $params = {
            Action       => 'Add',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [ {} ], # NB: autovivification?
            'invalid price and autobudget camp strategy - no BadPrice error'
        );
    }

    # multicurrency
    {
        no strict 'refs';
        no warnings 'redefine';

        *{"API::Methods::Retargeting::filter_arch_groups"} = sub { [ @{ $_[0] } ] };

        # multicurrency - bad retargeting currency
        my $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, price_context => 5, currency => 'YND_FIXED', ret_cond_id => 7}, 
                ],
                pid2cid => { 1 => 1, },
                camps => {
                    1 => { currency => 'USD', }
                },
                adgroup_types => { 1 => 'base' },
            },
        };
        my $params = {
            Action       => 'Add',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 245,
                            FaultString => 'Недопустимое значение валюты',
                            FaultDetail => '',
                        },
                    ],
                },
            ],
            'multicurrency - bad retargeting currency'
        );

        # multicurrency - not exists currency
        $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, price_context => 5, currency => 'MINIKI', ret_cond_id => 7}, 
                ],
                pid2cid => { 1 => 1, },
                camps => {
                    1 => { currency => 'MINIKI', }
                },
                adgroup_types => { 1 => 'base' },
            },
        };
        $params = {
            Action       => 'Add',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 245,
                            FaultString => 'Недопустимое значение валюты',
                            FaultDetail => '',
                        },
                    ],
                },
            ],
            'multicurrency - not exists currency'
        );

        # unsupported adgroup type
        $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, price_context => 5, currency => 'RUB', ret_cond_id => 7},
                    { bid => 2, pid => 2, price_context => 5, currency => 'RUB', ret_cond_id => 7},
                    { bid => 3, pid => 3, price_context => 5, currency => 'RUB', ret_cond_id => 7},
                ],
                pid2cid => { 1 => 1, 2 => 2, 3 => 3 },
                camps => {
                    1 => { currency => 'RUB', },
                    2 => { currency => 'RUB', },
                    3 => { currency => 'RUB', },
                },
                adgroup_types => { 1 => 'dynamic', 2 => 'performance', 3 => 'performance' },
            },
        };
        $params = {
            Action       => 'Add',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 3500,
                            FaultString => 'Не поддерживается.',
                            FaultDetail => 'Тип группы объявлений не поддерживается. Объявление: 1',
                        },
                    ],
                },
                {
                    Errors => [
                        {
                            FaultCode   => 3500,
                            FaultString => 'Не поддерживается.',
                            FaultDetail => 'Тип группы объявлений не поддерживается. Объявление: 2',
                        },
                    ],
                },
                {
                    Errors => [
                        {
                            FaultCode   => 3500,
                            FaultString => 'Не поддерживается.',
                            FaultDetail => 'Тип группы объявлений не поддерживается. Объявление: 3',
                        },
                    ],
                },
            ],
            'unsupported adgroup types'
        );

    }

    #done_testing();
};

subtest 'API::Methods::Retargeting::validate_retargeting - Update method' => sub {
    plan tests => 7;

    # do not process retargeting with errors
    {
        no strict 'refs';
        no warnings 'redefine';

        *{"API::Methods::Retargeting::filter_arch_groups"} = sub { [ @{ $_[0] } ] };

        my $self = {
            ret => [
                { Errors => [ 'fake' ], },
            ],
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, ret_cond_id => 7, currency => 'RUB' },
                ],
                pid2cid => { 1 => 1, },
                camps => {
                    1 => { currency => 'RUB', }
                },
            },
        };
        my $params = {
            Action       => 'Update',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                { Errors => [ 'fake' ], },
            ],
            'do not process retargeting with errors'
        );
    }

    # ArchiveEdit error
    {
        no strict 'refs';
        no warnings 'redefine';

        *{"API::Methods::Retargeting::filter_arch_groups"} = sub { [] };

        my $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, ret_cond_id => 7},
                ],
                pid2cid => { 1 => 1, },
                camp_strategy => {
                    1 => { is_autobudget => 1, }
                },
            },
        };
        my $params = {
            Action       => 'Update',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 156,
                            FaultString => 'Не позволяется изменение архивных кампаний или объявлений',
                            FaultDetail => 'Все объявления группы 1 перенесены в архив и поэтому группа недоступна для редактирования',
                        },
                    ],
                },
            ],
            'ArchiveEdit error'
        );
    }

    # autobudget camp strategy and price validation
    {
        no strict 'refs';
        no warnings 'redefine';

        *{"API::Methods::Retargeting::filter_arch_groups"} = sub { [ @{ $_[0] } ] };

        my $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, price_context => 'abc', ret_cond_id => 7, currency => 'RUB' },
                ],
                pid2cid => { 1 => 1, },
                camps => {
                    1 => { currency => 'RUB', }
                },
            },
        };
        my $params = {
            Action       => 'Update',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 242,
                            FaultString => 'Неверно указана цена',
                            FaultDetail => "Некорректная цена: 'abc'",
                        },
                    ],
                },
            ],
            'invalid price and not autobudget camp strategy - BadPrice error'
        );

        $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, price_context => 'abc', },
                ],
                pid2cid => { 1 => 1, },
                camp_strategy => { 1 => { is_autobudget => 1, } },
            },
        };
        $params = {
            Action       => 'Update',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [ {} ], # NB: autovivification?
            'invalid price and autobudget camp strategy - no BadPrice error'
        );
    }

    # multicurrency
    {
        no strict 'refs';
        no warnings 'redefine';

        *{"API::Methods::Retargeting::filter_arch_groups"} = sub { [ @{ $_[0] } ] };

        # multicurrency - bad retargeting currency
        my $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, price_context => 5, currency => 'rub', ret_cond_id => 7}, 
                ],
                pid2cid => { 1 => 1, },
                camps => {
                    1 => { currency => 'YND_FIXED', }
                },
            },
        };
        my $params = {
            Action       => 'Update',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 245,
                            FaultString => 'Недопустимое значение валюты',
                            FaultDetail => '',
                        },
                    ],
                },
            ],
            'multicurrency - bad retargeting currency'
        );

        # multicurrency - not allowed currency
        $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, price_context => 5, currency => 'USD', ret_cond_id => 7}, 
                ],
                pid2cid => { 1 => 1, },
                camps => {
                    1 => { currency => 'RUB', }
                },
            },
        };
        $params = {
            Action       => 'Update',
            Retargetings => [ { Currency => 'USD' }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 245,
                            FaultString => 'Недопустимое значение валюты',
                            FaultDetail => 'Допустимые значения Currency: RUB',
                        },
                    ],
                },
            ],
            'multicurrency - not allowed currency'
        );


        # multicurrency - not exists currency
        $self = {
            preprocess => {
                converted => [
                    { bid => 1, pid => 1, price_context => 5, currency => 'MINIKI', ret_cond_id => 7}, 
                ],
                pid2cid => { 1 => 1, },
                camps => {
                    1 => { currency => 'MINIKI', }
                },
            },
        };
        $params = {
            Action       => 'Update',
            Retargetings => [ { }, ],
        };

        validate_retargeting( $self, $params );

        is_deeply(
            $self->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 245,
                            FaultString => 'Недопустимое значение валюты',
                            FaultDetail => '',
                        },
                    ],
                },
            ],
            'multicurrency - not exists currency'
        );
    }

    #done_testing();
};

subtest 'API::Methods::Retargeting::validate_retargeting - Get & Delete methods' => sub {
    plan tests => 3;

    # BadCurrency - Get and retargetings with different currencies
    my $self = {
        preprocess => {
            retargetings => {
                1 => { currency => 'RUB', },
                2 => { currency => 'KZT', },
            },
        },
    };
    my $params = {
        Action  => 'Get',
        Options => { Currency => 1, },
    };

    is( validate_retargeting( $self, $params ), 'BadCurrency', 'BadCurrency - retargetings with different currencies');

    # multicurrency - bad request currency
    $self = {
        preprocess => {
            retargetings => {
                1 => { currency => 'RUB', }, 
            },
        },
    };
    $params = {
        Action  => 'Get',
        Options => { Currency => 'abc', },
    };

    is_deeply(
        [ validate_retargeting( $self, $params ) ],
        [ 'BadCurrency', ],
        'mmulticurrency - bad request currency'
    );

    # multicurrency - specified request currency is not allowed
    $self = {
        preprocess => {
            retargetings => {
                1 => { currency => 'RUB', }, 
            },
        },
    };
    $params = {
        Action  => 'Get',
        Options => { Currency => 'USD', },
    };

    validate_retargeting( $self, $params );

    is_deeply(
        [ validate_retargeting( $self, $params ) ],
        [ 'BadCurrency', 'Допустимые значения Currency: RUB', ],
        'multicurrency - specified request currency is not allowed'
    );

    #done_testing();
};

#done_testing();
