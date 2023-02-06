#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use Currencies;
use geo_regions;

is(Currencies::REGION_ID_BY(), $geo_regions::BY, 'BY has same region id in Currencies and geo_regions');

done_testing();
