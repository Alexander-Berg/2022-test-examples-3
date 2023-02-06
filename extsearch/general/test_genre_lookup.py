from extsearch.ymusic.scripts.reindex.genre_lookup import Genre, GenreLookup


def test__genre_lookup__find_titles_genres__only_words_matches():
    lookup = GenreLookup(genres=[
        Genre('k pop', 'kpop'),
        Genre('rusfolk popular', 'rusfolk'),
    ])
    assert ['rusfolk'] == lookup.find_titles_genres(['rusfolk popular music'])


def test__genre_lookup__find_titles_genres__genre_must_be_substring():
    lookup = GenreLookup(genres=[
        Genre('k pop', 'kpop'),
        Genre('kpop', 'kpop'),
    ])
    assert [] == lookup.find_titles_genres(['k'])
    assert ['kpop'] == lookup.find_titles_genres(['k pop'])
    assert ['kpop'] == lookup.find_titles_genres(['kpop'])


def test__genre_lookup__find_titles_genres__matches_in_the_middle():
    lookup = GenreLookup(genres=[
        Genre('russian folk', 'rusfolk'),
    ])
    assert ['rusfolk'] == lookup.find_titles_genres(['my very best russian folk music!'])
