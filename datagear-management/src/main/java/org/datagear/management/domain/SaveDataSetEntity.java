package org.datagear.management.domain;

import lombok.Data;

import java.util.Date;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author gwl
 * @date 2020/10/28 11:52
 * @since 1.0.0
 */
@Data
public class SaveDataSetEntity {

    private String dsId;
    private String dsName;
    private String dsCreateUserId;
    private Date dsCreateTime;
    private String dsType;
    private String dsApId;
    private String daDatagearUserId;

}
