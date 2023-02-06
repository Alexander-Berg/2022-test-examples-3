package API::Test::Reports::FakePageIdMap;
use Direct::Modern;

=head1 NAME

API::Test::Reports::FakePageIdMap

=head1 DESCRIPTION

Компонент, который перехватывает (с записью запросов-ответов) или подменяет (с записью
запросов и нужными ответами) вызовы к Stat::Tools::get_page_id_by_name.

=cut

use Guard;
use List::MoreUtils 'uniq';
use Yandex::ListUtils 'xflatten';

our $NO_REAL_CALLS;

# name => [ PageID, PageID, ... ]
our %PAGE_NAME_TO_PAGE_IDS;

=head2 get_override_guard

Перекрыть процедуру и получить объект Guard, который при разрушении вернёт всё обратно.

=cut

sub get_override_guard {
    my ($class) = @_;

    no warnings 'redefine';

    my $orig_get_page_id_by_name = \&Stat::Tools::get_page_id_by_name;

    *Stat::Tools::get_page_id_by_name = sub {
        my ($page_names) = @_;

        if ($NO_REAL_CALLS) {
            my @result;
            for my $name (xflatten($page_names)) {
                die "No IDs for page_name=$name" unless exists $PAGE_NAME_TO_PAGE_IDS{$name};
                push @result, @{ $PAGE_NAME_TO_PAGE_IDS{$name} };
            }

            return [ uniq(@result) ];
        }

        my @result;
        for my $name (@$page_names) {
            my $orig_result = $orig_get_page_id_by_name->($name);
            $PAGE_NAME_TO_PAGE_IDS{$name} = $orig_result;
            push @result, @$orig_result;
        }

        return [ uniq @result ];
    };

    return guard {
        *Stat::Tools::get_page_id_by_name = $orig_get_page_id_by_name;
        %PAGE_NAME_TO_PAGE_IDS = ();
    };
}

1;
