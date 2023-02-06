#!/usr/bin/perl

use strict;
use warnings;

use Test::More;

use Settings;
use Yandex::Shell qw/yash_qx/;


Test::More::plan(tests => 2);


my $base_path = "$Settings::ROOT/unit_tests/translations";
my $translations_path = "$Settings::ROOT/locale";

my $validation_script = "validate_translation_tags.py";
my $success_output = "Ok\n";


my $res;

$res = yash_qx($validation_script, "--format", "json", "--exceptions", "$base_path/validate_emails_exceptions.json", $translations_path);
is($res, $success_output, 'validate email exceptions');

$res = yash_qx($validation_script, "--format", "po", "--exceptions", "$base_path/validate_pos_exceptions.json", $translations_path);
is($res, $success_output, 'validate pos exceptions');
