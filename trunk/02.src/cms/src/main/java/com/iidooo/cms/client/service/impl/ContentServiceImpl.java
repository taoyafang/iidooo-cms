package com.iidooo.cms.client.service.impl;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iidooo.cms.client.service.ContentService;
import com.iidooo.cms.dao.extend.CmsContentDao;
import com.iidooo.cms.dao.extend.CmsContentTagDao;
import com.iidooo.cms.dto.extend.CmsContentDto;
import com.iidooo.cms.dto.extend.CmsContentTagDto;

@Service
public class ContentServiceImpl implements ContentService {
    private static final Logger logger = Logger.getLogger(ContentServiceImpl.class);

    @Autowired
    private CmsContentDao cmsContentDao;

    @Autowired
    private CmsContentTagDao cmsTagDao;

    public CmsContentDto getContentByID(int contentID) {
        try {
            CmsContentDto cmsContentDto = cmsContentDao.selectContentByID(contentID);

            List<CmsContentTagDto> cmsTagDtos = cmsTagDao.selectTagsByContentID(contentID);
            cmsContentDto.setTags(cmsTagDtos);
            
            return cmsContentDto;
        } catch (Exception e) {
            e.printStackTrace();
            logger.fatal(e.getMessage());
            return null;
        }
    }

}