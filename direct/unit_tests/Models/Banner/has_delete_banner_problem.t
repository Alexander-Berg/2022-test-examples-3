#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use Test::Deep;

BEGIN { use_ok('Models::Banner'); }


my @tests_have_problems = (
    {bid => 100, sum => 10, group_statusModerate => 'Sending', group_statusPostModerate => 'No',
     banner_statusModerate => 'Sending', banner_statusPostModerate => 'No', BannerID => 12731, test_name=>'no moderation'},

    {bid => 100, sum => 0, group_statusModerate => 'Sending', group_statusPostModerate => 'No',
     banner_statusModerate => 'Sending', banner_statusPostModerate => 'No', BannerID => 12731, test_name=>'no sum and no moderation'},

    {bid => 100, sum => 0, group_statusModerate => 'Yes', group_statusPostModerate => 'Yes',
     banner_statusModerate => 'Yes', banner_statusPostModerate => 'Yes', BannerID => 12731, test_name=>'no sum, ok moderation'},

    {bid => 100, sum => 892, group_statusModerate => 'Yes', group_statusPostModerate => 'Yes',
     banner_statusModerate => 'Yes', banner_statusPostModerate => 'Yes', BannerID => 0, test_name=>'no BannerID'},

    {bid => 100, sum => 892, group_statusModerate => 'Yes', group_statusPostModerate => 'Rejected',
     banner_statusModerate => 'Yes', banner_statusPostModerate => 'Yes', BannerID => 0, test_name=>'no BannerID, rejected moderation'},

    {bid => 100, sum => 0, group_statusModerate => 'Yes', group_statusPostModerate => 'Yes', camp_in_bs_queue => 1,
     banner_statusModerate => 'New', banner_statusPostModerate => 'No', BannerID => 12731, test_name=>'ok camp_in_bs_queue, fail moderate'},

    {bid => 100, sum => 0, group_statusModerate => 'Yes', group_statusPostModerate => 'Yes', camp_in_bs_queue => 0,
     banner_statusModerate => 'Yes', banner_statusPostModerate => 'Yes', BannerID => 12731, test_name=>'no camp_in_bs_queue, no sum, ok moderate'},

);
my @tests_no_problems = (
    {bid => 100, sum => 0, group_statusModerate => 'Sending', group_statusPostModerate => 'No',
     banner_statusModerate => 'Sending', banner_statusPostModerate => 'No', BannerID => 0, test_name=>'no BannerID, no moderation, no sum'},

    {bid => 100, sum => 0, group_statusModerate => 'Yes', group_statusPostModerate => 'Yes', camp_in_bs_queue => 0,
     banner_statusModerate => 'Yes', banner_statusPostModerate => 'Yes', BannerID => 0, test_name=>'no sum, no BannerID'},

    {bid => 100, sum => 10, group_statusModerate => 'Sending', group_statusPostModerate => 'No',
     banner_statusModerate => 'Sending', banner_statusPostModerate => 'No', BannerID => 0, test_name=>'no moderation, ok BannerID'},

    {bid => 100, sum => 0, group_statusModerate => 'Yes', group_statusPostModerate => 'Yes', camp_in_bs_queue => 0,
     banner_statusModerate => 'Yes', banner_statusPostModerate => 'Yes', BannerID => 0, test_name=>'no camp_in_bs_queue, ok moderate, no sum'},
);

foreach my $test (@tests_have_problems) {
    is (Models::Banner::has_delete_banner_problem($test), sprintf("Удаление баннера %s невозможно", $test->{bid}), $test->{test_name});
}
foreach my $test (@tests_no_problems) {
    is (Models::Banner::has_delete_banner_problem($test), 0, $test->{test_name});
}


done_testing;
