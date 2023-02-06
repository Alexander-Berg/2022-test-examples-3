package ru.yandex.market.mbo.db.transfer;

import ru.yandex.market.mbo.gwt.models.User;
import ru.yandex.market.mbo.gwt.models.transfer.DestinationCategory;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.SourceCategory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author danfertev
 * @since 06.11.2018
 */
public class ModelTransferBuilder {
    private long id;
    private ModelTransfer.Type transferType;
    private Date created;
    private User author;
    private Date transferDate;
    private User manager;
    private ModelTransfer.Status status;
    private Date statusChanged;
    private Date modified;
    private User userModified;
    private String description;
    private List<SourceCategory> sourceCategories = new ArrayList<>();
    private List<DestinationCategory> destinationCategories = new ArrayList<>();

    private ModelTransferBuilder() {
    }

    public static ModelTransferBuilder newBuilder() {
        return new ModelTransferBuilder();
    }

    public ModelTransferBuilder id(long id) {
        this.id = id;
        return this;
    }

    public ModelTransferBuilder transferType(ModelTransfer.Type transferType) {
        this.transferType = transferType;
        return this;
    }

    public ModelTransferBuilder created(Date created) {
        this.created = created;
        return this;
    }

    public ModelTransferBuilder author(User author) {
        this.author = author;
        return this;
    }

    public ModelTransferBuilder transferDate(Date transferDate) {
        this.transferDate = transferDate;
        return this;
    }

    public ModelTransferBuilder manager(User manager) {
        this.manager = manager;
        return this;
    }

    public ModelTransferBuilder status(ModelTransfer.Status status) {
        this.status = status;
        return this;
    }

    public ModelTransferBuilder statusChanged(Date statusChanged) {
        this.statusChanged = statusChanged;
        return this;
    }

    public ModelTransferBuilder modified(Date modified) {
        this.modified = modified;
        return this;
    }

    public ModelTransferBuilder userModified(User userModified) {
        this.userModified = userModified;
        return this;
    }

    public ModelTransferBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ModelTransferBuilder sourceCategory(SourceCategory sourceCategory) {
        this.sourceCategories.add(sourceCategory);
        return this;
    }

    public ModelTransferBuilder sourceCategory(long id, boolean unpublish, boolean leaf) {
        SourceCategory sourceCategory = new SourceCategory();
        sourceCategory.setId(id);
        sourceCategory.setUnpublish(unpublish);
        sourceCategory.setLeaf(leaf);
        this.sourceCategories.add(sourceCategory);
        return this;
    }

    public ModelTransferBuilder sourceCategory(long id, boolean unpublish) {
        return sourceCategory(id, unpublish, true);
    }

    public ModelTransferBuilder sourceCategory(long id) {
        return sourceCategory(id, false);
    }

    public ModelTransferBuilder destinationCategory(DestinationCategory destinationCategory) {
        this.destinationCategories.add(destinationCategory);
        return this;
    }

    public ModelTransferBuilder destinationCategory(long id, boolean newCategory) {
        DestinationCategory destinationCategory = new DestinationCategory();
        destinationCategory.setId(id);
        destinationCategory.setNewCategory(newCategory);
        this.destinationCategories.add(destinationCategory);
        return this;
    }

    public ModelTransferBuilder destinationCategory(long id) {
        return destinationCategory(id, false);
    }

    public ModelTransfer build() {
        ModelTransfer transfer = new ModelTransfer();
        transfer.setId(id);
        transfer.setTransferType(transferType);
        transfer.setCreated(created);
        transfer.setAuthor(author);
        transfer.setTransferDate(transferDate);
        transfer.setManager(manager);
        transfer.setStatus(status);
        transfer.setStatusChanged(statusChanged);
        transfer.setModified(modified);
        transfer.setUserModified(userModified);
        transfer.setDescription(description);
        Stream.concat(sourceCategories.stream(), destinationCategories.stream()).forEach(mtc -> {
            mtc.setTransferId(id);
            mtc.setModified(modified);
            mtc.setUserModified(userModified);
        });
        transfer.setSourceCategories(sourceCategories);
        transfer.setDestinationCategories(destinationCategories);
        return transfer;
    }
}
