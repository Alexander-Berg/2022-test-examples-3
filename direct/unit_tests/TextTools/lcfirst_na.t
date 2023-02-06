#!/usr/bin/perl

use warnings;
use strict;
use Test::More tests => 4;

use TextTools;


is(lcfirst_na("This is an extremely simple"), lcfirst("This is an extremely simple"), "This is an extremely simple");
is(lcfirst_na("UNIX"), "UNIX", "UNIX");
is(lcfirst_na("F1qwe"), "F1qwe", "F1qwe");
is(lcfirst_na("aQWE"), "aQWE", "aQWE");

