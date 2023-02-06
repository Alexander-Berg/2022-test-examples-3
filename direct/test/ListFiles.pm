package Test::ListFiles;
use strict;
use warnings;
use utf8;

use Cwd;
use Yandex::Svn qw/svn_files/;
use Yandex::Shell qw/yash_qx/;
use File::Basename qw/dirname/;
use File::Find;
use File::Spec;
use List::MoreUtils qw/uniq/;

=encoding utf8

=head1 NAME

Test::ListFiles

=head1 DESCRIPTION

Умеет искать файлы в аркадии, аналогично svn_files который там не работает

=head1 SYNOPSIS

Test::ListFiles->list_repository(["/var/www/direct.yandex.ru"])

=cut


=head2 list_repository

Возвращает список всех файлов относительно переданных путей.

=cut

sub list_repository {
    my ($class, $paths, %O) = @_;

    my @dirs = ref($paths) ? @$paths : ($paths);

    if ($ENV{ARCADIA_ROOT}) {
        my @list;

        for my $dir (@dirs) {
            next unless -e $dir;

            find({ 
                preprocess => sub {
                    ($O{depth} && $O{depth} eq 'files' ? grep { -f } @_ : @_ )
                },
                wanted => sub {push @list, $_ }, no_chdir => 1 }, $dir);
        }

        return uniq @list;
    }

    if (-d $dirs[0] && system('svn info ' . $dirs[0] . ' >/dev/null 2>&1')) {
        return _arc_files(\@dirs, %O);
    }
    
    return svn_files($paths, %O);
}

sub _arc_files {
    my ($paths, %O) = @_;

    my @result;

    for my $dir (@$paths) {
        next unless -e $dir;
        $dir = File::Spec->rel2abs($dir);

        my $output = yash_qx('arc', 'ls-files', $dir);

        my @files = 
            map { File::Spec->rel2abs($_) } 
            split "\n", $output;

        if ($O{depth} && $O{depth} eq 'files') {
            @files = grep { dirname($_) eq $dir && -f $_ } @files;
        }

        push @result, @files;
    }

    return @result;
}


1;
