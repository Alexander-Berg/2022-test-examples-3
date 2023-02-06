#!/usr/bin/perl

use strict;
use my_inc '../..';

use JSON;
use Encode;

use Yandex::DBTools;
use Yandex::Shell;
use Yandex::DBUnitTest qw/copy_table UT/;

use lib::abs "../../protected";

use Settings;

my $file = lib::abs::path("./_data.json.gz");
my $data = from_json(Encode::decode_utf8(yash_qx(zcat => $file)));

copy_table(PPCDICT, "suggest_phrases");
do_mass_insert_sql(UT, "REPLACE INTO suggest_phrases (".join(",", @{$data->{phrases}->{fields}}).") VALUES %s", $data->{phrases}->{data});


copy_table(PPCDICT, "suggest_phrases_links");
do_mass_insert_sql(UT, "REPLACE INTO suggest_phrases_links (".join(",", @{$data->{links}->{fields}}).") VALUES %s", $data->{links}->{data});

1;
