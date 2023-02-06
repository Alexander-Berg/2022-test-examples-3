#!/usr/bin/perl

BEGIN {
    # нам нужны реальные инстансы -memcached- (выпилено) и redis, поэтому используем разработческую конфигурацию
    $ENV{SETTINGS_LOCAL_SUFFIX} //= 'DevTest';
}

use Direct::Modern;

use Test::MockTime 'set_fixed_time';
use Test::More;
use Parallel::ForkManager;

use my_inc '../../..', for => 'protected';

use Settings;
use HashingTools qw/get_random_string/;

use Units::Storage::RedisCluster;

my $FAKE_REDIS = FakeRedisCluster->new;
my %STORAGES = (
    redis_cluster => sub {
        Units::Storage::RedisCluster->new(
            redis => $FAKE_REDIS,
            ),
    }
);

my $start_time = CORE::time;

my %TIME_MANIPULATION_HANDLERS = (
    redis_cluster => {
        start => sub { set_fixed_time($start_time); },
        sleep => sub { set_fixed_time( $start_time + $_[0] ); },
    },
);

for my $name ( sort keys %STORAGES ) {
    my $storage_provider = $STORAGES{$name};
    my $storage = $storage_provider->();
    my $key = "ustests_".get_random_string();
    subtest "first_incr for $name" => sub {
        ok $storage->incr_expire($key, 5, 60);
        is $storage->get($key), 5;
    };

    subtest "second_incr for $name" => sub {
        ok $storage->incr_expire($key, 3, 60);
        is $storage->get($key), 8;
    };

    subtest "parallel_incr for $name" => sub {
        for my $iter (1..20) {
            my $pkey = $key."_iter_$iter";
            my $PARALLEL = 10;
            my $pm = Parallel::ForkManager->new($PARALLEL);
            for(1..$PARALLEL) {
                my $pid = $pm->start and next;
                $storage_provider->()->incr_expire($pkey, 2, 60);
                $pm->finish;
            }
            $pm->wait_all_children;
            is $storage->get($pkey), $PARALLEL * 2, "iter $iter, parallel $PARALLEL";
        }
    };

    subtest "expire for $name" => sub {
        my $pkey = $key."_ex";
        $TIME_MANIPULATION_HANDLERS{$name}->{start}->();
        ok $storage->incr_expire($pkey, 3, 1);
        is $storage->get($pkey), 3;
        ok $storage->incr_expire($pkey, 3, 1);
        is $storage->get($pkey), 6;
        $TIME_MANIPULATION_HANDLERS{$name}->{sleep}->(2);
        is $storage->get($pkey), undef;        
    };
}

done_testing;

package FakeRedisCluster;
use parent 'DirectRedis';

use Carp 'cluck';
use Fcntl ':flock';
use File::Slurp;
use File::Temp;
use File::Path qw/make_path/;
use Guard;
use JSON;

our $TEMPORARY_FILE_FILENAME_TEMPLATE;
BEGIN { 
        
        my $prefix = "";

        if (defined $ENV{WRITABLE_PATH}) {
            $prefix = $ENV{WRITABLE_PATH};
        }

        make_path($prefix.'/tmp/temp-ttl/ttl_1d/');
        $TEMPORARY_FILE_FILENAME_TEMPLATE = $prefix.'/tmp/temp-ttl/ttl_1d/' . ( 'X' x 10 ); 
}

sub new {
    my ($class) = @_;
    my $state_file = File::Temp->new( TEMPLATE => $TEMPORARY_FILE_FILENAME_TEMPLATE );
    write_file( $state_file->filename, { atomic => 1 }, '{}' );
    return bless { state_file => $state_file, lock_file => File::Temp->new( TEMPLATE => $TEMPORARY_FILE_FILENAME_TEMPLATE ) }, $class;
}

sub _get_state {
    my ($self) = @_;
    return from_json( read_file( $self->{state_file}->filename ) );
}

sub _save_state {
    my ( $self, $state ) = @_;
    write_file( $self->{state_file}->filename, { atomic => 1 }, to_json($state) );
}

sub _lock_state {
    my ($self) = @_;
    open my $fh, $self->{lock_file}->filename;
    flock $fh, LOCK_EX;
    return guard {
        flock $fh, LOCK_UN;
        close $fh;
    };
}

sub _get_from_state {
    my ( $self, $state, $key ) = @_;
    my $entry = $state->{$key};
    if ( ! defined $entry->{exptime} || $entry->{exptime} > time ) {
        return $entry->{value};
    }
    return undef;
}

sub get {
    my ( $self, $key ) = @_;
    my $lock_guard = $self->_lock_state;
    return $self->_get_from_state( $self->_get_state, $key );
}

sub mget {
    my ( $self, @keys ) = @_;
    my $lock_guard = $self->_lock_state;
    my $state = $self->_get_state;
    return map { $self->_get_from_state( $state, $_ ) } @keys;
}

sub setex {
    my ( $self, $key, $expiration, $val ) = @_;
    my $lock_guard = $self->_lock_state;
    my $state = $self->_get_state;
    $state->{$key} = { exptime => time + $expiration, value => $val };
    $self->_save_state($state);
}

sub incrby {
    my ( $self, $key, $val ) = @_;
    my $lock_guard = $self->_lock_state;
    my $state = $self->_get_state;
    my $entry = $state->{$key};
    unless ( $entry && ( ! defined $entry->{exptime} || $entry->{exptime} > time ) ) {
        $state->{$key} = { value => $val };
        $self->_save_state($state);
        return $val;
    }
    $entry->{value} ||= 0;
    $entry->{value} += $val;
    $self->_save_state($state);
    return $entry->{value};
}

sub expire {
    my ( $self, $key, $expiration ) = @_;
    my $lock_guard = $self->_lock_state;
    my $state = $self->_get_state;
    my $entry = $state->{$key};
    return unless $entry;
    $entry->{exptime} = time + $expiration;
    $self->_save_state($state);
}

sub delete_multi {
    my ( $self, @keys ) = @_;
    my $lock_guard = $self->_lock_state;
    my $state = $self->_get_state;
    delete $self->{state}->{$_} for @keys;
    $self->_save_state($state);
}

our $AUTOLOAD;
sub AUTOLOAD {
    # потому что Units::Storage глотает исключения
    cluck "Unsupported method: $AUTOLOAD";
    die "Unsupported method: $AUTOLOAD";
}

1;

