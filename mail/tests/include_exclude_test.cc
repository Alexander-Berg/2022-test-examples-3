#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/webmail/corgi/include/resolve/include_exclude.h>
#include <mail/webmail/corgi/include/types.h>


using namespace ::testing;

namespace corgi::tests {

const UidsSet uidsSet{Uid(1)};
const DepartmentsSet departmentsSet{DepartmentId(1)};
const GroupsSet groupsSet{GroupId(1)};
const DirectoryUser uidInSet{.uid=Uid(1), .departmentId=std::nullopt};
const DirectoryUser uidNotInSet{.uid=Uid(2), .departmentId=std::nullopt};

const DirectoryUser departmentInSet{.uid=Uid(100), .departmentId=std::make_optional(DepartmentId(1))};
const DirectoryUser groupInSet{.uid=Uid(100), .departmentId=std::nullopt, .groups=GroupsSet{GroupId(1)}};
const DirectoryUser nothing{.uid=Uid(100), .departmentId=std::nullopt};

const DepartmentsTree universalTree {
    { DepartmentId(1), Departments() },
    { DepartmentId(2), Departments() },
    { DepartmentId(3), Departments() },
    { DepartmentId(4), Departments() },
};

TEST(IsAnyOfSetsContainUserTest, shouldReturnTrueForUserWithUidInUndsSet) {
    EXPECT_TRUE(detail::isAnyOfSetsContainUser(uidsSet, departmentsSet, groupsSet, uidInSet));
}

TEST(IsAnyOfSetsContainUserTest, shouldReturnTrueForUserWithDepartmentIdInDepartmentsSet) {
    EXPECT_TRUE(detail::isAnyOfSetsContainUser(uidsSet, departmentsSet, groupsSet, departmentInSet));
}

TEST(IsAnyOfSetsContainUserTest, shouldReturnTrueForUserWithGroupIdInGroupsSet) {
    EXPECT_TRUE(detail::isAnyOfSetsContainUser(uidsSet, departmentsSet, groupsSet, groupInSet));
}

TEST(IsAnyOfSetsContainUserTest, shouldReturnFalseForUserUserWithGroupIdInGroupsSet) {
    EXPECT_FALSE(detail::isAnyOfSetsContainUser(uidsSet, departmentsSet, groupsSet, nothing));
}

TEST(IncludeAndExcludeTest, shouldReturnAllUidsIfParamsAreEmpty) {
    const ResolveUsers emptyParams;
    const IncludeAndExcludeByUidAndDepartment obj(emptyParams, universalTree);
    EXPECT_TRUE(emptyParams.empty());
    EXPECT_THAT(
        obj.filter({uidInSet, uidNotInSet}),
        UnorderedElementsAre(uidInSet.uid, uidNotInSet.uid)
    );
}

TEST(IncludeAndExcludeTest, shouldReturnFilteredUids) {
    const ResolveUsers params {
        .includeDepartments=std::make_optional(Departments{ DepartmentId(1), DepartmentId(2) }),
        .excludeDepartments=std::make_optional(Departments{ DepartmentId(3) }),
        .includeUids=std::make_optional(Uids{ Uid(1), Uid(2) }),
        .excludeUids=std::make_optional(Uids{ Uid(3) }),
        .includeGroups=std::make_optional(Groups{ GroupId(1) }),
        .excludeGroups=std::make_optional(Groups{ GroupId(2) }),
    };
    const IncludeAndExcludeByUidAndDepartment obj(params, universalTree);
    EXPECT_FALSE(params.empty());

    const DirectoryUser includedByDepId1{ .uid=Uid(10), .departmentId=std::make_optional(DepartmentId(1)) };
    const DirectoryUser includedByDepId2{ .uid=Uid(11), .departmentId=std::make_optional(DepartmentId(2)) };
    const DirectoryUser includedByUid{ .uid=Uid(2), .departmentId=std::make_optional(DepartmentId(4)) };
    const DirectoryUser includedByGroup{ .uid=Uid(20), .groups={GroupId(1)} };

    const DirectoryUser excludedByDepId{ .uid=Uid(12), .departmentId=std::make_optional(DepartmentId(3)) };
    const DirectoryUser includedByUidButExcludedByDepId{ .uid=Uid(1), .departmentId=std::make_optional(DepartmentId(3)) };
    const DirectoryUser excludedByUid{ .uid=Uid(3), .departmentId=std::make_optional(DepartmentId(1)) };
    const DirectoryUser excludedByGroup{ .uid=Uid(21), .groups={GroupId(2)} };

    const DirectoryUsers users = {
        includedByDepId1, includedByDepId2, excludedByDepId, includedByGroup,
        includedByUidButExcludedByDepId, includedByUid, excludedByUid, excludedByGroup
    };

    EXPECT_THAT(obj.filter(users), UnorderedElementsAre(includedByDepId1.uid, includedByDepId2.uid, includedByUid.uid, includedByGroup.uid));
}

TEST(IncludeAndExcludeTest, shouldTakeEmptyIncludeOptionsAndIncludeAllUsers) {
    const ResolveUsers params {
        .excludeDepartments=std::make_optional(Departments{ DepartmentId(2) }),
        .excludeUids=std::make_optional(Uids{ Uid(2) }),
        .excludeGroups=std::make_optional(Groups{ GroupId(2) }),
    };
    const IncludeAndExcludeByUidAndDepartment obj(params, universalTree);
    EXPECT_FALSE(params.empty());

    const DirectoryUser included1{ .uid=Uid(1) };
    const DirectoryUser excludedByUid{ .uid=Uid(2) };
    const DirectoryUser included2{ .uid=Uid(3), .departmentId=std::make_optional(DepartmentId(1)) };
    const DirectoryUser excludedByDepId{ .uid=Uid(4), .departmentId=std::make_optional(DepartmentId(2)) };
    const DirectoryUser included3{ .uid=Uid(5), .groups={GroupId(1)} };
    const DirectoryUser excludedByGroup{ .uid=Uid(6), .groups={GroupId(2)} };

    const DirectoryUsers users = {
        included1, excludedByUid, included2,
        excludedByDepId, included3, excludedByGroup
    };

    EXPECT_THAT(obj.filter(users), UnorderedElementsAre(included1.uid, included2.uid, included3.uid));
}

TEST(IncludeAndExcludeTest, shouldThrowAnExceptionInCaseOfDepartmentsIntersection) {
    const ResolveUsers params {
        .includeDepartments=std::make_optional(Departments{ DepartmentId(1), DepartmentId(2) }),
        .excludeDepartments=std::make_optional(Departments{ DepartmentId(1), DepartmentId(3) }),
    };

    EXPECT_THROW(const IncludeAndExcludeByUidAndDepartment p(params, universalTree), mail_errors::system_error);
}

TEST(IncludeAndExcludeTest, shouldThrowAnExceptionInCaseOfUidsIntersection) {
    const ResolveUsers params {
        .includeUids=std::make_optional(Uids{ Uid(1), Uid(2) }),
        .excludeUids=std::make_optional(Uids{ Uid(1), Uid(3) }),
    };

    EXPECT_THROW(const IncludeAndExcludeByUidAndDepartment p(params, universalTree), mail_errors::system_error);
}

TEST(IncludeAndExcludeTest, shouldThrowAnExceptionInCaseOfGroupsIntersection) {
    const ResolveUsers params {
        .includeGroups=Groups{ GroupId(1), GroupId(2) },
        .excludeGroups=Groups{ GroupId(1), GroupId(3) },
    };

    EXPECT_THROW(const IncludeAndExcludeByUidAndDepartment p(params, universalTree), mail_errors::system_error);
}

TEST(IncludeAndExcludeTest, shouldResolveSubDepartments) {
    const DepartmentsTree tree {
        { DepartmentId(1), Departments{DepartmentId(2)} },
        { DepartmentId(3), Departments{DepartmentId(4)} },
    };

    const ResolveUsers params {
        .includeDepartments=std::make_optional(Departments{ DepartmentId(1) }),
        .excludeDepartments=std::make_optional(Departments{ DepartmentId(3) })
    };

    const DirectoryUser included{ .uid=Uid(1), .departmentId=std::make_optional(DepartmentId(2)) };
    const DirectoryUser excluded{ .uid=Uid(2), .departmentId=std::make_optional(DepartmentId(4)) };

    const IncludeAndExcludeByUidAndDepartment obj(params, tree);

    EXPECT_THAT(obj.filter({included, excluded}), UnorderedElementsAre(included.uid));
}

TEST(ResolveSubDepartmentsTest, shouldResolveSubDepartments) {
    const DepartmentsTree tree {
        { DepartmentId(1), Departments{ DepartmentId(2), DepartmentId(3), DepartmentId(4) } },
        { DepartmentId(4), Departments{ DepartmentId(5), DepartmentId(6) } },
    };

    DepartmentsSet set { DepartmentId(1), DepartmentId(2), DepartmentId(4) };
    const DepartmentsSet expected {
        DepartmentId(1), DepartmentId(2), DepartmentId(3),
        DepartmentId(4), DepartmentId(5), DepartmentId(6)
    };

    EXPECT_EQ(detail::resolveSubDepartments(set, tree), expected);
}

}
