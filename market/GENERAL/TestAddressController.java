package ru.yandex.market.pers.notify.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.market.pers.notify.testing.TestAddressService;

import java.util.List;

@RestController
@RequestMapping("/test-address")
public class TestAddressController {

    @Autowired
    private TestAddressService testAddressService;

    @RequestMapping(value = "/email", method = RequestMethod.POST)
    public void addTestEmail(@RequestBody String email) {
        testAddressService.addTestEmail(email);
    }

    @RequestMapping(value = "/uuid", method = RequestMethod.POST)
    public void addTestUUID(@RequestBody String uuid) {
        testAddressService.addTestUUID(uuid);
    }

    @RequestMapping(value = "/uid", method = RequestMethod.POST)
    public void addTestUid(@RequestBody Long uid) {
        testAddressService.addTestUid(uid);
    }

    @RequestMapping(value = "/email", method = RequestMethod.DELETE)
    public void deleteTestEmail(@RequestParam("email") String email) {
        testAddressService.deleteTestEmail(email);
    }

    @RequestMapping(value = "/uuid", method = RequestMethod.DELETE)
    public void deleteTestUUID(@RequestParam("uuid") String uuid) {
        testAddressService.deleteTestUUID(uuid);
    }

    @RequestMapping(value = "/uid", method = RequestMethod.DELETE)
    public void deleteTestUid(@RequestParam("uid") Long uid) {
        testAddressService.deleteTestUid(uid);
    }

    @RequestMapping(value = "/email", method = RequestMethod.GET, produces = "application/json")
    public List<String> getAllTestEmails() {
        return testAddressService.getTestEmails();
    }

    @RequestMapping(value = "/uuid", method = RequestMethod.GET, produces = "application/json")
    public List<String> getAllTestUUIDs() {
        return testAddressService.getTestUUIDs();
    }

    @RequestMapping(value = "/uid", method = RequestMethod.GET, produces = "application/json")
    public List<Long> getAllTestUids() {
        return testAddressService.getTestUids();
    }

}
