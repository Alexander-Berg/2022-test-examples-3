#!/usr/bin/perl

use Direct::Modern;

use my_inc '../../../../';

use Settings;

use Test::Exception;
use Test::More tests => 4;

use Yandex::DBUnitTest qw/:all/;

use Yandex::Test::UTF8Builder;

use_ok('API::Methods::Retargeting');

my $dataset = {

    shard_client_id => {
        original_db => PPCDICT(),
        rows => [ 
            { ClientID => 2, shard => 1, },
        ],
    },

    shard_inc_bid => {
        original_db => PPCDICT(),
        rows => [ 
            { bid => 1, ClientID => 2, },
            { bid => 2, ClientID => 2, },
            { bid => 3, ClientID => 2, },
        ],
    },

    shard_inc_pid => {
        original_db => PPCDICT(),
        rows => [ 
            { pid => 1, ClientID => 2, },
            { pid => 2, ClientID => 2, },
            { pid => 3, ClientID => 2, },
        ],
    },

    shard_inc_ret_cond_id => {
        original_db => PPCDICT(),
        rows => [ 
            { ret_cond_id => 1, ClientID => 2, },
            { ret_cond_id => 2, ClientID => 2, },
            { ret_cond_id => 3, ClientID => 2, },
        ],
    },

    shard_login => {
        original_db => PPCDICT(),
        rows => [ 
            { login => 'holodilnikru', uid => 2, },
        ],
    },

    shard_uid => {
        original_db => PPCDICT(),
        rows => [ 
            { uid => 2, ClientID => 2, },
        ],
    },

    banners => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { bid => 1, pid => 1, cid => 1, },
                { bid => 2, pid => 2, cid => 2, },
                { bid => 3, pid => 3, cid => 3, },
            ],
        },
    },

    banner_turbolandings => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [],
        },
    },

    bids_retargeting => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { ret_id => 1, ret_cond_id => 1, bid => 1, pid => 1, cid => 1, price_context => 5, },
                { ret_id => 2, ret_cond_id => 2, bid => 2, pid => 2, cid => 2, price_context => 7, },
                { ret_id => 3, ret_cond_id => 3, bid => 3, pid => 3, cid => 3, price_context => 9, is_suspended => 1, autobudgetPriority => 1, },
            ],
        },
    },
    
    retargeting_conditions => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { ret_cond_id => 1, condition_json => '[]' },
                { ret_cond_id => 2, condition_json => '[]' },
                { ret_cond_id => 3, condition_json => '[]' },
            ],
        },
    },
    
    retargeting_goals => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { ret_cond_id => 1, goal_id => 1 },
                { ret_cond_id => 2, goal_id => 2 },
                { ret_cond_id => 3, goal_id => 3 },
            ],
        },
    },

    campaigns => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { cid => 1, uid => 2, statusEmpty => 'no', currency => 'YND_FIXED' },
                { cid => 2, uid => 2, statusEmpty => 'no', currency => 'YND_FIXED' },
                { cid => 3, uid => 2, statusEmpty => 'no', currency => 'RUB', },
            ],
        },
    },

    phrases => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { pid => 1, cid => 1, adgroup_type => 'base', },
                { pid => 2, cid => 2, adgroup_type => 'base', },
                { pid => 3, cid => 3, adgroup_type => 'base', },
            ],
        },
    },

    users => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { uid => 2, ClientID => 2, login => 'holodilnikru' },
            ],
        },
    },
};

init_test_dataset( $dataset );

sub get_self {
    my %self = (
        manager => {
            rbac_login_rights => {
                role => 'manager'
            },
            user_info => {
                login => 'holodilnikru'
            },
            uid => 1
        },
        client => {
            rbac_login_rights => {
                role => 'client'
            },
            user_info => {
                login => 'holodilnikru'
            },
            uid => 2
        },
    );
    return $self{ $_[0] };
}

