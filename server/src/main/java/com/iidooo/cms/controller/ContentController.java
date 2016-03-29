package com.iidooo.cms.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.iidooo.cms.enums.ContentType;
import com.iidooo.cms.enums.TableName;
import com.iidooo.cms.model.po.CmsContent;
import com.iidooo.cms.model.po.CmsContentNews;
import com.iidooo.cms.service.ContentService;
import com.iidooo.cms.service.HisOperatorService;
import com.iidooo.core.enums.MessageLevel;
import com.iidooo.core.enums.MessageType;
import com.iidooo.core.enums.ResponseStatus;
import com.iidooo.core.enums.SortField;
import com.iidooo.core.enums.SortType;
import com.iidooo.core.model.Message;
import com.iidooo.core.model.Page;
import com.iidooo.core.model.ResponseResult;
import com.iidooo.core.util.StringUtil;

@Controller
public class ContentController {

    private static final Logger logger = Logger.getLogger(ContentController.class);

    @Autowired
    private ContentService contentService;
    
    @Autowired
    private HisOperatorService hisOperatorService;

    @ResponseBody
    @RequestMapping(value = "/getContent", method = RequestMethod.POST)
    public ResponseResult getContent(HttpServletRequest request, HttpServletResponse response) {
        ResponseResult result = new ResponseResult();
        try {
            // 获取和验证字段
            String contentID = request.getParameter("contentID");
            String contentType = request.getParameter("contentType");
            if (StringUtil.isBlank(contentID)) {
                Message message = new Message(MessageType.FieldRequired.getCode(), MessageLevel.WARN, "contentID");
                result.getMessages().add(message);
            }
            if (StringUtil.isBlank(contentType)) {
                Message message = new Message(MessageType.FieldRequired.getCode(), MessageLevel.WARN, "contentType");
                result.getMessages().add(message);
            }

            if (result.getMessages().size() > 0) {
                // 验证失败，返回message
                result.setStatus(ResponseStatus.Failed.getCode());
                return result;
            }

            // 查询获得内容对象
            CmsContent content = contentService.getContent(contentType, Integer.valueOf(contentID));
            if (content == null) {
                result.setStatus(ResponseStatus.QueryEmpty.getCode());
                return result;
            }
            
            // 返回找到的内容对象
            result.setStatus(ResponseStatus.OK.getCode());
            result.setData(content);
            
            // 更新浏览记录
            hisOperatorService.createHisOperator(TableName.CMS_CONTENT.toString(), content.getContentID(), request);
            
            // 更新该内容的PV和UV
            int pvCount = hisOperatorService.getPVCount(TableName.CMS_CONTENT.toString(), content.getContentID(), request);
            int uvCount = hisOperatorService.getUVCount(TableName.CMS_CONTENT.toString(), content.getContentID(), request);
            contentService.updateViewCount(content.getContentID(), pvCount, uvCount);
            content.setPageViewCount(pvCount);
            content.setUniqueVisitorCount(uvCount);
        } catch (Exception e) {
            logger.fatal(e);
            Message message = new Message(MessageType.Exception.getCode(), MessageLevel.FATAL, e.getMessage());
            message.setDescription(e.getMessage());
            result.getMessages().add(message);
        }
        return result;
    }

