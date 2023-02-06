#!/usr/bin/perl

=head1 NAME

    carp-test.pl - посылает USR2 апачам и смотрит в логе, чем они занимаются

=head1 SYNOPSIS

    ./bin/carp-test.pl \
        --pid=apache/run/ppc.yandex.ru.pid

=cut

use my_inc '..';

use lib::abs '../protected';

use Data::Dumper;
use JSON;
use Scalar::Util 'looks_like_number';
use List::Util 'min';
use Getopt::Long::Descriptive;
use Pod::Simple::Text;
use File::Slurp 'read_file';

use Tools;

use warnings;
use strict;

my $pod;
my $p=Pod::Simple::Text->new; 
$p->output_string(\$pod);
$p->parse_file($0);

my ($opt, $usage) = describe_options(
    "%c %o <some-arg>",
    [ 'pid|p=s', 'pid apache parent process or path to pid file',
      { default=>lib::abs::path(".").'/../apache/run/ppc.yandex.ru.pid' } ],
    [ 'log-path|l=s', 'path to log file', 
      { default=>lib::abs::path(".").'/../apache/logs/error.log' } ],
    [],
    [ 'timeout|t=i', 'wait signal in log tailer', { default => 5, } ],
    [ 'first-frame|f', 'show first frame', { default => undef, } ],
    [ 'verbose|v', 'print information about all steps', { default => undef, } ],
    [],
    [ 'help|h' => 'Полный help' ]
);

$usage="USING$/$/$usage";

if($opt->help) {
    print STDERR 
        $pod, $/,
        $usage, $/, 
    ;
    exit;
}
elsif (!$opt->pid || !$opt->timeout || !$opt->log_path) {
    print STDERR $usage;
    exit;
};


# get apache parent pid
my $apache_parent_pid = looks_like_number( $opt->pid ) ? $opt->pid : read_file($opt->pid)
    or die "Need pid!";

# get apache childs pids
my @apaches = get_childpids_by_parentpid( $apache_parent_pid )
    or die "No child pids found!";

print 'Apache child pids: ', join(', ', @apaches), $/ if $opt->verbose;
kill 'USR2', @apaches;

my $log_path   = $opt->log_path;
print "Path to log file: $log_path", $/ if $opt->verbose;
my $pos_start  = -s $log_path; 
sleep($opt->timeout);

my $logsize = ( -s $log_path ) - $pos_start;
my $cursor_log_size = 0;

my $MAX_CHUNK_SIZE = 1024*1024*10;
my %result;
-e $log_path or die "No log file found!";
if ($logsize && open my $error_log, '<', $log_path ) {
    seek( $error_log, $pos_start, 1 );
    my $serializator = JSON->new();
    LINE: while(my $line =  <$error_log>) {
        $cursor_log_size+=length($line);
        last LINE if ($cursor_log_size>min( $logsize,$MAX_CHUNK_SIZE ));

        if (my ($data) = $line =~ m/(\[\{.*?\}\])/six) {
            my $_result = eval { $serializator->decode( $data ) };
            if (! $@) {
                my $flag;
                
                for my $frame ( @$_result ) {
                    if ( ( $frame->{subroutine} || '' ) =~ /^DoCmd::cmd_/ ) {
                        $flag=$frame->{subroutine};
                    };
                }
                if ( $opt->first_frame ) {
                    warn Dumper $_result->[0];
                }
                $result{ $flag } += 1 if ($flag);
            }
            else {
                print $data;
            }
        } 
    }
    close( $error_log );
}

my @res = 
    sort { $result{$a}||0 <=> $result{b}||0 }
    grep { $_ && $result{$_} }
    keys %result
;
print "$result{$_}\t$_", $/ for @res;

