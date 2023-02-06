#! /usr/bin/perl

use strict;
use warnings;

use YAML;

my $conf = YAML::LoadFile($ARGV[0]);

my @lines = (
    '#!/bin/bash -e',
    '',
    'case "$1" in'
);

my @aliases;
for my $host ( sort keys %$conf ){
    push( @lines, 
        "$host)", 
        "ssh updater\@$conf->{$host}->{hostname} \${*:2}",
        ";;"
    );
    push @aliases, $host;
}

my $groups = YAML::LoadFile($ARGV[1]);
push( @lines,
    "ppcdev-all)",
    (join " && ", map { "ssh updater\@$_ \${*:2}"} @{$groups->{ppcdev}}),
    ";;",
);
push @aliases, 'ppcdev-all';

push( @lines,
    "*)",
    'echo "unknown alias $1"',
    'echo "available aliases: ' . join('|', @aliases) .'"',
    ";;",
    "esac",
);

print join "", map {"$_\n"} @lines;