{
    no strict 'refs';
    no warnings 'redefine';

    *{"API::Methods::Retargeting::get_camp_info"} = sub {
        my $cids = shift;

        my %cids = map { $_ => undef } @$cids;

        my $shard = 1;
        return [
            map {
                +{
                    cid      => $_->{cid},
                    currency => $_->{currency} // 'YND_FIXED',
                    ClientID => $dataset->{users}{rows}{$shard}->[0]->{ClientID},
                }
            }
            grep {
                exists $cids{ $_->{cid} }
            } @{ $dataset->{campaigns}{rows}{$shard} }
        ];
    };
    *{"API::Methods::Retargeting::currency_price_rounding"} = sub { $_[0] };
    *{"API::Methods::Retargeting::convert_currency"} = sub { $_[0] };

    *{"Campaign::mass_campaign_strategy"} = sub { { } };
}

*preprocess_retargeting = \&API::Methods::Retargeting::preprocess_retargeting;



#######
# TODO
#   - how to change pid for bids_retargeting?
#   - is it better to check only needed facts, not the whole response?
#   - autovivification of $self->{converted}[$i]{Fields} in API::Methods::Retargeting, l.501
#   - autovivification of $self->{ret}[$i] in API::Methods::Retargeting, l.484
#   - update of not exists row in bids_retargeting (row inserted?)
#

