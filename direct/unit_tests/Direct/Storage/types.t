#!/usr/bin/perl

=head1 DESCRIPTION

Тест проверяет соответствие между типами файлов, опсанными в Direct::Storage::Types
и полем type в таблицах mds_metadata (ppc и ppcdict)

=cut

use Direct::Modern;
use Test::More;
use Direct::Storage::Types;
use Settings;
use Path::Tiny;
use List::MoreUtils qw(uniq);

my %IGNORE_TYPES = map {$_ => 1} qw(
    sale_reports
);

=head2 get_types_from_schema

Распарсить (регуляркой) схему таблицы, вернуть содержимое enum

Параметры:
    $path - путь к файлу со схемой

Возвращает:
    хеш вида
        ( type => 1, type2 => 1 )

=cut

sub get_types_from_schema
{
    my $path = shift;
    my $text = path($path)->slurp();

    $text =~ s!\`!!g;
    my ($types) = ($text =~ /type \s+ enum \s* \((.+?)\) \s+ NOT \s+ NULL/gx);
    return
        map { $_ => 1 }
        map { s!\'!!gr }
        split /[, ]+/, $types;
}

my %types_ppc     = get_types_from_schema(path($Settings::ROOT, 'db_schema/ppc/mds_metadata.schema.sql'));
my %types_ppcdict = get_types_from_schema(path($Settings::ROOT, 'db_schema/ppcdict/mds_metadata.schema.sql'));

for my $type (keys %Direct::Storage::Types::MDS_FILE_TYPES) {
    if (delete $IGNORE_TYPES{$type}) {
        delete $types_ppcdict{$type};
        delete $types_ppc{$type};
    } elsif (mds_check_type_trait($type, 'empty_client_id')) {
        ok($types_ppcdict{$type} && !$types_ppc{$type}, "$type in schema for ppcdict.mds_metadata");
        delete $types_ppcdict{$type};
    }
    else {
        ok(!$types_ppcdict{$type} && $types_ppc{$type}, "$type in schema for ppc.mds_metadata");
        delete $types_ppc{$type};
    }
}

for my $missing (uniq ((keys %types_ppc), (keys %types_ppcdict))) {
    fail("missing type description for $missing (found in schema, but not in Direct::Storage::Types)");
}

my $unused_ignores = join(',', keys %IGNORE_TYPES);
is($unused_ignores, '', 'no unused IGNORE_TYPES');

done_testing();
