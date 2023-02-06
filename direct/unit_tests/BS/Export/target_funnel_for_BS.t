#!/usr/bin/perl

use Direct::Modern;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Deep;

use utf8;

use BS::Export ();

# [$target_funnel, $expected_result]
my @tests = (
    [
        'same_products',
        {
            OnlyOfferRetargeting => 0,
            OnlyNewAuditory => 0,
        },
    ],
    [
        'product_page_visit',
        {
            OnlyOfferRetargeting => 1,
            OnlyNewAuditory => 0,
        },
    ],
    [
        'new_auditory',
        {
            OnlyOfferRetargeting => 0,
            OnlyNewAuditory => 1,
        },
    ],
);

Test::More::plan(tests => scalar(@tests));

foreach my $test (@tests) {
    my ($target_funnel, $expected_result) = @$test;
    my $test_name = $target_funnel;
    my $result = BS::Export::target_funnel_for_BS($target_funnel);
    cmp_deeply($result, $expected_result, $test_name);
}
