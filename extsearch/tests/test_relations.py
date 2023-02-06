import xml.etree.ElementTree as ETree

import extsearch.ymusic.indexer.tools.cacher.lib.relations as relations


def test__extract_track_relations():
    track = ETree.fromstring("""
    <track>
        <artist id="123"/>
        <artist id="234"/>
        <region-data>
            <album id="1"/>
        </region-data>
        <region-data>
            <album id="5"/>
        </region-data>
    </track>
    """)

    dependencies = list(relations.extract_track_dependencies(track))
    assert dependencies == [
        ('artist', 123, True),
        ('artist', 234, True),
        ('album', 1, True),
        ('album', 5, True),
    ]


def test__extract_album_relations():
    album = ETree.fromstring("""
    <album>
        <artist id="123"/>
        <artist id="345"/>
        <region-data>
            <first-tracks>
                <track id="1"/>
                <track id="3"/>
            </first-tracks>
        </region-data>
        <region-data>
            <first-tracks>
                <track id="1"/>
                <track id="2"/>
            </first-tracks>
        </region-data>
    </album>
    """)

    dependencies = list(relations.extract_album_dependencies(album))
    assert dependencies == [
        ('artist', 123, True),
        ('artist', 345, True),
        ('track', 1, False),
        ('track', 3, False),
        ('track', 1, False),
        ('track', 2, False),
    ]


def test_extract_artist_relations():
    artist = ETree.fromstring("""
    <artist>
        <region-data>
            <popular-tracks>
                <track id="1"/>
                <track id="2"/>
                <track id="3"/>
            </popular-tracks>
            <similar-artists>
                <artist id="1"/>
            </similar-artists>
        </region-data>
        <region-data>
            <popular-tracks>
                <track id="1"/>
                <track id="2"/>
                <track id="3"/>
                <track id="4"/>
            </popular-tracks>
            <similar-artists>
                <artist id="1"/>
                <artist id="2"/>
            </similar-artists>
        </region-data>
    </artist>
    """)
    dependencies = list(relations.extract_artist_dependencies(artist))
    assert dependencies == [
        ('track', 1, False),
        ('track_albums', 1, False),
        ('track', 2, False),
        ('track_albums', 2, False),
        ('track', 3, False),
        ('track_albums', 3, False),
        ('track', 1, False),
        ('track_albums', 1, False),
        ('track', 2, False),
        ('track_albums', 2, False),
        ('track', 3, False),
        ('track_albums', 3, False),
        ('track', 4, False),
        ('track_albums', 4, False),
        ('artist', 1, False),
        ('artist', 1, False),
        ('artist', 2, False),
    ]
