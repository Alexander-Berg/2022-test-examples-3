package ru.yandex.market.crm.campaign.yt;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import ru.yandex.market.crm.core.domain.categories.CategoryImageLink;

/**
 * @author dimkarp93
 */
public class CategoriesImageLinkBuilder {
    private int hid;

    private List<ImageLinkBuilder> builders = Lists.newArrayList();

    public CategoriesImageLinkBuilder(int hid) {
        this.hid = hid;
    }

    public CategoriesImageLinkBuilder addImageLink(ImageLinkBuilder builder) {
        builders.add(builder);
        return this;
    }

    public Int2ObjectMap<List<CategoryImageLink>> build() {
        Int2ObjectMap<List<CategoryImageLink>> map = new Int2ObjectOpenHashMap<>();
        map.put(hid,
                builders.stream()
                        .map(b -> b.build(hid))
                        .collect(Collectors.toList())
        );
        return map;
    }

    public static class ImageLinkBuilder {
        private String imageUrl;
        private String imageAlt;

        public ImageLinkBuilder setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public ImageLinkBuilder setImageAlt(String imageAlt) {
            this.imageAlt = imageAlt;
            return this;
        }

        private CategoryImageLink build(int hid) {
            return new CategoryImageLink(hid, imageUrl, imageAlt, null);
        }
    }
}