    @ResponseBody
    @RequestMapping(value = "/getContentList", method = RequestMethod.POST)
    public ResponseResult getContentList(HttpServletRequest request, HttpServletResponse response) {
        ResponseResult result = new ResponseResult();
        try {
            // 解析获得传入的参数
            // 必填参数
            String channelPath = request.getParameter("channelPath");
            String contentType = request.getParameter("contentType");
            if (StringUtil.isBlank(channelPath)) {
                Message message = new Message(MessageType.FieldRequired.getCode(), MessageLevel.WARN, "channelPath");
                result.getMessages().add(message);
            }
            if (StringUtil.isBlank(contentType)) {
                Message message = new Message(MessageType.FieldRequired.getCode(), MessageLevel.WARN, "contentType");
                result.getMessages().add(message);
            }
            if (result.getMessages().size() > 0) {
                // 验证失败，返回message
                result.setStatus(ResponseStatus.Failed.getCode());
                return result;
            }

            String sortField = request.getParameter("sortField");
            if (StringUtil.isBlank(sortField)) {
                sortField = SortField.UpdateTime.toString();
            }

            String sortType = request.getParameter("sortType");
            if (StringUtil.isBlank(sortType)) {
                sortType = SortType.desc.toString();
            }

            String start = request.getParameter("start");
            if (StringUtil.isBlank(start)) {
                start = "0";
            }

            String pageSize = request.getParameter("pageSize");
            if (StringUtil.isBlank(pageSize)) {
                pageSize = "10";
            }

            Page page = new Page();
            page.setSortField(sortField);
            page.setSortType(sortType);
            page.setStart(Integer.valueOf(start));
            page.setPageSize(Integer.valueOf(pageSize));

            CmsContent cmsContent = new CmsContent();
            cmsContent.setContentType(contentType);
            String createUserID = request.getParameter("createUserID");
            if (StringUtil.isNotBlank(createUserID)) {
                cmsContent.setCreateUserID(Integer.parseInt(createUserID));
            } else {
                cmsContent.setCreateUserID(null);
            }
            
            List<CmsContent> contentList = this.contentService.getContentListByType(channelPath, cmsContent, page);
            if (contentList.size() <= 0) {
                result.setStatus(ResponseStatus.QueryEmpty.getCode());
            } else {
                result.setStatus(ResponseStatus.OK.getCode());
                result.setData(contentList);
            }

        } catch (Exception e) {
            logger.fatal(e);
            Message message = new Message(MessageType.Exception.getCode(), MessageLevel.FATAL, e.getMessage());
            result.getMessages().add(message);
        }
        return result;
    }

    @RequestMapping(value = "/createContent", method = RequestMethod.POST)
    public @ResponseBody ResponseResult createContent(HttpServletRequest request, HttpServletResponse response) {
        ResponseResult result = new ResponseResult();
        try {
            // 解析获得传入的参数
            // 必填参数
            String channelID = request.getParameter("channelID");
            String contentType = request.getParameter("contentType");
            List<Message> messages = validateCreateContent(channelID, contentType);
            if (messages.size() > 0) {
                // 验证失败，返回message
                result.setStatus(ResponseStatus.Failed.getCode());
                result.setMessages(messages);
                return result;
            }

            if (contentType.equals(ContentType.Default.getCode())) {
                CmsContent content = new CmsContent();
                content = contentService.createContent(content);
                if (content == null) {
                    result.setStatus(ResponseStatus.InsertFailed.getCode());
                } else {
                    result.setStatus(ResponseStatus.OK.getCode());
                    result.setData(content);
                }
            } else if (contentType.equals(ContentType.News.getCode())) {
                CmsContentNews contentNews = new CmsContentNews();
                contentNews = contentService.createContentNews(contentNews);
                if (contentNews == null) {
                    result.setStatus(ResponseStatus.InsertFailed.getCode());
                } else {
                    result.setStatus(ResponseStatus.OK.getCode());
                    result.setData(contentNews);
                }
            }

        } catch (Exception e) {
            logger.fatal(e);
            Message message = new Message(MessageType.Exception.getCode(), MessageLevel.FATAL, e.getMessage());
            result.getMessages().add(message);
        }
        return result;
    }

    private List<Message> validateCreateContent(String channelID, String contentType) {
        List<Message> result = new ArrayList<Message>();
        try {
            if (StringUtil.isBlank(channelID)) {
                Message message = new Message(MessageType.FieldRequired.getCode(), MessageLevel.WARN, "channelID");
                result.add(message);
            }

            if (StringUtil.isBlank(contentType)) {
                Message message = new Message(MessageType.FieldRequired.getCode(), MessageLevel.WARN, "contentType");
                result.add(message);
            }

        } catch (Exception e) {
            logger.fatal(e);
            Message message = new Message(MessageType.Exception.getCode(), MessageLevel.FATAL);
            message.setDescription(e.getMessage());
            result.add(message);
        }
        return result;
    }
}