subtest 'API::Methods::Retargeting::preprocess_retargeting - Add method' => sub {
    plan tests => 50;

    # client call - login added to params
    my $client = get_self('client');
    my $params = {
        Action       => 'Add',
        Retargetings => [
            { AdID => 1, RetargetingConditionID => 1, },
            { AdID => 2, RetargetingConditionID => 2, },
        ],
    };
    preprocess_retargeting( $client, $params );

    is( $params->{Login}, $client->{user_info}{login}, 'login added to params when client call method' );

    is_deeply( [sort keys %{$client->{preprocess}}],
        ['adgroup_types', 'camp_strategy', 'camps', 'cid2clientid', 'converted', 'old_retargetings_by_pid_hash', 'pid2cid', 
          'retargeting_conditions_by_id'] );
    
    is_deeply( $client->{preprocess}{old_retargetings_by_pid_hash},
        {
            1 => {
                1 => {
                    ret_id             => 1, 
                    ret_cond_id        => 1,
                    pid                => 1,
                    price_context      => '5.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 1,
                    cid                => 1,
                    currency           => 'YND_FIXED',
                },
            },
            2 => {
                2 => {
                    ret_id             => 2, 
                    ret_cond_id        => 2,
                    pid                => 2,
                    price_context      => '7.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 2,
                    cid                => 2,
                    currency           => 'YND_FIXED',
                },
            },
        }
    );
    is_deeply( $client->{preprocess}{pid2cid}, { 1 => 1, 2 => 2, } );
    is_deeply( $client->{preprocess}{camps}, {
                1 => { cid => 1, currency => 'YND_FIXED', ClientID => 2 },
                2 => { cid => 2, currency => 'YND_FIXED', ClientID => 2 },
            } );
    is_deeply( $client->{preprocess}{camp_strategy}, {} );
    is_deeply( $client->{preprocess}{converted}, [
                {
                    bid            => 1,
                    pid            => 1,
                    ret_cond_id    => 1,
                    price_currency => 'YND_FIXED',
                },
                {
                    bid            => 2,
                    pid            => 2,
                    ret_cond_id    => 2,
                    price_currency => 'YND_FIXED',
                },
            ] );
    is_deeply( $client->{preprocess}{adgroup_types}, { 1 => 'base', 2 => 'base' } );

    # not client call - login not added to params
    my $manager = get_self('manager');
    $params = {
        Action       => 'Add',
        Retargetings => [
            { AdID => 1, RetargetingConditionID => 1, },
            { AdID => 2, RetargetingConditionID => 2, },
        ],
    };
    preprocess_retargeting( $manager, $params );

    ok( ! exists( $params->{Login} ), 'login not added to params when not client call method' );
    
    is_deeply( [sort keys %{$client->{preprocess}}],
        ['adgroup_types', 'camp_strategy', 'camps', 'cid2clientid', 'converted', 'old_retargetings_by_pid_hash', 'pid2cid', 
          'retargeting_conditions_by_id'] );
    
    is_deeply( $client->{preprocess}{old_retargetings_by_pid_hash},
        {
            1 => {
                1 => {
                    ret_id             => 1, 
                    ret_cond_id        => 1,
                    pid                => 1,
                    price_context      => '5.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 1,
                    cid                => 1,
                    currency           => 'YND_FIXED',
                },
            },
            2 => {
                2 => {
                    ret_id             => 2, 
                    ret_cond_id        => 2,
                    pid                => 2,
                    price_context      => '7.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 2,
                    cid                => 2,
                    currency           => 'YND_FIXED',
                },
            },
        }
    );
    is_deeply( $client->{preprocess}{pid2cid}, { 1 => 1, 2 => 2, } );
    is_deeply( $client->{preprocess}{camps}, {
                1 => { cid => 1, currency => 'YND_FIXED', ClientID => 2 },
                2 => { cid => 2, currency => 'YND_FIXED', ClientID => 2 },
            } );
    is_deeply( $client->{preprocess}{camp_strategy}, {} );
    is_deeply( $client->{preprocess}{converted}, [
                {
                    bid            => 1,
                    pid            => 1,
                    ret_cond_id    => 1,
                    price_currency => 'YND_FIXED',
                },
                {
                    bid            => 2,
                    pid            => 2,
                    ret_cond_id    => 2,
                    price_currency => 'YND_FIXED',
                },
            ] );
    is_deeply( $client->{preprocess}{adgroup_types}, { 1 => 'base', 2 => 'base' } );

    # do not process retargeting with errors
    $manager = get_self('manager');
    $manager->{ret} = [ { Errors => [ 'fake' ] } ];
    $params = {
        Action       => 'Add',
        Retargetings => [
            { AdID => 1, RetargetingConditionID => 1, RetargetingID => 1000, },
            { AdID => 2, RetargetingConditionID => 2, RetargetingID => 1001, },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply( [sort keys %{$client->{preprocess}}],
        ['adgroup_types', 'camp_strategy', 'camps', 'cid2clientid', 'converted', 'old_retargetings_by_pid_hash', 'pid2cid', 
          'retargeting_conditions_by_id'] );

    is_deeply( $client->{preprocess}{old_retargetings_by_pid_hash},
        {
            1 => {
                1 => {
                    ret_id             => 1, 
                    ret_cond_id        => 1,
                    pid                => 1,
                    price_context      => '5.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 1,
                    cid                => 1,
                    currency           => 'YND_FIXED',
                },
            },
            2 => {
                2 => {
                    ret_id             => 2, 
                    ret_cond_id        => 2,
                    pid                => 2,
                    price_context      => '7.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 2,
                    cid                => 2,
                    currency           => 'YND_FIXED',
                },
            },
        }
    );
    is_deeply( $client->{preprocess}{pid2cid}, { 1 => 1, 2 => 2, } );
    is_deeply( $client->{preprocess}{camps}, {
                1 => { cid => 1, currency => 'YND_FIXED', ClientID => 2 },
                2 => { cid => 2, currency => 'YND_FIXED', ClientID => 2 },
            } );
    is_deeply( $client->{preprocess}{camp_strategy}, {} );
    is_deeply( $client->{preprocess}{converted}, [
                {
                    bid            => 1,
                    pid            => 1,
                    ret_cond_id    => 1,
                    price_currency => 'YND_FIXED',
                },
                {
                    bid            => 2,
                    pid            => 2,
                    ret_cond_id    => 2,
                    price_currency => 'YND_FIXED',
                },
            ] );
    is_deeply( $client->{preprocess}{adgroup_types}, { 1 => 'base', 2 => 'base' } );

    # NoRights error
    $client = get_self('client');
    $params = {
        Action       => 'Add',
        Retargetings => [
            { AdID => 100, RetargetingConditionID => 1, },
        ],
    };
    preprocess_retargeting( $client, $params );

    is_deeply(
        $client->{ret},
        [
            {
                Errors => [
                    {
                        FaultCode   => 54,
                        FaultString => 'Недостаточно прав',
                        FaultDetail => '',
                    },
                ],
            },
        ],
        'NoRights error'
    );

    is_deeply( [sort keys %{$client->{preprocess}}],
        ['camp_strategy', 'camps', 'cid2clientid', 'converted', 'old_retargetings_by_pid_hash', 'pid2cid', 
          'retargeting_conditions_by_id'] );

    is_deeply( $client->{preprocess}{old_retargetings_by_pid_hash}, undef);
    is_deeply( $client->{preprocess}{pid2cid}, {} );
    is_deeply( $client->{preprocess}{camps}, {} );
    is_deeply( $client->{preprocess}{camp_strategy}, {} );
    is_deeply( $client->{preprocess}{converted}, [
                {
                    bid            => 100,
                    ret_cond_id    => 1,
                }
            ] );

    # convertion of StatusPaused & AutoBudgetPriority
    $manager = get_self('manager');
    $params = {
        Action       => 'Add',
        Retargetings => [
            { AdID => 2, RetargetingConditionID => 2, StatusPaused => 'Yes', AutoBudgetPriority => 'High', },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply( [sort keys %{$manager->{preprocess}}],
        ['adgroup_types', 'camp_strategy', 'camps', 'cid2clientid', 'converted', 'old_retargetings_by_pid_hash', 'pid2cid', 
          'retargeting_conditions_by_id'] );

    is_deeply( $manager->{preprocess}{old_retargetings_by_pid_hash}, {
            2 => {
                2 => {
                    ret_id             => 2, 
                    ret_cond_id        => 2,
                    pid                => 2,
                    price_context      => '7.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 2,
                    cid                => 2,
                    currency           => 'YND_FIXED',
                },
            },
        });
    is_deeply( $manager->{preprocess}{pid2cid}, { 2 => 2, } );
    is_deeply( $manager->{preprocess}{camps}, { 2 => { cid => 2, currency => 'YND_FIXED', ClientID => 2 },} );
    is_deeply( $manager->{preprocess}{camp_strategy}, {} );
    is_deeply( $manager->{preprocess}{converted}, [
                {
                    bid                => 2,
                    pid                => 2,
                    ret_cond_id        => 2,
                    is_suspended       => 1,
                    autobudgetPriority => 5,
                    price_currency     => 'YND_FIXED',
                },
            ] );

    # deletion of ret_id
    $manager = get_self('manager');
    $params = {
        Action       => 'Add',
        Retargetings => [
            { AdID => 2, RetargetingConditionID => 2, RetargetingID => 1000, },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply( [sort keys %{$manager->{preprocess}}],
        ['adgroup_types', 'camp_strategy', 'camps', 'cid2clientid', 'converted', 'old_retargetings_by_pid_hash', 'pid2cid', 
          'retargeting_conditions_by_id'] );

    is_deeply( $manager->{preprocess}{old_retargetings_by_pid_hash}, {
            2 => {
                2 => {
                    ret_id             => 2, 
                    ret_cond_id        => 2,
                    pid                => 2,
                    price_context      => '7.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 2,
                    cid                => 2,
                    currency           => 'YND_FIXED',
                },
            },
        });
    is_deeply( $manager->{preprocess}{pid2cid}, { 2 => 2, } );
    is_deeply( $manager->{preprocess}{camps}, { 2 => { cid => 2, currency => 'YND_FIXED', ClientID => 2 },} );
    is_deeply( $manager->{preprocess}{camp_strategy}, {} );
    is_deeply( $manager->{preprocess}{converted}, [
                {
                    bid            => 2,
                    pid            => 2,
                    ret_cond_id    => 2,
                    price_currency => 'YND_FIXED',
                },
            ] );
    is_deeply( $manager->{preprocess}{adgroup_types}, { 2 => 'base' } );

    # currency convertion
    $manager = get_self('manager');
    $params = {
        Action       => 'Add',
        Retargetings => [
            { AdID => 3, RetargetingConditionID => 3, ContextPrice => 15, },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply( [sort keys %{$manager->{preprocess}}],
        ['adgroup_types', 'camp_strategy', 'camps', 'cid2clientid', 'converted', 'old_retargetings_by_pid_hash', 'pid2cid', 
          'retargeting_conditions_by_id'] );

    is_deeply( $manager->{preprocess}{old_retargetings_by_pid_hash}, {
                3 => {
                    3 => {
                        ret_id             => 3, 
                        ret_cond_id        => 3,
                        pid                => 3,
                        price_context      => '9.00', 
                        autobudgetPriority => 1,
                        is_suspended       => 1,
                        bid                => 3,
                        cid                => 3,
                        currency           => 'RUB',
                    },
                },
            });
    is_deeply( $manager->{preprocess}{pid2cid}, { 3 => 3, } );
    is_deeply( $manager->{preprocess}{camps}, {
                3 => { cid => 3, currency => 'RUB', ClientID => 2 },
            } );
    is_deeply( $manager->{preprocess}{camp_strategy}, {} );
    is_deeply( $manager->{preprocess}{converted}, [
                {
                    bid            => 3,
                    pid            => 3,
                    ret_cond_id    => 3,
                    price_context  => 15, 
                    price_currency => 'RUB',
                },
            ], );
    is_deeply( $manager->{preprocess}{adgroup_types}, { 3 => 'base' } );

    #done_testing();
};

subtest 'API::Methods::Retargeting::preprocess_retargeting - Update method' => sub {
    plan tests => 18;

    # client call - login added to params
    my $client = get_self('client');
    my $params = {
        Action       => 'Update',
        Retargetings => [
            { RetargetingID => 1, },
            { RetargetingID => 2, },
        ],
    };
    preprocess_retargeting( $client, $params );

    is( $params->{Login}, $client->{user_info}{login}, 'login added to params when client call method' );

    is_deeply( [sort keys %{$client->{preprocess}}],
        ['camp_strategy', 'camps', 'cid2clientid', 'converted', 'old_retargetings', 'pid2cid'] );

    is_deeply( $client->{preprocess}{old_retargetings}, {
                1 => {
                    ret_id             => 1, 
                    ret_cond_id        => 1,
                    pid                => 1,
                    price_context      => '5.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 1,
                    cid                => 1,
                    currency           => 'YND_FIXED',
                },
                2 => {
                    ret_id             => 2, 
                    ret_cond_id        => 2,
                    pid                => 2,
                    price_context      => '7.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 2,
                    cid                => 2,
                    currency           => 'YND_FIXED',
                },
            });
    is_deeply( $client->{preprocess}{pid2cid}, { 1 => 1, 2 => 2, } );
    is_deeply( $client->{preprocess}{camps}, {
                1 => { cid => 1, currency => 'YND_FIXED', ClientID => 2 },
                2 => { cid => 2, currency => 'YND_FIXED', ClientID => 2 },
            } );
    is_deeply( $client->{preprocess}{camp_strategy}, {} );
    is_deeply( $client->{preprocess}{converted}, [
                {
                    bid            => 1,
                    pid            => 1,
                    ret_id         => 1,
                    ret_cond_id    => 1,
                    price_currency => 'YND_FIXED',
                },
                {
                    bid            => 2,
                    pid            => 2,
                    ret_id         => 2,
                    ret_cond_id    => 2,
                    price_currency => 'YND_FIXED',
                },
            ], );

    # not client call - login must be given in params
    my $manager = get_self('manager');
    $params = {
        Action       => 'Update',
        Retargetings => [
            { RetargetingID => 1, },
            { RetargetingID => 2, },
        ],
    };
    throws_ok { preprocess_retargeting( $manager, $params ); } qr/Can not choose shard param/, 'error - login must be given in params for not client';

    $params = {
        Action       => 'Update',
        Login        => $manager->{user_info}{login},
        Retargetings => [
            { RetargetingID => 1, },
            { RetargetingID => 2, },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply(
        $manager->{preprocess},
        {
            old_retargetings => {
                1 => {
                    ret_id             => 1, 
                    ret_cond_id        => 1,
                    pid                => 1,
                    price_context      => '5.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 1,
                    cid                => 1,
                    currency           => 'YND_FIXED',
                },
                2 => {
                    ret_id             => 2, 
                    ret_cond_id        => 2,
                    pid                => 2,
                    price_context      => '7.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 2,
                    cid                => 2,
                    currency           => 'YND_FIXED',
                },
            },
            pid2cid => { 1 => 1, 2 => 2, },
            camps   => {
                1 => { cid => 1, currency => 'YND_FIXED', ClientID => 2 },
                2 => { cid => 2, currency => 'YND_FIXED', ClientID => 2 },
            },
            camp_strategy => { },
            cid2clientid => {
                1 => 2,
                2 => 2,
            },
            converted => [
                {
                    bid            => 1,
                    pid            => 1,
                    ret_id         => 1,
                    ret_cond_id    => 1,
                    price_currency => 'YND_FIXED',
                },
                {
                    bid            => 2,
                    pid            => 2,
                    ret_id         => 2,
                    ret_cond_id    => 2,
                    price_currency => 'YND_FIXED',
                },
            ],
        },
        'ok - login must be given in params for not client'
    );

    # do not process retargeting with errors
    $manager = get_self('manager');
    $manager->{ret} = [ { Errors => [ 'fake' ] } ];
    $params = {
        Action       => 'Update',
        Login        => $manager->{user_info}{login},
        Retargetings => [
            { RetargetingID => 1, },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply(
        $manager->{preprocess},
        {
            old_retargetings => {
                1 => {
                    ret_id             => 1, 
                    ret_cond_id        => 1,
                    pid                => 1,
                    price_context      => '5.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 1,
                    cid                => 1,
                    currency           => 'YND_FIXED',
                },
            },
            pid2cid => { },
            camps   => { },
            camp_strategy => { },
            cid2clientid => {
            },
            converted => [
                { ret_id => 1, },
            ],
        },
        'do not process retargeting with errors'
    );

    # update for not exists ret_id
    $manager = get_self('manager');
    $params = {
        Action       => 'Update',
        Login        => $manager->{user_info}{login},
        Retargetings => [
            { RetargetingID => 5, },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply(
        $manager->{preprocess},
        {
            old_retargetings => { },
            pid2cid => { },
            camps   => { },
            camp_strategy => { },
            cid2clientid => {
            },
            converted => [
                {
                    ret_id         => 5,
                    price_currency => 'YND_FIXED',
                },
            ],
        },
        'update for not exists ret_id'
    );

    # convertion of StatusPaused & AutoBudgetPriority
    $manager = get_self('manager');
    $params = {
        Action       => 'Update',
        Login        => $manager->{user_info}{login},
        Retargetings => [
            { RetargetingID => 1, StatusPaused => 'Yes', AutoBudgetPriority => 'High', },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply(
        $manager->{preprocess},
        {
            old_retargetings => {
                1 => {
                    ret_id             => 1, 
                    ret_cond_id        => 1,
                    pid                => 1,
                    price_context      => '5.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 1,
                    cid                => 1,
                    currency           => 'YND_FIXED',
                },
            },
            pid2cid => { 1 => 1, },
            camps   => {
                1 => { cid => 1, currency => 'YND_FIXED', ClientID => 2 },
            },
            camp_strategy => { },
            cid2clientid => {
                1 => 2,
            },
            converted => [
                {
                    bid                => 1,
                    pid                => 1,
                    ret_id             => 1,
                    ret_cond_id        => 1,
                    is_suspended       => 1,
                    autobudgetPriority => 5,
                    price_currency     => 'YND_FIXED',
                },
            ],
        },
        'convertion of StatusPaused & AutoBudgetPriority'
    );

    # $ret_cond_id ne $old_ret_cond_id && warning RetargetingConditionIDIgnored
    $manager = get_self('manager');
    $params = {
        Action       => 'Update',
        Login        => $manager->{user_info}{login},
        Retargetings => [
            { RetargetingID => 2, RetargetingConditionID => 3, },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply(
        $manager->{preprocess},
        {
            old_retargetings => {
                2 => {
                    ret_id             => 2, 
                    ret_cond_id        => 2,
                    pid                => 2,
                    price_context      => '7.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 2,
                    cid                => 2,
                    currency           => 'YND_FIXED',
                },
            },
            pid2cid => { 2 => 2, },
            camps   => {
                2 => { cid => 2, currency => 'YND_FIXED', ClientID => 2 },
            },
            camp_strategy => { },
            cid2clientid => {
                2 => 2,
            },
            converted => [
                {
                    bid            => 2,
                    pid            => 2,
                    ret_id         => 2,
                    ret_cond_id    => 2,
                    price_currency => 'YND_FIXED',
                },
            ],
        },
        'new ret_cond_id ne old ret_cond_id - warning RetargetingConditionIDIgnored'
    );

    is_deeply(
        $manager->{ret},
        [
            {
                Warnings => [
                    {
                        WarningCode   => 211,
                        WarningString => 'Идентификатор условия RetargetingConditionID был проигнорирован',
                        Description   => '{}',
                    }
                ],
            },
        ],
        'proper warning'
    );

    # update ContextPrice => update Currency also
    $client = get_self('client');
    $params = {
        Action       => 'Update',
        Retargetings => [
            { Fields => [qw/ ContextPrice /], RetargetingID => 1, ContextPrice => 3, },
        ],
    };
    preprocess_retargeting( $client, $params );

    is_deeply(
        $client->{preprocess},
        {
            old_retargetings => {
                1 => {
                    ret_id             => 1, 
                    ret_cond_id        => 1,
                    pid                => 1,
                    price_context      => '5.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 1,
                    cid                => 1,
                    currency           => 'YND_FIXED',
                },
            },
            pid2cid => { 1 => 1, },
            camps   => {
                1 => { cid => 1, currency => 'YND_FIXED', ClientID => 2 },
            },
            camp_strategy => { },
            cid2clientid => {
                1 => 2,
            },
            converted => [
                {
                    Fields             => [qw/ ContextPrice /],
                    bid                => 1,
                    pid                => 1,
                    ret_id             => 1,
                    ret_cond_id        => 1,
                    price_context      => 3,
                    price_currency     => 'YND_FIXED',
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                },
            ],
        },
        'update ContextPrice => update Currency also'
    );

    # update & Currency eq 'YND_FIXED'
    $client = get_self('client');
    $params = {
        Action       => 'Update',
        Retargetings => [
            { RetargetingID => 1, Currency => 'YND_FIXED', },
        ],
    };
    preprocess_retargeting( $client, $params );

    is_deeply(
        $client->{preprocess},
        {
            old_retargetings => {
                1 => {
                    ret_id             => 1, 
                    ret_cond_id        => 1,
                    pid                => 1,
                    price_context      => '5.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 1,
                    cid                => 1,
                    currency           => 'YND_FIXED',
                },
            },
            pid2cid => { 1 => 1, },
            camps   => {
                1 => { cid => 1, currency => 'YND_FIXED', ClientID => 2 },
            },
            camp_strategy => { },
            cid2clientid => {
                1 => 2,
            },
            converted => [
                {
                    bid                => 1,
                    pid                => 1,
                    ret_id             => 1,
                    ret_cond_id        => 1,
                    price_currency     => 'YND_FIXED',
                },
            ],
        },
        'update & Currency eq "YND_FIXED"'
    );

    # currency convertion
    $manager = get_self('manager');
    $params = {
        Action       => 'Update',
        Login        => $manager->{user_info}{login},
        Retargetings => [
            { RetargetingID => 3, ContextPrice => 15, },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply(
        $manager->{preprocess},
        {
            old_retargetings => {
                3 => {
                    ret_id             => 3, 
                    ret_cond_id        => 3,
                    pid                => 3,
                    price_context      => '9.00', 
                    autobudgetPriority => 1,
                    is_suspended       => 1,
                    bid                => 3,
                    cid                => 3,
                    currency           => 'RUB',
                },
            },
            pid2cid => { 3 => 3, },
            camps   => {
                3 => { cid => 3, currency => 'RUB', ClientID => 2 },
            },
            camp_strategy => { },
            cid2clientid => {
                3 => 2,
            },
            converted => [
                {
                    bid            => 3,
                    pid            => 3,
                    ret_id         => 3,
                    ret_cond_id    => 3,
                    price_context  => 15, 
                    price_currency => 'RUB',
                },
            ],
        },
        'currency convertion'
    );

    # save old values for other fields
    $manager = get_self('manager');
    $params = {
        Action       => 'Update',
        Login        => $manager->{user_info}{login},
        Retargetings => [
            { Fields => [qw/ ContextPrice /], RetargetingID => 3, ContextPrice => 15, },
        ],
    };
    preprocess_retargeting( $manager, $params );

    is_deeply(
        $manager->{preprocess},
        {
            old_retargetings => {
                3 => {
                    ret_id             => 3, 
                    ret_cond_id        => 3,
                    pid                => 3,
                    price_context      => '9.00', 
                    autobudgetPriority => 1,
                    is_suspended       => 1,
                    bid                => 3,
                    cid                => 3,
                    currency           => 'RUB',
                },
            },
            pid2cid => { 3 => 3, },
            camps   => {
                3 => { cid => 3, currency => 'RUB', ClientID => 2 },
            },
            camp_strategy => { },
            cid2clientid => {
                3 => 2,
            },
            converted => [
                {
                    Fields             => [qw/ ContextPrice /],
                    bid                => 3,
                    pid                => 3,
                    ret_id             => 3,
                    ret_cond_id        => 3,
                    price_context      => 15, 
                    price_currency     => 'RUB',
                    is_suspended       => 1,
                    autobudgetPriority => 1,
                },
            ],
        },
        'save old values for other fields'
    );

    #done_testing();
};

subtest 'API::Methods::Retargeting::preprocess_retargeting - Get & Delete methods' => sub {
    plan tests => 7;

    # client call - login added to params
    my $client = get_self('client');
    my $params = {
        Action            => 'Get',
        SelectionCriteria => {
            AdIDS => [1],
        },
    };
    preprocess_retargeting( $client, $params );

    is( $params->{Login}, $client->{user_info}{login}, 'login added to params when client call method' );

    is_deeply(
        $client->{preprocess},
        {
            retargetings => {
                1 => {
                    ret_id             => 1, 
                    ret_cond_id        => 1,
                    pid                => 1,
                    price_context      => '5.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 1,
                    cid                => 1,
                    currency           => 'YND_FIXED',
                },
            },
            pid2bid => { 1 => 1, },
        },
        'preprocess collect right dataset for client'
    );

    # not client call - login must be given in params
    my $manager = get_self('manager');
    $params = {
        Action            => 'Delete',
        SelectionCriteria => {
            RetargetingIDS => [2],
        },
    };
    throws_ok { preprocess_retargeting( $manager, $params ) } qr/Can not choose shard param/, 'error - login must be given in params for not client';

    $params = {
        Action            => 'Delete',
        Login             => $manager->{user_info}{login},
        SelectionCriteria => {
            RetargetingIDS => [2],
        },
    };
    preprocess_retargeting( $manager, $params );

    is_deeply(
        $manager->{preprocess},
        {
            retargetings => {
                2 => {
                    ret_id             => 2, 
                    ret_cond_id        => 2,
                    pid                => 2,
                    price_context      => '7.00', 
                    autobudgetPriority => undef,
                    is_suspended       => 0,
                    bid                => 2,
                    cid                => 2,
                    currency           => 'YND_FIXED',
                },
            },
            pid2bid => { 2 => 2, },
        },
        'ok - login given in params for not client'
    );

    # selection criteria join with 'and'
    $manager = get_self('manager');
    $params = {
        Action            => 'Get',
        Login             => $manager->{user_info}{login},
        SelectionCriteria => {
            AdIDS                   => [1],
            RetargetingIDS          => [2],
            RetargetingConditionIDS => [3],
        },
    };
    preprocess_retargeting( $manager, $params );

    is_deeply(
        $manager->{preprocess},
        {
            retargetings => {},
            pid2bid => { 1 => 1, },
        },
        'selection criteria join with "and"'
    );

    # No selective conditions for get_group_retargeting
    $client = get_self('client');
    $params = {
        Action            => 'Delete',
        SelectionCriteria => {},
    };
    throws_ok { preprocess_retargeting( $client, $params ) } qr/No selective conditions for get_group_retargeting/, 'error - no selection criteria given';

    # use get_main_banner_ids_by_pids for collecting pid2bid when no AdIDS given
    {
        no strict 'refs';
        no warnings 'redefine';

        *{"Primitives::get_main_banner_ids_by_pids"} = sub {
            return { map { $_ => undef } @_ };
        };

        $client = get_self('client');
        $params = {
            Action            => 'Get',
            SelectionCriteria => {
                RetargetingIDS          => [3],
                RetargetingConditionIDS => [3],
            },
        };
        preprocess_retargeting( $client, $params );

        is_deeply(
            $client->{preprocess},
            {
                retargetings => {
                    3 => {
                        ret_id             => 3, 
                        ret_cond_id        => 3,
                        pid                => 3,
                        price_context      => '9.00', 
                        autobudgetPriority => 1,
                        is_suspended       => 1,
                        bid                => 3,
                        cid                => 3,
                        currency           => 'RUB',
                    },
                },
                pid2bid => { 3 => undef, },
            },
            'use get_main_banner_ids_by_pids for collecting pid2bid when no AdIDS given'
        );
    }

    #done_testing();
};

#done_testing();
