package org.datagear.management.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author gwl
 * @date 2020/10/20 14:44
 * @since 1.0.0
 */
@Data
public class UserEntity {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String username;
    private String password;
    private boolean enabled;
    private String nickName;
    private String roleName;
    private String email;
    private String avatar;
    private String phone;
    private Long groupId;
    private String pathName;
    private String groupName;
    private Set<String> roles;
    private Set<String> permissions;
    private Map<String, Object> permission;
    private List<Map<String, Object>> menus;
    private Long tenantId;
    private String gender;
    private Integer oaId;
    private String oaXh;
    private String oaDep1;
    private String oaDep3;
    private String oaPos1;
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private Date oaUpdateTime;
    private String jsDlzhDm;
    private String jsSwryDm;
    private String jsSwrysfDm;
    private String jsSwryxm;
    private String createdBy;
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private Date createdTime;
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private Date updatedTime;
    private String updatedBy;
    private String groupCode;
    private String parentGroupCode;
}
