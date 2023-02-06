import itertools

import pytest

import common.types.task as tt


class TestTypesTask:
    # TODO: Remove after complete transition to new statuses [SANDBOX-2654]
    ONLY_NEW_STATUSES = (
        tt.Status.DELETING,
        tt.Status.FINISHING,
        tt.Status.NOT_RELEASED,
        tt.Status.RELEASING,
        tt.Status.RELEASED,
        tt.Status.NO_RES,
        tt.Status.PREPARING,
        tt.Status.SUSPENDING,
        tt.Status.SUSPENDED,
        tt.Status.TEMPORARY,
        tt.Status.TIMEOUT,
        tt.Status.WAIT_TIME
    )

    # TODO: Remove after complete transition to new statuses [SANDBOX-2654]
    def test__unambiguous_translation(self):
        assert set(
            tt.Status.new_status(tt.Status.old_status(st))
            for st in tt.Status
        ) == set(st for st in tt.Status if st not in self.ONLY_NEW_STATUSES)

    # TODO: Remove after complete transition to new statuses [SANDBOX-2654]
    def test__ambiguous_translation(self):
        old_to_new = {st: tt.Status.new_statuses(st) for st in tt.TaskStatus}
        for old_status, new_statuses in old_to_new.iteritems():
            assert all(tt.Status.old_status(st) == old_status for st in new_statuses)

    def test__expand(self):
        assert tt.Status.Group.expand((
            tt.Status.Group.BREAK,
            tt.Status.Group.EXECUTE,
            tt.Status.Group.FINISH
        )) == set(itertools.chain(
            tt.Status.Group.BREAK,
            tt.Status.Group.EXECUTE,
            tt.Status.Group.FINISH
        ))
        assert tt.Status.Group.expand((
            tt.Status.Group.QUEUE,
            tt.Status.Group.EXECUTE,
            tt.Status.Group.DRAFT
        )) == set(itertools.chain(
            tt.Status.Group.QUEUE,
            tt.Status.Group.EXECUTE,
            tt.Status.Group.DRAFT
        ))
        assert tt.Status.Group.expand((
            tt.Status.Group.DRAFT,
            tt.Status.Group.QUEUE,
            tt.Status.DRAFT,
            tt.Status.EXCEPTION
        )) == {tt.Status.DRAFT, tt.Status.ENQUEUING, tt.Status.ENQUEUED, tt.Status.EXCEPTION}
        st = {tt.Status.DRAFT, tt.Status.ENQUEUING, tt.Status.ENQUEUED, tt.Status.EXCEPTION}
        assert tt.Status.Group.expand(st) == st

    def test__collapse(self):
        assert tt.Status.Group.collapse(itertools.chain(gr for gr in tt.Status.Group)) == {gr for gr in tt.Status.Group}
        assert tt.Status.Group.collapse((
            tt.Status.DRAFT, tt.Status.ENQUEUING, tt.Status.ENQUEUED, tt.Status.EXCEPTION,
            tt.Status.SUCCESS, tt.Status.FAILURE,
            tt.Status.WAIT_RES, tt.Status.WAIT_TASK, tt.Status.WAIT_TIME, tt.Status.WAIT_OUT,
        )) == {
            str(tt.Status.Group.DRAFT), str(tt.Status.Group.QUEUE), tt.Status.EXCEPTION,
            tt.Status.SUCCESS, tt.Status.FAILURE, str(tt.Status.Group.WAIT)
        }

    def test__collapse_primary(self):
        primary_groups = filter(lambda _: _.primary, iter(tt.Status.Group))
        secondary_groups = filter(lambda _: not _.primary, iter(tt.Status.Group))
        for group in primary_groups:
            assert tt.Status.Group.collapse(set(iter(group))) == {str(group)}
        for group in secondary_groups:
            statuses = set(iter(group))
            assert tt.Status.Group.expand(statuses) == statuses

    def test__set_priority_state(self):
        priority = tt.Priority()

        priority.__setstate__(12)
        priority.__setstate__(("USER", "LOW"))
        priority.__setstate__((tt.Priority.Class.SERVICE, tt.Priority.Subclass.HIGH))

        with pytest.raises(ValueError):
            priority.__setstate__("highest priority")
        with pytest.raises(ValueError):
            priority.__setstate__(6)
        with pytest.raises(KeyError):
            priority.__setstate__(("USERZ", "LOW"))
