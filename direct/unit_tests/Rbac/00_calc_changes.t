#!/usr/bin/perl

use Direct::Modern;

use Test::More tests => 7;

use my_inc '../..';

use Settings;

use Rbac;

{ # unset supervisor of manager
    my $managers_info = {
        1 => {
            manager_client_id      => 1,
            manager_uid            => 1,
            chiefs_client_id       => undef,
            chiefs_uid             => undef,
            subordinates_client_id => [2, 3],
            subordinates_uid       => [2, 3],
        },
        2 => {
            manager_client_id      => 2,
            manager_uid            => 2,
            chiefs_client_id       => [1],
            chiefs_uid             => [1],
            subordinates_client_id => [3],
            subordinates_uid       => [3],
        },
        3 => {
            manager_client_id      => 3,
            manager_uid            => 3,
            chiefs_client_id       => [1,2],
            chiefs_uid             => [1,2],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
    };

    my $manager_client_id    = 3;
    my $supervisor_client_id = 0;

    my $actual = Rbac::_calc_changes( $manager_client_id, $supervisor_client_id, $managers_info );
    
    my $expected = {
        1 => {
            subordinates_client_id => [2],
            subordinates_uid       => [2],
        },
        2 => {
            subordinates_client_id => [],
            subordinates_uid       => [],
        },
        3 => {
            supervisor_client_id => 0,
            supervisor_uid       => 0,
            chiefs_client_id     => [],
            chiefs_uid           => [],
        },
    };

    is_deeply( $actual, $expected, 'unset supervisor of manager' );
}

{ # unset supervisor of teamleader
    my $managers_info = {
        1 => {
            manager_client_id      => 1,
            manager_uid            => 1,
            chiefs_client_id       => undef,
            chiefs_uid             => undef,
            subordinates_client_id => [2, 3],
            subordinates_uid       => [2, 3],
        },
        2 => {
            manager_client_id      => 2,
            manager_uid            => 2,
            chiefs_client_id       => [1],
            chiefs_uid             => [1],
            subordinates_client_id => [3],
            subordinates_uid       => [3],
        },
        3 => {
            manager_client_id      => 3,
            manager_uid            => 3,
            chiefs_client_id       => [1,2],
            chiefs_uid             => [1,2],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
    };

    my $manager_client_id    = 2;
    my $supervisor_client_id = 0;

    my $actual = Rbac::_calc_changes( $manager_client_id, $supervisor_client_id, $managers_info );

    my $expected = {
        1 => {
            subordinates_client_id => [],
            subordinates_uid       => [],
        },
        2 => {
            supervisor_client_id => 0,
            supervisor_uid       => 0,
            chiefs_client_id     => [],
            chiefs_uid           => [],
        },
        3 => {
            chiefs_client_id     => [2],
            chiefs_uid           => [2],
        },
    };

    is_deeply( $actual, $expected, 'unset supervisor of teamleader' );
}

{ # unset supervisor for superteamleader - хотя странный какой-то тестовый кейс
    my $managers_info = {
        1 => {
            manager_client_id      => 1,
            manager_uid            => 1,
            chiefs_client_id       => undef,
            chiefs_uid             => undef,
            subordinates_client_id => [2, 3],
            subordinates_uid       => [2, 3],
        },
        2 => {
            manager_client_id      => 2,
            manager_uid            => 2,
            chiefs_client_id       => [1],
            chiefs_uid             => [1],
            subordinates_client_id => [3],
            subordinates_uid       => [3],
        },
        3 => {
            manager_client_id      => 3,
            manager_uid            => 3,
            chiefs_client_id       => [1,2],
            chiefs_uid             => [1,2],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
    };

    my $manager_client_id    = 1;
    my $supervisor_client_id = 0;

    my $actual = Rbac::_calc_changes( $manager_client_id, $supervisor_client_id, $managers_info );

    my $expected = {
        1 => {
            supervisor_client_id => 0,
            supervisor_uid       => 0,
            chiefs_client_id     => [],
            chiefs_uid           => [],
        },
        2 => {
            chiefs_client_id     => [1],
            chiefs_uid           => [1],
        },
        3 => {
            chiefs_client_id     => [1, 2],
            chiefs_uid           => [1, 2],
        },
    };

    is_deeply( $actual, $expected, 'unset supervisor for superteamleader' );
}

{ # move manager to another teamleader of another superteamleader
    my $managers_info = {
        1 => {
            manager_client_id      => 1,
            manager_uid            => 1,
            chiefs_client_id       => undef,
            chiefs_uid             => undef,
            subordinates_client_id => [2, 3],
            subordinates_uid       => [2, 3],
        },
        2 => {
            manager_client_id      => 2,
            manager_uid            => 2,
            chiefs_client_id       => [1],
            chiefs_uid             => [1],
            subordinates_client_id => [3],
            subordinates_uid       => [3],
        },
        3 => {
            manager_client_id      => 3,
            manager_uid            => 3,
            chiefs_client_id       => [1, 2],
            chiefs_uid             => [1, 2],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
        4 => {
            manager_client_id      => 4,
            manager_uid            => 4,
            chiefs_client_id       => undef,
            chiefs_uid             => undef,
            subordinates_client_id => [5, 6],
            subordinates_uid       => [5, 6],
        },
        5 => {
            manager_client_id      => 5,
            manager_uid            => 5,
            chiefs_client_id       => [4],
            chiefs_uid             => [4],
            subordinates_client_id => [6],
            subordinates_uid       => [6],
        },
        6 => {
            manager_client_id      => 6,
            manager_uid            => 6,
            chiefs_client_id       => [4, 5],
            chiefs_uid             => [4, 5],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
    };

    my $manager_client_id    = 3;
    my $supervisor_client_id = 5;

    my $actual = Rbac::_calc_changes( $manager_client_id, $supervisor_client_id, $managers_info );

    my $expected = {
        1 => {
            subordinates_client_id => [2],
            subordinates_uid       => [2],
        },
        2 => {
            subordinates_client_id => [],
            subordinates_uid       => [],
        },
        3 => {
            supervisor_client_id => 5,
            supervisor_uid       => 5,
            chiefs_client_id     => [4, 5],
            chiefs_uid           => [4, 5],
        },
        4 => {
            subordinates_client_id => [5, 6, 3],
            subordinates_uid       => [5, 6, 3],
        },
        5 => {
            subordinates_client_id => [6, 3],
            subordinates_uid       => [6, 3],
        },
    };

    is_deeply( $actual, $expected, 'move manager to another teamleader' );
}

{ # move teamleader to another superteamleader
    my $managers_info = {
        1 => {
            manager_client_id      => 1,
            manager_uid            => 1,
            chiefs_client_id       => undef,
            chiefs_uid             => undef,
            subordinates_client_id => [2, 3],
            subordinates_uid       => [2, 3],
        },
        2 => {
            manager_client_id      => 2,
            manager_uid            => 2,
            chiefs_client_id       => [1],
            chiefs_uid             => [1],
            subordinates_client_id => [3],
            subordinates_uid       => [3],
        },
        3 => {
            manager_client_id      => 3,
            manager_uid            => 3,
            chiefs_client_id       => [1, 2],
            chiefs_uid             => [1, 2],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
        4 => {
            manager_client_id      => 4,
            manager_uid            => 4,
            chiefs_client_id       => undef,
            chiefs_uid             => undef,
            subordinates_client_id => [5, 6],
            subordinates_uid       => [5, 6],
        },
        5 => {
            manager_client_id      => 5,
            manager_uid            => 5,
            chiefs_client_id       => [4],
            chiefs_uid             => [4],
            subordinates_client_id => [6],
            subordinates_uid       => [6],
        },
        6 => {
            manager_client_id      => 6,
            manager_uid            => 6,
            chiefs_client_id       => [4, 5],
            chiefs_uid             => [4, 5],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
    };

    my $manager_client_id    = 2;
    my $supervisor_client_id = 4;

    my $actual = Rbac::_calc_changes( $manager_client_id, $supervisor_client_id, $managers_info );

    my $expected = {
        1 => {
            subordinates_client_id => [],
            subordinates_uid       => [],
        },
        2 => {
            supervisor_client_id => 4,
            supervisor_uid       => 4,
            chiefs_client_id     => [4],
            chiefs_uid           => [4],
        },
        3 => {
            chiefs_client_id     => [4, 2],
            chiefs_uid           => [4, 2],
        },
        4 => {
            subordinates_client_id => [5, 6, 2, 3],
            subordinates_uid       => [5, 6, 2, 3],
        },
    };

    is_deeply( $actual, $expected, 'move teamleader to another superteamleader' );
}

{ # move manager to another teamleader of the same superteamleader
    my $managers_info = {
        1 => {
            manager_client_id      => 1,
            manager_uid            => 1,
            chiefs_client_id       => undef,
            chiefs_uid             => undef,
            subordinates_client_id => [2, 3, 4, 5],
            subordinates_uid       => [2, 3, 4, 5],
        },
        2 => {
            manager_client_id      => 2,
            manager_uid            => 2,
            chiefs_client_id       => [1],
            chiefs_uid             => [1],
            subordinates_client_id => [3],
            subordinates_uid       => [3],
        },
        3 => {
            manager_client_id      => 3,
            manager_uid            => 3,
            chiefs_client_id       => [1, 2],
            chiefs_uid             => [1, 2],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
        4 => {
            manager_client_id      => 4,
            manager_uid            => 4,
            chiefs_client_id       => [1],
            chiefs_uid             => [1],
            subordinates_client_id => [5],
            subordinates_uid       => [5],
        },
        5 => {
            manager_client_id      => 5,
            manager_uid            => 5,
            chiefs_client_id       => [1, 4],
            chiefs_uid             => [1, 4],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
    };

    my $manager_client_id    = 3;
    my $supervisor_client_id = 4;

    my $actual = Rbac::_calc_changes( $manager_client_id, $supervisor_client_id, {%$managers_info} );

    my $expected = {
        2 => {
            subordinates_client_id => [],
            subordinates_uid       => [],
        },
        3 => {
            supervisor_client_id => 4,
            supervisor_uid       => 4,
            chiefs_client_id     => [1, 4],
            chiefs_uid           => [1, 4],
        },
        4 => {
            subordinates_client_id => [5, 3],
            subordinates_uid       => [5, 3],
        },
    };

    is_deeply( $actual, $expected, 'move manager to another teamleader of the same superteamleader' );
}

{ # move teamleader from under superteamleader
    my $managers_info = {
        1 => {
            manager_client_id      => 1,
            manager_uid            => 1,
            chiefs_client_id       => undef,
            chiefs_uid             => undef,
            subordinates_client_id => [2, 3, 4],
            subordinates_uid       => [2, 3, 4],
        },
        2 => {
            manager_client_id      => 2,
            manager_uid            => 2,
            chiefs_client_id       => [1],
            chiefs_uid             => [1],
            subordinates_client_id => [3, 4],
            subordinates_uid       => [3, 4],
        },
        3 => {
            manager_client_id      => 3,
            manager_uid            => 3,
            chiefs_client_id       => [1, 2],
            chiefs_uid             => [1, 2],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
        4 => {
            manager_client_id      => 4,
            manager_uid            => 4,
            chiefs_client_id       => [1, 2],
            chiefs_uid             => [1, 2],
            subordinates_client_id => undef,
            subordinates_uid       => undef,
        },
    };

    my $manager_client_id    = 2;
    my $supervisor_client_id = undef;

    my $actual = Rbac::_calc_changes( $manager_client_id, $supervisor_client_id, $managers_info );

    my $expected = {
        1 => {
            subordinates_client_id => [],
            subordinates_uid       => [],
        },
        2 => {
            supervisor_client_id   => 0,
            supervisor_uid         => 0,
            chiefs_client_id       => [],
            chiefs_uid             => [],
        },
        3 => {
            chiefs_client_id     => [2],
            chiefs_uid           => [2],
        },
        4 => {
            chiefs_client_id     => [2],
            chiefs_uid           => [2],
        },
    };

    is_deeply( $actual, $expected, 'move teamleader from under superteamleader' );
}