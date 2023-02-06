#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;
use Test::Deep;

BEGIN { use_ok( 'GeoTools' ); }

sub er {
    my ($geo, $exclude, $opt) = @_;
    $opt->{tree} //= 'ua';
    return GeoTools::exclude_region($geo, $exclude, $opt);
};

my $russia = 225;
my $siberia = 59;
my $moscow = 213;
my $mo = 1;
my $spb = 2;
my $center = 3;
my $north_west = 17;
my $spbo = 10174;
my $africa = 241;
my $ukraine = 187;
my $krim = 977;
my $kiev = 143;
my $belarus = 149;
my $cis = 166;

# вычитаем страны
is(er("$moscow,$kiev", [$ukraine]), "$moscow", "-ua");
is(er("$moscow,$kiev", [$russia,$ukraine]), "", "-ru,-ua");
is(er("$russia,$ukraine", [$russia]), "$ukraine", "-ua");
is(er("$cis", [$ukraine]), "$cis,-$ukraine", "-ua");

{
my ($geo, $ex) = er("$moscow,$kiev", [$ukraine]);
cmp_deeply($ex, [$ukraine]);
}
{
my ($geo, $ex) = er("$cis", [$ukraine,$belarus]);
cmp_deeply($ex, [$ukraine,$belarus]);
}
{
my ($geo, $ex) = er("$russia", [$ukraine,$belarus]);
cmp_deeply($ex, []);
}
{
my ($geo, $ex) = er("$russia,$ukraine", [$ukraine,$belarus]);
cmp_deeply($ex, [$ukraine]);
}

{
    my ($geo, $ex) = er("0", []);
    is($geo, '0');
    cmp_deeply($ex, []);
}

{
my ($geo, $ex) = er("$russia,$ukraine", [$russia, $krim], {tree => 'ua'});
cmp_deeply($ex, [$russia, $krim], "russia,ukraine - russia,krim; disabled_geo = russia, krim (for ua)");
}
{
my ($geo, $ex) = er("666,$russia,$ukraine,$krim", [$russia, $krim], {tree => 'ua'});
cmp_deeply($ex, [$russia, $krim], "russia,ukraine - russia,krim; disabled_geo = russia, krim (for ua)");
}
{
my ($geo, $ex) = er("$russia,$ukraine", [$russia, $krim], {tree => 'ru'});
cmp_deeply($ex, [$russia], "russia,ukraine - russia; disabled_geo = russia");
}

# вычитаем города
is(er($russia,[$moscow]), "$russia,-$moscow", "russia");
is(er($russia,[$moscow,$spb]), "$russia,-$moscow,-$spb", "russia");
is(er("$russia,-$center",[$moscow]), "$russia,-$center", "russia,-center");
is(er("$russia,-$center",[$moscow,$spb]), "$russia,-$center,-$spb", "russia,-center");
is(er(undef,[$moscow,$spb]),"0,-$moscow,-$spb","world");
is(er($siberia,[$moscow,$spb]),$siberia,"siberia");
is(er($center, [$moscow,$spb]), "$center,-$moscow", "center");
is(er($north_west, [$moscow,$spb]), "$north_west,-$spb", "NW");
is(er("$russia,-$center,$north_west", [$moscow,$spb]),"$russia,-$center,$north_west,-$spb", "center,nw-center");
is(er("1",[$moscow,$spb]),"1,-$moscow","mo");

# вычитаем области
is(er($russia,[$mo]), "$russia,-$mo", "russia");
is(er($russia,[$mo,$spbo]), "$russia,-$mo,-$spbo", "russia");
is(er("$russia,-$center",[$mo]), "$russia,-$center", "russia,-center");
is(er("$russia,-$center",[$mo,$spbo]), "$russia,-$center,-$spbo", "russia,-center");
is(er(undef,[$mo,$spbo]),"0,-$mo,-$spbo","world");
is(er($siberia,[$mo,$spbo]),$siberia,"siberia");
is(er($center, [$mo,$spbo]), "$center,-$mo", "center");
is(er($north_west, [$mo,$spbo]), "$north_west,-$spbo", "NW");
is(er("$russia,-$center,$north_west", [$mo,$spbo]),"$russia,-$center,$north_west,-$spbo", "center,nw-center");
is(er("1",[$mo,$spbo], {return_nonempty_geo => 1}),"0,-$mo,-$spbo","mo");

# вычитаем город и область
is(er($russia,[$moscow,$spbo]), "$russia,-$moscow,-$spbo", "russia");
is(er("$russia,-$center",[$moscow,$spbo]), "$russia,-$center,-$spbo", "russia,-center");
is(er(undef,[$moscow,$spbo]),"0,-$moscow,-$spbo","world");
is(er($siberia,[$moscow,$spbo]),$siberia,"siberia");
is(er($center, [$moscow,$spbo]), "$center,-$moscow", "center");
is(er($north_west, [$moscow,$spbo]), "$north_west,-$spbo", "NW");
is(er("1",[$moscow,$spbo]),"1,-$moscow","moscow");

is(er("$russia,-$center,-$north_west",[$mo,$spbo]), "$russia,-$center,-$north_west","ru,-center,-nw");
is(er("$africa,$russia,-$center,-$north_west",[$mo,$spbo]), "$africa,$russia,-$center,-$north_west","africa,ru,-center,-nw");
is(er("$africa",[$mo,$spbo]), "$africa","africa");

is(er("$russia,$moscow", [$mo,$spbo]), "$russia,-$mo,-$spbo", "russia,moscow");
is(er("$russia,$moscow,$center", [$mo,$spbo]), "$russia,-$spbo,$center,-$mo", "russia,moscow,center");

# транслокальность
is(er("$russia", [$krim], {tree => 'ru'}), "$russia,-$krim", "russia,-krim (for russia)");
is(er("$russia", [$krim], {tree => 'ua'}), "$russia", "russia (for russia)");

is(er("$ukraine", [$krim], {tree => 'ru'}), "$ukraine", "russia (for ukraine)");
is(er("$ukraine", [$krim], {tree => 'ua'}), "$ukraine,-$krim", "russia,-krim (for ukraine)");

done_testing();
