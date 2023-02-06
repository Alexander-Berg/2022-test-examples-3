#!/usr/bin/perl

use Direct::Modern;
use Test::More;

use Models::Banner;

*ivd = \&Models::Banner::is_videomotion_disabled;

my @tests = (
    [{ flags => undef, categories_bs => undef, real_banner_type => 'text', }, 0],
    [{ flags => '', categories_bs => '', real_banner_type => 'text', }, 0],
    [{ flags => '', categories_bs => '', real_banner_type => 'mobile_content', }, 1],
    [{ flags => undef, categories_bs => '42', real_banner_type => 'text', }, 0],
    [{ flags => 'flag1,flag2', categories_bs => undef, real_banner_type => 'text', }, 0],
    [{ flags => 'media_disclaimer', categories_bs => undef, real_banner_type => 'text', }, 1],
    [{ flags => undef, categories_bs => '200007526,42', real_banner_type => 'text', }, 0],
    [{ flags => 'media_disclaimer,flag3', categories_bs => '200007526,42', real_banner_type => 'text', }, 1],
);

for my $t (@tests) {
    my ($banner, $result) = @$t;
    is(ivd($banner) ? 1 : 0, $result, "result for flags=@{[$banner->{flags}//'undef']} cats=@{[$banner->{categories_bs}//'undef']} shoult be $result");
}

done_testing();
