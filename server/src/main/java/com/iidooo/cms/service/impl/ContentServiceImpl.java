package com.iidooo.cms.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iidooo.cms.enums.ContentType;
import com.iidooo.cms.mapper.CmsCommentMapper;
import com.iidooo.cms.mapper.CmsContentMapper;
import com.iidooo.cms.mapper.CmsContentNewsMapper;
import com.iidooo.cms.mapper.CmsPictureMapper;
import com.iidooo.cms.model.po.CmsContent;
import com.iidooo.cms.model.po.CmsContentNews;
import com.iidooo.cms.model.po.CmsPicture;
import com.iidooo.cms.service.ContentService;
import com.iidooo.core.model.Page;
import com.iidooo.core.util.DateUtil;

@Service
public class ContentServiceImpl implements ContentService {

    private static final Logger logger = Logger.getLogger(ContentServiceImpl.class);

    @Autowired
    private CmsContentMapper cmsContentDao;

    @Autowired
    private CmsContentNewsMapper cmsContentNewsDao;

    @Autowired
    private CmsPictureMapper cmsPictureDao;

    @Autowired
    private CmsCommentMapper cmsCommentMapper;

    @Override
    public CmsContent getContent(Integer contentID) {
        try {
            CmsContent result = null;

            result = cmsContentNewsDao.selectByContentID(contentID);
            if (result == null) {
                result = cmsContentDao.selectByContentID(contentID);
            }

            return result;
        } catch (Exception e) {
            logger.fatal(e);
            throw e;
        }
    }

    @Override
    public List<CmsContent> getContentListByType(String channelPath, CmsContent cmsContent, Page page) {
        try {
            List<CmsContent> result = new ArrayList<CmsContent>();

            String contentType = cmsContent.getContentType();
            cmsContent.setStartShowDate(DateUtil.getNow(DateUtil.DATE_HYPHEN));
            cmsContent.setStartShowTime(DateUtil.getNow(DateUtil.TIME_COLON));
            cmsContent.setEndShowDate(DateUtil.getNow(DateUtil.DATE_HYPHEN));
            cmsContent.setEndShowTime(DateUtil.getNow(DateUtil.TIME_COLON));
            if (contentType.equals(ContentType.News.getCode())) {
                if (channelPath.equals("ToxicWave")) {
                    result = cmsContentNewsDao.selectContentListForToxicWaveTab1(cmsContent, page);
                } else {
                    result = cmsContentNewsDao.selectContentNewsList(channelPath, cmsContent, page);
                }
            } else {
                if (channelPath.equals("ToxicWave")) {
                    result = cmsContentDao.selectContentListForToxicWaveTab2(cmsContent, page);
                } else {
                    result = cmsContentDao.selectContentListByChannelPath(channelPath, cmsContent, page);
                }
            }

            return result;
        } catch (Exception e) {
            logger.fatal(e);
            throw e;
        }
    }

    @Override
    public int getContentListCount(CmsContent cmsContent, String startDate, String endDate) {
        try {
            int count = cmsContentDao.selectCountForSearch(cmsContent, startDate, endDate);
            return count;
        } catch (Exception e) {
            logger.fatal(e);
            throw e;
        }
    }

    @Override
    public List<CmsContent> getContentList(CmsContent cmsContent, String startDate, String endDate, Page page) {
        try {
            List<CmsContent> result = cmsContentDao.selectForSearch(cmsContent, startDate, endDate, page);
            return result;
        } catch (Exception e) {
            logger.fatal(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean createContent(CmsContent content) throws Exception {
        try {
            if (cmsContentDao.insert(content) <= 0) {
                throw new Exception();
            }

            if (content.getContentType().equals(ContentType.News.getCode())) {
                CmsContentNews cmsContentNews = (CmsContentNews) content;
                if (cmsContentNewsDao.insert(cmsContentNews) <= 0) {
                    throw new Exception();
                }
            }

            for (CmsPicture picture : content.getPictureList()) {
                picture.setCreateTime(new Date());
                picture.setCreateUserID(content.getCreateUserID());
                picture.setUpdateTime(new Date());
                picture.setUpdateUserID(content.getCreateUserID());
                picture.setContentID(content.getContentID());
                if (cmsPictureDao.insert(picture) <= 0) {
                    throw new Exception();
                }
            }

            return true;
        } catch (Exception e) {
            logger.fatal(e);
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean updateContent(CmsContent content) throws Exception{
        try {
            if (cmsContentDao.updateByContentID(content) <= 0) {
                throw new Exception();
            }

            if (content.getContentType().equals(ContentType.News.getCode())) {
                CmsContentNews cmsContentNews = (CmsContentNews) content;
                if (cmsContentNewsDao.updateByContentID(cmsContentNews) <= 0) {
                    throw new Exception();
                }
            }

            // 为了处理直观，先全部删除再新建
            cmsPictureDao.deleteByContentID(content.getContentID());
            for (CmsPicture picture : content.getPictureList()) {
                picture.setCreateTime(new Date());
                picture.setCreateUserID(content.getCreateUserID());
                picture.setUpdateTime(new Date());
                picture.setUpdateUserID(content.getCreateUserID());
                picture.setContentID(content.getContentID());
                if (cmsPictureDao.insert(picture) <= 0) {
                    throw new Exception();
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.fatal(e);
            throw e;
        }
    }
    
    @Override
    public int getUserContentCount(Integer userID) {
        try {
            int result = cmsContentDao.selectCountByUserID(userID);
            return result;
        } catch (Exception e) {
            logger.fatal(e);
            throw e;
        }
    }

    @Override
    public int getPVCountSum() {
        try {
            int result = cmsContentDao.selectPVCountSum();
            return result;
        } catch (Exception e) {
            logger.fatal(e);
            throw e;
        }
    }

    @Override
    public void updateViewCount(Integer contentID, int pvCount, int uvCount) {
        try {
            cmsContentDao.updateViewCount(contentID, pvCount, uvCount);
        } catch (Exception e) {
            logger.fatal(e);
        }
    }

    @Override
    public void updateCommentCount(Integer contentID) {
        try {
            int commentCount = cmsCommentMapper.selectCommentCount(contentID);
            cmsContentDao.updateCommentCount(contentID, commentCount);
        } catch (Exception e) {
            logger.fatal(e);
        }

    }

    @Override
    public int getContentStarCount(Integer contentID) {
        try {
            return cmsContentDao.selectStarCount(contentID);

        } catch (Exception e) {
            logger.fatal(e);
            return 0;
        }
    }

}
