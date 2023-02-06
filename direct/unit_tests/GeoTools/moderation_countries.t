use my_inc "../..";
use Direct::Modern;

use Test::More;

use Test::Subtest;

BEGIN {
    use_ok 'GeoTools';
}

sub mc_ru {
    GeoTools::moderation_countries($_[0], { tree => 'ru' });
}

sub mc_api {
    GeoTools::moderation_countries($_[0], { tree => 'api' });
}

sub mc_ua {
    GeoTools::moderation_countries($_[0], { tree => 'ua' });
}

subtest_ "Empty geo" => sub {
    is mc_ru(undef), undef;
    is mc_ru(''), undef;
};

subtest "Empty regions explained" => sub {
    is mc_ru( GeoTools::get_targetings_union(["225,-3,-17,-40,-26,-59,-73,-102444,-52,-977"],{tree => 'ru'})), "225";
};

subtest "Mixed empty and non empty regions" => sub {
    is mc_ru( GeoTools::get_targetings_union(["10002,10003,-93,-94"],{tree => 'ru'})), "84,93,94,95,20917,20968";
};

subtest_ "Country derivation from its subregions" => sub {
    is mc_ru("213"), "225"; # Moscow -> Russia
    is mc_ua("143,213"), "187,225"; # Moscow,Kiev -> Russia,Ukraine
};

subtest_ "Country derivation from its superregions" => sub {
    my %countries = map { $_ => 1 } split /,/, mc_api("0"); # World
    # 222 at the time of writing
    ok keys %countries > 200;
    ok keys %countries < 250;

    # 27 at the time of writing, 30 now
    my %europe_countries = map { $_ => 1 } split /,/, mc_api("111"); # Europe
    cmp_ok(keys %europe_countries, '>', 20);
    cmp_ok(keys %europe_countries, '<=', 30);
};

subtest_ "Direct country derivation" => sub {
    is mc_ru("187,225"), "187,225"; # Russia,Ukraine -> Russia,Ukraine
};

subtest_ "Crimea special cases" => sub {
    is mc_ru("213,977"), "225"; # ru: Moscow,Crimea -> Russia
    is mc_ua("213,977"), "187,225"; # ua: Moscow,Crimea -> Russia,Ukraine
    is mc_ua("977"), "187"; # ua: Crimea -> Ukraine
    my %countries = map { $_ => 1 } split /,/, mc_api("213,977");
    # 222 at the time of writing
    ok keys %countries > 200;
    ok keys %countries < 250;
};

run_subtests();
