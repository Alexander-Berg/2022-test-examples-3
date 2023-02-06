#!/usr/bin/env perl
use my_inc "../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;

use Settings;
use Yandex::DBShards;
use Test::CreateDBObjects;

sub is_sequence_working: Test {
    ok(get_new_id('hierarchical_multiplier_id') > 0);
}

create_tables;

__PACKAGE__->runtests();
