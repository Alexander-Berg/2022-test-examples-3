import os
import unittest
from tracker_api_client.tracker_client import TrackerClient, Transition, Resolution, Status, Action

OAUTH_TOKEN_STARTREK = os.environ['OAUTH_TOKEN_STARTREK']


class TestTrackerClient(unittest.TestCase):
    Tracker = TrackerClient(auth=OAUTH_TOKEN_STARTREK)

    def test_get_fix_version(self):
        """
        Test getting fix version
        """
        no_fix_version = self.Tracker.get_issue_fix_version(issue_id='TEST-45034')
        one_fix_version = self.Tracker.get_issue_fix_version(issue_id='TEST-45033')
        two_fix_version = self.Tracker.get_issue_fix_version(issue_id='TEST-45035')
        self.assertIsNone(no_fix_version)
        self.assertEqual(one_fix_version, ['fix_version_test'])
        self.assertEqual(two_fix_version, ['fix_version_test', 'fix_version2'])

    def test_get_affected_version(self):
        """
        Test getting affected version
        """
        no_affected_version = self.Tracker.get_issue_affected_version(issue_id='TEST-45034')
        one_affected_version = self.Tracker.get_issue_affected_version(issue_id='TEST-45033')
        two_affected_version = self.Tracker.get_issue_affected_version(issue_id='TEST-45035')
        self.assertIsNone(no_affected_version)
        self.assertEqual(one_affected_version, ['fix_version2'])
        self.assertEqual(two_affected_version, ['fix_version_test', 'fix_version2'])

    def test_get_component(self):
        """
        Test getting component
        """
        no_component = self.Tracker.get_issue_component(issue_id='TEST-45034')
        one_component = self.Tracker.get_issue_component(issue_id='TEST-45033')
        two_component = self.Tracker.get_issue_component(issue_id='TEST-45035')
        self.assertIsNone(no_component)
        self.assertEqual(one_component, ['mobmail_test'])
        self.assertEqual(two_component, ['mobmail_test', 'mobmail_test2'])

    def test_get_author(self):
        """
        Test getting author
        """
        author = self.Tracker.get_issue_author(issue_id='TEST-45034')
        self.assertEqual(author, 'fanem')

    def test_get_assignee(self):
        """
        Test getting assignee
        """
        assignee = self.Tracker.get_issue_assignee(issue_id='TEST-45033')
        no_assignee = self.Tracker.get_issue_assignee(issue_id='TEST-45034')
        self.assertEqual(assignee, 'fanem')
        self.assertIsNone(no_assignee)

    def test_get_status(self):
        """
        Test getting status
        """
        open = self.Tracker.get_issue_status(issue_id='TEST-45033')
        closed = self.Tracker.get_issue_status(issue_id='TEST-45034')
        ready_for_release = self.Tracker.get_issue_status(issue_id='TEST-45035')
        self.assertEqual(open, Status.open)
        self.assertEqual(closed, Status.closed)
        self.assertEqual(ready_for_release, Status.ready_for_release)

    def test_get_followers(self):
        """
        Test getting followers
        """
        no_followers = self.Tracker.get_issue_followers(issue_id='TEST-45034')
        one_follower = self.Tracker.get_issue_followers(issue_id='TEST-45033')
        three_follower = self.Tracker.get_issue_followers(issue_id='TEST-45035')
        self.assertIsNone(no_followers)
        self.assertEqual(one_follower, ['fanem'])
        self.assertEqual(three_follower, ['zomb-mobmail-qa', 'robot-mobmailstat', 'fanem'])

    def test_get_sprint(self):
        """
        Test getting sprint
        """
        no_sprint = self.Tracker.get_issue_sprint(issue_id='TEST-45033')
        one_sprint = self.Tracker.get_issue_sprint(issue_id='TEST-45034')
        self.assertIsNone(no_sprint)
        self.assertEqual(one_sprint, ['Sprint 1'])

    def test_get_comments(self):
        """
        Test getting comments
        """
        no_comments = self.Tracker.get_comments(issue_id='TEST-45033')
        two_comments = self.Tracker.get_comments(issue_id='TEST-45035')
        self.assertEqual(no_comments, [])
        self.assertEqual(two_comments, [
            {'text': 'One comment', 'date': '2019-12-25T15:04:14.066+0000', 'id': 63237063, 'author': 'fanem'},
            {'text': 'Two comment', 'date': '2019-12-25T15:04:20.723+0000', 'id': 63237085, 'author': 'fanem'}
        ])

    def test_get_tags(self):
        """
        Test getting tags
        """
        no_tags = self.Tracker.get_issue_tags(issue_id='TEST-45034')
        one_tag = self.Tracker.get_issue_tags(issue_id='TEST-45033')
        two_tags = self.Tracker.get_issue_tags(issue_id='TEST-45035')
        self.assertIsNone(no_tags)
        self.assertEqual(one_tag, ['test.f'])
        self.assertEqual(two_tags, ['test.f', 'test.ff'])

    def test_bulk_add_and_remove_tags(self):
        """
        Test bulk adding and removing comments
        """
        no_tags = self.Tracker.get_issue_tags(issue_id='TEST-45034')
        self.assertIsNone(no_tags)
        self.Tracker.bulk_update_issues_tags(issue_ids=['TEST-45034'], action=Action.add, tags=['test.f', 'test.ff'])
        two_tags = self.Tracker.get_issue_tags(issue_id='TEST-45034')
        self.assertEqual(two_tags, ['test.f', 'test.ff'])
        self.Tracker.bulk_update_issues_tags(issue_ids=['TEST-45034'], action=Action.remove, tags=['test.f', 'test.ff'])
        no_tags = self.Tracker.get_issue_tags(issue_id='TEST-45034')
        self.assertIsNone(no_tags)

    def test_changing_issue_status(self):
        """
        Test changing issues status
        """
        open = self.Tracker.get_issue_status(issue_id='TEST-45033')
        self.assertEqual(open, Status.open)
        self.Tracker.change_issue_status(issue_id='TEST-45033', transition=Transition.close, resolution=Resolution.fixed)
        closed = self.Tracker.get_issue_status(issue_id='TEST-45033')
        self.assertEqual(closed, Status.closed)
        self.Tracker.change_issue_status(issue_id='TEST-45033', transition=Transition.reopen, resolution=None)
        reopened = self.Tracker.get_issue_status(issue_id='TEST-45033')
        self.assertEqual(reopened, Status.open)

    def test_get_priority_fields(self):
        """
        Test getting priority fields
        """
        priority_fields = self.Tracker.get_issue_priority_fields(issue_id='TEST-45034')
        self.assertEqual(priority_fields, {'tags': None, 'audience': 1.0, 'bugType': 'Линейный баг', 'crash': 'No',
                                           'eventFromMetrica': 'a', 'howReproduce': '100%'})

    def test_get_weight(self):
        """
        Test getting issue weight
        """
        no_weight = self.Tracker.get_issue_weight(issue_id='TEST-45033')
        negative_weight = self.Tracker.get_issue_weight(issue_id='TEST-45034')
        positive_weight = self.Tracker.get_issue_weight(issue_id='TEST-45035')
        self.assertIsNone(no_weight)
        self.assertEqual(negative_weight, -1)
        self.assertEqual(positive_weight, 50)


if __name__ == '__main__':
    unittest.main()
