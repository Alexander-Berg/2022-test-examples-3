package ru.yandex.direct.ansiblejuggler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AnsibleWrapperConfigurationTest {
    @Test
    public void getInventoryFileType_onEmptyConfiguration_ExpectNone() {
        AnsibleWrapperConfiguration conf = new AnsibleWrapperConfiguration.Builder()
                .build();
        assertEquals(conf.getInventoryFileType(), AnsibleWrapperConfiguration.InventoryFileType.NONE);
    }

    @Test(expected = IllegalStateException.class)
    public void getAnsibleInventoryPath_onEmptyConfiguration_ThrowsException() {
        AnsibleWrapperConfiguration conf = new AnsibleWrapperConfiguration.Builder()
                .build();
        conf.getAnsibleInventoryPath();
    }

    @Test(expected = IllegalStateException.class)
    public void getAnsibleInventoryContent_onEmptyConfiguration_ThrowsException() {
        AnsibleWrapperConfiguration conf = new AnsibleWrapperConfiguration.Builder()
                .build();
        conf.getAnsibleInventoryContent();
    }

    @Test
    public void getInventoryFileType_onConfigurationWithInventoryFile_ExpectRegular() {
        AnsibleWrapperConfiguration conf = new AnsibleWrapperConfiguration.Builder()
                .withInventoryFile("/path/to/some/file")
                .build();
        assertEquals(conf.getInventoryFileType(), AnsibleWrapperConfiguration.InventoryFileType.REGULAR);
    }

    @Test
    public void getAnsibleInventoryPath_onConfigurationWithInventoryFile_ExpectSame() {
        AnsibleWrapperConfiguration conf = new AnsibleWrapperConfiguration.Builder()
                .withInventoryFile("/path/to/some/file")
                .build();
        assertEquals(conf.getAnsibleInventoryPath(), "/path/to/some/file");
    }

    @Test(expected = IllegalStateException.class)
    public void getAnsibleInventoryContent_onConfigurationWithInventoryFile_ThrowsException() {
        AnsibleWrapperConfiguration conf = new AnsibleWrapperConfiguration.Builder()
                .withInventoryFile("/path/to/some/file")
                .build();
        conf.getAnsibleInventoryContent();
    }

    @Test
    public void getInventoryFileType_onConfigurationWithInventoryContent_ExpectTemporary() {
        AnsibleWrapperConfiguration conf = new AnsibleWrapperConfiguration.Builder()
                .withInventoryContent("content of inventory file\n")
                .build();
        assertEquals(conf.getInventoryFileType(), AnsibleWrapperConfiguration.InventoryFileType.TEMPORARY);
    }

    @Test(expected = IllegalStateException.class)
    public void getAnsibleInventoryPath_onConfigurationWithInventoryContent_ThrowsException() {
        AnsibleWrapperConfiguration conf = new AnsibleWrapperConfiguration.Builder()
                .withInventoryContent("content of inventory file\n")
                .build();
        conf.getAnsibleInventoryPath();
    }

    @Test
    public void getAnsibleInventoryContent_onConfigurationWithInventoryContent_ExpectSame() {
        AnsibleWrapperConfiguration conf = new AnsibleWrapperConfiguration.Builder()
                .withInventoryContent("content of inventory file\n")
                .build();
        assertEquals(conf.getAnsibleInventoryContent(), "content of inventory file\n");
    }

    @Test
    public void getInventoryFileType_onConfigurationWithInventoryContentAndFile_ExpectTemporary() {
        AnsibleWrapperConfiguration conf = new AnsibleWrapperConfiguration.Builder()
                .withInventoryContent("content of inventory file\n")
                .withInventoryFile("/path/to/some/file")
                .build();
        assertEquals(conf.getInventoryFileType(), AnsibleWrapperConfiguration.InventoryFileType.TEMPORARY);
    }
}
