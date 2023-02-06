package support

import (
	"fmt"
	"testing"
)

//группа содержит хосты в Hosts, ничего удаляться не должно
func TestRemoveEmptyGroups(t *testing.T) {
	gms := NewGroupsManager()
	hosts := NewHosts()
	groupname := "group1"
	for _, hostname := range []string{"host1", "host2"} {
		host := NewHost(hostname, groupname)
		*hosts = append(*hosts, host)
	}
	group := NewGroupManager(groupname, "start", "finish", hosts, Config{})
	*gms = append(*gms, &group)
	fmt.Println(group.Servers, *hosts, *gms, "TEST", group.MyGroup)
	gms.RemoveEmptyGroups()
	fmt.Println(group.Servers, *hosts, *gms)
	if len(*gms) == 0 {
		t.Error("В результате удаления не должно быть удалено ни одной группы, сейчас это не так", *gms)
	}
}

//группа не содержит хостов в Hosts и должна быть удалена
func TestRemoveEmptyGroups2(t *testing.T) {
	gms := NewGroupsManager()
	hosts := NewHosts()
	group := NewGroupManager("group1", "start", "finish", hosts, Config{})
	*gms = append(*gms, &group)
	gms.RemoveEmptyGroups()
	if len(*gms) != 0 {
		t.Error("В результате удаления не должно остаться ни одной группы, сейчас это не так", *gms)
	}
}

//группа group1 пустая и должна быть удалена
func TestRemoveEmptyGroups3(t *testing.T) {
	gms := NewGroupsManager()
	hosts := NewHosts()
	for _, groupName := range []string{"group1", "group2"} {
		group := NewGroupManager(groupName, "start", "finish", hosts, Config{})
		*gms = append(*gms, &group)
	}
	for _, hostname := range []string{"host1", "host2"} {
		host := NewHost(hostname, "group2")
		*hosts = append(*hosts, host)
	}
	gms.RemoveEmptyGroups()
	if len(*gms) == 2 {
		t.Error("В результате удаления должна остаться одна группа, сейчас это не так", *gms)
	} else if (*gms)[0].MyGroup.GetGroupName() == "group1" {
		t.Error("В результате удаления должна остаться одна группа 'group2', сейчас это не так", *gms)
	}
}
