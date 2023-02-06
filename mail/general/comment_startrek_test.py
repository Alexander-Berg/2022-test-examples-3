import os
import unittest

from startrek_client import Startrek

from tools.release.comment_startrek import parse_ticket, create_comment, find_expected_tickets_in_release, \
    get_teamcity_changes, read_config, get_android_version, parse_ios_version, last_major_version, get_used_sdk


class TestReleaseCommentStartrek(unittest.TestCase):
    def test_parse_ticket_from_commit_message(self):
        self.assertListEqual(['MOBILEMAIL-12350'],
                             parse_ticket('MOBILEMAIL-12350: [regress] No attach button on share compose'))
        self.assertListEqual(['MOBILEMAIL-12308'],
                             parse_ticket('[MOBILEMAIL-12308] Use status bar height instead of maxY'))

    def test_parse_ticket_from_merge_commit_message(self):
        msg = 'Merge pull request #3141 in MA/mobile-mail-client-ios from feature/12353-fix-logs-sending to release/3.71.0\n\n' \
              + "* commit 'edbb1502194ce6744bf03e9bba79dca3c0c044fa':" \
              + 'MOBILEMAIL-12353: Fix log sending layout'
        self.assertListEqual(['MOBILEMAIL-12353'], parse_ticket(msg))

    def test_create_comment(self):
        comment = create_comment('iOS 4.19.2', None, 19952324, ['MOBILEMAIL-8397'])
        print(comment)

    def test_find_expected_tickets_in_release(self):
        client = Startrek(token='***', useragent='python')
        print(find_expected_tickets_in_release(client, 'iOS 3.71.0'))

    def test_get_teamcity_changes(self):
        os.environ['TEAMCITY_USER'] = 'robot-scraper'
        os.environ['TEAMCITY_PASSWORD'] = '***'
        print(get_teamcity_changes(19952324))

    def test_empty_changes(self):
        os.environ['TEAMCITY_USER'] = 'robot-scraper'
        os.environ['TEAMCITY_PASSWORD'] = '***'
        self.assertTrue(len(get_teamcity_changes(20151173)) == 0)

    def test_read_properties(self):
        config = read_config()
        self.assertTrue(config['teamcity.build.id'], 20116313)

    def test_android_version(self):
        self.assertEqual(get_android_version('build.gradle'), 'Android 4.11.0')

    def test_used_sdk(self):
        sdk = get_used_sdk('.')
        self.assertTrue("espresso:'2.2.1'" in sdk)
        self.assertTrue("compileSdk:26" in sdk)
        self.assertTrue("autoValue:'1.4.1'" in sdk)
        self.assertTrue("junitQuickcheck:'0.6'" in sdk)

    def test_ios_version(self):
        self.assertEqual(parse_ios_version('release/3.74'), 'iOS 3.74.0')

    def test_last_major_version(self):
        self.assertEqual(last_major_version('Android 4.11.1'), 'Android 4.11.0')


if __name__ == '__main__':
    unittest.main()
