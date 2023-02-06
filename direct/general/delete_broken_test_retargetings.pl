#!/usr/bin/env perl

=pod

$Id$
$Author$

=cut

use Direct::Modern;

use Yandex::DBTools;
use Yandex::DBShards;

use my_inc '../..';

use JSON;

use ScriptHelper;
use Settings;

$log->out('START');

extract_script_params(
    'clientid=i' => \my @client_ids,
) or $log->die('bad params');
$log->die('need at least one clientid param') unless (@client_ids);

my $retargeting_conditions = get_all_sql(PPC(ClientID => \@client_ids),[ q!select * from retargeting_conditions!, where => {condition_json__like => q!%time\":null%!, clientid => SHARD_IDS}]);
$log->out("Fetched records to delete:", to_json($retargeting_conditions, {canonical => 1}));

my %clientid2ret_cond_ids;
foreach my $ret_cond (@$retargeting_conditions) {
    push @{$clientid2ret_cond_ids{$ret_cond->{ClientID}}}, $ret_cond->{ret_cond_id};
}
foreach my $clientid (sort keys %clientid2ret_cond_ids) {
    my @ret_cond_ids = @{$clientid2ret_cond_ids{$clientid}};
    $log->out("Deleting records for client $clientid, ret_cond_ids:", (join ', ', @ret_cond_ids));
    do_sql(PPC(ClientID => $clientid), ["delete from retargeting_conditions", where => {ret_cond_id => \@ret_cond_ids}]);
}

$log->out('FINISH');
