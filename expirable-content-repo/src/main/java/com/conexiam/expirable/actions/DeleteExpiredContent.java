package com.conexiam.expirable.actions;

import com.conexiam.expirable.beans.ReportData;
import com.conexiam.expirable.beans.ReportWriter;
import com.conexiam.expirable.model.ExpirableContentModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DeleteExpiredContent extends ActionExecuterAbstractBase {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteExpiredContent.class);

    public static final String NAME = "delete-expired-content";

    private SearchService searchService;
    private NodeService nodeService;
    private ReportWriter reportWriter;

    @Override
    public void executeImpl(Action ruleAction, NodeRef actionedUponNodeRef) {
        List<ReportData> reportDataList = new ArrayList<ReportData>();
        ResultSet expiredDocs = getExpiredContent();
        for (int i = 0; i < expiredDocs.length(); i++) {
            NodeRef expiredDoc = expiredDocs.getNodeRef(i);

            ReportData reportData = new ReportData();
            reportData.setName((String) nodeService.getProperty(expiredDoc, ContentModel.PROP_NAME));
            reportData.setNodeRef(expiredDoc.toString());
            reportData.setExpirationDate((Date) nodeService.getProperty(expiredDoc, ExpirableContentModel.PROP_EXPIRATION_DATE));
            reportData.setPath(nodeService.getPath(expiredDoc).toString());

            nodeService.deleteNode(expiredDoc);

            reportDataList.add(reportData);
        }
        if (reportDataList.size() > 0) {
            LOG.info("Delete expired content action deleted: " + reportDataList.size() + " documents.");
            reportWriter.save(reportDataList);
        } else {
            LOG.info("Delete expired content action found nothing to delete.");
        }
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
    }

    public ResultSet getExpiredContent() {
        String query = "ASPECT:\"cxme:expirable\" AND cxme:expirationDate:[MIN to \"" + Instant.now().toString() + "\"]";
        return searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_FTS_ALFRESCO, query);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setReportWriter(ReportWriter reportWriter) {
        this.reportWriter = reportWriter;
    }
}