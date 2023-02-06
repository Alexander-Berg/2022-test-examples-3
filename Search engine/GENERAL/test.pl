#!/usr/bin/y-local-env

print "OK\n";

my $VERSION = `perl -V`;
print $VERSION;

print join(": ", $k, $v)."\n" while ($k,$v) = each %ENV;

1

