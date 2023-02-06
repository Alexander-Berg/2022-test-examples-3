#!/usr/bin/perl

use warnings;
use strict;

use Test::More;

use Settings;
use HttpTools;

use utf8;
use open ':std' => ':utf8';

sub ff {
    return HttpTools::_validate_date(@_);
}

# test sub for validate date

# good dates
ok(ff("2008-11-01"), "good date 1");
ok(ff("1980-01-01"), "good date 2");
ok(ff("2038-01-01"), "good date 3");

# bad dates
ok(!ff("2138-01-01"), "bad date 1");
ok(!ff("1138-01-01"), "bad date 2");
ok(!ff("2009-00-01"), "bad date 3");
ok(!ff("2008-01-00"), "bad date 4");
ok(!ff("0000-00-00"), "bad date 5");
ok(!ff("-01-01"), "bad date 6");

done_testing;
