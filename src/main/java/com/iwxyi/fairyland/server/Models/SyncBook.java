package com.iwxyi.fairyland.server.Models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAlias;

import org.hibernate.validator.constraints.Length;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class SyncBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookIndex;
    private Long userId;
    @Length(min = 1, max = 10, message = "书名不能超过10个字")
    @Pattern(regexp = "^([\\u4e00-\\u9fa5\\w\\d][\\u4e00-\\u9fa5\\w\\d:：.“”\"]{0,9})$", message = "书名只能由中英文、数字、冒号组成，且不能为符号开头")
    @JsonAlias({ "name" })
    private String bookName;
    private String catalog; // 目录正文
    private int publishState; // 0不发布，1仅为好友可见，2全部公开
    private Date createTime; // 创建时间（云端为准）
    private Date uploadTime; // 上传时间（云端为准）
    private Date modifyTime; // 修改时间（客户端为准）
    private Date updateTime; // 更新时间：包括目录增删改、章节修改时间
    private boolean deleted;

    public boolean hasBookIndex() {
        return bookIndex != null && bookIndex > 0;
    }

    public void refreshUpdateTime(Date time) {
        if (updateTime == null || (time != null && time.getTime() > updateTime.getTime())) {
            setUpdateTime(time);
        }
    }
}
