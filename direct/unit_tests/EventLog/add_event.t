#!/usr/bin/perl
use Direct::Modern;
use Test::More;

use List::MoreUtils qw/any/;
use EventLog;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/init_test_dataset/;
use Test::CreateDBObjects;

use Settings;

BEGIN {use_ok 'EventLog'}

create_tables();

my $ClientID = int(rand() * 10000);
my $cid = int(rand() * 10000);

subtest "Save different events to ppc.eventlog" => sub {
    foreach my $event_type (keys %EventLog::EVENTS) {
        $ClientID++;
        $cid++;

        _init_test_dataset();
        my %event = _create_event($event_type);

        EventLog::log_event(%event);
        my $result = get_all_sql(PPC(shard => 1), [
            "SELECT * FROM eventlog",
            WHERE => {
                cid => $cid,
                ClientID => $ClientID
            }]);
        ok (@$result);
    }
};


sub _init_test_dataset {
    my %db = (
        shard_client_id => {
            original_db => PPCDICT,
            rows    => [
                { ClientID => $ClientID, shard => 1 }
            ]
        });
    init_test_dataset(\%db);
}

sub _create_event {
    my ($event_type) = @_;

    my %event = (
        slug     => $event_type,
        ClientID => $ClientID,
        cid      => $cid,
    );
    if (my $params = $EventLog::EVENTS{$event_type}{params}) {
        $event{params} = { map {$_ => 1} @$params };
    }
    if (any { $EventLog::EVENTS{$event_type}{object} eq $_} qw/banner phrase/) {
        $event{bid} = 1;
    }
    if (any { $EventLog::EVENTS{$event_type}{object} eq $_} qw/phrase/) {
        $event{bids_id} = 1;
    }

    return %event;
}

done_testing();

