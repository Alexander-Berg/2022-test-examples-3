my @tmg = (); 
my $exp = 0;
my $glo = 0;
my $unk = 0;
my $hit = 0;
my $upd = 0;
my $ins = 0;
my $stale = 0;
while (<>) {
    push(@tmg,$1) if /blackbox request.*time=(.*)$/ ; 
    if (/authorization failed/) {
        if (/expired_token/) {
            $exp++;
        } elsif (/globally logged out/) {
            $glo++;
        } else {
            $unk++;
        }
    }
    $hit++ if /cache action: hit/;
    $ins++ if /cache action: inserted/;
    $upd++ if /cache action: update/;
    $stale++ if /cache action: stale/;
}

print "bb_timings " . join(" ", @tmg) . "\n";
print "fail_expired $exp\n";
print "fail_logged_out $glo\n";
print "fail_unknown $unk\n";
print "cache_hit $hit\n";
print "cache_upd $upd\n";
print "cache_ins $ins\n";
print "cache_stale $stale\n";
