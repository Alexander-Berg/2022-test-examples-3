#!/usr/bin/env perl

=head1 NAME

watch.pl - вызывает указанные unit-тесты в ответ на изменения файлов

=head1 SYNOPSIS

    perl unit_tests/watch.pl [... command to run tests ...]

например

    perl unit_tests/watch.pl prove -v unit_tests/HierarchicalOptions/validate.t

=head1 DESCRIPTION

В отличие от App::Prove::Watch, не запускается по многу раз в ответ на одно изменение.

=cut
use my_inc "..";
use Direct::Modern;

use FindBin;
use AnyEvent;
use AnyEvent::Handle;
use Linux::Inotify2;
use POSIX;
use File::Find;
use Term::ANSIColor;
use 5.14.0;

use my_inc "..", for => 'api/t';

{
    our %MASKS;
    for my $mask (@Linux::Inotify2::EXPORT) {
        next if $mask !~ /^IN_/ or $mask eq 'IN_ALL_EVENTS';
        $MASKS{Linux::Inotify2->$mask} = $mask;
    }
}

my $run_timer;

sub main {
    our %W; # место для хранения AnyEvent watcher'ов, чтобы они не уничтожились раньше времени.
    my $inotify = Linux::Inotify2->new or die "inotify: $!";
    find(
        {
            wanted => sub {
                if ($_ eq '.svn') {
                    $File::Find::prune = 1;
                    return;
                }
                if (-d $_) {
                    $W{$File::Find::name} = create_watcher($inotify, $File::Find::fullname);
                }
            },
            follow => 1,
        },
        @my_inc::MY_INC, my_inc::path('')
    );

    my $inotify_w = AnyEvent->io(
        fh => $inotify->fileno, poll => 'r', cb => sub { $inotify->poll },
    );

    require Yandex::DBUnitTest;
    use Test::CreateDBObjects;
    Yandex::DBUnitTest->import(qw/:create_db/);
    create_tables;

    AnyEvent->condvar->recv;
}

sub create_watcher {
    my ($inotify, $dir) = @_;
    say "Watching $dir";
    return $inotify->watch(
        $dir, IN_CLOSE_WRITE | IN_MOVED_TO | IN_DELETE, sub {
            my $e = shift;
            my $name = $e->fullname;
            my @mask_names;
            while (my($mask, $name) = each %::MASKS) {
                if ($e->mask & $mask) {
                    push @mask_names, $name;
                }
            }
            if ($name =~ /\.(pm|pl|t)$/) {
                say "[@{[dt()]}] $name: @{[join ',', @mask_names]}";
                $run_timer = AnyEvent->timer(after => 0.1, cb => \&run_tests);
            }
        }
    );
}

sub run_tests {
    $run_timer = undef;
    print color('yellow'), '=' x 72, color('reset'), "\n";
    system(@ARGV);
}

sub dt {
    strftime('%Y-%m-%d %H:%M:%S', localtime);
}

main();
