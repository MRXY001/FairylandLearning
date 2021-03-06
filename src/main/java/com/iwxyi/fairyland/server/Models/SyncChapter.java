package com.iwxyi.fairyland.server.Models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.validator.constraints.Length;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class SyncChapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chapterIndex;
    private Long bookIndex;
    private Long userId;
    private String chapterId; // 这个是作品内章节唯一ID，后台不使用这个
    @Length(min = 1, max = 20, message = "章节名不能超过20个字")
    private String title; // 标题名字
    private String content; // 正文内容
    private int chapterType; // 章节种类：0章节（默认）
    private Date createTime;
    private Date uploadTime;
    private Date modifyTime;
    private int publishState;
    private boolean bookDeleted;
    private boolean deleted;

    public SyncChapter(Long userId, Long bookIndex, String chapterId, String title, String content) {
        this.userId = userId;
        this.bookIndex = bookIndex;
        this.chapterId = chapterId;
        this.title = title;
        this.content = content;
    }
}
