package ru.yandex.market.markup2.utils.cards;

import ru.yandex.market.markup2.utils.report.ReportCard;

/**
 * @author inenakhov
 */
public class InStorageCard {
    private final long id;
    private final String title;
    private final String description;
    private final String imageUrl;
    private final CardType type;
    private final long categoryId;
    private final boolean mboPublished;

    public InStorageCard(long id, String title, String imageUrl, String description,
                         long categoryId, CardType type, boolean mboPublished) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.type = type;
        this.categoryId = categoryId;
        this.mboPublished = mboPublished;
    }

    public InStorageCard(Card card, boolean mboPublished) {
        this.id = card.getId();
        this.title = card.getTitle();
        this.description = card.getDescription();
        this.imageUrl = card.getImageUrl();
        this.type = card.getType();
        this.categoryId = card.getCategoryId();
        this.mboPublished = mboPublished;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public CardType getType() {
        return type;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public boolean isMboPublished() {
        return mboPublished;
    }

    public Card toCard() {
        return new Card(id, title, imageUrl, description, categoryId, type);
    }

    public ReportCard toReportCard(String categoryName) {
        return new ReportCard(id, title, imageUrl, description, categoryName, categoryId, type);
    }
}
