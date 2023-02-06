#!/usr/bin/perl
use strict;
use warnings;
use utf8;

=pod

=head1 NAME

    errors_non_intersect.t

=head1 SYNOPSIS

    ./unit_tests/runtests.pl unit_tests/Direct/errors_non_intersect.t

=head1 DESCRIPTION

    Тест проверяет непересекаемость кодов и идентификаторов ошибок между
    файлами с ошибками на базе Direct::Errors, файлы ищутся по коду автоматически,
    модули пересечения в которых намерянные (например для тестирования) нужно
    добавлять в @IGNORE

    Модули из Direct::TestErrors::* игнорируются автоматом.

    Для непересекаемости ошибок, они между файлами деляться по диапазонам, один
    диапазон - один файл. Подробнее см. Direct::Errors

=cut

use my_inc '../../';


use File::Spec;
use File::Slurp;
use Test::More;

use Test::ListFiles;

# SomeDebug::Errors
my @IGNORE = (
    'Direct::Errors::Messages' # аггрегатор
);


my $to_ignore = {
    map { $_ => 1 } @IGNORE
};

foreach my $dir (@my_inc::MY_INC) {
    foreach my $file ( Test::ListFiles->list_repository($dir) ) {
        next unless -f $file && $file =~ /\.pm$/;

        my $module = package_by_file($dir, $file);
        next if $to_ignore->{$module};
        next if $module =~ /^Direct::TestErrors::/;

        my $text = read_file($file, binmode => ':utf8');
        if($text =~ /use\s+Direct::Errors/) {
            use_ok($module);
        }
    }
}

done_testing;


sub package_by_file {
    my $dir = shift;
    my $file_path = shift;

    my $file_name = File::Spec->abs2rel($file_path, $dir);

    my @parts = File::Spec->splitdir( $file_name );
    $parts[-1] =~ s/\.pm$// if @parts;

    for ( @parts ) {
        if ( /^([a-zA-Z0-9_\.\-]+)$/ && ($_ eq $1) ) {
            $_ = $1;  # Untaint the original
        } else {
            die qq{Invalid and untaintable filename "$file_path"!};
        }
    }

    return join( "::", @parts );
}

