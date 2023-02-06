use Direct::Modern;

use open ':std' => ':utf8';
use List::MoreUtils qw/any/;
use Test::Deep qw/cmp_deeply set/;
use Test::MockObject;
use Test::More;

use Yandex::HashUtils qw/hash_cut/;

use RedisLock;

my $redis = Test::MockObject->new();
my %fail_locks;
$redis->mock('evalsha', sub {
    my $self = shift;
    my $sha = shift;
    my $key_num = shift;

    my @result = (1) x $key_num;
    for (my $i = 0; $i < $key_num; ++$i) {
        my $key = $_[$i];
        if ($fail_locks{$key}) {
            $result[$i] = undef;
        }
    }

    return \@result;
});

$RedisLock::MULTILOCK_BATCH_SIZE = 3;

test([
    {
        name => 'empty',
        keys => [],
        calls => [],
        locked => [],
        failed => [],
    },
    {
        name => 'one group',
        keys => [qw/hello{group0} world{group0} test{group0}/],
        calls => [
            [qw/hello{group0} world{group0} test{group0}/],
        ],
        locked => 'all keys',
        failed => [],
    },
    {
        name => 'two groups',
        keys => [qw/hello{group0} world{group0} peace{group1} bubblegum{group1}/],
        calls => [
            [qw/hello{group0} world{group0}/],
            [qw/peace{group1} bubblegum{group1}/],
        ],
        locked => 'all keys',
        failed => [],
    },
    {
        name => 'two groups with overflow',
        keys => [qw/hello{group0} world{group0} mary{group0} hada{group0} little{group0} peace{group1} bubblegum{group1} lamb{group0}/],
        calls => [
            [qw/hello{group0} world{group0} mary{group0}/],
            [qw/hada{group0} little{group0} lamb{group0}/],
            [qw/peace{group1} bubblegum{group1}/],
        ],
        locked => 'all keys',
        failed => [],
    },
    {
        name => 'two groups with overflow and a fail',
        prelock => [qw/lamb{group0}/],
        keys => [qw/hello{group0} world{group0} mary{group0} hada{group0} little{group0} peace{group1} bubblegum{group1} lamb{group0}/],
        calls => [
            [qw/hello{group0} world{group0} mary{group0}/],
            [qw/hada{group0} little{group0} lamb{group0}/],
            [qw/peace{group1} bubblegum{group1}/],
        ],
        locked => [qw/hello{group0} world{group0} mary{group0} hada{group0} little{group0} peace{group1} bubblegum{group1}/],
        failed => [qw/lamb{group0}/],
    },
]);

done_testing();

sub test {
    my ($cases) = @_;

    my $TTL = 1_234;
    for my $case (@$cases) {
        %fail_locks = map { $_ => 1 } @{$case->{prelock}};
        my ($locked, $lock_failed) = RedisLock::lock_multi($redis, $case->{keys}, $TTL);
        my $name = $case->{name};

        my $key_values = verify_lock_calls(
            $case->{calls},
            $TTL * 1000,
            $name,
        );

        my $expected_locked = $case->{locked};
        if (ref($expected_locked) eq '' && $expected_locked eq 'all keys') {
            $expected_locked = $case->{keys},
        }
        verify_locked($locked, $expected_locked, $key_values, $name);
        verify_failed($lock_failed, $case->{failed}, $name);
    }
}

sub verify_locked {
    my ($locked, $expected_locked, $key_values, $name) = @_;


    cmp_deeply([keys %$locked], set(@$expected_locked), "$name: locked keys");
    my $expected_locked_key_values = hash_cut $key_values, [keys %$locked];
    cmp_deeply($locked, $expected_locked_key_values, "$name: locked key values");
}

sub verify_failed {
    my ($failed, $expected_failed, $name) = @_;

    cmp_deeply($failed, set(@$expected_failed), "$name: failed locks");
}


sub verify_lock_calls {
    my ($keys, $expected_ttl, $test_name) = @_;
    $test_name //= '';

    my %expected_keys = map { join(',', @$_) => 1 } @$keys;
    my %passed_values;

    my $calls_checked;
    for ($calls_checked = 0; $calls_checked < (keys %expected_keys); ++$calls_checked) {
        my ($method, $args) = $redis->next_call();
        if (!defined($method)) {
            fail("$test_name: expected a call to evalsha");
            last;
        }
        is($method, 'evalsha', "$test_name: redis call method");

        my ($called_redis, $sha, $keys_num) = splice(@$args, 0, 3);
        is(int($called_redis), int($redis), "$test_name: redis call self object");
        like($sha, qr/[0-9a-f]{40}/i, "$test_name: redis call SHA");

        unless ($keys_num && $keys_num =~ /^\d+$/) {
            $keys_num //= 'undef';
            fail("$test_name: redis call keys number is not an unsigned integer: $keys_num");
            last;
        }
        unless (@$args == ($keys_num * 2 + 1)) {
            fail("$test_name: unexpected argument count. Expected $keys_num keys, $keys_num values and TTL, got ".scalar(@$args));
            last;
        }
        pass("$test_name: correct argument number");

        my @keys = splice(@$args, 0, $keys_num);
        my $keys_str = join(',', @keys);
        if (!$expected_keys{$keys_str}) {
            fail("$test_name: did not expect a call with keys $keys_str");
            last;
        }
        pass("$test_name: call with expected keys");
        $expected_keys{$keys_str} = 0;

        my @values = splice(@$args, 0, $keys_num);
        if (any { !defined } @values) {
            fail("$test_name: redis call with undefined values");
            last;
        }
        pass("$test_name: call with all defined values");
        for my $i (keys @keys) {
            $passed_values{$keys[$i]} = $values[$i];
        }

        my $ttl = $args->[0];
        is($ttl, $expected_ttl, "$test_name: redis call TTL");
    }

    if ($calls_checked == (keys %expected_keys)) {
        my ($method, $args) = $redis->next_call();
        if (defined($method)) {
            fail("$test_name: unexpected call to $method");
        } else {
            pass("$test_name: no unexpected calls");
        }
    } else {
        $redis->clear();
    }

    return \%passed_values;
}
