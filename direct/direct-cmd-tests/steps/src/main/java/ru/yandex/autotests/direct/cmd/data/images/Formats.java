package ru.yandex.autotests.direct.cmd.data.images;

public class Formats {

        private String path;

        private Integer width;

        private Integer height;

        public String getPath() {
            return path;
        }

        public Formats withPath(String path) {
            this.path = path;
            return this;
        }

        public Integer getWidth() {
            return width;
        }

        public Formats withWidth(Integer width) {
            this.width = width;
            return this;
        }

        public Integer getHeight() {
            return height;
        }

        public Formats withHeight(Integer height) {
            this.height = height;
            return this;
        }

}
