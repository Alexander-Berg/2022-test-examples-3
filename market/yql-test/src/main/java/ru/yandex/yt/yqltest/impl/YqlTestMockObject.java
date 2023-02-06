package ru.yandex.yt.yqltest.impl;

import java.util.UUID;

import ru.yandex.inside.yt.kosher.cypress.YPath;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 06.09.2021
 */
public class YqlTestMockObject {
    private final String uuid = UUID.randomUUID().toString();

    private YPath path;
    private YPath mockPath;

    public YqlTestMockObject() {
    }

    public YqlTestMockObject(YqlTestMockObject src) {
        this.path = src.path;
        this.mockPath = src.mockPath;
    }

    public YPath getPath() {
        return path;
    }

    public void setPath(YPath path) {
        this.path = path;
    }

    public YPath getMockPath() {
        return mockPath;
    }

    public void setMockPath(YPath mockPath) {
        this.mockPath = mockPath;
    }

    public void setMockPath(String mockPath) {
        this.mockPath = YPath.simple(mockPath);
    }

    public void init(YPath basePath) {
        this.mockPath = basePath.child(uuid);
    }
}
