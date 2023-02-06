#!/usr/bin/perl

use warnings;
use strict;

use Test::More;
use Test::Deep;

use AdGroupTools;

# --------------------------------------------------------------------
sub main {
    my @tests = (
        {
            test_name => 'dont change',
            before => {
                banners => [
                    {bid => 1
                     , title => "t"
                     , body => "b"
                     , pid => 10
                    },
                    {bid => 2
                     , title => "t"
                     , body => "b"
                     , pid => 10
                    },
                ]
            },
            after => {
                adgroups => [
                    {
                        pid => 10,
                        group_name => undef,
                        minus_geo => '',
                        all_geo_disabled => undef,
                        banners => [
                            {bid => 1
                             , title => "t"
                             , body => "b"
                             , pid => 10
                            },
                            {bid => 2
                             , title => "t"
                             , body => "b"
                             , pid => 10
                            },
                        ]
                    }
                ]
            },
            opt => {}
        },
        # ---------
        {
            test_name => 'group by group name',
            before => {
                banners => [
                    {bid => 1
                     , title => "t"
                     , body => "b"
                     , pid => 10
                     , group_name => "Group1"
                    },
                    {bid => 2
                     , title => "t"
                     , body => "b"
                     , pid => 10
                     , group_name => "Group1"
                    },
                    {bid => 3
                     , title => "t"
                     , body => "b"
                     , pid => 11
                    },
                ]
            },
            after => {
                adgroups => [
                    {
                        pid => 10,
                        group_name => "Group1",
                        minus_geo => '',
                        all_geo_disabled => undef,
                        banners => [
                            {bid => 1
                             , title => "t"
                             , body => "b"
                             , pid => 10
                             , group_name => "Group1"
                            },
                            {bid => 2
                             , title => "t"
                             , body => "b"
                             , pid => 10
                             , group_name => "Group1"
                            },
                        ]
                    },
                    {
                        pid => 11,
                        group_name => undef,
                        minus_geo => '',
                        all_geo_disabled => undef,
                        banners => [
                            {bid => 3
                             , title => "t"
                             , body => "b"
                             , pid => 11
                            },
                        ]
                    },
                ]
            },
            opt => {}
        },
        # ---------
        {
            test_name => 'group by group name (keep_one_banner)',
            before => {
                banners => [
                    {bid => 1
                     , title => "t"
                     , body => "b"
                     , pid => 10
                     , group_name => "Group1"
                    },
                    {bid => 2
                     , title => "t"
                     , body => "b"
                     , pid => 10
                     , group_name => "Group1"
                    },
                    {bid => 3
                     , title => "t"
                     , body => "b"
                     , pid => 10
                     , group_name => "Group1"
                    },
                ]
            },
            after => {
                adgroups => [
                    {
                        pid => 10,
                        group_name => "Group1",
                        minus_geo => '',
                        all_geo_disabled => undef,
                        banners => [
                            {bid => 1
                             , title => "t"
                             , body => "b"
                             , pid => 10
                             , group_name => "Group1"
                            },
                        ]
                    }
                ]
            },
            opt => {keep_one_banner => 1}
        },
        # ---------
        {
            test_name => 'group by group name and extract bool flags',
            before => {
                banners => [
                    {bid => 1
                     , title => "t"
                     , body => "b"
                     , pid => 10
                     , group_name => "Group1"
                     , is_first => 1
                    },
                    {bid => 2
                     , title => "t"
                     , body => "b"
                     , pid => 10
                     , group_name => "Group1"
                     , is_last => 1
                    },
                    {bid => 3
                     , title => "t"
                     , body => "b"
                     , pid => 11
                     , is_first => 1
                    },
                ]
            },
            after => {
                adgroups => [
                    {
                        pid => 10,
                        group_name => "Group1",
                        minus_geo => '',
                        all_geo_disabled => undef,
                        is_first => 1,
                        is_last => 1,
                        banners => [
                            {bid => 1
                             , title => "t"
                             , body => "b"
                             , pid => 10
                             , group_name => "Group1"
                             , is_first => 1
                            },
                            {bid => 2
                             , title => "t"
                             , body => "b"
                             , pid => 10
                             , group_name => "Group1"
                             , is_last => 1
                            },
                        ]
                    },
                    {
                        pid => 11,
                        group_name => undef,
                        minus_geo => '',
                        all_geo_disabled => undef,
                        is_first => 1,
                        banners => [
                            {bid => 3
                             , title => "t"
                             , body => "b"
                             , pid => 11
                             , is_first => 1
                            }
                        ]
                    }
                ]
            },
            opt => {extract_bool_flags => [qw/is_first is_last/]}
        },
    );

    for my $test (@tests) {
        my $before = $test->{before};
        AdGroupTools::separate_adgroups_from_banners_struct($before, %{$test->{opt}});
        cmp_deeply($before, $test->{after}, $test->{test_name});
    }

    done_testing();
}

# --------------------------------------------------------------------
main()
