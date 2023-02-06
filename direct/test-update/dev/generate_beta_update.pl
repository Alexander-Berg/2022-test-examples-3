#!/usr/bin/perl

use strict;
use warnings;

use YAML;

my $groups = YAML::LoadFile($ARGV[0]);

my @lines = ('#!/bin/bash -ex', '');

my @hostnames = map {/^([^\.]+)/} @{$groups->{ppcdev}};
push @hostnames, 'ppcdev-precise';  # пока ppcdev1 опознаётся как ppcdev-precise

push( @lines,
    'h=`hostname`',
    'case "$h" in',
    (join '|', @hostnames) . ')',
    'if [[ "$1" = "--ppcdev-all" ]]',
    'then',
    'direct-test-update ppcdev-all ${*:2}',
    'exit',
    'fi',
    ';;',
    'esac',
);

push @lines, 'ssh updater@$h $@';

print map {"$_\n"} @lines;
