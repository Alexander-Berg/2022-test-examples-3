#!/usr/bin/perl

use strict;
use warnings;

use Test::More;

use Yandex::DBUnitTest;
use Yandex::Test::UTF8Builder;

use Settings;
use Tools;

use utf8;

$Yandex::DBTools::QUOTE_DB = Yandex::DBUnitTest::UT;

my @tests = (
    {
        params      => [[qw/id phone country city/], {id=>10}],
        fields_str  => "`id`, `phone`, `country`, `city`",
        values_str  => "'10' AS `id`, `phone`, `country`, `city`",
    }, {
        params      => [[qw/bid title body/], { bid => {10 => 20}, vcard_id => {10 => 201} }, by => "bid"],
        fields_str  => "`bid`, `vcard_id`, `title`, `body`",
        values_str  => "CASE `bid` WHEN '10' THEN '20' ELSE NULL END AS `bid`, CASE `bid` WHEN '10' THEN '201' ELSE NULL END AS `vcard_id`, `title`, `body`",
    }, {
        # DIRECT-32009
        params      => [[qw/bid title body/], { bid => {10 => 20}, vcard_id => {} }, by => "bid"],
        fields_str  => "`bid`, `vcard_id`, `title`, `body`",
        values_str  => "CASE `bid` WHEN '10' THEN '20' ELSE NULL END AS `bid`, NULL AS `vcard_id`, `title`, `body`",
    },
    {
        # DIRECT-76284
        params      => [[qw/bid title body/], { bid => {10 => 20}, vcard_id => {10 => 201} }, by => "bid", override_fields_to_copy_only => 1],
        fields_str  => "`bid`, `title`, `body`",
        values_str  => "CASE `bid` WHEN '10' THEN '20' ELSE NULL END AS `bid`, `title`, `body`",
    },
    {
        params      => [[qw/bid title body/], { bid => 10, title => 'lol' }, override_fields_to_copy_only => 1],
        fields_str  => "`bid`, `title`, `body`",
        values_str  => "'10' AS `bid`, 'lol' AS `title`, `body`",
    },
    {
        params      => [[qw/bid title body statusModerate/], { bid => 10, title => 'lol', statusModerate__sql => "IF(statusModerate IN ('Sent', 'Sending'), 'Ready', statusModerate)"}, override_fields_to_copy_only => 1],
        fields_str  => "`bid`, `statusModerate`, `title`, `body`",
        values_str  => "'10' AS `bid`, IF(statusModerate IN ('Sent', 'Sending'), 'Ready', statusModerate) AS `statusModerate`, 'lol' AS `title`, `body`",
    },
    {
        params      => [[qw/bid title body/], { bid => 10, title => 'lol', statusModerate__sql => "IF(statusModerate IN ('Sent', 'Sending'), 'Ready', statusModerate)"}, override_fields_to_copy_only => 1],
        fields_str  => "`bid`, `title`, `body`",
        values_str  => "'10' AS `bid`, 'lol' AS `title`, `body`",
    },
    {
        params      => [[qw/bid title body/], { bid => 10, title => 'lol', statusModerate__sql => "IF(statusModerate IN ('Sent', 'Sending'), 'Ready', statusModerate)"}],
        fields_str  => "`bid`, `statusModerate`, `title`, `body`",
        values_str  => "'10' AS `bid`, IF(statusModerate IN ('Sent', 'Sending'), 'Ready', statusModerate) AS `statusModerate`, 'lol' AS `title`, `body`",
    },
    {
        params      => [[qw/bid title body statusModerate/], { bid => 10, title => 'lol', statusModerate__sql => "IF(statusModerate IN ('Sent', 'Sending'), 'Ready', statusModerate)"}],
        fields_str  => "`bid`, `statusModerate`, `title`, `body`",
        values_str  => "'10' AS `bid`, IF(statusModerate IN ('Sent', 'Sending'), 'Ready', statusModerate) AS `statusModerate`, 'lol' AS `title`, `body`",
    },
);

Test::More::plan(tests => 2 * scalar(@tests));

my $i = 0;
for my $test(@tests) {
	my ($fields_str, $values_str) = Tools::make_copy_sql_strings(@{ $test->{params} });
	is($fields_str, $test->{fields_str}, "test $i-1");
	is($values_str, $test->{values_str}, "test $i-2");
    $i++;
}
