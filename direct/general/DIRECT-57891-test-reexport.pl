#!/usr/bin/perl

=head2 NAME

    DIRECT-57891-test-reexport - постановка кампаний на тестовую переотправку в БК

=head2 DESCRIPTION

    Скрипт ставит в очередь на ленивую переотправку в БК некоторое количество кампаний.
    Скрипт выбирает со всех шардов самые свежие кампании не моложе некоторого
    cid, пока не наберет пачку (по умолчанию в пачке 100000 кампаний).

    Принимает опциональные параметры:

    --max-cid - cid самой новой кампании в пачке. Если не задан, берется текущий максимальный - 100000;
    --campaigns-num - количество кампаний в пачке на переотправку;

=cut

use Direct::Modern;

use my_inc '../..';

use Yandex::DBTools;

use ScriptHelper;
use Settings;
use Yandex::ListUtils qw/chunks nsort/;
use Yandex::Log::Messages;

use BS::ResyncQueue qw/bs_resync_whole_camps/;

my $CHUNK_SIZE = 500;
my $CIDS_STEP = 20000;
my $STEPBACK = 100000;
my $CAMPAIGNS_NUM;
my $MAX_CID;

$log = Yandex::Log::Messages->new();
extract_script_params(
    'max-cid=i' => \$MAX_CID,
    'campaigns-num=i' => \$CAMPAIGNS_NUM,
);
$CAMPAIGNS_NUM //= 100000;

$log->out('Start');
if (!defined $MAX_CID) {
    $MAX_CID = get_one_field_sql(PPCDICT, "SELECT MAX(cid) FROM shard_inc_cid") - $STEPBACK;
}
$log->out("Going to reexport $CAMPAIGNS_NUM campaigns with maximum cid $MAX_CID");

my $max_cid = $MAX_CID;
my $starting_cid = $max_cid - $CIDS_STEP;

my @campaigns;
my $camps_num = 0;
do {
    if ($starting_cid < 0) {
        $starting_cid = 0;
    }
    $log->out("Getting existing cids from $starting_cid to $max_cid");
    my $existing_cids = get_one_column_sql(PPC(shard => 'all'), [
        "SELECT cid FROM campaigns",
            WHERE => {
                cid__int__between => [$starting_cid, $max_cid],
            }
        ]
    );
    $existing_cids = [reverse nsort @$existing_cids];
    if (scalar(@campaigns) + scalar(@$existing_cids) <= $CAMPAIGNS_NUM) {
        push @campaigns, @$existing_cids;
    } else {
        my $filter_num = $CAMPAIGNS_NUM - scalar(@campaigns);
        push @campaigns, splice(@$existing_cids, 0, $filter_num);
    }
    $camps_num = scalar(@campaigns);
    $log->out("Currently have $camps_num campaigns to reexport");

    $max_cid = $starting_cid - 1;
    $starting_cid = $max_cid - $CIDS_STEP;
} while ($camps_num < $CAMPAIGNS_NUM && $max_cid > 0);

my $min_cid = $campaigns[-1];
$max_cid = $campaigns[0];
$log->out("Script will resync $camps_num campaigns starting from cid $min_cid and finishing with cid $max_cid");
my $cnt = 0;
for my $chunk (chunks(\@campaigns, $CHUNK_SIZE)) {
    $log->out({resync_chunk => $chunk});
    my $result = bs_resync_whole_camps($chunk, priority => BS::ResyncQueue::PRIORITY_INTAPI_RESYNC_CAMPAIGNS);
    $cnt += $result->{cid_count} // 0;
    $log->out({result => $result});
}
$log->out("Succesfully placed $cnt campaigns in reexport queue");
$log->out('Finish');
