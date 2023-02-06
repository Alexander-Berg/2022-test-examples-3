#!/usr/bin/perl
use Direct::Modern;
use Test::More;
use POSIX qw/strftime/;

use List::MoreUtils qw/any/;
use Direct::Test::DBObjects;
use EventLog;
use Client;
use Yandex::DBUnitTest qw/init_test_dataset/;

use API::Methods::Events;

use Settings;

BEGIN {
    use_ok 'EventLog';
    use_ok 'API::Methods::Events';
}

*GetEventsLog = \&API::Methods::Events::GetEventsLog;

Direct::Test::DBObjects->create_tables();

my $test_data = Direct::Test::DBObjects->new(shard => 1);
my $time_from = strftime("%Y%m%d", localtime(time() - 86400));
my $cid = int(rand() * 10000);

subtest "Get different events using API method, no filter" => sub {
    foreach my $event_type (keys %EventLog::EVENTS) {
        my $user = $test_data->create_user();
        Client::update_role($user->client_id, 'client', undef);
        _add_client_nds($user->client_id);

        my %event = _create_event($user, $event_type);
        EventLog::log_event(%event);

        my $self = _get_self($user);
        my $params = { TimestampFrom => $time_from };
        my $result = GetEventsLog($self, $params);

        if ($EventLog::EVENTS{$event_type}{hidden}) {
            ok(!@$result);
        } else {
            ok(@$result);
        }
    }
};

subtest "Get different events using API method, filter by type" => sub {
    foreach my $event_type (keys %EventLog::EVENTS) {
        my $user = $test_data->create_user();
        Client::update_role($user->client_id, 'client', undef);
        _add_client_nds($user->client_id);

        my %event = _create_event($user, $event_type);
        EventLog::log_event(%event);

        my $self = _get_self($user);
        my $params = {
            TimestampFrom => $time_from,
            EventType     => $APICommon::EVENT_TYPE2TOKEN{$event_type} // 'unknown'
        };
        my $result = GetEventsLog($self, $params);

        if ($EventLog::EVENTS{$event_type}{hidden}) {
            ok(!@$result);
        } else {
            ok(@$result);
        }
    }
};

sub _create_event {
    my ($user, $event_type) = @_;

    my %event = (
        slug     => $event_type,
        ClientID => $user->client_id,
        cid      => $cid++,
    );
    if (my $params = $EventLog::EVENTS{$event_type}{params}) {
        $event{params} = { map { $_ => 1 } @$params };
    }
    # Дальше идут моки для параметров, специфичных для отдельных событий
    if ($event_type eq 'banner_moderated') {
        $event{params}{results} = { global => 'accepted' };
    }
    if ($event{params}{currency}) {
        $event{params}{currency} = 'RUB';
    }
    if ($event{params}{bs_stop_time}) {
        $event{params}{bs_stop_time} = strftime("%Y-%m-%d %H:%M:%S", localtime(time() - 86400));
    }
    if (any { $EventLog::EVENTS{$event_type}{object} eq $_ } qw/banner phrase/) {
        $event{bid} = 1;
    }
    if (any { $EventLog::EVENTS{$event_type}{object} eq $_ } qw/phrase/) {
        $event{bids_id} = 1;
    }

    return %event;
}

sub _get_self {
    my ($user) = @_;

    return {
        rbac_login_rights => { role => 'client' },
        uid               => $user->id,
        user_info         => { ClientID => $user->client_id },
    };
}

sub _add_client_nds {
    my ($client_id) = @_;

    my $dataset = {
        client_nds => {
            original_db => PPC(shard => 1),
            rows => [
                {ClientID => $client_id, date_from => '20000101', date_to => '20380101', nds => 12.5},
            ],
        },
    };
    init_test_dataset($dataset);
}

done_testing();
