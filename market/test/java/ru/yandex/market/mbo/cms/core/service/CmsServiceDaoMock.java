package ru.yandex.market.mbo.cms.core.service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.stream.JsonWriter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.market.mbo.cms.core.dao.CmsPageDao;
import ru.yandex.market.mbo.cms.core.dao.CmsServiceDao;
import ru.yandex.market.mbo.cms.core.dao.CmsServiceDaoInterface;
import ru.yandex.market.mbo.cms.core.dao.model.Node;
import ru.yandex.market.mbo.cms.core.dao.model.NodeMetadata;
import ru.yandex.market.mbo.cms.core.dao.model.Value;
import ru.yandex.market.mbo.cms.core.dto.CmsSchemaDto;
import ru.yandex.market.mbo.cms.core.models.CmsPageKey;
import ru.yandex.market.mbo.cms.core.models.CmsPagesFilter;
import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.NodeBlock;
import ru.yandex.market.mbo.cms.core.models.Page;
import ru.yandex.market.mbo.cms.core.models.Revision;
import ru.yandex.market.mbo.cms.core.models.SuggestResult;
import ru.yandex.market.mbo.cms.core.models.Zoom;

/**
 * @author ayratgdl
 * @date 01.03.18
 */
public class CmsServiceDaoMock implements CmsServiceDaoInterface {

    public class CmsPageDaoMock extends CmsPageDao {
        public CmsPageDaoMock(
            NamedParameterJdbcTemplate pgTemplate,
            NamedParameterJdbcTemplate pgTemplateReadonly,
            SchemaService schemaService
        ) {
            super(pgTemplate, pgTemplateReadonly, schemaService);
        }

        public List<Page> loadPages(CmsPagesFilter filter, CmsPagesFilter.Field sortField,
                                    boolean sortAsc) {
            return ntPromoDaoMock.loadPages(filter, sortField, sortAsc);
        }

        public Integer loadPagesCount(CmsPagesFilter filter) {
            throw new UnsupportedOperationException();
        }

        public List<Page> getPagesWithoutWidgets(List<Long> ids, boolean skipUnpublished, boolean skipDeleted) {
            List<Page> result = new ArrayList<>();
            for (Page page : ntPromoDaoMock.rows) {
                if (skipUnpublished && page.getPublishedRevisionId() == 0) {
                    continue;
                }
                if (skipDeleted && page.isDeleted()) {
                    continue;
                }
                if (ids.contains(page.getId())) {
                    Page pageInfo = new Page();
                    pageInfo.setId(page.getId());
                    pageInfo.setName(page.getName());
                    pageInfo.setDocumentDescription(new DocumentDescription(
                                    page.getNamespace(),
                                    page.getType()),
                            page.getSchemaId(),
                            page.getSchemaRevisionId()
                    );
                    pageInfo.setCreatorId(page.getCreatorId());
                    pageInfo.setUpdaterId(page.getUpdaterId());
                    pageInfo.setPublisherId(page.getPublisherId());
                    pageInfo.setCreated(page.getCreated());
                    pageInfo.setUpdated(page.getUpdated());
                    pageInfo.setPublished(page.getPublished());
                    pageInfo.setUnpublished(page.getUnpublished());
                    pageInfo.setForSearch(page.getForSearch());
                    pageInfo.setArchived(page.isArchived());
                    pageInfo.setLatestRevisionId(page.getLatestRevisionId());
                    pageInfo.setPublishedRevisionId(page.getPublishedRevisionId());
                    pageInfo.setPropertiesObject(page.getPropertiesObject());
                    pageInfo.setPropertiesPublishedObject(page.getPropertiesPublishedObject());
                    pageInfo.setExportToDzen(page.isExportToDzen());
                    result.add(pageInfo);
                }
            }
            return result;
        }

        public List<Page> getAllPagesWithoutWidgets(boolean draft) {
            throw new UnsupportedOperationException();
        }

        public boolean isPageExist(long pageId) {
            for (Page page : ntPromoDaoMock.rows) {
                if (page.getId() == pageId) {
                    return true;
                }
            }
            return false;
        }

    }

    private final NtPromoDaoMock ntPromoDaoMock = new NtPromoDaoMock();
    private final IdGenerator idGenerator = new IdGeneratorMock();
    private final CmsPageDaoMock pageDao = new CmsPageDaoMock(null, null, null);

    public CmsPageDaoMock getPageDao() {
        return pageDao;
    }

