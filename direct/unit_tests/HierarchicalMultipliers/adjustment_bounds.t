use my_inc "../..";
use Direct::Modern;

use Test::More;

BEGIN {
    use_ok 'HierarchicalMultipliers';
}

#     group mob, group dem, group ret, camp mob, camp dem, camp ret, expect lower, expect upper
my @tests = (
    [ 55       , undef    , undef    , undef   , undef   , undef   , undef       , 55  ],
    [ 155      , undef    , undef    , undef   , undef   , undef   , undef       , 155 ],
    [ 55       , 155      , undef    , undef   , undef   , undef   , 55          , 155 ],
    [ 155      , 155      , 155      , 150     , 150     , 150     , 155         , 372 ],
    [ 50       , undef    , 400      , undef   , 300     , undef   , 50,         , 1200 ],
    [ undef    , 168      , 320      , undef   , undef   , undef   , 168         , 538 ],
    [ undef    , undef    , undef    , undef   , undef   , undef   , undef       , undef ],
    [ 100      , undef    , undef    , undef   , undef   , undef   , undef       , undef ],
);

for my $test (@tests) {
    my ($gm, $gd, $gr, $cm, $cd, $cr, $expect_lower, $expect_upper) = @$test;
    my @display_names = map { $_ // 'undef' } @$test;
    my $name = join("/", @display_names[0..($#display_names - 2)]) . " -> " . join("/", @display_names[-2..-1]);
    subtest $name => sub {
        my $group = {};
        $group->{mobile_multiplier}{multiplier_pct} = $gm if $gm;
        $group->{demography_multiplier}{conditions}[0]{multiplier_pct} = $gd if $gd;
        $group->{retargeting_multiplier}{conditions}{1}{multiplier_pct} = $gr if $gr;
        my $camp;
        $camp->{mobile_multiplier}{multiplier_pct} = $cm if $cm;
        $camp->{demography_multiplier}{conditions}[0]{multiplier_pct} = $cd if $cd;
        $camp->{retargeting_multiplier}{conditions}{1}{multiplier_pct} = $cr if $cr;

        is_deeply HierarchicalMultipliers::adjustment_bounds($group, $camp), {
            adjustments_lower_bound => $expect_lower,
            adjustments_upper_bound => $expect_upper,
        };
    };
}

done_testing;
