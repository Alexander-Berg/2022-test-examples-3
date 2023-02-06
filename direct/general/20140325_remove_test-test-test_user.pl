#!/usr/bin/perl

use my_inc "..";

=head1 DEPLOY

# approved by lena-san
# .migr
{
  type => 'script',
  when => 'after',
  time_estimate => "1 sec",
  comment => "По какой-то причине RBAC не возвращает ClientID для пользователя test-test-test, поэтому, у него невозможно оторвать роль представителя агентства."
}

=cut

use strict;
use warnings;

use FindBin qw/$Bin/;
use lib "$Bin/../protected/";

use Settings;
use Yandex::DBTools;
use Yandex::DBShards;
use ScriptHelper;
use RBAC2::Extended;
use RBACDirect;

use utf8;

$log->out('START');

    my $rbac = get_singleton RBAC2::Extended();
    $rbac->InitReq(1) or $log->die("rbac error");
	my $res = rbac_drop_client_rep($rbac, 2063911, 11998990);
	$log->out("RBAC returned for deleting: ".Dumper($res));
	my $delete_res = do_delete_from_table(PPC(uid => 11998990), 'users', where=>{uid => SHARD_IDS});
	$log->out("Deleting from users: ".$delete_res);


$log->out('FINISH');