    @Override
    public Zoom loadView(long viewId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Zoom> loadAllViews() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeViews(Collection<Long> ids, boolean draft) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeViewsTemp(long pageId, boolean draft, String namespace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeViews(long pageId, boolean draft) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Long> getCompiledViewIds(long pageId, long schemaId, boolean draft) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer loadPageRevisionsCount(long documentId, boolean publishedOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Revision> loadPageRevisions(long documentId, int limit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Revision> loadPageRevisions(long documentId, int limit, int offset, boolean publishedOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Revision loadPageRevision(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LinkedHashSet<Long> loadUpdatersAfterPublish(long pageId, long publishedRevisionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, Map<Long, CmsSchemaDto>> loadRevisionSchemas(Set<Long> revisionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, Map<String, String>> getPluginProperties(long pageId, long revisionId,
                                                              Set<Integer> nodeSchemaIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unpublishCmsPage(long pageId, long userId, String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDocumentRevisionMessage(long revisionId, String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updatePagePublishedStatus(Page page,
                                          Date revisionPublishDate,
                                          Map<String, Set<String>> exportedParams) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> loadPageModels(long pageId, long revisionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> loadAllLabelsForSuggestTag() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, NodeBlock> loadAllParametersForPromoPage(long pageId, long revisionId,
                                                              Set<Integer> nodeSchemaIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long retrieveViewId(long pageId, long schemaId, String name,
                               Constants.Device device, Constants.Format format) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCmsPage(long pageId, long userId, String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCmsWidgetsAtContainer(long containerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SuggestResult> loadSuggestDataForBrand(String request, int limit, boolean findInText) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdGenerator getRevisionIdProvider() {
        return idGenerator;
    }

    @Override
    public IdGenerator getPromoWidgetIdProvider() {
        return idGenerator;
    }

    @Override
    public TransactionTemplate getTransactionTemplate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveCmsPageKeys(boolean draft, long schemaId, long schemaRevisionId,
                                List<CmsPageKey> keys, String similarDomain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCmsPageKeys(boolean draft, long pageId, long schemaId, CmsPageKey.KeyType keyType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCmsPageKeysTemp(boolean draft, long pageId, String namespace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCmsPageKeys(boolean draft, String similarDomain, Set<Long> schemaIds) {
    }

    @Override
    public Set<String> getCmsPageKeys(long pageId, long schemaId, boolean draft) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getCmsPageKeys(long pageId, boolean draft) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Set<Long>> getKeyPages(
            Collection<String> keys, long schemaId, boolean draft
    ) {
        return Collections.emptyMap();
    }

    @Override
    public void savePromoWidgets(List<Node> nodes, List<Value> values, List<NodeMetadata> metadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createPageRevision(Revision revision, long pageId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createRevisionSchemas(long revisionId, Map<Long, Long> schemasWithRevision) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteEditorConf(long pageId) {
        // nothing
    }

    public CmsServiceDaoMock addNtPromoRow(Page row) {
        ntPromoDaoMock.rows.add(row);
        return this;
    }

    private static class NtPromoDaoMock {
        List<Page> rows = new ArrayList<>();

        public List<Page> loadPages(CmsPagesFilter filter, CmsPagesFilter.Field sortField,
                                    boolean sortAsc) {
            List<Page> result = new ArrayList<>();
            for (Page row : rows) {
                if (test(filter, row)) {
                    result.add(row);
                }
            }

            return result;
        }

        public void savePage(Page page) {
            rows.removeIf(row -> row.getId() == page.getId());
            rows.add(page);
        }

        private static boolean test(CmsPagesFilter filter, Page row) {
            boolean result = true;
            if (!filter.getDocTypes().isEmpty() && !filter.getDocTypes().contains(row.getType())) {
                result = false;
            }
            if (!filter.getPageIds().isEmpty() && !filter.getPageIds().contains(row.getId())) {
                result = false;
            }
            return result;
        }
    }

    public static class PagePropertiesBuilder {
        private Map<String, List<String>> properties;

        public PagePropertiesBuilder addProperty(String key, String value) {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            return this;
        }

        public String build() {
            try {
                if (properties == null) {
                    return null;
                }

                StringWriter strWriter = new StringWriter();
                JsonWriter writer = new JsonWriter(strWriter);
                writer.beginObject();

                writer.name("exportParams").beginObject();
                for (String key : properties.keySet()) {
                    writer.name(key).beginArray();
                    for (String value : properties.get(key)) {
                        writer.value(value);
                    }

                    writer.endArray();
                }
                writer.endObject();

                writer.endObject();

                return strWriter.toString();

            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static class IdGeneratorMock implements IdGenerator {
        private long nextId = 1;

        @Override
        public long getId() {
            return nextId++;
        }
    }

    @Override
    public Set<Long> getAuthorsAndPublishers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertViews(Collection<CmsServiceDao.ViewUpdateInfo> views, boolean draft) {

    }

    @Override
    public void updateViews(Collection<CmsServiceDao.ViewUpdateInfo> views, boolean draft) {

    }

    @Override
    public String getDocTypeOfPage(long pageId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLatestRevisionForContainer(long containerId, Set<Long> schemaIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getPreviousRevisionId(long documentId, long revisionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getPreviousPublishedRevisionId(long documentId, long revisionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Long> getRevisionCompatibleSchemas(long revisionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void findAllWidgetsForContainerWithCallback(
            long containerId, long revisionId, Set<Integer> nodeSchemaIds, RowCallbackHandler consumer
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> findPublishedDocsWithNodes(Collection<String> nodeTypes, String namespace) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPageNamespace(Long pageId) {
        throw new UnsupportedOperationException();
    }
}
