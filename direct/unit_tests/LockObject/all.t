#!/usr/bin/perl

use strict;
use warnings;

use Test::More;

use Yandex::DBTools;
use Yandex::DBUnitTest;

use Settings;

use_ok('LockObject');

Yandex::DBUnitTest::copy_table(PPCDICT, "lock_object");
my $now = time;
sub set_tick {
    do_sql(UT, "SET TIMESTAMP=".int($now + $_[0]));
}

set_tick(0);
my $l = LockObject->new({object_type => 'campaign', object_id => 1, duration => 60});
is($l->save(), $l, "successful save returns self");
is($l->save(), undef, "second save returns undef");
set_tick(30);
is($l->save(), undef, "third save returns undef");
set_tick(90);
is($l->save(), $l, "successful save after sleep");

ok(LockObject->new({object_type => 'campaign', object_id => 2})->save(), 'another campaign lock');

set_tick(0);
my $l3 = LockObject->new({object_type => 'campaign', object_id => 3, half_md5_hash => 12, duration => 60});
ok($l3->save());
ok(!LockObject->new({object_type => 'campaign', object_id => 3, half_md5_hash => 14})->load());
ok(!LockObject->new({object_type => 'campaign', object_id => 4, half_md5_hash => 12})->load());
ok(LockObject->new({object_type => 'campaign', object_id => 3, half_md5_hash => 12})->load());

ok(LockObject->new({object_type => 'campaign', object_id => 3, half_md5_hash => 12})->load()->delete());

ok(LockObject->new({object_type => 'campaign', object_id => 3, half_md5_hash => 12})->save());

done_testing;
