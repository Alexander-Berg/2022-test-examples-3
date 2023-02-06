use Test::MockTime qw(:all);

sub get_last_log
{
    my $msg = $log->msgs->[-1];
    my $data = eval { from_json($msg->{message}); };
    if ($@) {
        warn "failed to parse $msg->{message} : $@\n";
        $data = [];
    }
    $log->clear();
    return $data;
}

sub test_sleep
{
    my $sec = shift;
    set_fixed_time(time + $sec);
}

sub mock_hires
{
    *Time::HiRes::time = *CORE::GLOBAL::time;
}

