#!/usr/bin/perl


use strict;
use utf8;
use open ':std' => ':utf8';

use Test::More;
use GeoTools;

use Settings;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

copy_table(PPCDICT, 'geo_regions');

my $data = [
    # region_id name ua_name ename tr_name
    [ 143, 'Киев','Київ','Kyiv','Kiev' ],
    [ 213, 'Москва','Москва','Moscow','Moskova' ],
    [ 11508, 'Стамбул','Стамбул','Istanbul','İstanbul' ],
];
do_mass_insert_sql(PPCDICT, "insert into geo_regions (region_id, name, ua_name, ename, tr_name) values %s", $data);

*g = \&get_geoid_by_cityname;

is(g('Киев'), 143);
is(g('Київ'), 143);
is(g('Москва'), 213);
is(g('İstanbul'), 11508);
is(g('Стамбул'), 11508);
is(g('Лондон'), 0);

is(g('киев'), 143);
is(g('КИЕВ'), 143);

done_testing();
