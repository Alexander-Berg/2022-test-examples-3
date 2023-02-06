#!/usr/bin/perl

=pod

    $Id: generate.t 123627 2016-08-25 13:03:46Z n-boy $

=cut

use strict;
use warnings;
use utf8;

use Test::Deep;
use Test::More;

use Yandex::Test::UTF8Builder;
use Yandex::Clone qw/yclone/;

use Stat::ReportMaster;

use Settings;

my @tests = (
    {before => {group_by => [qw/campaign adgroup/],
                filters  => {},
                columns  => [qw//]},
     lists  => {force_group_by_fields => [qw/banner/],
                allow_group_by_fields => [qw/campaign adgroup banner/],
                forbid_fields => undef},
     after  => {group_by => [qw/campaign adgroup banner/],
                filters  => {},
                columns  => [qw//]},
     message => 'force group by fields' },

    {before => {group_by => [qw/campaign adgroup banner/],
                filters  => {adgroup => {}, banner => {}},
                columns  => [qw//]},
     lists  => {force_group_by_fields => undef,
                allow_group_by_fields => [qw/campaign banner/],
                forbid_fields => undef},
     after  => {group_by => [qw/campaign banner/],
                filters  => {banner => {}},
                columns  => [qw//]},
     message => 'allow group by fields' },

    {before => {group_by => [qw/campaign adgroup banner/],
                filters  => {campaign => {}, adgroup => {}, winrate => {}},
                columns  => [qw/shows winrate/]},
     lists  => {force_group_by_fields => undef,
                allow_group_by_fields => undef,
                forbid_fields => [qw/campaign winrate/]},
     after  => {group_by => [qw/adgroup banner/],
                filters  => {adgroup => {}},
                columns  => [qw/shows/]},
     message => 'forbid fields' },

    {before => {group_by => [qw/campaign adgroup/],
                filters  => {campaign => {}, adgroup => {}, winrate => {}},
                columns  => [qw/shows winrate/]},
     lists  => {force_group_by_fields => undef,
                allow_group_by_fields => [qw/campaign adgroup/],
                forbid_fields => [qw/campaign winrate/]},
     after  => {group_by => [qw/adgroup/],
                filters  => {adgroup => {}},
                columns  => [qw/shows/]},
     message => 'forbid fields & allow group by fields' },

    {before => {group_by => [qw/campaign adgroup/],
                filters  => {shows => {}, winrate => {}},
                columns  => [qw//]},
     lists  => {force_group_by_fields => undef,
                allow_group_by_fields => undef,
                forbid_fields => [qw/winrate/]},
     after  => {group_by => [qw/campaign adgroup/],
                filters  => {shows => {}},
                columns  => [qw//]},
     message => 'forbid fields & filter by countable fields' },

    {before => {group_by => [qw/campaign adgroup/],
                filters  => {},
                columns  => [qw/clicks winrate/]},
     lists  => {force_group_by_fields => undef,
                allow_group_by_fields => undef,
                forbid_fields => [qw/winrate/]},
     after  => {group_by => [qw/campaign adgroup/],
                filters  => {},
                columns  => [qw/clicks/]},
     message => 'forbid fields & countable fields in columns' },

    {before => {group_by => [qw/adgroup/],
                filters  => {campaign_type => {}, adgroup => {}},
                columns  => [qw//]},
     lists  => {force_group_by_fields => undef,
                allow_group_by_fields => undef,
                forbid_fields => [qw/campaign/]},
     after  => {group_by => [qw/adgroup/],
                filters  => {adgroup => {}},
                columns  => [qw//]},
     message => 'forbid fields by filter alias' },

    {before => {group_by => [qw//],
                filters  => {clicks_a => {}, shows_b => {}, clicks => {}},
                columns  => [qw//]},
     lists  => {force_group_by_fields => undef,
                allow_group_by_fields => undef,
                forbid_fields => [qw/clicks/]},
     after  => {group_by => [qw//],
                filters  => {shows_b => {}},
                columns  => [qw//]},
     message => 'forbid fields & filters with compare periods suffix' },
);

for my $t (@tests) {
    my $report_options = yclone $t->{before};
    Stat::ReportMaster::_limit_fields_by_lists($report_options, %{$t->{lists}});
    cmp_deeply($report_options, $t->{after}, $t->{message});
}

done_testing;
